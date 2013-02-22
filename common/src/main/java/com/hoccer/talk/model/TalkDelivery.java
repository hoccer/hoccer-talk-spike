package com.hoccer.talk.model;

/**
 * Delivery objects represent the receiver-dependent
 * envelope of a given message and contain
 * receiver-dependent delivery state
 * 
 * - saved in the database
 * - manipulated by delivery logic
 * - used in RPC for requesting delivery
 * - used in RPC for reflecting delivery state
 * 
 * @author ingo
 */
public class TalkDelivery {

	String messageId;
	
	String receiverId;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}
	
}
