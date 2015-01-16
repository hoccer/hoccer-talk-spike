package com.hoccer.xo.android.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
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
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.audio.MediaPlaylistController;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.ArtworkImageView;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class FullscreenPlayerFragment extends Fragment implements MediaMetaData.ArtworkRetrieverListener {

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

    MediaMetaData mCurrentMetaData = null;

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

        if (mMediaPlayerService != null && mMediaPlayerService.getCurrentMediaItem() != null) {
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
            final boolean isPlaying;

            if ((mMediaPlayerService.isPaused()) || mMediaPlayerService.isStopped()) {
                isPlaying = true;
            } else if (!mMediaPlayerService.isPaused() && !mMediaPlayerService.isStopped()) {
                isPlaying = false;
            } else {
                isPlaying = !mPlayButton.isChecked();
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isPlaying) {
                        mPlayButton.setChecked(!isPlaying);
                        mBlinkAnimation.start();
                    } else {
                        if (mBlinkAnimation.isRunning()) {
                            mBlinkAnimation.cancel();
                        }

                        mCurrentTimeLabel.setTextColor(getResources().getColor(R.color.xo_media_player_secondary_text));
                        mPlayButton.setChecked(!isPlaying);
                    }
                }
            });
        }
    }

    private void updateConversationName() {
        int conversationContactId = mMediaPlayerService.getCurrentConversationContactId();
        TalkClientContact talkClientContact = null;
        try {
            talkClientContact = XoApplication.getXoClient().getDatabase().findContactById(conversationContactId);
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
        // ensure that we are not listening to any previous artwork retrieval tasks
        if(mCurrentMetaData != null) {
            mCurrentMetaData.unregisterArtworkRetrievalListener(this);
        }

        mCurrentMetaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(mMediaPlayerService.getCurrentMediaItem().getContentDataUrl()).getPath());
        final String trackArtist;
        final String trackTitle;
        final int totalDuration = mMediaPlayerService.getTotalDuration();
        final String durationLabel = getStringFromTimeStamp(totalDuration);
        final String playlistIndex = Integer.toString(mMediaPlayerService.getCurrentIndex() + 1);
        final String playlistSize = Integer.toString(mMediaPlayerService.getMediaListSize());

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
        switch (mMediaPlayerService.getRepeatMode()) {
            case NO_REPEAT:
                mMediaPlayerService.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
                break;
            case REPEAT_ALL:
                mMediaPlayerService.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ITEM);
                break;
            case REPEAT_ITEM:
                mMediaPlayerService.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
                break;
        }
        updateRepeatButton();
    }

    private void updateRepeatButton() {
        MediaPlaylistController.RepeatMode repeatMode = mMediaPlayerService.getRepeatMode();
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
            if (mMediaPlayerService != null) {
                switch (v.getId()) {
                    case R.id.bt_player_skip_back:
                        mTrackProgressBar.setProgress(0);
                        mMediaPlayerService.backward();
                        break;
                    case R.id.bt_player_skip_forward:
                        mTrackProgressBar.setProgress(0);
                        mMediaPlayerService.forward();
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
            final int currentProgress = mMediaPlayerService.getCurrentProgress();
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

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isAttached()) {
                    Log.d("FullscreenPlayerFragment", "Fragment is not yet attached. No Need to update.");
                    return;
                }

                if (intent.getAction().equals(IntentHelper.ACTION_PLAYER_STATE_CHANGED)) {
                    updatePlayState();
                } else if (intent.getAction().equals(IntentHelper.ACTION_PLAYER_TRACK_CHANGED)) {
                    updateTrackData();
                    updateConversationName();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_PLAYER_STATE_CHANGED);
        intentFilter.addAction(IntentHelper.ACTION_PLAYER_TRACK_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private boolean isAttached() {
        return getActivity() != null;
    }
}
