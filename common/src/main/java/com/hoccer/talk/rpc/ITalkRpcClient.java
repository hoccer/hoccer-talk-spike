package com.hoccer.talk.rpc;

import better.jsonrpc.annotations.JsonRpcNotification;
import com.hoccer.talk.model.*;
import com.hoccer.talk.model.TalkGroupPresence;

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
     * @talk.preconditions Client is logged in
     * @talk.behavior Client responds with null result or error
     * @talk.statechanges.clientobjects none
     * @talk.statechanges.serverobjects none
     * @talk.ui.client none
     * @talk.errors.client Error response when not connected
     */
    void ping();

    /**
     * Get group Keys for a particular group member
     *
     * @talk.preconditions Client is logged in
     * @talk.behavior Client responds with an array of encrypted group keys
     * @talk.statechanges.clientobjects none
     * @talk.statechanges.serverobjects none
     * @talk.ui.client none
     * @talk.errors.client Error response when not connected
     */
    String[] getEncryptedGroupKeys(String groupId, String sharedKeyId, String sharedKeyIdSalt, String[] clientIds, String[] publicKeyIds);

    /**
     * Alert the user immediately showing the given message
     * @param message the string to be shown to the user
     * @talk.preconditions Client is logged in
     * @talk.behavior Message is presented to user, no localization is performed on the client, must be done on the client; the client language can be made known to the server with the hello call
     * @talk.statechanges.clientobjects none yet
     * @talk.statechanges.serverobjects none yet
     * @talk.ui.client Show message text to user
     * @talk.errors.client none
     * @talk.todo This function needs a way to determine if a user has already been alerted; possibly we should change it to an RPC-Call or a server API call to confirm confirm message has been shown
     */
    @JsonRpcNotification
    void alertUser(String message);

    @JsonRpcNotification
    void settingsChanged(String setting, String value, String message);

    /**
     * Sent on login if the client has no push config
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server no push config on server
     * @talk.behavior.client call registerApns or registerGcm on server to transmit push token
     * @talk.statechanges.clientobjects none
     * @talk.statechanges.serverobjects
     * @talk.ui.client none
     * @talk.errors.client none
     * */
    @JsonRpcNotification
    void pushNotRegistered();

    /**
     * Delivers a message from the server that has been sent by another client
     *
     * @param d delivery object for M towards this client
     * @param m message to be delivered
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server undelivered message on server
     * @talk.preconditions.client message encrypted with a valid public key, sender known to client
     * @talk.behavior.client process message and call deliveryConfirm on server, possibly handle attachment download
     * @talk.statechanges.clientobjects new message and delivery objects created
     * @talk.statechanges.serverobjects set timeUpdatedIn
     * @talk.ui.client signal message reception, update views
     * @talk.errors.client call deliveryAbort when preconditions not met
     */
	@JsonRpcNotification
    void incomingDelivery(TalkDelivery d, TalkMessage m);

    @JsonRpcNotification
    void incomingDeliveryUpdated(TalkDelivery d);

    /**
     * Notifies a client about a state change on the server about an outgoing delivery sent by this client
     *
     * @param d delivery object that has changed state
     * @talk.preconditions Client is logged in, message has been sent by this client, delivery state is neither confirmed nor aborted
     * @talk.preconditions.server delivery state has changed on server (timeUpdatedOut < timeChanged)
     * @talk.preconditions.client
     * @talk.behavior.client call deliveryAcknowledge on server
     * @talk.statechanges.clientobjects update local delivery state to state in message
     * @talk.statechanges.serverobjects set timeUpdatedOut
     * @talk.ui.client change message appearance to reflect delivery status change
     * @talk.errors.client
     */
	@JsonRpcNotification
    void outgoingDeliveryUpdated(TalkDelivery d);

    /**
     * Sent to notify a client about a presence update on the server
     *
     * @param presence that has changed
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server presence has changed, connected client is friend or group friend with present client
     * @talk.preconditions.client
     * @talk.behavior.client  update local contact profile, eventually retrieve new key or avatar
     * @talk.statechanges.clientobjects create or delete or update local presence information for contact
     * @talk.ui.client reflect changes in profile in UI
     * @talk.errors.client
     * */
    @JsonRpcNotification
    void presenceUpdated(TalkPresence presence);

    /**
     * Sent to notify a client about a presence update on the server
     *
     * @param presence that has changed
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server presence has changed, connected client is friend or group friend with present client
     * @talk.preconditions.client
     * @talk.behavior.client  update local contact profile, eventually retrieve new key or avatar
     * @talk.statechanges.clientobjects create or delete or update local presence information for contact
     * @talk.ui.client reflect changes in profile in UI
     * @talk.errors.client
     * */
    @JsonRpcNotification
    void presenceModified(TalkPresence presence);


    /**
     * Sent to notify a client about a relationship update on the server
     *
     * @param relationship that has changed
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server relationsship has changed
     * @talk.preconditions.client
     * @talk.behavior.client
     * @talk.statechanges.clientobjects create or delete or update local presence information for contact
     * @talk.ui.client update contact profile, eventually retrieve new key or avatar
     * @talk.errors.client
     * */
    @JsonRpcNotification
    void relationshipUpdated(TalkRelationship relationship);

    /**
     * Sent to notify that a group for this client has been added or removed or group attributes have changed
     *
     * @param groupPresence that has changed
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server group attributes have changed (member changes not reflected here)
     * @talk.preconditions.client
     * @talk.behavior.client update local group attributes; create local group if necessary,
     process group deletion if group state moved to 'none',
     eventually retrieve group members, eventually retrieve new key or avatar
     * @talk.statechanges.clientobjects create or delete or update local group information (name, avatar, state)
     * @talk.ui.client update group profiler
     * @talk.errors.client
     * */
    @JsonRpcNotification
    void groupUpdated(TalkGroupPresence groupPresence);

    /**
     * Sent to notify an update to a group membership
     *
     * @param groupMember that has changed
     * @talk.preconditions Client is logged in
     * @talk.preconditions.server group members have been added or removed or changed state
     * @talk.preconditions.client
     * @talk.behavior.client
     * @talk.statechanges.clientobjects create or delete or update local membership information for contact;
     note that when a client leaves a group it will only receive a groupMemberUpdated() notification with state 'none'
    with its own clientId; there will be no further notification about the group or other group members, so the client
    is responsible to locally remove or disable the group and all other locally stored members.
     * @talk.ui.client update member information, eventually process member deletion if member state moved to 'none'
     * @talk.errors.client
     */
    @JsonRpcNotification
    void groupMemberUpdated(TalkGroupMember groupMember);


}
