package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.UUID;

/**
 * Message objects represent messages including the
 * receiver-independent envelope
 * 
 * @author ingo
 */
@DatabaseTable(tableName="message")
public class TalkMessage {

    public static final String FIELD_MESSAGE_ID  = "messageId";
    public static final String FIELD_MESSAGE_TAG = "messageTag";
    public static final String FIELD_SENDER_ID   = "senderId";
    public static final String FIELD_BODY        = "body";
    public static final String FIELD_ATTACHMENT  = "attachment";
    public static final String FIELD_SHARED_KEY_ID  = "sharedKeyId";

    private String _id;

    @DatabaseField(generatedId = true)
    int dbMessageId;

    /** Server-assigned message id */
    @DatabaseField
	String messageId;

    /** Sender-assigned message tag/id */
    @DatabaseField
    String messageTag;

    /** Client id of the sender */
    @DatabaseField
	String senderId;

    /** Message salt */
    @DatabaseField
    String salt;

    /** Message body */
    @DatabaseField
	String body;

    /** Message attachment */
    @DatabaseField(width = 2048)
    String attachment;

    /** Attachment file id (for housekeeping) */
    @DatabaseField
    String attachmentFileId;

    /** Time the message was sent */
    @DatabaseField
    Date timeSent;

    /** Total number of deliveries for this message */
    @DatabaseField
    int numDeliveries;

    /** an id for the shared key the body and attachment was encrypted with */
    @DatabaseField
    String sharedKeyId;

    /** an salt for the id for the shared key the body and attachment was encrypted with */
    @DatabaseField
    String sharedKeyIdSalt;

    /** an optional hmac */
    @DatabaseField
    String hmac;

    /** an optional signature for the hmac*/
    @DatabaseField
    String signature;

    public TalkMessage() {
    }

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

    public String generateMessageTag() {
        this.messageTag = UUID.randomUUID().toString();
        return this.messageTag;
    }

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getAttachmentFileId() {
        return attachmentFileId;
    }

    public void setAttachmentFileId(String attachmentFileId) {
        this.attachmentFileId = attachmentFileId;
    }

    public Date getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(Date timeSent) {
        this.timeSent = timeSent;
    }

    public int getNumDeliveries() {
        return numDeliveries;
    }

    public void setNumDeliveries(int numDeliveries) {
        this.numDeliveries = numDeliveries;
    }

    public String getSharedKeyId() {
        return sharedKeyId;
    }

    public void setSharedKeyId(String sharedKeyId) {
        this.sharedKeyId = sharedKeyId;
    }

    public String getSharedKeyIdSalt() {
        return sharedKeyIdSalt;
    }

    public void setSharedKeyIdSalt(String sharedKeyIdSalt) {
        this.sharedKeyIdSalt = sharedKeyIdSalt;
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


}
