package com.hoccer.talk.client.model;

import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Set;

@DatabaseTable(tableName = "clientMessage")
public class TalkClientMessage {

    public static final String TYPE_SEPARATOR = "separator";

    public static final String TYPE_INCOMING = "incoming";
    public static final String TYPE_OUTGOING = "outgoing";

    private static final Logger LOG = Logger.getLogger(TalkClientMessage.class);

    @DatabaseField(generatedId = true)
    private int clientMessageId;

    @DatabaseField
    private String messageId;

    @DatabaseField(canBeNull = true)
    private String messageTag;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientContact conversationContact;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientContact senderContact;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkMessage message;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkDelivery delivery;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientUpload attachmentUpload;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private TalkClientDownload attachmentDownload;

    @DatabaseField(width = 2048)
    private String text;

    @DatabaseField
    private boolean deleted;

    @DatabaseField
    private boolean seen;

    @DatabaseField
    private Date timestamp;

    @DatabaseField(width = 128)
    private String hmac;

    @DatabaseField(width = 1024)
    private String signature;

    @DatabaseField
    private boolean inProgress;

    @DatabaseField
    private String direction;

    public TalkClientMessage() {
        this.timestamp = new Date();
    }

    public int getClientMessageId() {
        return clientMessageId;
    }

    public boolean isIncoming() {
        return TYPE_INCOMING.equals(direction);
    }

    public boolean isOutgoing() {
        return TYPE_OUTGOING.equals(direction);
    }

    @Nullable
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageTag() {
        return messageTag;
    }

    public void setMessageTag(String messageTag) {
        this.messageTag = messageTag;
    }

    public TalkClientContact getConversationContact() {
        return conversationContact;
    }

    public void setConversationContact(TalkClientContact conversationContact) {
        this.conversationContact = conversationContact;
    }

    public TalkClientContact getSenderContact() {
        return senderContact;
    }

    public void setSenderContact(TalkClientContact senderContact) {
        this.senderContact = senderContact;
    }

    public TalkMessage getMessage() {
        return message;
    }

    public void setMessage(TalkMessage message) {
        this.message = message;
    }

    public TalkDelivery getDelivery() {
        return delivery;
    }

    public void setOutgoingDelivery(TalkDelivery delivery) {
        this.delivery = delivery;
        this.direction = TYPE_OUTGOING;
    }

    public boolean hasAttachment() {
        return attachmentDownload != null || attachmentUpload != null;
    }

    public TalkClientUpload getAttachmentUpload() {
        return attachmentUpload;
    }

    public void setAttachmentUpload(TalkClientUpload attachmentUpload) {
        this.attachmentUpload = attachmentUpload;
    }

    public TalkClientDownload getAttachmentDownload() {
        return attachmentDownload;
    }

    public void setAttachmentDownload(TalkClientDownload attachmentDownload) {
        this.attachmentDownload = attachmentDownload;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSeen() {
        return seen;
    }

    public void markAsSeen() {
        this.seen = true;
    }

    public void setProgressState(boolean state) {
        this.inProgress = state;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void updateIncoming(TalkDelivery delivery, TalkMessage message) {
        if (TYPE_OUTGOING.equals(direction)) {
            LOG.warn("incoming update for outgoing message");
            return;
        }
        this.message = message;
        if (this.delivery == null) {
            this.delivery = delivery;
        } else {
            Set<String> fields = delivery.nonNullFields();
            this.delivery.updateWith(delivery, fields);
            if (message.getTimeSent() != null) {
                this.timestamp = message.getTimeSent();
            }
        }
        direction = TYPE_INCOMING;
    }

    public void updateIncoming(TalkDelivery delivery) {
        if (TYPE_OUTGOING.equals(direction)) {
            LOG.error("incoming incremental update for outgoing message");
            return;
        }

        if (delivery == null) {
            LOG.error("incremental update for not yet received incoming delivery");
            return;
        }

        Set<String> fields = delivery.nonNullFields();
        this.delivery.updateWith(delivery, fields);
        direction = TYPE_INCOMING;
    }

    public void updateOutgoing(TalkDelivery delivery) {
        if (TYPE_INCOMING.equals(direction)) {
            LOG.warn("outgoing update for incoming message");
            return;
        }
        if (delivery == null) {
            this.delivery = delivery;
        } else {
            Set<String> fields = delivery.nonNullFields();
            this.delivery.updateWith(delivery, fields);
        }
        direction = TYPE_OUTGOING;
    }
 /*
    private void updateDelivery(TalkDelivery currentDelivery, TalkDelivery newDelivery) {
        currentDelivery.setState(newDelivery.getState());
        currentDelivery.setSenderId(newDelivery.getSenderId());
        currentDelivery.setGroupId(newDelivery.getGroupId());
        currentDelivery.setReceiverId(newDelivery.getReceiverId());
        currentDelivery.setKeyId(newDelivery.getKeyId());
        currentDelivery.setKeyCiphertext(newDelivery.getKeyCiphertext());
        currentDelivery.setTimeAccepted(newDelivery.getTimeAccepted());
        currentDelivery.setTimeChanged(newDelivery.getTimeChanged());
        currentDelivery.setTimeUpdatedIn(newDelivery.getTimeUpdatedIn());
        currentDelivery.setTimeUpdatedOut(newDelivery.getTimeUpdatedOut());
    }
    */

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TalkClientMessage)) {
            return false;
        }

        TalkClientMessage message = (TalkClientMessage) o;

        return messageId != null && message.messageId != null && messageId.equals(message.messageId);
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }
}
