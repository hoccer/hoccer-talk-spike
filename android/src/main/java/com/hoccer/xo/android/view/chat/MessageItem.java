package com.hoccer.xo.android.view.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import com.hoccer.xo.android.view.avatar.SimpleAvatarView;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferHandler;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferListener;
import com.hoccer.xo.android.view.chat.attachments.ChatItemType;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This class creates and configures layouts for incoming and outgoing messages.
 */
public class MessageItem implements AttachmentTransferListener {

    private final static Logger LOG = Logger.getLogger(MessageItem.class);

    private final XoClientDatabase mDatabase;

    protected Context mContext;
    protected TalkClientMessage mMessage;
    protected TextView mMessageText;
    protected SimpleAvatarView mSimpleAvatarView;
    protected TextView mContentDescription;
    protected XoTransfer mAttachment;
    protected RelativeLayout mAttachmentTransferContainer;
    protected AttachmentTransferHandler mAttachmentTransferHandler;
    protected LinearLayout mAttachmentContentContainer;
    protected RelativeLayout mMessageContainer;

    public MessageItem(Context context, TalkClientMessage message) {
        super();
        mContext = context;
        mDatabase = XoApplication.get().getClient().getDatabase();
        mMessage = message;

        mAttachment = mMessage.getAttachmentUpload();
        if (mAttachment == null) {
            mAttachment = mMessage.getAttachmentDownload();
        }
    }

    public TalkClientMessage getMessage() {
        return mMessage;
    }

    public void setMessage(TalkClientMessage message) {
        mMessage = message;
    }

    public XoTransfer getAttachment() {
        return mAttachment;
    }

    public View createAndUpdateView() {
        View view = createView();
        return updateView(view);
    }

    public View updateView(View view) {
        // if there is an old item attached to this view destroy it now
        MessageItem item = (MessageItem) view.getTag();
        if (item != null) {
            item.detachView();
        }

        configureViewForMessage(view);

        if (mAttachment != null) {
            configureAttachmentView(view);
        }

        // set item as tag for this view
        view.setTag(this);

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

    public void detachView() {
        if (mAttachment != null) {
            mAttachment.unregisterTransferListener(mAttachmentTransferHandler);
        }
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
        mSimpleAvatarView = (SimpleAvatarView) view.findViewById(R.id.view_avatar_simple);
        mMessageContainer = (RelativeLayout) view.findViewById(R.id.rl_message_container);
        TextView messageTime = (TextView) view.findViewById(R.id.tv_message_time);
        TextView messageText = (TextView) view.findViewById(R.id.tv_message_text);
        TextView messageContactInfo = (TextView) view.findViewById(R.id.tv_message_contact_info);
        TextView messageDeliveryInfo = (TextView) view.findViewById(R.id.tv_message_delivery_info);

        // Adjust layout for incoming / outgoing message
        setAvatar(mSimpleAvatarView, mMessage.getSenderContact());
        if (mMessage.isIncoming()) {
            if (mMessage.getConversationContact().isGroup()) {
                mSimpleAvatarView.setVisibility(View.VISIBLE);
            } else {
                mSimpleAvatarView.setVisibility(View.GONE);
            }
            updateIncomingMessageStatus(view);

            messageContactInfo.setVisibility(View.VISIBLE);
            messageContactInfo.setText(mMessage.getSenderContact().getNickname());
            messageContactInfo.setTextColor(messageContactInfo.getResources().getColor(android.R.color.secondary_text_dark));

            messageDeliveryInfo.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mMessageContainer.setBackground(getIncomingBackgroundDrawable());
            } else {
                mMessageContainer.setBackgroundDrawable(getIncomingBackgroundDrawable());
            }

            messageText.setTextColor(mContext.getResources().getColorStateList(R.color.message_incoming_text));
            messageText.setLinkTextColor(mContext.getResources().getColorStateList(R.color.message_incoming_text));

            updateLeftAndRightMargin(mMessageContainer, 10, 30);
        } else {
            mSimpleAvatarView.setVisibility(View.GONE);
            boolean isDeliveryFailed = isDeliveryFailed();
            updateOutgoingMessageStatus(view, isDeliveryFailed);

            messageContactInfo.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mMessageContainer.setBackground(getOutgoingBackgroundDrawable(isDeliveryFailed));
            } else {
                mMessageContainer.setBackgroundDrawable(getOutgoingBackgroundDrawable(isDeliveryFailed));
            }

            messageText.setTextColor(mContext.getResources().getColorStateList(R.color.message_outgoing_text));
            messageText.setLinkTextColor(mContext.getResources().getColorStateList(R.color.message_incoming_text));

            updateLeftAndRightMargin(mMessageContainer, 30, 10);
        }

        messageTime.setText(getMessageTimestamp(mMessage));
        messageText.setText(mMessage.getText());

        mMessageText = messageText;
        configureContextMenu(messageText);
    }

    private void updateLeftAndRightMargin(RelativeLayout messageContainer, int leftMargin, int rightMargin) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) messageContainer.getLayoutParams();
        float marginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, leftMargin, mContext.getResources().getDisplayMetrics());
        float marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rightMargin, mContext.getResources().getDisplayMetrics());
        layoutParams.leftMargin = (int) marginLeft;
        layoutParams.rightMargin = (int) marginRight;
        messageContainer.setLayoutParams(layoutParams);
    }

    private void updateIncomingMessageStatus(View view) {
        TextView deliveryInfo = (TextView) view.findViewById(R.id.tv_message_delivery_info);
        if (mMessage.getConversationContact().isGroup()) {
            deliveryInfo.setVisibility(View.GONE);
            return;
        }

        TalkDelivery incomingDelivery = mMessage.getDelivery();
        deliveryInfo.setVisibility(View.VISIBLE);

        if (incomingDelivery.isFailure()) {
            setMessageStatusText(deliveryInfo, stateStringForDelivery(incomingDelivery, view), R.color.message_delivery_failed);
        } else {
            deliveryInfo.setVisibility(View.GONE);
        }
    }

    private String stateStringForDelivery(TalkDelivery myDelivery, View view) {
        Resources res = view.getResources();

        if (myDelivery.isInState(TalkDelivery.STATE_NEW)) {
            return res.getString(R.string.message_pending_text);

        } else if (myDelivery.isInState(TalkDelivery.STATE_DELIVERING)) {
            return res.getString(R.string.message_sent_text);

        } else if (myDelivery.isDelivered()) {

            if (myDelivery.isAttachmentFailure()) {
                String mediaType = res.getString(getMediaTextResource());
                String text = res.getString(R.string.attachment_failed_text);
                return String.format(text, mediaType);
            } else if (myDelivery.isAttachmentPending()) {
                String mediaType = res.getString(getMediaTextResource());
                String text = res.getString(R.string.attachment_expects_text);
                return String.format(text, mediaType);
            } else {
                if (myDelivery.isSeen()) {
                    return res.getString(R.string.message_seen_text);
                } else if (!myDelivery.isPrivate()) {
                    return res.getString(R.string.message_unseen_text);
                } else {
                    return res.getString(R.string.message_privat_text);
                }
            }
        } else if (myDelivery.isFailure()) {
            if (myDelivery.isFailed()) {
                return res.getString(R.string.message_failed_text);
            } else if (myDelivery.isAborted()) {
                return res.getString(R.string.message_aborted_text);
            } else if (myDelivery.isRejected()) {
                return res.getString(R.string.message_rejected_text);
            }
        } else {
            throw new RuntimeException("unknown delivery state: " + myDelivery.getState());
        }
        return myDelivery.getState();
    }

    private void updateOutgoingMessageStatus(View view, boolean isDeliveryFailed) {
        TextView deliveryInfo = (TextView) view.findViewById(R.id.tv_message_delivery_info);
        if (mMessage.getConversationContact().isGroup()) {
            deliveryInfo.setVisibility(View.GONE);
            return;
        }

        TalkDelivery outgoingDelivery = mMessage.getDelivery();
        deliveryInfo.setVisibility(View.VISIBLE);

        int statusColor = isDeliveryFailed ? R.color.message_delivery_failed : R.color.primary;
        String statusText = stateStringForDelivery(outgoingDelivery, view);

        setMessageStatusText(deliveryInfo, statusText, statusColor);
    }

    private static void setMessageStatusText(TextView messageStatusLabel, String text, int colorId) {
        messageStatusLabel.setVisibility(View.VISIBLE);
        messageStatusLabel.setText(text);
        messageStatusLabel.setTextColor(messageStatusLabel.getResources().getColor(colorId));
    }

    public Drawable getOutgoingBackgroundDrawable(boolean isDeliveryFailed) {
        if (isDeliveryFailed) {
            return mContext.getResources().getDrawable(R.drawable.chat_bubble_error_outgoing);
        } else {
            return ColoredDrawable.create(R.drawable.chat_bubble_outgoing, R.color.message_outgoing_background);
        }
    }

    public Drawable getIncomingBackgroundDrawable() {
        String currentState = mMessage.getDelivery().getState();
        Drawable background;
        if (currentState != null) {
            if (TalkDelivery.isFailureState(currentState)) {
                background = mContext.getResources().getDrawable(R.drawable.chat_bubble_error_incoming);
            } else {
                background = ColoredDrawable.create(R.drawable.chat_bubble_incoming, R.color.message_incoming_background);
            }
        } else {
            background = ColoredDrawable.create(R.drawable.chat_bubble_incoming, R.color.message_incoming_background);
        }

        return background;
    }

    private static String getMessageTimestamp(TalkClientMessage message) {
        StringBuilder result = new StringBuilder();
        Date timeStamp = message.getTimestamp();
        String ts = timeStamp != null ? SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM).format(timeStamp) : "NULL";

        Date accepted = message.getDelivery().getTimeAccepted();
        String ac = accepted != null ? SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM).format(accepted) : "NULL";

        Date sent = message.getMessage().getTimeSent();
        String s = sent != null ? SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM).format(sent) : "NULL";

        if (timeStamp != null) {
            long timeStampDay = getTimeAtStartOfDay(timeStamp);
            long today = getTimeAtStartOfDay(new Date());
            long difference = Math.abs(today - timeStampDay);

            if (difference <= DateUtils.DAY_IN_MILLIS) {
                result.append(DateUtils.getRelativeTimeSpanString(timeStampDay, today, DateUtils.DAY_IN_MILLIS, 0));
            } else {
                result.append(SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(timeStamp));
            }

            result.append(' ');
            result.append(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(timeStamp));
        }


        //result = new StringBuilder();
        //result.append("T:"+ts+" A:"+ac+" S:"+s);


        return result.toString();
    }

    private static long getTimeAtStartOfDay(Date time) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    private void setAvatar(SimpleAvatarView avatarView, final TalkClientContact sendingContact) {
        avatarView.setContact(sendingContact);
        avatarView.setVisibility(View.VISIBLE);
        if (sendingContact != null) {
            avatarView.setOnClickListener(new View.OnClickListener() {
                final TalkClientContact contact = sendingContact;

                @Override
                public void onClick(View v) {
                    if (!contact.isSelf()) {
                        // TODO: reevaluate - might not work
                        ((BaseActivity) mContext).showContactProfile(contact);
                    }
                }
            });
        }
    }

    private void configureAttachmentView(View view) {
        RelativeLayout attachmentContainer = (RelativeLayout) view.findViewById(R.id.rl_attachment_container);
        attachmentContainer.setVisibility(View.VISIBLE);

        mAttachmentContentContainer = (LinearLayout) attachmentContainer.findViewById(R.id.ll_content_container);
        mAttachmentTransferContainer = (RelativeLayout) attachmentContainer.findViewById(R.id.rl_transfer);
        mContentDescription = (TextView) mAttachmentTransferContainer.findViewById(R.id.tv_content_description_text);

        if (isTransferCompleted()) {
            displayAttachment();
        } else {
            displayTransferControl();
        }

        mContentDescription.setText(getContentDescription(mAttachment));
    }

    private String getContentDescription(XoTransfer transfer) {
        String mediaTypeString = getMediaType(transfer);
        String sizeString = getHumanReadableSize(transfer);

        return mediaTypeString + sizeString;
    }

    private String getMediaType(XoTransfer transfer) {
        String mediaTypeString = "Unknown file";
        String mediaType = transfer.getMediaType();
        if (ContentMediaType.IMAGE.equals(mediaType)) {
            mediaTypeString = "Image";
        } else if (ContentMediaType.AUDIO.equals(mediaType)) {
            mediaTypeString = "Audio";
        } else if (ContentMediaType.VIDEO.equals(mediaType)) {
            mediaTypeString = "Video";
        } else if (ContentMediaType.VCARD.equals(mediaType)) {
            mediaTypeString = "Contact";
        } else if (ContentMediaType.LOCATION.equals(mediaType)) {
            mediaTypeString = "Location";
        } else if (ContentMediaType.FILE.equals(mediaType)) {
            mediaTypeString = "Data";
        }
        return mediaTypeString;
    }

    private String getHumanReadableSize(XoTransfer transfer) {
        String sizeString = "";
        if (transfer.getContentLength() > 0) {
            sizeString = " — " + humanReadableByteCount(transfer.getContentLength(), true);
        } else if (transfer.getTransferLength() > 0) {
            sizeString = " — " + humanReadableByteCount(transfer.getTransferLength(), true);
        }
        return sizeString;
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void displayTransferControl() {
        mAttachmentContentContainer.setVisibility(View.GONE);
        mAttachmentTransferContainer.setVisibility(View.VISIBLE);


        mAttachmentTransferHandler = new AttachmentTransferHandler(mAttachmentTransferContainer, mAttachment, this);
        mAttachment.registerTransferStateListener(mAttachmentTransferHandler);
    }

    protected void displayAttachment() {
        mAttachmentTransferContainer.setVisibility(View.GONE);
        mAttachmentContentContainer.setVisibility(View.VISIBLE);
        configureContextMenu(mAttachmentContentContainer);
    }

    protected boolean isTransferCompleted() {
        ContentState contentState = mAttachment.getContentState();
        return contentState == ContentState.UPLOAD_COMPLETE || contentState == ContentState.DOWNLOAD_COMPLETE;
    }

    private void configureContextMenu(View view) {
        final MessageItem messageItem = this;
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showPopupForMessageItem(messageItem, v);
                return true;
            }
        });
    }

    public void showPopupForMessageItem(final MessageItem messageItem, View messageItemView) {
        PopupMenu popup = new PopupMenu(mContext, messageItemView);
        popup.getMenuInflater().inflate(R.menu.popup_menu_messaging, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                popupItemSelected(item, messageItem);
                return true;
            }
        });
        popup.show();
    }

    private void popupItemSelected(MenuItem item, MessageItem messageItem) {
        switch (item.getItemId()) {
            case R.id.menu_copy_message:
                if (messageItem.getAttachment() != null && messageItem.getAttachment().isContentAvailable()) {
                    Clipboard.get().setContent(messageItem.getAttachment());
                } else {
                    putMessageTextInSystemClipboard(messageItem);
                }
                break;
            case R.id.menu_delete_message:
                XoApplication.get().getClient().deleteMessage(messageItem.getMessage());
                break;
        }
    }

    private void putMessageTextInSystemClipboard(MessageItem messageItem) {
        ClipboardManager clipboardText = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", messageItem.getText());
        clipboardText.setPrimaryClip(clip);
    }

    @Override
    public void onAttachmentTransferComplete(XoTransfer attachment) {
        displayAttachment();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageItem)) {
            return false;
        }

        MessageItem that = (MessageItem) o;

        return mMessage != null && that.mMessage != null && mMessage.equals(that.mMessage);
    }

    @Override
    public int hashCode() {
        return mMessage != null ? mMessage.hashCode() : 0;
    }

    public boolean isSeparator() {
        return TalkClientMessage.TYPE_SEPARATOR.equals(mMessage.getMessageId());
    }

    public String getText() {
        return mMessage.getText();
    }

    public int getConversationContactId() {
        return mMessage.getConversationContact().getClientContactId();
    }

    public int getMediaTextResource() {
        int stringResource;
        ChatItemType type = getType();
        switch (type) {
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
                LOG.error("No case statement for ChatItemType " + type);
                stringResource = R.string.message_state_default;
                break;
        }
        return stringResource;
    }

    public boolean isDeliveryFailed() {
        if (mMessage.getDelivery().isGroupDelivery()) {
            boolean isDeliveryFailed = false;
            try {
                List<TalkDelivery> deliveriesForMessage = mDatabase.getDeliveriesForMessage(mMessage);
                for (TalkDelivery delivery : deliveriesForMessage) {
                    if (delivery.isFailure()) {
                        isDeliveryFailed = true;
                        break;
                    }
                }
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
            return isDeliveryFailed;
        } else {
            return mMessage.getDelivery().isFailure();
        }
    }
}
