package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Message objects represent messages including the
 * receiver-independent envelope
 * 
 * @author ingo
 */
@DatabaseTable(tableName="message")
public class TalkMessage {

    public static String FIELD_MESSAGE_ID  = "messageId";
    public static String FIELD_MESSAGE_TAG = "messageTag";
    public static String FIELD_SENDER_ID   = "senderId";
    public static String FIELD_BODY        = "body";
    public static String FIELD_ATTACHMENT  = "attachment";

    private String _id;

    /** Server-assigned message id */
    @DatabaseField(id = true)
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
    @DatabaseField
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

}
