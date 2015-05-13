package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Delivery objects represent the receiver-dependent
 * envelope of a given message and contain
 * receiver-dependent delivery state
 * <p/>
 * - saved in the database
 * - manipulated by delivery logic
 * - used in RPC for requesting delivery
 * - used in RPC for reflecting delivery state
 */
@DatabaseTable(tableName = "delivery")
public class TalkDelivery {

    // The database field names
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

    public static final String[] REQUIRED_OUT_RESULT_FIELDS = {FIELD_DELIVERY_ID, FIELD_MESSAGE_ID, FIELD_MESSAGE_TAG,
            FIELD_SENDER_ID, FIELD_RECEIVER_ID, FIELD_GROUP_ID, FIELD_STATE, FIELD_TIME_ACCEPTED, FIELD_TIME_CHANGED,
            FIELD_ATTACHMENT_STATE, FIELD_TIME_ATTACHMENT_RECEIVED, FIELD_REASON
    };
    public static final Set<String> REQUIRED_OUT_RESULT_FIELDS_SET = new HashSet<String>(Arrays.asList(REQUIRED_OUT_RESULT_FIELDS));

    public static final String[] REQUIRED_OUT_UPDATE_FIELDS = {FIELD_DELIVERY_ID, FIELD_MESSAGE_ID, FIELD_MESSAGE_TAG,
            FIELD_SENDER_ID, FIELD_RECEIVER_ID, FIELD_GROUP_ID, FIELD_STATE, FIELD_TIME_ACCEPTED, FIELD_TIME_CHANGED,
            FIELD_ATTACHMENT_STATE, FIELD_TIME_ATTACHMENT_RECEIVED, FIELD_REASON
    };
    public static final Set<String> REQUIRED_OUT_UPDATE_FIELDS_SET = new HashSet<String>(Arrays.asList(REQUIRED_OUT_UPDATE_FIELDS));

    public static final String[] REQUIRED_IN_UPDATE_FIELDS = {FIELD_MESSAGE_ID, FIELD_RECEIVER_ID, FIELD_SENDER_ID, FIELD_GROUP_ID, FIELD_MESSAGE_TAG, FIELD_STATE, FIELD_TIME_CHANGED,
            FIELD_ATTACHMENT_STATE, FIELD_TIME_ATTACHMENT_RECEIVED, FIELD_REASON
    };
    public static final Set<String> REQUIRED_IN_UPDATE_FIELDS_SET = new HashSet<String>(Arrays.asList(REQUIRED_IN_UPDATE_FIELDS));

    // the delivery states
    public static final String STATE_DRAFT = "draft";
    public static final String STATE_NEW = "new";
    public static final String STATE_DELIVERING = "delivering";
    public static final String STATE_DELIVERED_PRIVATE = "deliveredPrivate";
    public static final String STATE_DELIVERED_PRIVATE_ACKNOWLEDGED = "deliveredPrivateAcknowledged";
    public static final String STATE_DELIVERED_UNSEEN = "deliveredUnseen";
    public static final String STATE_DELIVERED_UNSEEN_ACKNOWLEDGED = "deliveredUnseenAcknowledged";
    public static final String STATE_DELIVERED_SEEN = "deliveredSeen";
    public static final String STATE_DELIVERED_SEEN_ACKNOWLEDGED = "deliveredSeenAcknowledged";
    public static final String STATE_FAILED = "failed";
    public static final String STATE_ABORTED = "aborted";
    public static final String STATE_REJECTED = "rejected";
    public static final String STATE_FAILED_ACKNOWLEDGED = "failedAcknowledged";
    public static final String STATE_ABORTED_ACKNOWLEDGED = "abortedAcknowledged";
    public static final String STATE_REJECTED_ACKNOWLEDGED = "rejectedAcknowledged";
    public static final String STATE_EXPIRED = "expired";

    // Old states are only needed for Database migrations. Maybe we should collect them somewhere else?
    @Deprecated
    public static final String STATE_NEW_OLD = "new";
    @Deprecated
    public static final String STATE_DELIVERING_OLD = "delivering";
    @Deprecated
    public static final String STATE_DELIVERED_OLD = "delivered";
    @Deprecated
    public static final String STATE_CONFIRMED_OLD = "confirmed";
    @Deprecated
    public static final String STATE_FAILED_OLD = "failed";
    @Deprecated
    public static final String STATE_ABORTED_OLD = "aborted";

    public static final String[] ALL_STATES = {
            STATE_DRAFT,
            STATE_NEW,
            STATE_DELIVERING,
            STATE_DELIVERED_PRIVATE,
            STATE_DELIVERED_PRIVATE_ACKNOWLEDGED,
            STATE_DELIVERED_UNSEEN,
            STATE_DELIVERED_UNSEEN_ACKNOWLEDGED,
            STATE_DELIVERED_SEEN,
            STATE_DELIVERED_SEEN_ACKNOWLEDGED,
            STATE_FAILED,
            STATE_FAILED_ACKNOWLEDGED,
            STATE_ABORTED,
            STATE_ABORTED_ACKNOWLEDGED,
            STATE_REJECTED,
            STATE_REJECTED_ACKNOWLEDGED,
            STATE_EXPIRED
    };
    public static final Set<String> ALL_STATES_SET = new HashSet<String>(Arrays.asList(ALL_STATES));

    public static final String[] SENDER_CALL_STATES = {
            STATE_NEW,
            STATE_DELIVERING,
            STATE_DELIVERED_PRIVATE_ACKNOWLEDGED,
            STATE_DELIVERED_UNSEEN_ACKNOWLEDGED,
            STATE_DELIVERED_SEEN_ACKNOWLEDGED,
            STATE_FAILED,
            STATE_FAILED_ACKNOWLEDGED,
            STATE_ABORTED,
            STATE_ABORTED_ACKNOWLEDGED,
            STATE_REJECTED_ACKNOWLEDGED
    };
    public static final Set<String> SENDER_CALL_STATES_SET = new HashSet<String>(Arrays.asList(SENDER_CALL_STATES));

    public static final String[] SENDER_SHOULD_ACKNOWLEDGE_STATES = {
            STATE_DELIVERED_PRIVATE,
            STATE_DELIVERED_UNSEEN,
            STATE_DELIVERED_SEEN,
            STATE_FAILED,
            STATE_REJECTED
    };
    public static final Set<String> SENDER_SHOULD_ACKNOWLEDGE_STATES_SET = new HashSet<String>(Arrays.asList(SENDER_SHOULD_ACKNOWLEDGE_STATES));

    public static final String[] RECIPIENT_CALL_STATES = {
            STATE_DELIVERED_PRIVATE,
            STATE_DELIVERED_UNSEEN,
            STATE_DELIVERED_SEEN,
            STATE_REJECTED
    };
    public static final Set<String> RECIPIENT_CALL_STATES_SET = new HashSet<String>(Arrays.asList(RECIPIENT_CALL_STATES));

    // The delivery states the sender is interested in for outgoingDeliverUpdated regardless of attachmentState
    public static final String[] OUT_STATES = {STATE_DELIVERED_UNSEEN, STATE_DELIVERED_SEEN,
            STATE_DELIVERED_PRIVATE, STATE_FAILED, STATE_REJECTED};
    public static final Set<String> OUT_STATES_SET = new HashSet<String>(Arrays.asList(OUT_STATES));

    // attachment state and delivery state combinations the sender is interested in addition to OUT_STATES
    public final static String[] OUT_ATTACHMENT_DELIVERY_STATES = {STATE_DELIVERED_SEEN_ACKNOWLEDGED, STATE_DELIVERED_UNSEEN_ACKNOWLEDGED, STATE_DELIVERED_PRIVATE_ACKNOWLEDGED};
    public final static String[] OUT_ATTACHMENT_STATES = {TalkDelivery.ATTACHMENT_STATE_RECEIVED, TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_ABORTED,
            TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_FAILED};

    // The delivery states the receiver is interested in for incomingDeliveryUpdated regardless of attachmentState
    public static final String[] IN_STATES = {STATE_DELIVERING};
    public static final Set<String> IN_STATES_SET = new HashSet<String>(Arrays.asList(IN_STATES));

    // attachment state and delivery state combinations the receiver is interested in addition to IN_STATES
    public final static String[] IN_ATTACHMENT_DELIVERY_STATES = {STATE_DELIVERED_UNSEEN, STATE_DELIVERED_UNSEEN_ACKNOWLEDGED, STATE_DELIVERED_SEEN,
            STATE_DELIVERED_SEEN_ACKNOWLEDGED, STATE_DELIVERED_PRIVATE, STATE_DELIVERED_PRIVATE_ACKNOWLEDGED};

    public final static String[] IN_ATTACHMENT_STATES = {TalkDelivery.ATTACHMENT_STATE_UPLOADING, TalkDelivery.ATTACHMENT_STATE_UPLOADED,
            TalkDelivery.ATTACHMENT_STATE_UPLOAD_PAUSED, TalkDelivery.ATTACHMENT_STATE_UPLOAD_ABORTED, TalkDelivery.ATTACHMENT_STATE_UPLOAD_FAILED};

    public static final String[] DELIVERED_STATES = {
            STATE_DELIVERED_PRIVATE,
            STATE_DELIVERED_PRIVATE_ACKNOWLEDGED,
            STATE_DELIVERED_UNSEEN,
            STATE_DELIVERED_UNSEEN_ACKNOWLEDGED,
            STATE_DELIVERED_SEEN,
            STATE_DELIVERED_SEEN_ACKNOWLEDGED
    };
    public static final Set<String> DELIVERED_STATES_SET = new HashSet<String>(Arrays.asList(DELIVERED_STATES));

    public static final String[] FINAL_STATES = {
            STATE_DELIVERED_PRIVATE_ACKNOWLEDGED,
            STATE_DELIVERED_SEEN_ACKNOWLEDGED,
            STATE_FAILED_ACKNOWLEDGED,
            STATE_ABORTED_ACKNOWLEDGED,
            STATE_REJECTED_ACKNOWLEDGED,
            STATE_EXPIRED
    };
    public static final Set<String> FINAL_STATES_SET = new HashSet<String>(Arrays.asList(FINAL_STATES));

    public static final String[] FAILED_STATES = {
            STATE_FAILED,
            STATE_ABORTED,
            STATE_REJECTED,
            STATE_FAILED_ACKNOWLEDGED,
            STATE_ABORTED_ACKNOWLEDGED,
            STATE_REJECTED_ACKNOWLEDGED,
            STATE_EXPIRED
    };
    public static final Set<String> FAILED_STATES_SET = new HashSet<String>(Arrays.asList(FAILED_STATES));

    public static final String[] FINAL_FAILED_STATES = {
            STATE_FAILED_ACKNOWLEDGED,
            STATE_ABORTED_ACKNOWLEDGED,
            STATE_REJECTED_ACKNOWLEDGED,
            STATE_EXPIRED
    };
    public static final Set<String> FINAL_FAILED_STATES_SET = new HashSet<String>(Arrays.asList(FINAL_FAILED_STATES));


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
    public static final String ATTACHMENT_STATE_EXPIRED = "expired";

    public static final String[] ALL_ATTACHMENT_STATES = {
            ATTACHMENT_STATE_NONE,
            ATTACHMENT_STATE_NEW,
            ATTACHMENT_STATE_UPLOADING,
            ATTACHMENT_STATE_UPLOAD_PAUSED,
            ATTACHMENT_STATE_UPLOADED,
            ATTACHMENT_STATE_RECEIVED,
            ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_FAILED,
            ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_ABORTED,
            ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_FAILED,
            ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_ABORTED,
            ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED,
            ATTACHMENT_STATE_EXPIRED
    };
    public static final Set<String> ALL_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(ALL_ATTACHMENT_STATES));

    public static final String[] FINAL_ATTACHMENT_STATES = {
            ATTACHMENT_STATE_NONE,
            ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED,
            ATTACHMENT_STATE_EXPIRED
    };
    public static final Set<String> FINAL_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(FINAL_ATTACHMENT_STATES));

    public static final String[] FAILED_ATTACHMENT_STATES = {
            ATTACHMENT_STATE_UPLOAD_FAILED,
            ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_UPLOAD_ABORTED,
            ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_FAILED,
            ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED,
            ATTACHMENT_STATE_DOWNLOAD_ABORTED,
            ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED,
            ATTACHMENT_STATE_EXPIRED
    };
    public static final Set<String> FAILED_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(FAILED_ATTACHMENT_STATES));

    public static final String[] PENDING_ATTACHMENT_STATES = {
            ATTACHMENT_STATE_NEW,
            ATTACHMENT_STATE_UPLOADING,
            ATTACHMENT_STATE_UPLOAD_PAUSED,
            ATTACHMENT_STATE_UPLOADED,
    };
    public static final Set<String> PENDING_ATTACHMENT_STATES_SET = new HashSet<String>(Arrays.asList(PENDING_ATTACHMENT_STATES));

    /* The delivery State logic has the following logic:
    - states get advanced by subsequent rpc-calls from sender and receiver
    - there are final states that are contained in FINAL_STATES and FINAL_ATTACHMENT_STATES
    - once a delivery is in a final state (both state and attachmentState are in a final state),
     it will no longer be sent out to a client and can be deleted by server
    - end states can only be reached by a call from the sender
    - when a party has initiated a call that puts the delivery into a non-final state like "received" or "delivered" or "aborted",
    the counterparty is responsible to acknowledge the pre-final state, which will advance the delivery into a confirmed end-state
     */

    static final Map<String, Set<String>> nextState = new HashMap<String, Set<String>>();
    static final Map<String, Set<String>> nextAttachmentState = new HashMap<String, Set<String>>();

    // State transitions for in and out in case of unknown deliveries
    public static final Map<String, String> nextUnknownInState = new HashMap<String, String>();
    public static final Map<String, String> nextUnknownOutState = new HashMap<String, String>();

    public static final Map<String, String> nextUnknownInAttachmentState = new HashMap<String, String>();
    public static final Map<String, String> nextUnknownOutAttachmentState = new HashMap<String, String>();


    static {
        // nextstate tree init
        nextState.put(STATE_DRAFT, new HashSet<String>(Arrays.asList(new String[]{STATE_NEW})));
        nextState.put(STATE_NEW, new HashSet<String>(Arrays.asList(new String[]{STATE_DELIVERING, STATE_FAILED})));
        nextState.put(STATE_DELIVERING, new HashSet<String>(Arrays.asList(new String[]{STATE_DELIVERED_UNSEEN, STATE_DELIVERED_PRIVATE, STATE_REJECTED, STATE_ABORTED})));
        nextState.put(STATE_DELIVERED_PRIVATE, new HashSet<String>(Arrays.asList(new String[]{STATE_DELIVERED_PRIVATE_ACKNOWLEDGED})));
        nextState.put(STATE_DELIVERED_PRIVATE_ACKNOWLEDGED, new HashSet<String>());
        nextState.put(STATE_DELIVERED_UNSEEN, new HashSet<String>(Arrays.asList(new String[]{STATE_DELIVERED_UNSEEN_ACKNOWLEDGED})));
        nextState.put(STATE_DELIVERED_UNSEEN_ACKNOWLEDGED, new HashSet<String>(Arrays.asList(new String[]{STATE_DELIVERED_SEEN})));
        nextState.put(STATE_DELIVERED_SEEN, new HashSet<String>(Arrays.asList(new String[]{STATE_DELIVERED_SEEN_ACKNOWLEDGED})));
        nextState.put(STATE_DELIVERED_SEEN_ACKNOWLEDGED, new HashSet<String>());
        nextState.put(STATE_FAILED, new HashSet<String>(Arrays.asList(new String[]{STATE_FAILED_ACKNOWLEDGED})));
        nextState.put(STATE_FAILED_ACKNOWLEDGED, new HashSet<String>());
        nextState.put(STATE_ABORTED, new HashSet<String>(Arrays.asList(new String[]{STATE_ABORTED_ACKNOWLEDGED})));
        nextState.put(STATE_ABORTED_ACKNOWLEDGED, new HashSet<String>());
        nextState.put(STATE_REJECTED, new HashSet<String>(Arrays.asList(new String[]{STATE_REJECTED_ACKNOWLEDGED})));
        nextState.put(STATE_REJECTED_ACKNOWLEDGED, new HashSet<String>());
        nextState.put(STATE_EXPIRED, new HashSet<String>());

        // transitions into silent states in case of unknown out deliveries on the client side
        nextUnknownOutState.put(STATE_DELIVERED_PRIVATE, STATE_DELIVERED_PRIVATE_ACKNOWLEDGED);
        nextUnknownOutState.put(STATE_DELIVERED_UNSEEN, STATE_DELIVERED_UNSEEN_ACKNOWLEDGED);
        nextUnknownOutState.put(STATE_DELIVERED_SEEN, STATE_DELIVERED_SEEN_ACKNOWLEDGED);
        nextUnknownOutState.put(STATE_FAILED, STATE_FAILED_ACKNOWLEDGED);
        nextUnknownOutState.put(STATE_ABORTED, STATE_ABORTED_ACKNOWLEDGED);
        nextUnknownOutState.put(STATE_REJECTED, STATE_REJECTED_ACKNOWLEDGED);

        // transitions into silent states in case of unknown IN delivery updates on the client side
        // there are no such states

        // transitions into silent attachment states in case of unknown OUT delivery updates on the client side
        nextUnknownOutAttachmentState.put(ATTACHMENT_STATE_NEW, ATTACHMENT_STATE_UPLOAD_ABORTED);
        nextUnknownOutAttachmentState.put(ATTACHMENT_STATE_UPLOADING, ATTACHMENT_STATE_UPLOAD_ABORTED);
        nextUnknownOutAttachmentState.put(ATTACHMENT_STATE_UPLOAD_PAUSED, ATTACHMENT_STATE_UPLOAD_ABORTED);
        nextUnknownOutAttachmentState.put(ATTACHMENT_STATE_RECEIVED, ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED);
        nextUnknownOutAttachmentState.put(ATTACHMENT_STATE_DOWNLOAD_FAILED, ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED);
        nextUnknownOutAttachmentState.put(ATTACHMENT_STATE_DOWNLOAD_ABORTED, ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED);

        // transitions into silent attachment states in case of unknown IN delivery updates on the client side
        nextUnknownInAttachmentState.put(ATTACHMENT_STATE_NEW, ATTACHMENT_STATE_DOWNLOAD_ABORTED);
        nextUnknownInAttachmentState.put(ATTACHMENT_STATE_UPLOADING, ATTACHMENT_STATE_DOWNLOAD_ABORTED);
        nextUnknownInAttachmentState.put(ATTACHMENT_STATE_UPLOAD_PAUSED, ATTACHMENT_STATE_DOWNLOAD_ABORTED);
        nextUnknownInAttachmentState.put(ATTACHMENT_STATE_UPLOADED, ATTACHMENT_STATE_DOWNLOAD_ABORTED);
        nextUnknownInAttachmentState.put(ATTACHMENT_STATE_UPLOAD_FAILED, ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED);
        nextUnknownInAttachmentState.put(ATTACHMENT_STATE_UPLOAD_ABORTED, ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED);

        // nextAttachmentState tree init
        nextAttachmentState.put(ATTACHMENT_STATE_NONE, new HashSet<String>());
        nextAttachmentState.put(ATTACHMENT_STATE_NEW, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_UPLOADING})));
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOADING, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_UPLOADED, ATTACHMENT_STATE_UPLOAD_PAUSED, ATTACHMENT_STATE_UPLOAD_FAILED,ATTACHMENT_STATE_UPLOAD_ABORTED})));
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOAD_PAUSED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_UPLOADING})));
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOADED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_RECEIVED, ATTACHMENT_STATE_DOWNLOAD_FAILED, ATTACHMENT_STATE_DOWNLOAD_ABORTED})));
        nextAttachmentState.put(ATTACHMENT_STATE_RECEIVED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED})));
        nextAttachmentState.put(ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED, new HashSet<String>());
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOAD_FAILED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED})));
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED, new HashSet<String>());
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOAD_ABORTED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED})));
        nextAttachmentState.put(ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED, new HashSet<String>());
        nextAttachmentState.put(ATTACHMENT_STATE_DOWNLOAD_FAILED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED})));
        nextAttachmentState.put(ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED, new HashSet<String>());
        nextAttachmentState.put(ATTACHMENT_STATE_DOWNLOAD_ABORTED, new HashSet<String>(Arrays.asList(new String[]{ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED})));
        nextAttachmentState.put(ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED, new HashSet<String>());
        nextAttachmentState.put(ATTACHMENT_STATE_EXPIRED, new HashSet<String>());
    }

    static boolean statePathExists(final String stateA, final String stateB) {
        return statePathExists(nextState, stateA, stateB, new HashSet<String>());
    }

    static boolean attachmentStatePathExists(final String stateA, final String stateB) {
        return statePathExists(nextAttachmentState, stateA, stateB, new HashSet<String>());
    }

    static boolean statePathExists(final Map<String, Set<String>> graph, final String stateA, final String stateB, final Set<String> track) {
        if (track.size() > graph.size()) {
            throw new RuntimeException("impossible path length");
        }
        if (track.contains(stateA)) {
            // we have already been here
            return false;
        }

        final Set<String> aFollows = graph.get(stateA);
        if (aFollows == null) {
            throw new RuntimeException("state A ='" + stateA + "' does not exist");
        }

        if (!graph.containsKey(stateB)) {
            throw new RuntimeException("state B ='" + stateB + "' does not exist");
        }

        if (aFollows.contains(stateB)) {
            return true;
        }
        Set<String> downTrack = new HashSet<String>(track);
        downTrack.add(stateA);
        for (String next : aFollows) {
            if (statePathExists(graph, next, stateB, downTrack)) {
                return true;
            }
        }
        return false;
    }

    public static String findNextUnknownState(final Map<String, String> map, String state) {
        String next = map.get(state);
        if (next == null) {
            return state;
        } else {
            return next;
        }
    }

    @JsonIgnore
    public boolean isInState(String theState) {
        if (!ALL_STATES_SET.contains(theState)) {
            throw new RuntimeException("illegal state:"+theState);
        }
        return theState.equals(state);
    }

    @JsonIgnore
    public boolean isFinished() {
        return isFinalState(state) && (isFailureState(state) || isFinalAttachmentState(attachmentState));
    }

    @JsonIgnore
    public boolean isFailure() {
        return isFailureState(state);
    }

    @JsonIgnore
    public boolean isDelivered() {
        return isDeliveredState(state);
    }

    @JsonIgnore
    public boolean isSeen() {
        return isSeenState(state);
    }

    @JsonIgnore
    public boolean isPrivate() {
        return isPrivateState(state);
    }

    @JsonIgnore
    public boolean isUnseen() {
        return isUnseenState(state);
    }

    @JsonIgnore
    public boolean isFailed() {
        return isFailedState(state);
    }

    @JsonIgnore
    public boolean isAborted() {
        return isAbortedState(state);
    }

    @JsonIgnore
    public boolean isRejected() {
        return isRejectedState(state);
    }

    public static boolean isValidState(String state) {
        return ALL_STATES_SET.contains(state);
    }

    public static boolean isFinalState(String state) {
        return FINAL_STATES_SET.contains(state);
    }

    public static boolean isFailureState(String state) {
        return FAILED_STATES_SET.contains(state);
    }

    public static boolean isDeliveredState(String state) {
        return DELIVERED_STATES_SET.contains(state);
    }

    public static boolean isSeenState(String state) {
        return STATE_DELIVERED_SEEN.equals(state) || STATE_DELIVERED_SEEN_ACKNOWLEDGED.equals(state);
    }

    public static boolean isPrivateState(String state) {
        return STATE_DELIVERED_PRIVATE.equals(state) || STATE_DELIVERED_PRIVATE_ACKNOWLEDGED.equals(state);
    }

    public static boolean isUnseenState(String state) {
        return STATE_DELIVERED_UNSEEN.equals(state) || STATE_DELIVERED_UNSEEN_ACKNOWLEDGED.equals(state);
    }

    public static boolean isFailedState(String state) {
        return STATE_FAILED.equals(state) || STATE_FAILED_ACKNOWLEDGED.equals(state);
    }

    public static boolean isAbortedState(String state) {
        return STATE_ABORTED.equals(state) || STATE_ABORTED_ACKNOWLEDGED.equals(state);
    }

    public static boolean isRejectedState(String state) {
        return STATE_REJECTED.equals(state) || STATE_REJECTED_ACKNOWLEDGED.equals(state);
    }

    public void expireDelivery() {
        if (STATE_DELIVERED_OLD.equals(state)) {
            state = STATE_DELIVERED_PRIVATE_ACKNOWLEDGED;
        }
        if (STATE_DELIVERED_PRIVATE.equals(state)) {
            state = STATE_DELIVERED_PRIVATE_ACKNOWLEDGED;
        }
        if (STATE_DELIVERED_UNSEEN.equals(state)) {
            state = STATE_DELIVERED_UNSEEN_ACKNOWLEDGED;
        }
        if (STATE_DELIVERED_SEEN.equals(state)) {
            state = STATE_DELIVERED_SEEN_ACKNOWLEDGED;
        }
        if (STATE_FAILED.equals(state)) {
            state = STATE_FAILED_ACKNOWLEDGED;
        }
        if (STATE_ABORTED.equals(state)) {
            state = STATE_ABORTED_ACKNOWLEDGED;
        }
        if (STATE_REJECTED.equals(state)) {
            state = STATE_REJECTED_ACKNOWLEDGED;
        }
        if (!isFinalState(state)) {
            state = STATE_EXPIRED;
        }

        if (ATTACHMENT_STATE_RECEIVED.equals(attachmentState)) {
            attachmentState = ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED;
        }

        if (!isFinalAttachmentState(attachmentState)) {
            attachmentState = ATTACHMENT_STATE_EXPIRED;
        }
   }

    public static boolean nextStateAllowed(String currentState, String nextState) {
        if (!isValidState(currentState)) {
            return true;
        }
        if (!isValidState(nextState)) {
            return false;
        }
        if (currentState.equals(nextState)) {
            return true;
        }
        if (isFinalState(currentState)) {
            return false;
        }
        return statePathExists(currentState, nextState);
    }

    @JsonIgnore
    public boolean nextStateAllowed(String nextState) {
        return nextStateAllowed(this.state, nextState);
    }

    @JsonIgnore
    public boolean isAttachmentFailure() {
        return isFailedAttachmentState(attachmentState);
    }

    @JsonIgnore
    public boolean isAttachmentDelivered() {
        return isDeliveredAttachmentState(attachmentState);
    }

    @JsonIgnore
    public boolean isAttachmentPending() {
        return isPendingAttachmentState(attachmentState);
    }
    public static boolean isValidAttachmentState(String state) {
        return ALL_ATTACHMENT_STATES_SET.contains(state);
    }

    public static boolean isFinalAttachmentState(String state) {
        return FINAL_ATTACHMENT_STATES_SET.contains(state);
    }

    public static boolean isFailedAttachmentState(String state) {
        return FAILED_ATTACHMENT_STATES_SET.contains(state);
    }

    public static boolean isDeliveredAttachmentState(String state) {
        return ATTACHMENT_STATE_RECEIVED.equals(state) || ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED.equals(state);
    }

    public static boolean isPendingAttachmentState(String state) {
        return PENDING_ATTACHMENT_STATES_SET.contains(state);
    }

    // returns true if nextState is a valid state and there are one or more state transition that lead form the current state to nextSate
    @JsonIgnore
    public boolean nextAttachmentStateAllowed(String nextState) {
        if (!isValidAttachmentState(attachmentState)) {
            return true;
        }
        if (!isValidAttachmentState(nextState)) {
            return false;
        }
        if (attachmentState.equals(nextState)) {
            return true;
        }
        if (isFinalAttachmentState(attachmentState)) {
            return false;
        }
        return attachmentStatePathExists(attachmentState, nextState);
    }

    @JsonIgnore
    public boolean hasAttachment() {
        return attachmentState != null && !ATTACHMENT_STATE_NONE.equals(attachmentState);
    }

    @JsonIgnore
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
        if (init) {
            this.initialize();
        }
    }

    @JsonIgnore
    public void initialize() {
        this.state = STATE_NEW;
        this.ensureDates();
    }

    @JsonIgnore
    public void ensureDates() {
        if (this.timeAccepted == null) {
            this.timeAccepted = new Date(0);
        }
        if (this.timeChanged == null) {
            this.timeChanged = new Date(0);
        }
        if (this.timeUpdatedIn == null) {
            this.timeUpdatedIn = new Date(0);
        }
        if (this.timeUpdatedOut == null) {
            this.timeUpdatedOut = new Date(0);
        }
    }

    @JsonIgnore
    public boolean isOutUpToDate() {
        return getTimeUpdatedOut().getTime() > getTimeChanged().getTime();
    }

    @JsonIgnore
    public boolean isInUpToDate() {
        return getTimeUpdatedIn().getTime() > getTimeChanged().getTime();
    }

    @JsonIgnore
    public boolean isOutStaleFor(long msec) {
        return getTimeUpdatedOut().getTime() + msec <= new Date().getTime();
    }

    @JsonIgnore
    public boolean isInStaleFor(long msec) {
        return getTimeUpdatedIn().getTime() + msec <= new Date().getTime();
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
    public boolean isExpandedGroupDelivery() {
        return groupId != null && receiverId != null;
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
        if (!nextStateAllowed(state)) {
            throw new RuntimeException("Delivery: state change from ‘" + this.state + "‘ -> '" + state + "' not allowed");
        }
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
        if (!nextAttachmentStateAllowed(attachmentState)) {
            throw new RuntimeException("Delivery: state change from ‘" + this.attachmentState + "‘ -> '" + attachmentState + "' not allowed");
        }
        this.attachmentState = attachmentState;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonIgnore
    public String getId() {
        return _id;
    }

    @JsonIgnore
    public void updateWith(TalkDelivery delivery) {
        //TODO: Use 'updateWith(delivery, null)' instead...;
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
    public void updateWith(TalkDelivery delivery, @Nullable Set<String> fields) {
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
