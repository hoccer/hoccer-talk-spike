package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Delivery objects represent the receiver-dependent
 * envelope of a given message and contain
 * receiver-dependent delivery state
 * <p/>
 * - saved in the database
 * - manipulated by delivery logic
 * - used in RPC for requesting delivery
 * - used in RPC for reflecting delivery state
 *
 * @author ingo
 */
@DatabaseTable(tableName = "delivery")
public class TalkDelivery {

    // the fields
    public static final String FIELD_DELIVERY_ID = "deliveryId";
    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_MESSAGE_TAG = "messageTag";
    public static final String FIELD_SENDER_ID = "senderId";
    public static final String FIELD_RECEIVER_ID = "receiverId";
    public static final String FIELD_GROUP_ID = "groupId";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_KEY_ID = "keyId";
    public static final String FIELD_KEY_CIPHERTEXT = "keyCiphertext";
    public static final String FIELD_TIME_ACCEPTED = "timeAccepted";
    public static final String FIELD_TIME_CHANGED = "timeChanged";
    public static final String FIELD_TIME_UPDATED_OUT = "timeUpdatedOut";
    public static final String FIELD_TIME_UPDATED_IN = "timeUpdatedIn";
    public static final String FIELD_ATTACHMENT_STATE = "attachmentState";
    public static final String FIELD_TIME_ATTACHMENT_RECEIVED = "timeAttachmentReceived";
    public static final String FIELD_REASON = "reason";

    public static final String[] REQUIRED_UPDATE_FIELDS = {FIELD_DELIVERY_ID, FIELD_MESSAGE_ID, FIELD_MESSAGE_TAG,
            FIELD_SENDER_ID, FIELD_RECEIVER_ID, FIELD_GROUP_ID, FIELD_STATE, FIELD_TIME_ACCEPTED, FIELD_TIME_CHANGED,
            FIELD_ATTACHMENT_STATE, FIELD_TIME_ATTACHMENT_RECEIVED, FIELD_REASON
    };
    public static final Set<String> REQUIRED_UPDATE_FIELDS_SET = new HashSet<String>(Arrays.asList(REQUIRED_UPDATE_FIELDS));

    /* For migration:
    On Client and Server, change old state -> new state:
    confirmed -> deliveredAcknowledged
    aborted -> abortedAcknowledged
    failed -> failedAcknowledged

    Attachment states:
     On Server:
    - normal states -> ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED
    - retries exhausted ->  ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED or ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED
     */

    // the delivery states
    public static final String STATE_NEW = "new";
    public static final String STATE_DELIVERING = "delivering";
    public static final String STATE_DELIVERED = "delivered";
    public static final String STATE_DELIVERED_ACKNOWLEDGED = "deliveredAcknowledged";
    public static final String STATE_FAILED = "failed";
    public static final String STATE_ABORTED = "aborted";
    public static final String STATE_REJECTED = "rejected";
    public static final String STATE_FAILED_ACKNOWLEDGED = "failedAcknowledged";
    public static final String STATE_ABORTED_ACKNOWLEDGED = "abortedAcknowledged";
    public static final String STATE_REJECTED_ACKNOWLEDGED = "rejectedAcknowledged";

    public static final String[] ALL_STATES = {STATE_NEW, STATE_DELIVERING, STATE_DELIVERED,
            STATE_DELIVERED_ACKNOWLEDGED, STATE_FAILED, STATE_ABORTED, STATE_REJECTED, STATE_FAILED_ACKNOWLEDGED, STATE_ABORTED_ACKNOWLEDGED,
            STATE_REJECTED_ACKNOWLEDGED};
    public static final Set<String> ALL_STATES_SET = new HashSet<String>(Arrays.asList(ALL_STATES));

    public static final String[] PRE_FINAL_STATES = {STATE_DELIVERED, STATE_FAILED, STATE_ABORTED, STATE_REJECTED};
    public static final Set<String> PRE_FINAL_STATES_SET = new HashSet<String>(Arrays.asList(PRE_FINAL_STATES));

    public static final String[] FINAL_STATES = {STATE_DELIVERED_ACKNOWLEDGED, STATE_FAILED_ACKNOWLEDGED, STATE_ABORTED_ACKNOWLEDGED, STATE_REJECTED_ACKNOWLEDGED};
    public static final Set<String> FINAL_STATES_SET = new HashSet<String>(Arrays.asList(FINAL_STATES));

    // the attachment delivery states
    public static final String ATTACHMENT_STATE_NONE = "none";
    public static final String ATTACHMENT_STATE_NEW = "new";
    public static final String ATTACHMENT_STATE_UPLOADING = "uploading";
    public static final String ATTACHMENT_STATE_UPLOAD_PAUSED = "paused";
    public static final String ATTACHMENT_STATE_UPLOADED = "uploaded";
    public static final String ATTACHMENT_STATE_RECEIVED = "received";
    public static final String ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED = "receivedAcknowledged";
    public static final String ATTACHMENT_STATE_UPLOAD_FAILED = "uploadFailed";
    public static final String ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED = "uploadFailedAcknowledged";
    public static final String ATTACHMENT_STATE_UPLOAD_ABORTED = "uploadAborted";
    public static final String ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED = "uploadAbortedAcknowledged";
    public static final String ATTACHMENT_STATE_DOWNLOAD_FAILED = "downloadFailed";
    public static final String ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED = "downloadFailedAcknowledged";
    public static final String ATTACHMENT_STATE_DOWNLOAD_ABORTED = "downloadAborted";
    public static final String ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED = "downloadAbortedAcknowledged";

    public static final String[] ALL_ATTACHMENT_STATES = {ATTACHMENT_STATE_NONE,ATTACHMENT_STATE_NEW,ATTACHMENT_STATE_UPLOADING,
            ATTACHMENT_STATE_UPLOAD_PAUSED,ATTACHMENT_STATE_UPLOADED,ATTACHMENT_STATE_RECEIVED, ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_FAILED, ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED,ATTACHMENT_STATE_UPLOAD_ABORTED,
            ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED,ATTACHMENT_STATE_DOWNLOAD_FAILED, ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_ABORTED, ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED
    };
    public static final Set<String> ALL_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(ALL_ATTACHMENT_STATES));

    public static final String[] PRE_FINAL_ATTACHMENT_STATES = {ATTACHMENT_STATE_RECEIVED, ATTACHMENT_STATE_UPLOAD_FAILED,
            ATTACHMENT_STATE_UPLOAD_ABORTED,ATTACHMENT_STATE_DOWNLOAD_FAILED, ATTACHMENT_STATE_DOWNLOAD_ABORTED
    };
    public static final Set<String> PRE_FINAL_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(PRE_FINAL_ATTACHMENT_STATES));

    public static final String[] FINAL_ATTACHMENT_STATES = {ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED, ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED, ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED, ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED
    };
    public static final Set<String> FINAL_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(FINAL_ATTACHMENT_STATES));


    /* The delivery State logic has the following logic:
    - states get advanced by subsequent rpc-calls from sender and receiver
    - there are final states that are suffixed with "confirmed"
    - once a delivery is in a confirmed state (both state and attachmentState are in a final state),
     it will no longer be sent out to a client and can be deleted by server
    - end states can be reached either by a call from the sender or receiver
    - when a party has initiated a call that puts the delivery into a pre-final state like "received" or "delivered" or "aborted",
    the counterparty is responsible to acknowledge the pre-final state, which will advance the delivery into a confirmed end-state
     */


    public static boolean isValidState(String state) {
        return ALL_STATES_SET.contains(state);
    }
    public static boolean isFinalState(String state) {
        return FINAL_STATES_SET.contains(state);
    }

    @JsonIgnore
    public boolean nextStateAllowed(String nextState) {
        if (!isValidState(state)) return true;
        if (!isValidState(nextState)) return false;
        if (state.equals(nextState)) return true;
        if (isFinalState(state)) return false;
        if (state.equals(STATE_NEW)) return true;
        if (state.equals(STATE_DELIVERING)) {
            return !nextState.equals(STATE_NEW);
        }
        if (state.equals(STATE_DELIVERED)) {
            return !nextState.equals(STATE_NEW) && !nextState.equals(STATE_DELIVERING);
        }
        if (state.equals(STATE_FAILED)) {
            return nextState.equals(STATE_FAILED_ACKNOWLEDGED);
        }
        if (state.equals(STATE_ABORTED)) {
            return nextState.equals(STATE_ABORTED_ACKNOWLEDGED);
        }
        if (state.equals(STATE_REJECTED)) {
            return nextState.equals(STATE_REJECTED_ACKNOWLEDGED);
        }
        throw new RuntimeException("Internal Logic failure (nextStateAllowed)");
    }

    public static boolean isValidAttachmentState(String state) {
        return ALL_ATTACHMENT_STATES_SET.contains(state);
    }
    public static boolean isFinalAttachmentState(String state) {
        return FINAL_ATTACHMENT_STATES_SET.contains(state);
    }

    // returns true if nextState is a valid state and there are one or more state transition that lead form the current state to nextSate
    @JsonIgnore
    public boolean nextAttachmentStateAllowed(String nextState) {
        if (!isValidAttachmentState(attachmentState)) return true;
        if (!isValidAttachmentState(nextState)) return false;
        if (attachmentState.equals(nextState)) return true;
        if (attachmentState.equals(ATTACHMENT_STATE_NONE)) return false;
        if (nextState.equals(ATTACHMENT_STATE_NONE)) return false;
        if (attachmentState.equals(ATTACHMENT_STATE_NEW)) return true;

        if (attachmentState.equals(ATTACHMENT_STATE_UPLOADING)) {
            return !nextState.equals(ATTACHMENT_STATE_NEW);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_UPLOAD_PAUSED)) {
            return !nextState.equals(ATTACHMENT_STATE_NEW);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_UPLOADED)) {
            return !nextState.equals(ATTACHMENT_STATE_NEW) && !nextState.equals(ATTACHMENT_STATE_UPLOADING);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_RECEIVED)) {
            return nextState.equals(ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_UPLOAD_FAILED)) {
            return nextState.equals(ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_DOWNLOAD_FAILED)) {
            return nextState.equals(ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_UPLOAD_ABORTED)) {
            return nextState.equals(ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED);
        }
        if (attachmentState.equals(ATTACHMENT_STATE_DOWNLOAD_ABORTED)) {
            return nextState.equals(ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED);
        }
        throw new RuntimeException("Internal Logic failure (nextAttachmentStateAllowed)");
    }

    @JsonIgnore
    public boolean hasAttachment() {
        return attachmentState != null && !ATTACHMENT_STATE_NONE.equals(attachmentState);
    }


        boolean isInFinalState() {
        return isFinalState(state) && isFinalAttachmentState(attachmentState);
    }


    /**
     * unique object ID for the database, never transfered
     */
    private String _id;

    /**
     * another unique object ID for the database, never transfered
     */
    @DatabaseField(columnName = FIELD_DELIVERY_ID, generatedId = true)
    private int deliveryId;

    /**
     * a server generated UUID identifying the message globally within the system
     */
    @DatabaseField(columnName = FIELD_MESSAGE_ID)
    String messageId;

    /**
     * a sender generated UUID identifying the message to the sending client
     */
    @DatabaseField(columnName = FIELD_MESSAGE_TAG)
    String messageTag;

    /**
     * a UUID identifying the sending client
     */
    @DatabaseField(columnName = FIELD_SENDER_ID)
    String senderId;

    /**
     * a UUID identifying the receiving client
     */
    @DatabaseField(columnName = FIELD_RECEIVER_ID)
    String receiverId;

    /**
     * an optional UUID identifying the communication group
     */
    @DatabaseField(columnName = FIELD_GROUP_ID)
    String groupId;

    /**
     * the delivery state, can be "new","delivering","delivered","confirmed","failed","aborted";
     */
    @DatabaseField(columnName = FIELD_STATE)
    String state;

    /**
     * an id for the public key the keyCiphertext was encrypted with, typically a lower cased hex string the first 8 bytes of an SHA256-hash of the PKCS1 encoded public key, e.g. "83edb9ee04d8e372"
     */
    @DatabaseField(columnName = FIELD_KEY_ID)
    String keyId;

    /**
     * the public key encrypted cipherText of the shared symmetric (e.g. AES) key the message body and attachment is encrypted with, b64-encoded
     */
    @DatabaseField(columnName = FIELD_KEY_CIPHERTEXT, width = 1024)
    String keyCiphertext;

    /**
     * the server generated time stamp of the point in the message has been accepted by the server; this field denotes the official time ordering of all messages in a chat
     */
    @DatabaseField(columnName = FIELD_TIME_ACCEPTED)
    Date timeAccepted;

    /**
     * the server generated time stamp with the last time the delivery state has changed
     */
    @DatabaseField(columnName = FIELD_TIME_CHANGED, canBeNull = true)
    Date timeChanged;

    /**
     * the server generated time stamp with the last time an outgoingDelivery-Notification has been sent from the server to the
     */
    @DatabaseField(columnName = FIELD_TIME_UPDATED_OUT, canBeNull = true)
    Date timeUpdatedOut;

    @DatabaseField(columnName = FIELD_TIME_UPDATED_IN, canBeNull = true)
    Date timeUpdatedIn;

    @DatabaseField(columnName = FIELD_TIME_ATTACHMENT_RECEIVED, canBeNull = true)
    Date timeAttachmentReceived;

    @DatabaseField(columnName = FIELD_ATTACHMENT_STATE, canBeNull = true)
    String attachmentState;

    // may contain a reason for rejection, failure or abortion
    @DatabaseField(columnName = FIELD_REASON, canBeNull = true)
    String reason;

    public TalkDelivery() {
    }

    public TalkDelivery(boolean init) {
        if (init) this.initialize();
    }

    @JsonIgnore
    public void initialize() {
        this.state = STATE_NEW;
        this.ensureDates();
    }

    @JsonIgnore
    public void ensureDates() {
        if (this.timeAccepted == null) this.timeAccepted = new Date(0);
        if (this.timeChanged == null) this.timeChanged = new Date(0);
        if (this.timeUpdatedIn == null) this.timeUpdatedIn = new Date(0);
        if (this.timeUpdatedOut == null) this.timeUpdatedOut = new Date(0);
    }


    @JsonIgnore
    public boolean isFinished() {
        return state.equals(STATE_ABORTED) || state.equals(STATE_FAILED) || state.equals(STATE_DELIVERED_ACKNOWLEDGED);
    }

    @JsonIgnore
    public boolean isClientDelivery() {
        return receiverId != null && groupId == null;
    }

    @JsonIgnore
    public boolean isGroupDelivery() {
        return groupId != null && receiverId == null;
    }

    @JsonIgnore
    public boolean hasValidRecipient() {
        return isClientDelivery() || isGroupDelivery();
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

    public Date getTimeAttachmentReceived() {
        return timeAttachmentReceived;
    }

    public void setTimeAttachmentReceived(Date timeAttachmentReceived) {
        this.timeAttachmentReceived = timeAttachmentReceived;
    }

    public String getAttachmentState() {
        return attachmentState;
    }

    public void setAttachmentState(String attachmentState) {
        this.attachmentState = attachmentState;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonIgnore
    public void updateWith(TalkDelivery delivery) {
        this.messageId = delivery.getMessageId();
        this.messageTag = delivery.getMessageTag();
        this.senderId = delivery.getSenderId();
        this.receiverId = delivery.getReceiverId();
        this.groupId = delivery.getGroupId();
        this.state = delivery.getState();
        this.keyId = delivery.getKeyId();
        this.keyCiphertext = delivery.getKeyCiphertext();
        this.timeAccepted = delivery.getTimeAccepted();
        this.timeChanged = delivery.getTimeChanged();
        this.timeUpdatedOut = delivery.getTimeUpdatedOut();
        this.timeUpdatedIn = delivery.getTimeUpdatedIn();
        this.timeAttachmentReceived = delivery.getTimeAttachmentReceived();
        this.attachmentState = delivery.getAttachmentState();
    }

    @JsonIgnore
    public Set<String> nonNullFields() {
        Set<String> result = new HashSet<String>();
        if (this.messageId != null) {
            result.add(TalkDelivery.FIELD_MESSAGE_ID);
        }
        if (this.messageTag != null) {
            result.add(TalkDelivery.FIELD_MESSAGE_TAG);
        }
        if (this.senderId != null) {
            result.add(TalkDelivery.FIELD_SENDER_ID);
        }
        if (this.receiverId != null) {
            result.add(TalkDelivery.FIELD_RECEIVER_ID);
        }
        if (this.groupId != null) {
            result.add(TalkDelivery.FIELD_GROUP_ID);
        }
        if (this.state != null) {
            result.add(TalkDelivery.FIELD_STATE);
        }
        if (this.keyId != null) {
            result.add(TalkDelivery.FIELD_KEY_ID);
        }
        if (this.keyCiphertext != null) {
            result.add(TalkDelivery.FIELD_KEY_CIPHERTEXT);
        }
        if (this.timeAccepted != null) {
            result.add(TalkDelivery.FIELD_TIME_ACCEPTED);
        }
        if (this.timeChanged != null) {
            result.add(TalkDelivery.FIELD_TIME_CHANGED);
        }
        if (this.timeUpdatedOut != null) {
            result.add(TalkDelivery.FIELD_TIME_UPDATED_OUT);
        }
        if (this.timeUpdatedIn != null) {
            result.add(TalkDelivery.FIELD_TIME_UPDATED_IN);
        }
        if (this.timeAttachmentReceived != null) {
            result.add(TalkDelivery.FIELD_TIME_ATTACHMENT_RECEIVED);
        }
        if (this.attachmentState != null) {
            result.add(TalkDelivery.FIELD_ATTACHMENT_STATE);
        }
        return result;
    }

    @JsonIgnore
    public void updateWith(TalkDelivery delivery, Set<String> fields) {
        if (fields == null || fields.contains(TalkDelivery.FIELD_MESSAGE_ID)) {
            this.messageId = delivery.getMessageId();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_MESSAGE_TAG)) {
            this.messageTag = delivery.getMessageTag();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_SENDER_ID)) {
            this.senderId = delivery.getSenderId();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_RECEIVER_ID)) {
            this.receiverId = delivery.getReceiverId();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_GROUP_ID)) {
            this.groupId = delivery.getGroupId();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_STATE)) {
            this.state = delivery.getState();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_KEY_ID)) {
            this.keyId = delivery.getKeyId();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_KEY_CIPHERTEXT)) {
            this.keyCiphertext = delivery.getKeyCiphertext();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_TIME_ACCEPTED)) {
            this.timeAccepted = delivery.getTimeAccepted();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_TIME_CHANGED)) {
            this.timeChanged = delivery.getTimeChanged();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_TIME_UPDATED_OUT)) {
            this.timeUpdatedOut = delivery.getTimeUpdatedOut();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_TIME_UPDATED_IN)) {
            this.timeUpdatedIn = delivery.getTimeUpdatedIn();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_TIME_ATTACHMENT_RECEIVED)) {
            this.timeAttachmentReceived = delivery.getTimeAttachmentReceived();
        }
        if (fields == null || fields.contains(TalkDelivery.FIELD_ATTACHMENT_STATE)) {
            this.attachmentState = delivery.getAttachmentState();
        }

    }
}
