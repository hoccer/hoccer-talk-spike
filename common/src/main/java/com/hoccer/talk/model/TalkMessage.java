package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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

    private String _id;

    /** Server-assigned message id */
    @DatabaseField
	String messageId;

    /** Sender-assigned message tag/id */
    @DatabaseField
    String messageTag;

    /** Client id of the sender */
    @DatabaseField
	String senderId;

    /** Message body */
    @DatabaseField
	String body;

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

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
}
