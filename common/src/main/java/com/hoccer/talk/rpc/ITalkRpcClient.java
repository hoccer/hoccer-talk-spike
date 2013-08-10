package com.hoccer.talk.rpc;

import better.jsonrpc.annotations.JsonRpcNotification;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.talk.model.TalkMessage;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;

/**
 * This is the RPC interface exposed by clients.
 * 
 * Using it, clients get notifications for things
 * they requested using the matching "request*"
 * methods in the server interface.
 * 
 * @author ingo
 *
 */
public interface ITalkRpcClient {

    /**
     * The obligatory equivalent of ping
     *
     * The server may use this to measure RTT on the connection
     */
    void ping();

    /**
     * Alert the user immediately showing the given message
     * @param message
     */
    @JsonRpcNotification
    void alertUser(String message);

    /**
     * Sent on login if the client has no push config
     */
    @JsonRpcNotification
    void pushNotRegistered();

    /**
     * Sent to deliver a message from another client
     *
     * @param d delivery object for M towards this client
     * @param m message to be delivered
     */
	@JsonRpcNotification
	void incomingDelivery(TalkDelivery d, TalkMessage m);

    /**
     * Sent to notify of a state change on an outgoing delivery
     *
     * @param d delivery object that has changed state
     */
	@JsonRpcNotification
	void outgoingDelivery(TalkDelivery d);

    /**
     * Sent to notify of a presence update
     *
     * @param presence that has changed
     */
    @JsonRpcNotification
    void presenceUpdated(TalkPresence presence);

    /**
     * Sent to notify of a relationship update
     *
     * @param relationship that has changed
     */
    @JsonRpcNotification
    void relationshipUpdated(TalkRelationship relationship);

    /**
     * Sent to notify an update to a group
     *
     * @param group that has changed
     */
    @JsonRpcNotification
    void groupUpdated(TalkGroup group);

    /**
     * Sent to notify an update to a group membership
     *
     * @param groupMember that has changed
     */
    @JsonRpcNotification
    void groupMemberUpdated(TalkGroupMember groupMember);

}
