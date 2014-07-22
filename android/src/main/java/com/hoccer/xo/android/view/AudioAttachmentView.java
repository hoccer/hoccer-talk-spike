package com.hoccer.xo.android.view;

import android.content.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class AudioAttachmentView extends LinearLayout implements View.OnClickListener {

    private Context mContext;
    private AudioAttachmentItem mAudioAttachmentItem;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;

    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private ImageView mArtworkImageView;
    private View mPlayStatusView;
    private ImageButton mDragHandleView;

    private static final Logger LOG = Logger.getLogger(AudioAttachmentView.class);

    public AudioAttachmentView(Context context) {
        super(context);
        mContext = context;
        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector();
        addView(inflate(mContext, R.layout.item_audio_attachment, null));

        mTitleTextView = ((TextView) findViewById(R.id.tv_title_name));
        mArtistTextView = ((TextView) findViewById(R.id.tv_artist_name));
        mArtworkImageView = ((ImageView) findViewById(R.id.iv_artcover));
        mPlayStatusView = findViewById(R.id.iv_playing_status);
        mDragHandleView = (ImageButton) findViewById(R.id.ib_list_drag_handle);
    }

    public void setMediaItem(AudioAttachmentItem audioAttachmentItem) {
        if (mAudioAttachmentItem == null || !mAudioAttachmentItem.equals(audioAttachmentItem)) {
            mAudioAttachmentItem = audioAttachmentItem;
            updateAudioView();
        }
    }

    public boolean isActive() {
        if (mMediaPlayerServiceConnector.isConnected()) {
            MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            AudioAttachmentItem currentItem = service.getCurrentMediaItem();
            return !service.isPaused() && !service.isStopped() && (mAudioAttachmentItem.equals(currentItem));
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

        mMediaPlayerServiceConnector.connect(mContext,
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
        mTitleTextView.setText(mAudioAttachmentItem.getMetaData().getTitleOrFilename(mAudioAttachmentItem.getFilePath()).trim());

        String artist = mAudioAttachmentItem.getMetaData().getArtist();
        if (artist == null || artist.isEmpty()) {
            artist = getResources().getString(R.string.media_meta_data_unknown_artist);
        }

        mArtistTextView.setText(artist.trim());

        mAudioAttachmentItem.getMetaData().getArtwork(getResources(), new MediaMetaData.ArtworkRetrieverListener() {
            @Override
            public void onFinished(Drawable artwork) {
                mArtworkImageView.setImageDrawable(artwork);
            }
        });
    }
}
