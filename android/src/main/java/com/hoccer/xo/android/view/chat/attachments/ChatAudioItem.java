package com.hoccer.xo.android.view.chat.attachments;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.MediaPlayer;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.MediaPlaylist;
import com.hoccer.xo.android.content.SingleItemPlaylist;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import com.hoccer.xo.android.view.chat.ChatMessageItem;


public class ChatAudioItem extends ChatMessageItem implements MediaPlayer.Listener {

    private ImageButton mPlayPauseButton;

    public ChatAudioItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithAudio;
    }

    @Override
    protected void configureViewForMessage(View view) {
        super.configureViewForMessage(view);
        configureAttachmentViewForMessage(view);
    }

    @Override
    protected void displayAttachment(final XoTransfer transfer) {
        super.displayAttachment(transfer);

        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.content_audio, null);
            mContentWrapper.addView(v);
        }
        LinearLayout audioLayout = (LinearLayout) mContentWrapper.getChildAt(0);
        TextView captionTextView = (TextView) audioLayout.findViewById(R.id.tv_content_audio_caption);
        TextView nameTextView = (TextView) audioLayout.findViewById(R.id.tv_content_audio_name);
        mPlayPauseButton = (ImageButton) audioLayout.findViewById(R.id.ib_content_audio_play);
        setPlayButton();

        if (mMessage.isIncoming()) {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
            nameTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
        } else {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.compose_message_text));
            nameTextView.setTextColor(mContext.getResources().getColor(R.color.compose_message_text));
        }

        MediaMetaData metaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(transfer.getFilePath()).getPath());
        if (metaData.getArtist().isEmpty()) {
            nameTextView.setText(metaData.getTitleOrFilename());
        } else {
            nameTextView.setText(metaData.getArtist() + " - " + metaData.getTitleOrFilename());
        }

        mPlayPauseButton = (ImageButton) audioLayout.findViewById(R.id.ib_content_audio_play);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isActive()) {
                    pause();
                } else {
                    play();
                }
            }
        });

        updatePlayPauseView();

        MediaPlayer.get().registerListener(this);
    }

    public static void pause() {
        MediaPlayer.get().pause();
    }

    private void play() {
        MediaPlaylist playlist = new SingleItemPlaylist(XoApplication.get().getXoClient().getDatabase(), mAttachment);
        MediaPlayer.get().playItemInPlaylist(mAttachment, playlist);
    }

    @Override
    public void detachView() {
        MediaPlayer.get().unregisterListener(this);
    }


    private void setPlayButton() {
        if (mMessage.isIncoming()) {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_play, R.color.attachment_incoming));
        } else {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_play, R.color.attachment_outgoing));
        }
    }

    private void setPauseButton() {
        if (mMessage.isIncoming()) {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_pause, R.color.attachment_incoming));
        } else {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_pause, R.color.attachment_outgoing));
        }
    }

    public void updatePlayPauseView() {
        if (mPlayPauseButton != null && mMessage != null) {
            if (isActive()) {
                setPauseButton();
            } else {
                setPlayButton();
            }
        }
    }

    public boolean isActive() {
        MediaPlayer mediaPlayer = MediaPlayer.get();
        return !mediaPlayer.isPaused() && !mediaPlayer.isStopped() && mAttachment.equals(mediaPlayer.getCurrentMediaItem());
    }

    @Override
    public void onStateChanged() {
        updatePlayPauseView();
    }

    @Override
    public void onTrackChanged() {}
}
