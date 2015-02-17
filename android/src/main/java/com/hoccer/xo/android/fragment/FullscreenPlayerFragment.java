package com.hoccer.xo.android.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.*;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.MediaPlayer;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.audio.MediaPlaylistController;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.ArtworkImageView;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class FullscreenPlayerFragment extends Fragment implements MediaMetaData.ArtworkRetrieverListener, MediaPlayer.Listener {

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

    private final Handler mTimeProgressHandler = new Handler();
    private Runnable mUpdateTimeTask;
    private ValueAnimator mBlinkAnimation;

    MediaMetaData mCurrentMetaData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBlinkAnimation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_fullscreen_player, container);
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
        mTrackProgressBar.setProgress(0);
        mTrackProgressBar.setMax(100);
        mPlayButton = (ToggleButton) view.findViewById(R.id.bt_player_play);

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                adjustViewSizes();
            }
        });

        enableViewComponents(true);
        updateTrackData();
        updateConversationName();
        mPlayButton.setChecked(!MediaPlayer.get().isPaused());
        mShuffleButton.setChecked(MediaPlayer.get().isShuffleActive());
        updateRepeatButton();

        setupViewListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MediaPlayer.get().getCurrentMediaItem() != null) {
            updateTrackData();
            updatePlayState();
        }
        MediaPlayer.get().registerListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTimeProgressHandler.removeCallbacks(mUpdateTimeTask);
        mUpdateTimeTask = null;
        MediaPlayer.get().unregisterListener(this);
    }

    public void updatePlayState() {
        final boolean isPlaying;

        if ((MediaPlayer.get().isPaused()) || MediaPlayer.get().isStopped()) {
            isPlaying = true;
        } else if (!MediaPlayer.get().isPaused() && !MediaPlayer.get().isStopped()) {
            isPlaying = false;
        } else {
            isPlaying = !mPlayButton.isChecked();
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    mPlayButton.setChecked(false);
                    mBlinkAnimation.start();
                } else {
                    if (mBlinkAnimation.isRunning()) {
                        mBlinkAnimation.cancel();
                    }

                    mCurrentTimeLabel.setTextColor(getResources().getColor(R.color.media_player_text_secondary));
                    mPlayButton.setChecked(true);
                }
            }
        });
    }

    private void updateConversationName() {
        int conversationContactId = MediaPlayer.get().getCurrentConversationContactId();
        TalkClientContact talkClientContact = null;
        try {
            talkClientContact = XoApplication.get().getXoClient().getDatabase().findContactById(conversationContactId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (talkClientContact != null) {
            mConversationNameLabel.setText(talkClientContact.getName());
        } else {
            mConversationNameLabel.setText(R.string.deleted_contact_name);
        }
    }

    public void updateTrackData() {
        // ensure that we are not listening to any previous artwork retrieval tasks
        if (mCurrentMetaData != null) {
            mCurrentMetaData.unregisterArtworkRetrievalListener(this);
        }

        Uri mediaUri = UriUtils.getAbsoluteFileUri(MediaPlayer.get().getCurrentMediaItem().getFilePath());
        mCurrentMetaData = MediaMetaData.retrieveMetaData(mediaUri.getPath());
        final String trackArtist;
        final String trackTitle;
        final int totalDuration = MediaPlayer.get().getTotalDuration();
        final String durationLabel = getStringFromTimeStamp(totalDuration);
        final String playlistIndex = Integer.toString(MediaPlayer.get().getCurrentIndex() + 1);
        final String playlistSize = Integer.toString(MediaPlayer.get().getMediaListSize());

        if (mCurrentMetaData.getTitle() == null || mCurrentMetaData.getTitle().isEmpty()) {
            File file = new File(mCurrentMetaData.getFilePath());
            trackTitle = file.getName();
        } else {
            trackTitle = mCurrentMetaData.getTitle().trim();
        }

        if (mCurrentMetaData.getArtist() == null || mCurrentMetaData.getArtist().isEmpty()) {
            trackArtist = getActivity().getResources().getString(R.string.media_meta_data_unknown_artist);
        } else {
            trackArtist = mCurrentMetaData.getArtist().trim();
        }

        mCurrentMetaData.getArtwork(getResources(), FullscreenPlayerFragment.this);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mTrackTitleLabel.setText(trackTitle);
                mTrackArtistLabel.setText(trackArtist);
                mTrackProgressBar.setMax(totalDuration);

                mTotalDurationLabel.setText(durationLabel);
                mPlaylistIndexLabel.setText(playlistIndex);
                mPlaylistSizeLabel.setText(playlistSize);


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
        int colorFrom = getResources().getColor(R.color.media_player_text_secondary);
        int colorTo = getResources().getColor(R.color.primary);
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
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%2d:%02d", minutes, seconds);
    }


    private void adjustViewSizes() {
        RelativeLayout.LayoutParams playBtnLayoutParams = (RelativeLayout.LayoutParams) mPlayButton.getLayoutParams();
        playBtnLayoutParams.width = mPlayButton.getMeasuredHeight();
        mPlayButton.setLayoutParams(playBtnLayoutParams);
    }

    private void updateRepeatMode() {
        switch (MediaPlayer.get().getRepeatMode()) {
            case NO_REPEAT:
                MediaPlayer.get().setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
                break;
            case REPEAT_ALL:
                MediaPlayer.get().setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ITEM);
                break;
            case REPEAT_ITEM:
                MediaPlayer.get().setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
                break;
        }
        updateRepeatButton();
    }

    private void updateRepeatButton() {
        MediaPlaylistController.RepeatMode repeatMode = MediaPlayer.get().getRepeatMode();
        final Drawable buttonStateDrawable;
        switch (repeatMode) {
            case NO_REPEAT:
                buttonStateDrawable = getResources().getDrawable(R.drawable.btn_player_repeat);
                break;
            case REPEAT_ALL:
                buttonStateDrawable = getResources().getDrawable(R.drawable.btn_player_repeat_all);
                break;
            case REPEAT_ITEM:
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

    @Override
    public void onArtworkRetrieveFinished(final Drawable artwork) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mArtworkImageView.setImageDrawable(artwork);
            }
        });
    }

    private class OnPlayerInteractionListener implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, ToggleButton.OnCheckedChangeListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_player_skip_back:
                    mTrackProgressBar.setProgress(0);
                    MediaPlayer.get().backward();
                    break;
                case R.id.bt_player_skip_forward:
                    mTrackProgressBar.setProgress(0);
                    MediaPlayer.get().forward();
                    break;
                case R.id.bt_player_repeat:
                    updateRepeatMode();
                    break;
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
            MediaPlayer.get().setSeekPosition(seekBar.getProgress());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (MediaPlayer.get() != null) {
                switch (buttonView.getId()) {
                    case R.id.bt_player_play:
                        togglePlayPauseButton(isChecked);
                        break;
                    case R.id.bt_player_shuffle:
                        if (isChecked && !MediaPlayer.get().isShuffleActive()) {
                            MediaPlayer.get().setShuffleActive(true);
                        } else if (!isChecked && MediaPlayer.get().isShuffleActive()) {
                            MediaPlayer.get().setShuffleActive(false);
                        }
                        break;
                }
            }
        }
    }

    private void togglePlayPauseButton(boolean isChecked) {
        boolean isPlaying = !(MediaPlayer.get().isPaused() || MediaPlayer.get().isStopped());
        if (!isChecked && isPlaying) {
            MediaPlayer.get().pause();
            mBlinkAnimation.start();
        } else if (isChecked && !isPlaying) {
            MediaPlayer.get().play();
            if (mBlinkAnimation.isRunning()) {
                mBlinkAnimation.cancel();
            }

            mCurrentTimeLabel.setTextColor(getResources().getColor(R.color.media_player_text_secondary));
        }
    }

    private class UpdateTimeTask implements Runnable {

        @Override
        public void run() {
            final int currentProgress = MediaPlayer.get().getCurrentProgress();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCurrentTimeLabel.setText(getStringFromTimeStamp(currentProgress));
                    mTrackProgressBar.setProgress(currentProgress);
                }
            });
            mTimeProgressHandler.postDelayed(this, 1000);
        }
    }

    @Override
    public void onStateChanged() {
        updatePlayState();
    }

    @Override
    public void onTrackChanged() {
        updateTrackData();
        updateConversationName();
    }
}
