package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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
@DatabaseTable(tableName="delivery")
public class TalkDelivery {

    public static final String FIELD_MESSAGE_ID  = "messageId";
    public static final String FIELD_MESSAGE_TAG = "messageTag";
    public static final String FIELD_RECEIVER_ID = "receiverId";
    public static final String FIELD_STATE       = "state";

    public static final String STATE_NEW        = "new";
    public static final String STATE_DELIVERING = "delivering";
    public static final String STATE_DELIVERED  = "delivered";
    public static final String STATE_FAILED     = "failed";

    @DatabaseField
	String messageId;

    @DatabaseField
    String messageTag;

    @DatabaseField
	String receiverId;

    @DatabaseField
    String state;


    public TalkDelivery() {
        this.state = STATE_NEW;
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

    public String getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
