package com.hoccer.xo.android.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.*;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.FullscreenPlayerActivity;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.audio.MediaPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.view.ArtworkImageView;
import com.hoccer.bofrostmessenger.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class FullscreenPlayerFragment extends Fragment {

    static final Logger LOG = Logger.getLogger(FullscreenPlayerFragment.class);

    private ToggleButton mPlayButton;
    private ImageButton mSkipForwardButton;
    private ImageButton mSkipBackButton;
    private ImageButton mRepeatButton;
    private ToggleButton mShuffleButton;
    private SeekBar mTrackProgressBar;
    private TextView mTrackTitleLabel;
    private TextView mTrackArtistLabel;
    private TextView mCurrentTimeLabel;
    private TextView mTotalDurationLabel;
    private TextView mPlaylistIndexLabel;
    private TextView mPlaylistSizeLabel;
    private TextView mConversationNameLabel;
    private ArtworkImageView mArtworkImageView;
    private FrameLayout mArtworkContainer;

    private Handler mTimeProgressHandler = new Handler();
    private Runnable mUpdateTimeTask;
    private ValueAnimator mBlinkAnimation;
    private MediaPlayerService mMediaPlayerService;
    private LoadArtworkTask mCurrentLoadArtworkTask;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBlinkAnimation();

        createBroadcastReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_fullscreen_player, container);

        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSkipForwardButton = (ImageButton) getView().findViewById(R.id.bt_player_skip_forward);
        mSkipBackButton = (ImageButton) getView().findViewById(R.id.bt_player_skip_back);
        mRepeatButton = (ImageButton) getView().findViewById(R.id.bt_player_repeat);
        mShuffleButton = (ToggleButton) getView().findViewById(R.id.bt_player_shuffle);
        mTrackProgressBar = (SeekBar) getView().findViewById(R.id.pb_player_seek_bar);
        mTrackTitleLabel = (TextView) getView().findViewById(R.id.tv_player_track_title);
        mTrackArtistLabel = (TextView) getView().findViewById(R.id.tv_player_track_artist);
        mCurrentTimeLabel = (TextView) getView().findViewById(R.id.tv_player_current_time);
        mTotalDurationLabel = (TextView) getView().findViewById(R.id.tv_player_total_duration);
        mPlaylistIndexLabel = (TextView) getView().findViewById(R.id.tv_player_current_track_no);
        mPlaylistSizeLabel = (TextView) getView().findViewById(R.id.tv_player_playlist_size);
        mConversationNameLabel = (TextView) getView().findViewById(R.id.tv_conversation_name);
        mArtworkImageView = (ArtworkImageView) getView().findViewById(R.id.iv_player_artwork);
        mArtworkContainer = (FrameLayout) getView().findViewById(R.id.fl_player_artwork);
        mTrackProgressBar.setProgress(0);
        mTrackProgressBar.setMax(100);
        mPlayButton = (ToggleButton) view.findViewById(R.id.bt_player_play);

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }else{
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                adjustViewSizes();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mMediaPlayerService != null) {
            updateTrackData();
            updatePlayState();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mTimeProgressHandler.removeCallbacks(mUpdateTimeTask);
        mUpdateTimeTask = null;
    }

    public void initView() {
        mMediaPlayerService = ((FullscreenPlayerActivity) getActivity()).getMediaPlayerService();
        enableViewComponents(true);
        updateTrackData();
        updateConversationName();
        mPlayButton.setChecked(!mMediaPlayerService.isPaused());
        mShuffleButton.setChecked(mMediaPlayerService.isShuffleActive());

        setupViewListeners(); // must be last to call!
    }

    public void updatePlayState() {
        if (mMediaPlayerService != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ((mMediaPlayerService.isPaused()) || mMediaPlayerService.isStopped()) {
                        mPlayButton.setChecked(false);
                        mBlinkAnimation.start();
                    } else if (!mMediaPlayerService.isPaused() && !mMediaPlayerService.isStopped()) {
                        if (mBlinkAnimation.isRunning()) {
                            mBlinkAnimation.cancel();
                        }

                        mCurrentTimeLabel.setTextColor(getResources().getColor(R.color.xo_media_player_secondary_text));
                        mPlayButton.setChecked(true);
                    }
                }
            });
        }
    }

    private void updateConversationName() {
        int conversationContactId = mMediaPlayerService.getCurrentConversationContactId();
        TalkClientContact talkClientContact = null;
        try {
            talkClientContact = XoApplication.getXoClient().getDatabase().findClientContactById(conversationContactId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (talkClientContact != null){
            mConversationNameLabel.setText(talkClientContact.getName());
        } else {
            mConversationNameLabel.setText(R.string.deleted_contact_name);
        }
    }

    public void updateTrackData() {
        if (mCurrentLoadArtworkTask != null && mCurrentLoadArtworkTask.getStatus() != AsyncTask.Status.FINISHED) {
            mCurrentLoadArtworkTask.cancel(true);
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AudioAttachmentItem currentItem = mMediaPlayerService.getCurrentMediaItem();
                String trackArtist = currentItem.getMetaData().getArtist();
                String trackTitle = currentItem.getMetaData().getTitle();
                int totalDuration = mMediaPlayerService.getTotalDuration();

                if (trackTitle == null || trackTitle.isEmpty()) {
                    File file = new File(currentItem.getFilePath());
                    trackTitle = file.getName();
                }

                mTrackTitleLabel.setText(trackTitle.trim());
                if (trackArtist == null || trackArtist.isEmpty()) {
                    trackArtist = getActivity().getResources().getString(R.string.media_meta_data_unknown_artist);
                }

                mTrackArtistLabel.setText(trackArtist.trim());
                mTrackProgressBar.setMax(totalDuration);

                mTotalDurationLabel.setText(getStringFromTimeStamp(totalDuration));
                mPlaylistIndexLabel.setText(Integer.toString(mMediaPlayerService.getCurrentTrackNumber() + 1));
                mPlaylistSizeLabel.setText(Integer.toString(mMediaPlayerService.getMediaListSize()));

                if (currentItem.getMetaData().getArtwork() == null) {
                    mCurrentLoadArtworkTask = new LoadArtworkTask();
                    mCurrentLoadArtworkTask.execute(currentItem);
                } else {
                    mArtworkImageView.setImageDrawable(currentItem.getMetaData().getArtwork());
                }

                updatePlayState();

                mPlayButton.setVisibility(View.VISIBLE);
            }
        });

        if (mUpdateTimeTask == null) {
            mUpdateTimeTask = new UpdateTimeTask();
            mTimeProgressHandler.post(mUpdateTimeTask);
        }

    }

    private void setupViewListeners() {
        OnPlayerInteractionListener listener = new OnPlayerInteractionListener();
        mPlayButton.setOnCheckedChangeListener(listener);
        mTrackProgressBar.setOnSeekBarChangeListener(listener);
        mSkipBackButton.setOnClickListener(listener);
        mSkipForwardButton.setOnClickListener(listener);
        mRepeatButton.setOnClickListener(listener);
        mShuffleButton.setOnCheckedChangeListener(listener);
    }

    public void enableViewComponents(final boolean enable) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlayButton.setEnabled(enable);
                mSkipForwardButton.setEnabled(enable);
                mSkipBackButton.setEnabled(enable);
                mRepeatButton.setEnabled(enable);
                mShuffleButton.setEnabled(enable);
            }
        });
    }

    private void setupBlinkAnimation() {
        int colorFrom = getResources().getColor(R.color.xo_media_player_secondary_text);
        int colorTo = getResources().getColor(R.color.xo_app_main_color);
        mBlinkAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        mBlinkAnimation.setDuration(500);
        mBlinkAnimation.setRepeatMode(Animation.REVERSE);
        mBlinkAnimation.setRepeatCount(Animation.INFINITE);
        mBlinkAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentTimeLabel.setTextColor((Integer) animation.getAnimatedValue());
            }
        });
    }

    private String getStringFromTimeStamp(int timeInMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        if (hours > 0) {
            return String.format("%d:%02d:%02d", minutes, seconds);
        }

        return String.format("%2d:%02d", minutes, seconds);
    }


    private void adjustViewSizes() {
        RelativeLayout.LayoutParams playBtnLayoutParams = (RelativeLayout.LayoutParams) mPlayButton.getLayoutParams();
        playBtnLayoutParams.width = mPlayButton.getMeasuredHeight();
        mPlayButton.setLayoutParams(playBtnLayoutParams);
    }

    private void updateRepeatMode() {
        switch (mMediaPlayerService.getRepeatMode()) {
            case NO_REPEAT:
                mMediaPlayerService.setRepeatMode(MediaPlaylist.RepeatMode.REPEAT_ALL);
                break;
            case REPEAT_ALL:
                mMediaPlayerService.setRepeatMode(MediaPlaylist.RepeatMode.REPEAT_TITLE);
                break;
            case REPEAT_TITLE:
                mMediaPlayerService.setRepeatMode(MediaPlaylist.RepeatMode.NO_REPEAT);
                break;
        }
        updateRepeatButton();
    }

    private void updateRepeatButton() {
        MediaPlaylist.RepeatMode repeatMode = mMediaPlayerService.getRepeatMode();
        final Drawable buttonStateDrawable;
        switch (repeatMode) {
            case NO_REPEAT:
                buttonStateDrawable = getResources().getDrawable(R.drawable.btn_player_repeat);
                break;
            case REPEAT_ALL:
                buttonStateDrawable = getResources().getDrawable(R.drawable.btn_player_repeat_all);
                break;
            case REPEAT_TITLE:
                buttonStateDrawable = getResources().getDrawable(R.drawable.btn_player_repeat_title);
                break;
            default:
                buttonStateDrawable = null;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRepeatButton.setBackgroundDrawable(buttonStateDrawable);
            }
        });
    }


    private class OnPlayerInteractionListener implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, ToggleButton.OnCheckedChangeListener {


        @Override
        public void onClick(View v) {
            if (mMediaPlayerService != null) {
                switch (v.getId()) {
                    case R.id.bt_player_skip_back:
                        mTrackProgressBar.setProgress(0);
                        mMediaPlayerService.playPrevious();
                        break;
                    case R.id.bt_player_skip_forward:
                        mTrackProgressBar.setProgress(0);
                        mMediaPlayerService.playNext();
                        break;
                    case R.id.bt_player_repeat:
                        updateRepeatMode();
                        break;
                }
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mMediaPlayerService.setSeekPosition(seekBar.getProgress());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mMediaPlayerService != null) {
                switch (buttonView.getId()) {
                    case R.id.bt_player_play:
                        togglePlayPauseButton(isChecked);
                        break;
                    case R.id.bt_player_shuffle:
                        if (isChecked && !mMediaPlayerService.isShuffleActive()) {
                            mMediaPlayerService.setShuffleActive(true);
                        } else if (!isChecked && mMediaPlayerService.isShuffleActive()){
                            mMediaPlayerService.setShuffleActive(false);
                        }
                        break;
                }
            }
        }
    }

    private void togglePlayPauseButton(boolean isChecked){
        boolean isPlaying = (mMediaPlayerService.isPaused() || mMediaPlayerService.isStopped()) ? false : true;
        if (!isChecked && isPlaying) {
            mMediaPlayerService.pause();
            mBlinkAnimation.start();
        } else if (isChecked && !isPlaying){
            mMediaPlayerService.play();
            if (mBlinkAnimation.isRunning()) {
                mBlinkAnimation.cancel();
            }

            mCurrentTimeLabel.setTextColor(getResources().getColor(R.color.xo_media_player_secondary_text));
        }
    }

    private class UpdateTimeTask implements Runnable {

        @Override
        public void run() {
            try {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int currentProgress = mMediaPlayerService.getCurrentProgress();
                        mCurrentTimeLabel.setText(getStringFromTimeStamp(currentProgress));
                        mTrackProgressBar.setProgress(currentProgress);
                    }
                });

                mTimeProgressHandler.postDelayed(this, 1000);
            } catch (Exception e) {
                LOG.error(e);
            }
        }
    }

    private class LoadArtworkTask extends AsyncTask<AudioAttachmentItem, Void, Drawable> {

        private AudioAttachmentItem mAudioAttachmentItem;

        protected Drawable doInBackground(AudioAttachmentItem... params) {
            mAudioAttachmentItem = params[0];
            byte[] artworkRaw = MediaMetaData.getArtwork(mAudioAttachmentItem.getFilePath());
            if (artworkRaw != null) {
                return new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length));
            } else {
                return null;
            }
        }

        protected void onPostExecute(Drawable artwork) {
            super.onPostExecute(artwork);
            if (!this.isCancelled()) {
                mAudioAttachmentItem.getMetaData().setArtwork(artwork);
                mArtworkImageView.setImageDrawable(artwork);
            }
        }
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isAttached()) {
                    Log.d("FullscreenPlayerFragment", "Fragment is not yet attached. No Need to update.");
                    return;
                }

                if (intent.getAction().equals(MediaPlayerService.PLAYSTATE_CHANGED_ACTION)) {
                    updatePlayState();
                } else if (intent.getAction().equals(MediaPlayerService.TRACK_CHANGED_ACTION)) {
                    updateTrackData();
                    updateConversationName();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaPlayerService.PLAYSTATE_CHANGED_ACTION);
        intentFilter.addAction(MediaPlayerService.TRACK_CHANGED_ACTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private boolean isAttached() {
        return getActivity() != null;
    }
}