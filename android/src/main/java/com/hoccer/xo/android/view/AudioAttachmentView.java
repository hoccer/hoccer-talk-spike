package com.hoccer.xo.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.xo.android.MediaPlayer;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.UriUtils;

public class AudioAttachmentView extends LinearLayout implements View.OnClickListener, MediaMetaData.ArtworkRetrieverListener, MediaPlayer.Listener {

    private final Context mContext;
    private XoTransfer mItem;

    private final TextView mTitleTextView;
    private final TextView mArtistTextView;
    private final ImageView mArtworkImageView;
    private final View mPlayStatusView;
    private final ImageView mDragHandleView;
    private MediaMetaData mCurrentMetaData;

    public AudioAttachmentView(Context context) {
        super(context.getApplicationContext());
        mContext = context;
        addView(inflate(mContext, R.layout.item_audio_attachment, null));

        mTitleTextView = ((TextView) findViewById(R.id.tv_title_name));
        mArtistTextView = ((TextView) findViewById(R.id.tv_artist_name));
        mArtworkImageView = ((ImageView) findViewById(R.id.iv_artcover));
        mPlayStatusView = findViewById(R.id.iv_playing_status);
        mDragHandleView = (ImageView) findViewById(R.id.list_drag_handle);
    }

    public void setMediaItem(XoTransfer audioAttachmentItem) {
        if (mItem == null || !mItem.equals(audioAttachmentItem)) {
            mItem = audioAttachmentItem;
            updateAudioView();
        }
    }

    public boolean isActive() {
        MediaPlayer mediaPlayer = MediaPlayer.get();
        XoTransfer currentItem = mediaPlayer.getCurrentMediaItem();
        return !mediaPlayer.isPaused() && !mediaPlayer.isStopped() && (mItem.equals(currentItem));
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
        updatePlayPauseView();
        MediaPlayer.get().registerListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        MediaPlayer.get().unregisterListener(this);
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
        mCurrentMetaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(mItem.getFilePath()).getPath());
        mTitleTextView.setText(mCurrentMetaData.getTitleOrFilename());

        String artist = mCurrentMetaData.getArtist();
        if (artist.isEmpty()) {
            mArtistTextView.setText(getResources().getString(R.string.media_meta_data_unknown_artist));
        } else {
            mArtistTextView.setText(artist);
        }

        int thumbnailWidth = DisplayUtils.getDisplaySize(mContext).x / 4;
        mCurrentMetaData.getResizedArtwork(getResources(), this, thumbnailWidth);
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

    @Override
    public void onStateChanged() {
        updatePlayPauseView();
    }

    @Override
    public void onTrackChanged() {}
}
