package com.hoccer.xo.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class AudioAttachmentView extends LinearLayout implements View.OnClickListener, MediaMetaData.ArtworkRetrieverListener {

    private Context mContext;
    private IContentObject mItem;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;

    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private ImageView mArtworkImageView;
    private View mPlayStatusView;
    private ImageView mDragHandleView;
    private MediaMetaData mCurrentMetaData;

    private static final Logger LOG = Logger.getLogger(AudioAttachmentView.class);

    public AudioAttachmentView(Context context) {
        super(context.getApplicationContext());
        mContext = context;
        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector();
        addView(inflate(mContext, R.layout.item_audio_attachment, null));

        mTitleTextView = ((TextView) findViewById(R.id.tv_title_name));
        mArtistTextView = ((TextView) findViewById(R.id.tv_artist_name));
        mArtworkImageView = ((ImageView) findViewById(R.id.iv_artcover));
        mPlayStatusView = findViewById(R.id.iv_playing_status);
        mDragHandleView = (ImageView) findViewById(R.id.list_drag_handle);
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
        if (isActive() && !mDragHandleView.isShown()) {
            mPlayStatusView.setVisibility(View.VISIBLE);
        } else {
            mPlayStatusView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mMediaPlayerServiceConnector.connect(mContext, IntentHelper.ACTION_PLAYER_STATE_CHANGED,
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

    public void showDragHandle(boolean shallShow) {
        if (shallShow) {
            if (mPlayStatusView.isShown()) {
                mPlayStatusView.setVisibility(GONE);
            }

            mDragHandleView.setVisibility(VISIBLE);
        } else {
            mDragHandleView.setVisibility(GONE);

            if (isActive()) {
                mPlayStatusView.setVisibility(VISIBLE);
            }
        }
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
        new Handler(mContext.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mArtworkImageView.setImageDrawable(artwork);
            }
        });
    }
}
