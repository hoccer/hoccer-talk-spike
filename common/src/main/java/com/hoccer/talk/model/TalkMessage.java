package com.hoccer.talk.model;

/**
 * Message objects represent messages including the
 * receiver-independent envelope
 * 
 * @author ingo
 */
public class TalkMessage {
	
	String messageId;

	String senderId;
	
	String body;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
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
