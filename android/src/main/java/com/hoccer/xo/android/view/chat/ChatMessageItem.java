package com.hoccer.xo.android.view.chat;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.TypedValue;
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
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.ContentRegistry;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferControlView;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferHandler;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferListener;
import com.hoccer.xo.android.view.chat.attachments.ChatItemType;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.util.Date;

/**
 * This class creates and configures layouts for incoming and outgoing messages.
 */
public class ChatMessageItem implements AttachmentTransferListener {

    protected Logger LOG = Logger.getLogger(getClass());

    protected Context mContext;
    protected AttachmentTransferHandler mAttachmentTransferHandler;
    protected TalkClientMessage mMessage;
    protected IContentObject mContentObject;
    protected ContentRegistry mContentRegistry;

    protected TextView mMessageText;
    protected RelativeLayout mContentTransferProgress;
    protected TextView mContentDescription;
    protected RelativeLayout mAttachmentView;
    protected LinearLayout mContentWrapper;
    protected AttachmentTransferControlView mContentTransferControl;

    public ChatMessageItem(Context context, TalkClientMessage message) {
        super();
        mContext = context;
        mMessage = message;
        mContentRegistry = ContentRegistry.get(context);
    }

    public TalkClientMessage getMessage() {
        return mMessage;
    }

    public void setMessage(TalkClientMessage message) {
        mMessage = message;
    }

    public IContentObject getContent() {
        return mContentObject;
    }

    /**
     * Returns a new and fully configured View object containing the layout for a given message.
     *
     * @return A new View object containing the message layout
     */
    public View getViewForMessage() {
        View view = createView();
        configureViewForMessage(view);
        return view;
    }

    /**
     * Reconfigures a given message layout from a given message.
     *
     * @param view The message layout to reconfigure
     * @return The fully reconfigured message layout
     */
    public View recycleViewForMessage(View view) {
        configureViewForMessage(view);
        return view;
    }

    /**
     * Returns the type of message item defined in ChatItemType.
     * <p/>
     * Subtypes need to overwrite this method and return the appropriate ChatItemType.
     *
     * @return The ChatItemType of this message item
     */
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithText;
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
     * Subtypes will have to overwrite this method to enhance the configuration of the message
     * layout.
     *
     * @param view The given layout
     */
    protected void configureViewForMessage(View view) {
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.av_message_avatar);
        TextView messageTime = (TextView) view.findViewById(R.id.tv_message_time);
        TextView messageText = (TextView) view.findViewById(R.id.tv_message_text);
        TextView messageContactInfo = (TextView) view.findViewById(R.id.tv_message_contact_info);
        TextView messageDeliveryInfo = (TextView) view.findViewById(R.id.tv_message_delivery_info);

        // Adjust layout for incoming / outgoing message
        setAvatar(avatarView, mMessage.getSenderContact());
        if (mMessage.isIncoming()) {
            if (mMessage.getConversationContact().isGroup()) {
                avatarView.setVisibility(View.VISIBLE);
            } else {
                avatarView.setVisibility(View.GONE);
            }
            messageContactInfo.setVisibility(View.VISIBLE);
            messageContactInfo.setText(mMessage.getSenderContact().getNickname());
            messageContactInfo.setTextColor(messageContactInfo.getResources().getColor(android.R.color.secondary_text_dark));

            messageDeliveryInfo.setVisibility(View.GONE);

            messageText.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_grey));
            messageText.setTextColor(mContext.getResources().getColorStateList(android.R.color.black));
            messageText.setLinkTextColor(mContext.getResources().getColorStateList(android.R.color.black));

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) messageText.getLayoutParams();
            float marginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mContext.getResources().getDisplayMetrics());
            float marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, mContext.getResources().getDisplayMetrics());
            layoutParams.leftMargin = (int) marginLeft;
            layoutParams.rightMargin = (int) marginRight;
            messageText.setLayoutParams(layoutParams);

        } else {
            avatarView.setVisibility(View.GONE);
            updateMessageStatus(view);

            messageContactInfo.setVisibility(View.GONE);

            messageText.setBackgroundDrawable(mContext.getResources().getDrawable(getBackgroundResource()));
            messageText.setTextColor(mContext.getResources().getColorStateList(android.R.color.white));
            messageText.setLinkTextColor(mContext.getResources().getColorStateList(android.R.color.white));

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) messageText.getLayoutParams();
            float marginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, mContext.getResources().getDisplayMetrics());
            float marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mContext.getResources().getDisplayMetrics());
            layoutParams.leftMargin = (int) marginLeft;
            layoutParams.rightMargin = (int) marginRight;
            messageText.setLayoutParams(layoutParams);
        }

        messageTime.setText(getMessageTimestamp(mMessage));
        messageText.setText(mMessage.getText());

        mMessageText = messageText;
    }


    private void updateMessageStatus(View view) {
        TextView deliveryInfo = (TextView) view.findViewById(R.id.tv_message_delivery_info);
        if(mMessage.getConversationContact().isGroup() ) {
            deliveryInfo.setVisibility(View.GONE);
            return;
        }

        TalkDelivery outgoingDelivery = mMessage.getOutgoingDelivery();
        String currentState = outgoingDelivery.getState();
        String attachmentState = outgoingDelivery.getAttachmentState();
        deliveryInfo.setVisibility(View.VISIBLE);
        if (attachmentState != null && !attachmentState.equals(TalkDelivery.ATTACHMENT_STATE_NONE) &&
                (!attachmentState.equals(TalkDelivery.ATTACHMENT_STATE_RECEIVED)
                && !attachmentState.equals(TalkDelivery.ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED))) {

            String text = view.getResources().getString(R.string.attachment_expects_text);
            String mediaType = view.getResources().getString(getMediaTextResource());
            setMessageStatusText(deliveryInfo, String.format(text, mediaType));
        } else if ((currentState.equals(TalkDelivery.STATE_DELIVERED_SEEN)
                || currentState.equals(TalkDelivery.STATE_DELIVERED_SEEN_ACKNOWLEDGED))
                && !outgoingDelivery.isGroupDelivery()) {

            setMessageStatusText(deliveryInfo, view.getResources().getString(R.string.message_seen_text));
        } else if(currentState.equals(TalkDelivery.STATE_DELIVERED_UNSEEN) ||
                  currentState.equals(TalkDelivery.STATE_DELIVERED_UNSEEN_ACKNOWLEDGED)) {
            setMessageStatusText(deliveryInfo, view.getResources().getString(R.string.message_unseen_text));
        } else if(currentState.equals(TalkDelivery.STATE_DELIVERED_PRIVATE) ||
                  currentState.equals(TalkDelivery.STATE_DELIVERED_PRIVATE_ACKNOWLEDGED)) {
            setMessageStatusText(deliveryInfo, view.getResources().getString(R.string.message_privat_text));
        } else {
            deliveryInfo.setVisibility(View.GONE);
        }
    }

    private void setMessageStatusText(TextView messageStatusLabel, String text) {
        messageStatusLabel.setVisibility(View.VISIBLE);
        messageStatusLabel.setText(text);
        messageStatusLabel.setTextColor(messageStatusLabel.getResources().getColor(R.color.xo_app_main_color));
    }

    public int getBackgroundResource() {
        String currentState = mMessage.getOutgoingDelivery().getState();
        if(currentState == null) {
            return R.drawable.bubble_light_green;
        }
        if (currentState.equals(TalkDelivery.STATE_DELIVERING)) {
            return R.drawable.bubble_light_green;
        } else if(currentState.equals(TalkDelivery.STATE_ABORTED) || currentState.equals(TalkDelivery.STATE_ABORTED_ACKNOWLEDGED)) {
            return R.drawable.bubble_red;
        } else if(currentState.equals(TalkDelivery.STATE_FAILED) || currentState.equals(TalkDelivery.STATE_FAILED_ACKNOWLEDGED)) {
            return R.drawable.bubble_red;
        }
        return R.drawable.bubble_green;
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
     * <p/>
     * Subtypes will have to call this method to trigger the configuration of the attachment layout.
     *
     * @param view The chat message item's view to configure
     */
    protected void configureAttachmentViewForMessage(View view) {

        mAttachmentView = (RelativeLayout) view.findViewById(R.id.rl_message_attachment);

        // add content view
        if (mAttachmentView.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View attachmentWrapper = inflater.inflate(R.layout.view_attachment_wrapper, null);
            mAttachmentView.addView(attachmentWrapper);
        }
        mAttachmentView.setVisibility(View.VISIBLE);

        mContentWrapper = (LinearLayout) mAttachmentView.findViewById(R.id.ll_content_wrapper);

        // adjust layout for incoming / outgoing attachment
        if (mMessage.isIncoming()) {
            mAttachmentView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_grey));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mAttachmentView.getLayoutParams();
            float marginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mContext.getResources().getDisplayMetrics());
            float marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, mContext.getResources().getDisplayMetrics());
            layoutParams.leftMargin = (int) marginLeft;
            layoutParams.rightMargin = (int) marginRight;
            mAttachmentView.setLayoutParams(layoutParams);
        } else {
            mAttachmentView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.bubble_green));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mAttachmentView.getLayoutParams();
            float marginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, mContext.getResources().getDisplayMetrics());
            float marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, mContext.getResources().getDisplayMetrics());
            layoutParams.leftMargin = (int) marginLeft;
            layoutParams.rightMargin = (int) marginRight;
            mAttachmentView.setLayoutParams(layoutParams);
        }

        // configure transfer progress view
        mContentTransferProgress = (RelativeLayout) mAttachmentView.findViewById(R.id.rl_content_transfer_progress);
        mContentTransferControl = (AttachmentTransferControlView) mContentTransferProgress.findViewById(R.id.atcv_content_transfer_control);
        mContentDescription = (TextView) mContentTransferProgress.findViewById(R.id.tv_content_description_text);
        IContentObject contentObject = mMessage.getAttachmentUpload();
        if (contentObject == null) {
            contentObject = mMessage.getAttachmentDownload();
        }
        mContentObject = contentObject;

        mContentDescription.setText(mContentRegistry.getContentDescription(mContentObject));
        if (shouldDisplayTransferControl(getTransferState(contentObject))) {
            mContentWrapper.setVisibility(View.GONE);
            mContentTransferProgress.setVisibility(View.VISIBLE);

            // create handler for a pending attachment transfer
            mAttachmentTransferHandler = new AttachmentTransferHandler(mContentTransferControl, contentObject, this);
            XoApplication.getXoClient().registerTransferListener(mAttachmentTransferHandler);
            mContentTransferControl.setOnClickListener(new AttachmentTransferHandler(mContentTransferControl, contentObject, this));

        } else {
            mContentTransferControl.setOnClickListener(null);
            displayAttachment(contentObject);
        }

        // hide message text field when empty - there is still an attachment to display
        if (mMessage.getText() == null || mMessage.getText().isEmpty()) {
            mMessageText.setVisibility(View.GONE);
        } else {
            mMessageText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Configures the attachment view for a given message / attachment.
     * <p/>
     * Subtypes will have to overwrite this method to configure the attachment layout.
     *
     * @param contentObject The IContentObject to display
     */
    protected void displayAttachment(IContentObject contentObject) {
        mContentTransferProgress.setVisibility(View.GONE);
        mContentWrapper.setVisibility(View.VISIBLE);
        configureContextMenu();
    }

    /**
     * Returns true when the transfer (upload or download) of the attachment is not completed.
     *
     * @param state The current state of the content object
     * @return true if the transfer control should be displayed for a incomplete transfer
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

    private void configureContextMenu() {
        final ChatMessageItem messageItem = this;
        mContentWrapper.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                XoActivity activity = (XoActivity)mContext;
                activity.showPopupForMessageItem(messageItem, v);
                return true;
            }
        });
    }


    @Override
    public void onAttachmentTransferComplete(IContentObject contentObject) {
        displayAttachment(contentObject);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessageItem)) return false;

        ChatMessageItem that = (ChatMessageItem) o;

        if(mMessage != null && that.getMessage() != null) {
            return mMessage.equals(that.getMessage());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return mMessage != null ? mMessage.hashCode() : 0;
    }

    public boolean isSeparator() {
        return mMessage.getMessageId() != null &&  mMessage.getMessageId().equals("SEPARATOR");
    }

    public String getText() {
        return mMessage.getText();
    }

    public int getConversationContactId() {
        return mMessage.getConversationContact().getClientContactId();
    }

    public int getMediaTextResource() {
        int stringResource = -1;
        ChatItemType type = getType();
        switch(type) {
            case ChatItemWithImage:
                stringResource = R.string.message_state_image;
                break;
            case ChatItemWithVideo:
                stringResource = R.string.message_state_video;
                break;
            case ChatItemWithAudio:
                stringResource = R.string.message_state_audio;
                break;
            case ChatItemWithData:
                stringResource = R.string.message_state_data;
                break;
            case ChatItemWithContact:
                stringResource = R.string.message_state_contact;
                break;
            case ChatItemWithLocation:
                stringResource = R.string.message_state_location;
                break;
            default:
                LOG.error("No case statement for ChatItemType " + type.toString());
                stringResource = R.string.message_state_default;
                break;
        }
        return stringResource;
    }
}
