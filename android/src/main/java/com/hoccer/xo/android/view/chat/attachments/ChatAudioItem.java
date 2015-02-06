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
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.UriUtils;
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
        TextView fileNameTextView = (TextView) audioLayout.findViewById(R.id.tv_content_audio_name);
        mPlayPauseButton = (ImageButton) audioLayout.findViewById(R.id.ib_content_audio_play);
        setPlayButton();

        if (mMessage.isIncoming()) {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.xo_incoming_message_textColor));
            fileNameTextView.setTextColor(mContext.getResources().getColor(R.color.xo_incoming_message_textColor));
        } else {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.xo_compose_message_textColor));
            fileNameTextView.setTextColor(mContext.getResources().getColor(R.color.xo_compose_message_textColor));
        }

        MediaMetaData metaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(transfer.getFilePath()).getPath());
        String displayName = metaData.getTitleOrFilename().trim();
        if (metaData.getArtist() != null) {
            displayName = metaData.getArtist().trim() + " - " + displayName;
        }
        fileNameTextView.setText(displayName);

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
        MediaPlaylist playlist = new SingleItemPlaylist(XoApplication.getXoClient().getDatabase(), mAttachment);
        MediaPlayer.get().playItemInPlaylist(mAttachment, playlist);
    }

    @Override
    public void detachView() {
        MediaPlayer.get().unregisterListener(this);
    }


    private void setPlayButton() {
        mPlayPauseButton.setBackgroundDrawable(null);
        mPlayPauseButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_play, mMessage.isIncoming()));
    }

    private void setPauseButton() {
        mPlayPauseButton.setBackgroundDrawable(null);
        mPlayPauseButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_pause, mMessage.isIncoming()));
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
