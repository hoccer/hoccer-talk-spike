package com.hoccer.xo.android.view;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class AudioAttachmentView extends LinearLayout implements View.OnClickListener, MediaMetaData.ArtworkRetrieverListener {

    private Activity mActivity;
    private IContentObject mItem;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;

    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private ImageView mArtworkImageView;
    private MediaMetaData mCurrentMetaData;

    private static final Logger LOG = Logger.getLogger(AudioAttachmentView.class);

    public AudioAttachmentView(Activity activity) {
        super(activity.getApplicationContext());
        mActivity = activity;
        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector();
        addView(inflate(mActivity, R.layout.item_audio_attachment, null));

        mTitleTextView = ((TextView) findViewById(R.id.tv_title_name));
        mArtistTextView = ((TextView) findViewById(R.id.tv_artist_name));
        mArtworkImageView = ((ImageView) findViewById(R.id.iv_artcover));
    }

    public void setMediaItem(IContentObject audioAttachmentItem) {
        if (mItem == null || !mItem.equals(audioAttachmentItem)) {
            mItem = audioAttachmentItem;
            updateAudioView();
        }
    }

    public boolean isActive() {
        if (mMediaPlayerServiceConnector.isConnected()) {
            MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            IContentObject currentItem = service.getCurrentMediaItem();
            return !service.isPaused() && !service.isStopped() && (mItem.equals(currentItem));
        } else {
            return false;
        }
    }

    public void updatePlayPauseView() {
        View view = findViewById(R.id.iv_playing_status);
        if (isActive()) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mMediaPlayerServiceConnector.connect(mActivity,
                MediaPlayerService.PLAYSTATE_CHANGED_ACTION,
                new MediaPlayerServiceConnector.Listener() {
                    @Override
                    public void onConnected(MediaPlayerService service) {
                        updatePlayPauseView();
                    }
                    @Override
                    public void onDisconnected() {
                    }
                    @Override
                    public void onAction(String action, MediaPlayerService service) {
                        updatePlayPauseView();
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMediaPlayerServiceConnector.disconnect();
    }

    @Override
    public void onClick(View v) {
    }

    private void updateAudioView() {
        // ensure that we are not listening to any previous artwork retrieval tasks
        if(mCurrentMetaData != null) {
            mCurrentMetaData.unregisterArtworkRetrievalListener(this);
        }

        mCurrentMetaData = MediaMetaData.retrieveMetaData(mItem.getContentDataUrl());
        mTitleTextView.setText(mCurrentMetaData.getTitleOrFilename().trim());

        String artist = mCurrentMetaData.getArtist();
        if (artist == null || artist.isEmpty()) {
            artist = getResources().getString(R.string.media_meta_data_unknown_artist);
        }

        mArtistTextView.setText(artist.trim());

        mCurrentMetaData.getArtwork(getResources(), this);
    }

    @Override
    public void onArtworkRetrieveFinished(final Drawable artwork) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mArtworkImageView.setImageDrawable(artwork);
            }
        });
    }
}
