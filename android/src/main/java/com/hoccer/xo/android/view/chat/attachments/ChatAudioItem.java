package com.hoccer.xo.android.view.chat.attachments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.MediaPlaylist;
import com.hoccer.xo.android.content.SingleItemPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.artcom.hoccer.R;


public class ChatAudioItem extends ChatMessageItem {

    private ImageButton mPlayPauseButton;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;
    private IContentObject mAudioContentObject;
    private boolean mIsPlayable = false;

    public ChatAudioItem(Context context, TalkClientMessage message) {
        super(context, message);
        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector(context);
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
    protected void displayAttachment(final IContentObject contentObject) {
        super.displayAttachment(contentObject);

        // add view lazily
        if (mContentWrapper.getChildCount() == 0)
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View v =  inflater.inflate(R.layout.content_audio, null);
            mContentWrapper.addView(v);
        }
        LinearLayout audioLayout = (LinearLayout) mContentWrapper.getChildAt(0);
        TextView captionTextView = (TextView) audioLayout.findViewById(R.id.tv_content_audio_caption);
        TextView fileNameTextView = (TextView) audioLayout.findViewById(R.id.tv_content_audio_name);
        mPlayPauseButton = (ImageButton) audioLayout.findViewById(R.id.ib_content_audio_play);
        setPlayButton();

        if(mMessage.isIncoming()) {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
            fileNameTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
        } else {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.compose_message_text));
            fileNameTextView.setTextColor(mContext.getResources().getColor(R.color.compose_message_text));
        }

        MediaMetaData metaData = MediaMetaData.retrieveMetaData(UriUtils.getAbsoluteFileUri(contentObject.getFilePath()).getPath());
        String displayName = metaData.getTitleOrFilename().trim();
        if (metaData.getArtist() != null) {
            displayName = metaData.getArtist().trim() + " - " + displayName;
        }
        fileNameTextView.setText(displayName);

        mPlayPauseButton = (ImageButton) audioLayout.findViewById(R.id.ib_content_audio_play);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsPlayable) {
                    if (isActive()) {
                        pausePlaying();
                    } else {
                        startPlaying();
                    }
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                    alertDialog.setMessage(mContext.getResources().getString(R.string.content_not_supported_audio_msg));
                    alertDialog.setTitle(mContext.getString(R.string.content_not_supported_audio_title));
                    DialogInterface.OnClickListener nullListener = null;
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", nullListener);
                    alertDialog.show();
                }
            }
        });

        mAudioContentObject = contentObject;
        mIsPlayable = mAudioContentObject != null;
        updatePlayPauseView();

        initializeMediaPlayerService();
    }

    @Override
    public void detachView() {
        mMediaPlayerServiceConnector.disconnect();
    }

    private void pausePlaying() {
        if (mMediaPlayerServiceConnector.isConnected()) {
            mMediaPlayerServiceConnector.getService().pause();
        }
    }

    private void startPlaying() {
        if (mMediaPlayerServiceConnector.isConnected()) {
            MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            MediaPlaylist playlist = new SingleItemPlaylist(XoApplication.getXoClient().getDatabase(), mAudioContentObject);
            service.playItemInPlaylist(mAudioContentObject, playlist);
        }
    }

    private void setPlayButton(){
        if (mMessage.isIncoming()) {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_play, R.color.attachment_incoming));
        } else {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_play, R.color.attachment_outgoing));
        }
    }

    private void setPauseButton(){
        if (mMessage.isIncoming()) {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_pause, R.color.attachment_incoming));
        } else {
            mPlayPauseButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_pause, R.color.attachment_outgoing));
        }
    }

    public void updatePlayPauseView() {
        if(mPlayPauseButton != null && mMessage != null) {
            if (isActive()) {
                setPauseButton();
            } else {
                setPlayButton();
            }
        }
    }

    public boolean isActive() {
        boolean isActive = false;
        if (mAudioContentObject != null && mMediaPlayerServiceConnector.isConnected()) {
            MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            isActive = !service.isPaused() && !service.isStopped() && mAudioContentObject.equals(service.getCurrentMediaItem());
        }

        return isActive;
    }

    private void initializeMediaPlayerService(){
        mMediaPlayerServiceConnector.connect(
                IntentHelper.ACTION_PLAYER_STATE_CHANGED,
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
}
