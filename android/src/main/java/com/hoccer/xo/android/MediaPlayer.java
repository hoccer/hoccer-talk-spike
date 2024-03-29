package com.hoccer.xo.android;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.activity.NotificationBridgeActivity;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.MediaPlaylist;
import com.hoccer.xo.android.content.audio.MediaPlaylistController;
import com.hoccer.xo.android.service.NotificationId;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayer implements android.media.MediaPlayer.OnErrorListener, android.media.MediaPlayer.OnCompletionListener, MediaPlaylistController.Listener, BackgroundManager.Listener {

    private static final Logger LOG = Logger.getLogger(MediaPlayer.class);

    public static final int UNDEFINED_CONTACT_ID = -1;

    private static MediaPlayer sInstance;

    private final Context mContext;
    private final NotificationManager mNotificationManager;

    private final List<Listener> mListener = new ArrayList<Listener>();

    private static final String UPDATE_PLAYSTATE_ACTION = "com.hoccer.xo.android.content.audio.UPDATE_PLAYSTATE_ACTION";

    private final AudioManager mAudioManager;
    private final BackgroundManager mBackgroundManager;
    private android.media.MediaPlayer mMediaPlayer;

    private boolean mPaused;
    private boolean mStopped = true;

    private XoTransfer mCurrentItem;
    private final RemoteViews mRemoteViews;
    private final MediaPlaylistController mPlaylistController = new MediaPlaylistController();

    public interface Listener {
        void onStateChanged();

        void onTrackChanged();
    }

    private final BroadcastReceiver mHeadsetActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                int headSetState = intent.getIntExtra("state", 0);
                if (headSetState == 0) {
                    if (mMediaPlayer != null && !isStopped()) {
                        pause();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mPlaystateActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isPaused()) {
                play(mCurrentItem);
            } else {
                pause();
            }
        }
    };

    public static MediaPlayer get() {
        if (sInstance == null) {
            sInstance = new MediaPlayer();
        }
        return sInstance;
    }

    private MediaPlayer() {
        mContext = XoApplication.get();
        mBackgroundManager = BackgroundManager.get();
        mPlaylistController.registerListener(this);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mRemoteViews = createRemoteViews();

        registerHeadsetHandlerReceiver();
        registerUpdatePlayStateReceiver();

        mBackgroundManager.registerListener(this);
    }

    @Override
    public void onCurrentItemChanged(XoTransfer newItem) {
        if (newItem != null) {
            playNewTrack(mPlaylistController.getCurrentItem());
        } else {
            reset();
        }
    }

    @Override
    public void onPlaylistChanged(MediaPlaylist newPlaylist) {
        notifyTrackChanged();
    }

    @Override
    public void onRepeatModeChanged(MediaPlaylistController.RepeatMode newMode) {}

    @Override
    public void onShuffleChanged(boolean isShuffled) {}

    private RemoteViews createRemoteViews() {
        Intent updatePlayStateIntent = new Intent(UPDATE_PLAYSTATE_ACTION);
        PendingIntent updatePlayStatePendingIntent = PendingIntent.getBroadcast(mContext, 0, updatePlayStateIntent, 0);

        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.view_audioplayer_notification);
        views.setOnClickPendingIntent(R.id.btn_play_pause, updatePlayStatePendingIntent);
        return views;
    }

    private void registerHeadsetHandlerReceiver() {
        IntentFilter receiverFilter = new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mContext.registerReceiver(mHeadsetActionReceiver, receiverFilter);
    }

    private void registerUpdatePlayStateReceiver() {
        IntentFilter updatePlayStateIntent = new IntentFilter(UPDATE_PLAYSTATE_ACTION);
        mContext.registerReceiver(mPlaystateActionReceiver, updatePlayStateIntent);
    }

    private void createMediaPlayer() {
        mMediaPlayer = new android.media.MediaPlayer();
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
    }

    private final OnAudioFocusChangeListener mAudioFocusChangeListener = new OnAudioFocusChangeListener() {

        public void onAudioFocusChange(int focusChange) {
            LOG.debug("AUDIO FOCUS CHANGED: " + focusChange);

            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                LOG.debug("AUDIOFOCUS_LOSS_TRANSIENT");
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                LOG.debug("AUDIOFOCUS_GAIN");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                LOG.debug("AUDIOFOCUS_LOSS");
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                LOG.debug("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
            }
        }
    };

    private void updateRemoteViewButton() {
        if (!mPaused) {
            mRemoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_dark_content_pause);
        } else {
            mRemoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_dark_content_play);
        }
    }

    private void updateRemoteViewMetaData() {
        MediaMetaData metaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(mCurrentItem.getFilePath()).getPath());
        String title = metaData.getTitle();
        String artist = metaData.getArtist();
        if (title.isEmpty() && artist.isEmpty()) {
            mRemoteViews.setViewVisibility(R.id.ll_media_metadata, View.GONE);
            mRemoteViews.setViewVisibility(R.id.tv_filename, View.VISIBLE);
            mRemoteViews.setTextViewText(R.id.tv_filename, mCurrentItem.getFilename());
        } else {
            mRemoteViews.setViewVisibility(R.id.ll_media_metadata, View.VISIBLE);
            mRemoteViews.setViewVisibility(R.id.tv_filename, View.GONE);
            mRemoteViews.setTextViewText(R.id.tv_metadata_title, title);
            mRemoteViews.setTextViewText(R.id.tv_metadata_artist, artist);
        }
    }

    private void resetAndPrepareMediaPlayer(XoTransfer item) {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(UriUtils.getAbsoluteFileUri(item.getFilePath()).getPath());
            mMediaPlayer.prepare();
        } catch (Exception e) {
            LOG.error("setFile: exception setting data source", e);
        }
    }

    public int getMediaListSize() {
        return mPlaylistController.size();
    }

    public void playItemInPlaylist(XoTransfer item, MediaPlaylist playlist) {
        boolean isCurrentMediaItem = item.equals(mCurrentItem);
        boolean isCurrentlyPlaying = isCurrentMediaItem && !mPaused && !mStopped;
        boolean isCurrentlyPaused = isCurrentMediaItem && mPaused;

        setPlaylist(playlist);
        setCurrentIndex(playlist.indexOf(item));

        if (isCurrentlyPlaying) {
            // do nothing
        } else if (isCurrentlyPaused) {
            play();
        } else {
            playNewTrack(mPlaylistController.getCurrentItem());
        }
    }

    public void play() {
        if (mStopped) {
            mPlaylistController.setCurrentIndex(0);
        }
        play(mPlaylistController.getCurrentItem());
    }

    private void play(final XoTransfer item) {
        if (mMediaPlayer == null) {
            createMediaPlayerAndPlay(item);
        } else {
            int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mMediaPlayer.start();
                mPaused = false;
                mStopped = false;
                if (item != mCurrentItem) {
                    mCurrentItem = item;
                    notifyTrackChanged();
                }

                if (mBackgroundManager.isInBackground()) {
                    mNotificationManager.notify(NotificationId.MUSIC_PLAYER, buildNotification());
                }

                notifyPlayStateChanged();
            } else {
                LOG.debug("Audio focus request not granted");
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(mContext, NotificationBridgeActivity.class);
        intent.setAction(NotificationBridgeActivity.ACTION_FULLSCREEN_PLAYER_ACTIVITY_TO_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, NotificationId.MUSIC_PLAYER, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        updateRemoteViewButton();
        updateRemoteViewMetaData();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification_music)
                .setContent(mRemoteViews)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
        return mBuilder.build();
    }

    private void createMediaPlayerAndPlay(final XoTransfer item) {
        createMediaPlayer();
        mMediaPlayer.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(android.media.MediaPlayer mp) {
                play(item);
            }
        });
        resetAndPrepareMediaPlayer(item);
    }

    private void playNewTrack(XoTransfer item) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        play(item);
    }

    public void forward() {
        if (mPlaylistController.canForward()) {
            playNewTrack(mPlaylistController.forward());
        }
    }

    public void backward() {
        if (mPlaylistController.canBackward()) {
            playNewTrack(mPlaylistController.backward());
        }
    }

    public void pause() {
        mMediaPlayer.pause();

        mPaused = true;
        mStopped = false;

        if (mBackgroundManager.isInBackground()) {
            mNotificationManager.notify(NotificationId.MUSIC_PLAYER, buildNotification());
        }

        notifyPlayStateChanged();
    }

    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        mPaused = false;
        mStopped = true;
        mCurrentItem = null;
        mPlaylistController.reset();

        removeNotification();

        notifyTrackChanged();
        notifyPlayStateChanged();
    }

    public void setSeekPosition(final int position) {
        if (mMediaPlayer == null) {
            createMediaPlayer();
            mMediaPlayer.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(android.media.MediaPlayer mp) {
                    if (isStopped()) {
                        mStopped = false;
                        mPaused = true;
                    }
                    mMediaPlayer.seekTo(position);
                }
            });
            resetAndPrepareMediaPlayer(mCurrentItem);
        } else {
            mMediaPlayer.seekTo(position);
        }
    }

    @Override
    public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
        LOG.debug("onError(" + what + "," + extra + ")");
        return false;
    }

    @Override
    public void onCompletion(android.media.MediaPlayer mp) {
        if (mPlaylistController.canForward()) {
            forward();
        } else {
            reset();
        }
    }

    public boolean isPaused() {
        return mPaused;
    }

    public boolean isStopped() {
        return mStopped;
    }

    public int getTotalDuration() {
        return (mStopped) ? 0 : mMediaPlayer.getDuration();
    }

    public int getCurrentIndex() {
        return mPlaylistController.getCurrentIndex();
    }

    public int getCurrentProgress() {
        int result = 0;
        if (mMediaPlayer != null) {
            result = mMediaPlayer.getCurrentPosition();
        }
        return result;
    }

    public XoTransfer getCurrentMediaItem() {
        return mCurrentItem;
    }

    public int getCurrentConversationContactId() {
        int conversationContactId = UNDEFINED_CONTACT_ID;

        try {
            TalkClientMessage message;
            XoClientDatabase database = XoApplication.get().getClient().getDatabase();
            if (mCurrentItem instanceof TalkClientDownload) {
                int attachmentId = ((TalkClientDownload) mCurrentItem).getClientDownloadId();
                message = database.findClientMessageByTalkClientDownloadId(attachmentId);
                if (message != null) {
                    conversationContactId = message.getSenderContact().getClientContactId();
                }
            } else if (mCurrentItem instanceof TalkClientUpload) {
                int attachmentId = ((TalkClientUpload) mCurrentItem).getClientUploadId();
                message = database.findClientMessageByTalkClientUploadId(attachmentId);
                if (message != null) {
                    conversationContactId = message.getSenderContact().getClientContactId();
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }

        return conversationContactId;
    }

    private void setPlaylist(MediaPlaylist playlist) {
        mPlaylistController.setPlaylist(playlist);
    }

    private void setCurrentIndex(int pos) {
        mPlaylistController.setCurrentIndex(pos);
    }

    public MediaPlaylistController.RepeatMode getRepeatMode() {
        return mPlaylistController.getRepeatMode();
    }

    public void setRepeatMode(MediaPlaylistController.RepeatMode mode) {
        mPlaylistController.setRepeatMode(mode);
    }

    public boolean isShuffleActive() {
        return mPlaylistController.getShuffleActive();
    }

    public void setShuffleActive(boolean isActive) {
        mPlaylistController.setShuffleActive(isActive);
    }

    @Override
    public void onBecameForeground(Activity activity) {
        removeNotification();
    }

    public void removeNotification() {
        mNotificationManager.cancel(NotificationId.MUSIC_PLAYER);
    }


    @Override
    public void onBecameBackground(Activity activity) {
        if (!mPaused && !mStopped) {
            mNotificationManager.notify(NotificationId.MUSIC_PLAYER, buildNotification());
        }
    }

    public void registerListener(Listener listener) {
        mListener.add(listener);
    }

    public void unregisterListener(Listener listener) {
        mListener.remove(listener);
    }

    private void notifyPlayStateChanged() {
        for (Listener listener : mListener) {
            listener.onStateChanged();
        }
    }

    private void notifyTrackChanged() {
        for (Listener listener : mListener) {
            listener.onTrackChanged();
        }
    }
}
