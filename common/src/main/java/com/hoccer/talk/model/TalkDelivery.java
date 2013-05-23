package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

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

    public static final String FIELD_MESSAGE_ID     = "messageId";
    public static final String FIELD_MESSAGE_TAG    = "messageTag";
    public static final String FIELD_RECEIVER_ID    = "receiverId";
    public static final String FIELD_STATE          = "state";
    public static final String FIELD_KEY_ID         = "keyId";
    public static final String FIELD_KEY_CIPHERTEXT = "keyCiphertext";

    public static final String STATE_NEW        = "new";
    public static final String STATE_DELIVERING = "delivering";
    public static final String STATE_DELIVERED  = "delivered";
    public static final String STATE_CONFIRMED  = "confirmed";
    public static final String STATE_FAILED     = "failed";
    public static final String STATE_ABORTED    = "aborted";

    private String _id;

    @DatabaseField
	String messageId;

    @DatabaseField
    String messageTag;

    @DatabaseField
    String senderId;

    @DatabaseField
	String receiverId;

    @DatabaseField
    String groupId;

    @DatabaseField
    String state;

    @DatabaseField
    String keyId;

    @DatabaseField
    String keyCiphertext;

    @DatabaseField
    Date timeAccepted;

    @DatabaseField
    Date timeChanged;

    @DatabaseField
    Date timeUpdatedOut;

    @DatabaseField
    Date timeUpdatedIn;

    public TalkDelivery() {
        this.state = STATE_NEW;
        this.timeAccepted = new Date(0);
        this.timeChanged = new Date(0);
        this.timeUpdatedIn = new Date(0);
        this.timeUpdatedOut = new Date(0);
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

    public String getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyCiphertext() {
        return keyCiphertext;
    }

    public void setKeyCiphertext(String keyCiphertext) {
        this.keyCiphertext = keyCiphertext;
    }

    public Date getTimeAccepted() {
        return timeAccepted;
    }

    public void setTimeAccepted(Date timeAccepted) {
        this.timeAccepted = timeAccepted;
    }

    public Date getTimeChanged() {
        return timeChanged;
    }

    public void setTimeChanged(Date timeChanged) {
        this.timeChanged = timeChanged;
    }

    public Date getTimeUpdatedOut() {
        return timeUpdatedOut;
    }

    public void setTimeUpdatedOut(Date timeUpdatedOut) {
        this.timeUpdatedOut = timeUpdatedOut;
    }

    public Date getTimeUpdatedIn() {
        return timeUpdatedIn;
    }

    public void setTimeUpdatedIn(Date timeUpdatedIn) {
        this.timeUpdatedIn = timeUpdatedIn;
    }
}
