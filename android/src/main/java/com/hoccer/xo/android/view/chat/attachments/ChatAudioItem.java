package com.hoccer.xo.android.view.chat.attachments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.MediaMetaData;
import com.hoccer.xo.android.content.SingleItemPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;


public class ChatAudioItem extends ChatMessageItem {

    private ImageButton mPlayPauseButton;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;
    private IContentObject mAudioContentObject;
    private boolean mIsPlayable = false;

    public ChatAudioItem(Context context, TalkClientMessage message) {
        super(context, message);
        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector();
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
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.xo_incoming_message_textColor));
            fileNameTextView.setTextColor(mContext.getResources().getColor(R.color.xo_incoming_message_textColor));
        } else {
            captionTextView.setTextColor(mContext.getResources().getColor(R.color.xo_compose_message_textColor));
            fileNameTextView.setTextColor(mContext.getResources().getColor(R.color.xo_compose_message_textColor));
        }

        MediaMetaData metaData = MediaMetaData.retrieveMetaData(contentObject.getContentDataUrl());
        String displayName;
        if (metaData.getTitle() != null) {
            displayName = metaData.getTitle().trim();

            if (metaData.getArtist() != null) {
                displayName = metaData.getArtist().trim() + " - " + displayName;
            }
        } else {
            try {
                URI fileUri = new URI(contentObject.getContentDataUrl());
                File contentFile = new File(fileUri);
                displayName = contentFile.getName();
            } catch (URISyntaxException e) {
                e.printStackTrace();
                displayName = contentObject.getFileName();
            }
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
    }

    private void pausePlaying() {
        if (mMediaPlayerServiceConnector.isConnected()) {
            mMediaPlayerServiceConnector.getService().pause();
        }
    }

    private void startPlaying() {
        if (mMediaPlayerServiceConnector.isConnected()) {
            MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            if (service.isPaused() && service.getCurrentMediaItem() != null && mAudioContentObject.equals(service.getCurrentMediaItem())) {
                service.play();
            } else {
                service.setPlaylist(new SingleItemPlaylist(XoApplication.getXoClient().getDatabase(), mAudioContentObject));
                service.play(0);
            }
        }
    }

    private void setPlayButton(){
        mPlayPauseButton.setBackgroundDrawable(null);
        mPlayPauseButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_play, mMessage.isIncoming()));
    }

    private void setPauseButton(){
        mPlayPauseButton.setBackgroundDrawable(null);
        mPlayPauseButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_pause, mMessage.isIncoming()));
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
        mMediaPlayerServiceConnector.connect(mContext,
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

    private void destroyMediaPlayerService(){
        mMediaPlayerServiceConnector.disconnect();
    }

    @Override
    public void setVisibility(boolean visible) {
        if ( mVisible == visible){
            return;
        }

        super.setVisibility(visible);

        if( mVisible){
            initializeMediaPlayerService();
        }else{
            destroyMediaPlayerService();
        }
    }
}
