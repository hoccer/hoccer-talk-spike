package com.hoccer.talk.model;

/**
 * Message objects represent messages including the
 * receiver-independent envelope
 * 
 * @author ingo
 */
public class TalkMessage {

    /** Server-assigned message id */
	String messageId;

    /** Sender-assigned message tag/id */
    String messageTag;

    /** Client id of the sender */
	String senderId;

    /** Message body */
	String body;

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
