package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    public static final String FIELD_DELIVERY_ID      = "deliveryId";
    public static final String FIELD_MESSAGE_ID       = "messageId";
    public static final String FIELD_MESSAGE_TAG      = "messageTag";
    public static final String FIELD_SENDER_ID        = "senderId";
    public static final String FIELD_RECEIVER_ID      = "receiverId";
    public static final String FIELD_GROUP_ID         = "groupId";
    public static final String FIELD_STATE            = "state";
    public static final String FIELD_KEY_ID           = "keyId";
    public static final String FIELD_KEY_CIPHERTEXT   = "keyCiphertext";
    public static final String FIELD_TIME_ACCEPTED    = "timeAccepted";
    public static final String FIELD_TIME_CHANGED     = "timeChanged";
    public static final String FIELD_TIME_UPDATED_OUT = "timeUpdatedOut";
    public static final String FIELD_TIME_UPDATED_IN  = "timeUpdatedIn";

    public static final String STATE_NEW        = "new";
    public static final String STATE_DELIVERING = "delivering";
    public static final String STATE_DELIVERED  = "delivered";
    public static final String STATE_CONFIRMED  = "confirmed";
    public static final String STATE_FAILED     = "failed";
    public static final String STATE_ABORTED    = "aborted";

    public static boolean isValidState(String state) {
        if(state != null) {
            if(state.equals(STATE_NEW)
                    || state.equals(STATE_DELIVERING)
                    || state.equals(STATE_DELIVERED)
                    || state.equals(STATE_CONFIRMED)
                    || state.equals(STATE_FAILED)
                    || state.equals(STATE_ABORTED)) {
                return true;
            }
        }
        return false;
    }

    /** unique object ID for the database, never transfered */
    private String _id;

    /** another unique object ID for the database, never transfered */
    @DatabaseField(columnName = FIELD_DELIVERY_ID, generatedId = true)
    private int deliveryId;

    /** a server generated UUID identifying the message globally within the system */
    @DatabaseField(columnName = FIELD_MESSAGE_ID)
	String messageId;

    /** a sender generated UUID identifying the message to the sending client */
    @DatabaseField(columnName = FIELD_MESSAGE_TAG)
    String messageTag;

    /** a UUID identifying the sending client */
    @DatabaseField(columnName = FIELD_SENDER_ID)
    String senderId;

    /** a UUID identifying the receiving client */
    @DatabaseField(columnName = FIELD_RECEIVER_ID)
	String receiverId;

    /** an optional UUID identifying the communication group */
    @DatabaseField(columnName = FIELD_GROUP_ID, canBeNull = true)
    String groupId;

    /** the delivery state, can be "new","delivering","delivered","confirmed","failed","aborted";     */
    @DatabaseField(columnName = FIELD_STATE)
    String state;

    /** an id for the public key the keyCiphertext was encrypted with, typically a lower cased hex string the first 8 bytes of an SHA256-hash of the PKCS1 encoded public key, e.g. "83edb9ee04d8e372" */
    @DatabaseField(columnName = FIELD_KEY_ID)
    String keyId;

    /** the public key encrypted cipherText of the shared symmetric (e.g. AES) key the message body and attachment is encrypted with, b64-encoded */
    @DatabaseField(columnName = FIELD_KEY_CIPHERTEXT)
    String keyCiphertext;

    /** the server generated time stamp of the point in the message has been accepted by the server; this field denotes the official time ordering of all messages in a chat */
    @DatabaseField(columnName = FIELD_TIME_ACCEPTED)
    Date timeAccepted;

    /** the server generated time stamp with the last time the delivery state has changed */
    @DatabaseField(columnName = FIELD_TIME_CHANGED, canBeNull = true)
    Date timeChanged;

    /** the server generated time stamp with the last time an outgoingDelivery-Notification has been sent from the server to the  */
    @DatabaseField(columnName = FIELD_TIME_UPDATED_OUT, canBeNull = true)
    Date timeUpdatedOut;

    @DatabaseField(columnName = FIELD_TIME_UPDATED_IN, canBeNull = true)
    Date timeUpdatedIn;

    public TalkDelivery() {
        this.state = STATE_NEW;
        this.timeAccepted = new Date(0);
        this.timeChanged = new Date(0);
        this.timeUpdatedIn = new Date(0);
        this.timeUpdatedOut = new Date(0);
    }

    @JsonIgnore
    public boolean isFinished() {
        return state.equals(STATE_ABORTED) || state.equals(STATE_FAILED) || state.equals(STATE_CONFIRMED);
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
        // TODO: validate state (e.g. with isValidState)
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
