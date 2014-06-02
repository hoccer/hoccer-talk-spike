package com.hoccer.xo.android.view.chat;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hoccer.talk.client.XoTransferAgent;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AttachmentTransferControlView;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferHandler;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.util.Date;

/**
 * This class creates and configures layouts for incoming or outgoing messages.
 */
public class ChatMessageItem {

    protected Logger LOG = Logger.getLogger(getClass());

    protected Context mContext;
    protected boolean mWaitUntilOperationIsFinished;

    protected TextView mMessageText;
    protected RelativeLayout mContentTransferProgress;
    protected LinearLayout mContentWrapper;
    protected AttachmentTransferControlView mTransferControl;


    public ChatMessageItem(Context context) {
        super();
        mContext = context;
    }

    /**
     * Creates a new empty message layout.
     *
     * @return a View object containing an empty message layout
     */
    private View createView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.item_chat_message, null);
    }

    /**
     * Configures a given message layout using data from a given TalkClientMessage object.
     * <p/>
     * Subtypes will have to overwrite this method to enhance the configuration of the message layout.
     *
     * @param view    The given layout
     * @param message The given TalkClientMessage object
     */
    protected void configureViewForMessage(View view, TalkClientMessage message) {
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.av_message_avatar);
        TextView messageTime = (TextView) view.findViewById(R.id.tv_message_time);
        TextView messageName = (TextView) view.findViewById(R.id.tv_message_contact_name);
        TextView messageText = (TextView) view.findViewById(R.id.tv_message_text);

        // Adjust layout for incoming / outgoing message
        if (message.isIncoming()) {
            if (message.getConversationContact().isGroup()) {
                setAvatar(avatarView, message.getSenderContact());
            } else {
                avatarView.setVisibility(View.GONE);
            }
            messageName.setVisibility(View.VISIBLE);
            messageName.setText(message.getSenderContact().getName());

            messageText.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_grey));
            messageText.setTextColor(mContext.getResources().getColorStateList(android.R.color.black));
            messageText.setLinkTextColor(mContext.getResources().getColorStateList(android.R.color.black));
        } else {
            avatarView.setVisibility(View.GONE);
            messageName.setVisibility(View.GONE);

            messageText.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_green));
            messageText.setTextColor(mContext.getResources().getColorStateList(android.R.color.white));
            messageText.setLinkTextColor(mContext.getResources().getColorStateList(android.R.color.white));
        }

        messageTime.setText(getMessageTimestamp(message));
        messageText.setText(message.getText());

        mMessageText = messageText;
    }

    private String getMessageTimestamp(TalkClientMessage message) {
        String timeStamp = null;
        Date time = message.getTimestamp();
        if (time != null) {

            timeStamp = (String) DateUtils.getRelativeDateTimeString(
                    mContext,
                    time.getTime(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
            );
        }
        return timeStamp;
    }

    private void setAvatar(AvatarView avatarView, final TalkClientContact sendingContact) {
        avatarView.setContact(sendingContact);
        avatarView.setVisibility(View.VISIBLE);
        if (sendingContact != null) {
            avatarView.setOnClickListener(new View.OnClickListener() {
                final TalkClientContact contact = sendingContact;

                @Override
                public void onClick(View v) {
                    if (!contact.isSelf()) {

                        // TODO: reevaluate - might not work
                        ((XoActivity) mContext).showContactProfile(contact);
                    }
                }
            });
        }
    }

    /**
     * Configures the attachment view for a given message / attachment.
     *
     * * Subtypes will have to overwrite this method to enhance the configuration of the attachment layout.
     *
     * @param view    The chat message item's view to configure
     * @param message The given TalkClientMessage
     */
    protected void configureAttachmentViewForMessage(View view, TalkClientMessage message) {

        RelativeLayout attachmentView = (RelativeLayout) view.findViewById(R.id.v_message_attachment);

        // add content view
        if (attachmentView.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View contentView = inflater.inflate(R.layout.view_content_new, null);
            attachmentView.addView(contentView);
        }
        attachmentView.setVisibility(View.VISIBLE);

        mContentWrapper = (LinearLayout) attachmentView.findViewById(R.id.content_wrapper);

        // adjust layout for incoming / outgoing attachment
        if (message.isIncoming()) {
            attachmentView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_grey));
        } else {
            attachmentView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_green));
        }

        // configure transfer progress view
        mContentTransferProgress = (RelativeLayout) attachmentView.findViewById(R.id.content_transfer_progress);
        mTransferControl = (AttachmentTransferControlView) attachmentView.findViewById(R.id.content_transfer_control);
        IContentObject contentObject = message.getAttachmentUpload();
        if (contentObject == null) {
            contentObject = message.getAttachmentDownload();
        }

        if (shouldDisplayTransferControl(getTransferState(contentObject))) {
            mContentWrapper.setVisibility(View.GONE);
            mContentTransferProgress.setVisibility(View.VISIBLE);
            mTransferControl.setOnClickListener(new AttachmentTransferHandler(mTransferControl, contentObject));
            updateTransferControl(contentObject);
        } else {
            mTransferControl.setOnClickListener(null);
            mContentTransferProgress.setVisibility(View.GONE);
            mContentWrapper.setVisibility(View.VISIBLE);
            displayAttachment(contentObject);
        }

        // hide message text field when empty - there is still an attachment to display
        if (message.getText() == null || message.getText().isEmpty()) {
            mMessageText.setVisibility(View.GONE);
        } else {
            mMessageText.setVisibility(View.VISIBLE);
        }
    }

    protected void displayAttachment(IContentObject contentObject) {

    }

    /**
     * Returns true when the transfer (upload or download) of the attachment is not completed.
     *
     * @param state
     * @return
     */
    protected boolean shouldDisplayTransferControl(ContentState state) {
        return !(state == ContentState.SELECTED || state == ContentState.UPLOAD_COMPLETE || state == ContentState.DOWNLOAD_COMPLETE);
    }

    protected ContentState getTransferState(IContentObject object) {
        XoTransferAgent agent = XoApplication.getXoClient().getTransferAgent();
        ContentState state = object.getContentState();
        if (object instanceof TalkClientDownload) {
            TalkClientDownload download = (TalkClientDownload) object;
            switch (state) {
                case DOWNLOAD_DOWNLOADING:
                case DOWNLOAD_DECRYPTING:
                case DOWNLOAD_DETECTING:
                    if (agent.isDownloadActive(download)) {
                        return state;
                    } else {
                        return ContentState.DOWNLOAD_PAUSED;
                    }
            }
        }
        if (object instanceof TalkClientUpload) {
            TalkClientUpload upload = (TalkClientUpload) object;
            switch (state) {
                case UPLOAD_REGISTERING:
                case UPLOAD_ENCRYPTING:
                case UPLOAD_UPLOADING:
                    if (agent.isUploadActive(upload)) {
                        return state;
                    } else {
                        return ContentState.UPLOAD_PAUSED;
                    }
            }
        }
        return state;
    }

    /**
     * Updates the AttachmentTransferControlView from a given IContentObject
     *
     * @param contentObject The given content object
     */
    protected void updateTransferControl(IContentObject contentObject) {
        int length = 0;
        int progress = 0;
        Resources res = mContext.getResources();
        ContentState contentState = getTransferState(contentObject);
        switch (contentState) {
            case DOWNLOAD_DETECTING:
                break;
            case DOWNLOAD_NEW:
                mTransferControl.setVisibility(View.VISIBLE);
                mTransferControl.prepareToDownload();
                mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                mTransferControl.pause();
                break;
            case DOWNLOAD_PAUSED:
                length = contentObject.getTransferLength();
                progress = contentObject.getTransferProgress();
                mTransferControl.setMax(length);
                mTransferControl.setProgressImmediately(progress);
                mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                mTransferControl.prepareToDownload();
                mTransferControl.pause();
                break;
            case DOWNLOAD_DOWNLOADING:
                length = contentObject.getTransferLength();
                progress = contentObject.getTransferProgress();
                if (length == 0 || progress == 0) {
                    length = 360;
                    progress = 18;
                }
                mTransferControl.prepareToDownload();
                mTransferControl.setText(res.getString(R.string.transfer_state_downloading));
                mTransferControl.setMax(length);
                mTransferControl.setProgress(progress);
                break;
            case DOWNLOAD_DECRYPTING:
                length = contentObject.getTransferLength();
                mTransferControl.setText(res.getString(R.string.transfer_state_decrypting));
                mTransferControl.setProgress(length);
                mTransferControl.spin();
                mWaitUntilOperationIsFinished = true;
                break;
            case DOWNLOAD_COMPLETE:
                mTransferControl.finishSpinningAndProceed();
                displayAttachment(contentObject);
            case UPLOAD_REGISTERING:
                break;
            case UPLOAD_NEW:
                mTransferControl.prepareToUpload();
                mTransferControl.setText(res.getString(R.string.transfer_state_encrypting));
                mTransferControl.setVisibility(View.VISIBLE);
                break;
            case UPLOAD_ENCRYPTING:
                mTransferControl.prepareToUpload();
                mTransferControl.setText(res.getString(R.string.transfer_state_encrypting));
                mTransferControl.setVisibility(View.VISIBLE);
                mTransferControl.spin();
                break;
            case UPLOAD_PAUSED:
                length = contentObject.getTransferLength();
                progress = contentObject.getTransferProgress();
                mTransferControl.setMax(length);
                mTransferControl.setProgressImmediately(progress);
                mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                mTransferControl.pause();
                break;
            case UPLOAD_UPLOADING:
                mTransferControl.finishSpinningAndProceed();
                mTransferControl.setText(res.getString(R.string.transfer_state_uploading));
                mWaitUntilOperationIsFinished = true;
                length = contentObject.getTransferLength();
                progress = contentObject.getTransferProgress();
                mTransferControl.setMax(length);
                mTransferControl.setProgress(progress);
                break;
            case UPLOAD_COMPLETE:
                mTransferControl.completeAndGone();
                displayAttachment(contentObject);
                break;
            default:
                mTransferControl.setVisibility(View.GONE);
                displayAttachment(contentObject);
                break;
        }
    }

    /**
     * Returns a new and fully configured View object containing the layout for a given message.
     *
     * @param message The given TalkClientMessage object
     * @return A new View object containing the message layout
     */
    public View getViewForMessage(TalkClientMessage message) {
        View view = createView();
        configureViewForMessage(view, message);
        return view;
    }

    /**
     * Reconfigures a given message layout from a given message.
     *
     * @param view    The message layout to reconfigure
     * @param message The given TalkClientMessage object
     * @return The fully reconfigured message layout
     */
    public View recycleViewForMessage(View view, TalkClientMessage message) {
        configureViewForMessage(view, message);
        return view;
    }
}
