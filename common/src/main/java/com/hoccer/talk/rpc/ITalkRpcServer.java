package com.hoccer.talk.rpc;

import com.hoccer.talk.model.*;

import java.util.Date;

/**
 * This is the RPC interface exposed by the talk server
 *
 * It contains all methods that the client can invoke
 * while connected to the server.
 *
 * @author ingo
 */
// Suppress unused warnings since they are external API entry points and are called from the outside via JsonRpc (via Websockets)
@SuppressWarnings("UnusedDeclaration")
public interface ITalkRpcServer {

    /**
     * The obligatory equivalent of ping, only called bing to differentiate it from the server ping to the client
     *
     * The client may use this to measure RTT on the connection
     * @talk.preconditions Client is logged in
     * @talk.behavior Server responds with null result or error
     * @talk.statechanges.clientobjects none
     * @talk.statechanges.serverobjects none
     * @talk.ui.client none
     * @talk.errors.client Error response when not connected or logged in
     */
    void bing();

    /**
     * The client can signal by calling ready() that is has completed initial synchronisation and is ready to handle heavy stuff
     *
     * @talk.preconditions Client is logged in
     * @talk.behavior Server responds with null result or error
     * @talk.statechanges.clientobjects none
     * @talk.statechanges.serverobjects something is set to ready state
     * @talk.ui.client none
     * @talk.errors.client Error response when not connected or logged in
     */
    void ready();

    /**
     * The client can signal by calling finishedIncoming() that is has completed processing an incoming delivery
     * and the delivery state change has been received by the server
     *
     * @talk.preconditions Client is logged in
     * @talk.behavior Server responds with null result or error and schedules another delivery agent perform in order to check if there are more deliveries; if there are none, the server will send a deliveriesReady notification
     * @talk.statechanges.clientobjects none
     * @talk.statechanges.serverobjects none
     * @talk.ui.client none
     * @talk.errors.client Error response when not connected or logged in
     */
    void finishedIncoming();

    /**
     * Generate a new client ID for registration
     *
     * The ID will be remembered as part of connection state,
     * so SRP setup must happen on the same connection.
     *
     * @return a new client id
     * @talk.preconditions Client is not logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.client
     * @talk.statechanges.clientobjects
     * @talk.ui.client
     * @talk.errors.client
     */
    String generateId();

    /**
     * Register a new client with the given SRP parameters
     */
    String srpRegister(String verifier, String salt);
    /**
     * Change the SRP parameters for an existing logged in client
     */
    String srpChangeVerifier(String verifier, String salt);

        /**
         * Initiate SRP login
         *
         * Client passes public value A.
         *
         * After this call we have a shared secret K.
         *
         * @return public value B
         */
    String srpPhase1(String clientId, String A);

    /**
     * Finish SRP login
     *
     * Client passes evidence that it has K, called M1.
     *
     * Iff M1 could be verified to be proof of K,
     * the server will prove knowledge of K by returning M2.
     *
     * Authentication has succeeded if M2 is returned
     * and can be verified by the client to be proof of K.
     *
     * If authentication fails then either null will be
     * returned or an error response will be sent.
     *
     * @return evidence value M2
     */
    String srpPhase2(String M1);

    /** Send Information about the client and retrieve information about the server
    * @return information about the server, currently only server time and connection debug mode setting
    * @talk.preconditions none
    * @talk.preconditions.server none
    * @talk.preconditions.client none
    * @talk.behavior.server
    * @talk.statechanges.serverobjects set connection debug mode status
    * @talk.errors.server
    */
    TalkServerInfo hello(TalkClientInfo clientInfo);

    void deleteAccount(String reason);

    /** get the server time
     * @return server time
     * @talk.preconditions none
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server
     * @talk.statechanges.serverobjects none
     * @talk.errors.server
     */
    Date getTime();

    /** Retrieve established relationships changed after given date
     * @param lastKnown is the date in milliseconds since start of year 1970
     * @return array of relationship objects that have a change date greater than the given date
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server query database for all relationships changed after given date and return result
     * @talk.statechanges.serverobjects none
     * @talk.errors.server
     */

    TalkRelationship[] getRelationships(Date lastKnown);

    /** Register client for GCM push notifications with the given parameters
     * @param registeredPackage is an string that identifies the app
     * @param registrationId is an string obtained by the client to identify himself to the Google GCM service
    * @talk.preconditions client must be logged in
    * @talk.preconditions.server
    * @talk.preconditions.client
    * @talk.behavior.server enable GCM for this client (should also disable APN)
    * @talk.statechanges.serverobjects store registrationId for the client
    * @talk.errors.server
    */
    void registerGcm(String registeredPackage, String registrationId);

    /** Clear Google GCM push notification registration
    * @talk.preconditions client must be logged in
    * @talk.preconditions.server
    * @talk.preconditions.client
    * @talk.behavior.server disable GCM for this client
    * @talk.statechanges.serverobjects delete registrationId for the client
    * @talk.errors.server
    */
    void unregisterGcm();

    /** Register client for APNS push notification with the given token
     * @param registrationToken is an string obtained by the client to identify himself to the Apple APN service
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server enable APN for this client (should also disable GCM)
     * @talk.statechanges.serverobjects store registrationToken for the client
     * @talk.errors.server
     */
    void registerApns(String registrationToken);

    /** Clear Apple APNS push notification registration
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server disable APN for this client
     * @talk.statechanges.serverobjects delete registrationToken for the client
     * @talk.errors.server
     */
    void unregisterApns();

    /** Hint about unread message count for use with APNS
     * @param numUnreadMessages number of unread messages on the client
     * @talk.preconditions client must be logged in, APNS must be enabled
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server store number of unread messages on the client and add it to the number on the server when notifying the client via APNS
     * @talk.statechanges.serverobjects store numUnreadMessages
     * @talk.errors.server
     */
    void hintApnsUnreadMessage(int numUnreadMessages);

    void setApnsMode(String mode);

    /** Update public encryption key on the server for this client
     * @param key the public key as b64 encoded binary representation opaque to the server; currently an X.509 RSA key
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server store the key and deliver it to other clients on a getKey request (clients are not notified about key change)
     * @talk.behavior.client client must call updatePresence after key updates in order to notify other clients about key change
     * @talk.statechanges.serverobjects update the key on the server
     * @talk.errors.server
     */
    void updateKey(TalkKey key);

    /** Verify if my latest public encryption key is on the server for this client
     * @param keyId the public key id
     * @return true the key with keyId is my public key on the server, false otherwise
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server
     * @talk.behavior.client client must call updateKey if this function returns false
     * @talk.statechanges.serverobjects update the key on the server
     * @talk.errors.server
     */
    boolean verifyKey(String keyId);

    /** Get key for given client with given keyid
     * @param clientId is a UUID denoting the client
     * @param keyId is a name for the key unique within the realm of the client, typically a fingerprint of the key
     * @return a TalkKey record containing among other things the public key for the client with the given clientId as b64 encoded binary representation opaque to the server;
     currently an X.509 RSA key
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server return the key
     * @talk.behavior.client store the retrieved key for the contact locally, update fingerprint display and validation
     * @talk.statechanges.serverobjects none
     * @talk.errors.server
     **/
    TalkKey getKey(String clientId, String keyId);

    /** Update client presence
     * @param presence is a structure containing information about a contact
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server store presence and notify other clients about presence changes
     * @talk.behavior.client none
     * @talk.statechanges.serverobjects update and timestamp the client's presence record on the server
     * @talk.errors.server
     */
    void updatePresence(TalkPresence presence);

    /** modify client presence
     * @param presence is a structure containing information about a contact
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server store only non-null fields of presence and notify other clients about presence changes via modifyPresense
     * @talk.behavior.client none
     * @talk.statechanges.serverobjects update and timestamp the client's presence record on the server
     * @talk.errors.server
     */
    void modifyPresence(TalkPresence presence);

    /** Retrieve presences changed after given date
     * @param lastKnown is the date in milliseconds since start of year 1970
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server query database for all relationships changed after given date and return result
     * @talk.behavior.client call getPresences after and only after login once
     * @talk.statechanges.serverobjects update client's connection status for all query results before returning results
     * @talk.errors.server
     */
    TalkPresence[] getPresences(Date lastKnown);

    /** Generate a secret token for the given purpose
     * @param tokenPurpose is a string describing a purpose, currently only 'pairing' defined
     * @param secondsValid is a number of seconds after which the token becomes invalid
     * @return a secret string that can be used with e.g. pairByToken()
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server generate and store token on server
     * @talk.behavior.client
     * @talk.statechanges.serverobjects create and store new token, stamp with creation or expiry date
     * @talk.errors.server
     * @talk.todo check if unify with generatePairingToken
     */
    String generateToken(String tokenPurpose, int secondsValid);

    /** Generate a secret token for pairing (establishing a friendship)
     * @param maxUseCount is the maximum number the token can be spent using pairByToken
     * @param secondsValid is a number of seconds after which the token becomes invalid
     * @return a secret string that can be used with e.g. pairByToken()
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server generate and store token on server
     * @talk.behavior.client
     * @talk.statechanges.serverobjects create and store new token, stamp with creation or expiry date
     * @talk.errors.server
     * @talk.todo check if unify with generateToken
     */
    String generatePairingToken(int maxUseCount, int secondsValid);

    /** Perform token-based client pairing generatePairingToken or generateToken
     * @param secret is is string created by
     * @return true if paring succeed, false if paring failed
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server create a friend relationShip with the client that generated the token
     * @talk.behavior.client
     * @talk.statechanges.serverobjects create relationShip record or change status to friend if it already exists
     * @talk.errors.server
     */
    boolean pairByToken(String secret);

    /** Block message exchange with the given client
     * @param clientId is a UUID denoting the client to block
     * @talk.preconditions client must be logged in, other client must be friend or group friend
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to block and notify partner via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShip with the client to blocked, update time stamp
     * @talk.errors.server
     * @talk.todo make blocking work with group friends where not relationship record exists
     **/
    void blockClient(String clientId);

    /** Unblock message exchange with the given client
     * @param clientId is a UUID denoting the client to unblock
     * @talk.preconditions client must be logged in, other client must be friend
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to 'friend' and notify partner via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShip with the client to friend again, update time stamp
     * @talk.errors.server  returns exception object when clientId unknown or no relationship exists
     * @talk.todo make unblocking work with group friends where not relationship record exists
     **/
    void unblockClient(String clientId);

    /** Invite the given client to become a friend
     * @param clientId is a UUID denoting the client to block
     * @talk.preconditions client must be logged in, other client must not be be friend
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to invited, reverse relationship to invitedMe and notify partners via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShips, update time stamp
     * @talk.errors.server
     **/
    void inviteFriend(String clientId);

    /** retract friend invitation with the given client
     * @param clientId is a UUID denoting the client to unblock
     * @talk.preconditions client must be logged in, other client must be invited
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to 'none' and notify partner via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShip with the client to none again, update time stamp
     * @talk.errors.server  returns exception object when clientId unknown or no relationship exists
     **/
    void disinviteFriend(String clientId);


    /** accept the given client as friend
     * @param clientId is a UUID denoting the client to block
     * @talk.preconditions client must be logged in, other client must have invited me as friend
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to friend and notify partner via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShip with the client to friend, update time stamp
     * @talk.errors.server
     **/
    void acceptFriend(String clientId);

    /** refuse to become friend with the given client
     * @param clientId is a UUID denoting the client to refuse friendship
     * @talk.preconditions client must be logged in, other client must have invited me as friend
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to 'none' and notify partner via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShip with the client to none again, update time stamp
     * @talk.errors.server  returns exception object when clientId unknown or no relationship exists
     **/
    void refuseFriend(String clientId);

    /** Depair the given client, removing the relationship between clients
     * @param clientId is a UUID denoting the client to unblock
     * @talk.preconditions client must be logged in, other client must be blocked or friend
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server set relationShip with the client to 'none' and notify partner via relationshipUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects set relationShip with the client to 'none', update time stamp
     * @talk.errors.server  returns exception object when clientId unknown or no relationship exists
     **/
    void depairClient(String clientId);

    /** Send message to server and request delivery to clients denoted in the delivery array
     * @param m is the message
     * @param d is an array denoting the receiving clients or groups
     * @return an array containing the deliveries with updated timeAccepted, timeUpdated, status and message id
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server store and forward the the message to denoted clients and notify partner(s) via incomingDelivery,
     report delivery progress via incomingDelivery notifications
     * @talk.behavior.client
     * @talk.statechanges.serverobjects store message and delivery objects, stamp with timeAccepted and create a message id
     * @talk.errors.server
     **/
	TalkDelivery[] outDeliveryRequest(TalkMessage m, TalkDelivery[] d);

    /** Confirm reception of the message with the given message id
     * @param messageId is the server-provided message id
     * @return a delivery object with updated timeUpdated and status 'delivered'
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server delivery object must exist and be in state 'delivering'
     * @talk.preconditions.client client must be a recipient of the message
     * @talk.behavior.server update delivery and notify sender via outgoingDelivery notification
     * @talk.behavior.client
     * @talk.statechanges.serverobjects update state in delivery object to 'delivered', update timeChanged
     * @talk.errors.server
     **/
    TalkDelivery   inDeliveryConfirmUnseen(String messageId);
    TalkDelivery   inDeliveryConfirmSeen(String messageId);
    TalkDelivery   inDeliveryConfirmPrivate(String messageId);

    /** Acknowledge reception of outgoing message delivery confirmation
     * @param messageId is the server-provided message id
     * @param recipientId is the client id of a recipient, equaling the receiverId from the delivery objects
     * @return a delivery object with updated timeUpdated and status 'confirmed'
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server delivery object must exist and be in state 'delivered'
     * @talk.preconditions.client client must be the sender of the message
     * @talk.behavior.server update delivery and notify sender via outgoingDelivery notification
     * @talk.behavior.client
     * @talk.statechanges.serverobjects update state in delivery object to 'confirmed', update timeChanged
     * @talk.errors.server
     **/
    TalkDelivery   outDeliveryAcknowledgeSeen(String messageId, String recipientId);
    TalkDelivery   outDeliveryAcknowledgeUnseen(String messageId, String recipientId);
    TalkDelivery   outDeliveryAcknowledgePrivate(String messageId, String recipientId);

    // As sender, acknowledge a "failed" delivery
    TalkDelivery   outDeliveryAcknowledgeFailed(String messageId, String recipientId);

    // As sender, acknowledge a "rejected" delivery
    TalkDelivery   outDeliveryAcknowledgeRejected(String messageId, String recipientId);

    /**
     * Reject/abort a delivery
     * @param messageId is the server-provided message id
     * @param recipientId is the client id of a recipient or the own id, equaling the receiverId from the delivery objects;
     when recipientId is the own ID, this method will abort an incoming delivery that got stuck.
     If it is the ID of another client, the corresponding outgoing message will be aborted.
     * @return a delivery object with updated timeUpdated and status 'aborted'
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server delivery object must exist not be in already in state 'confirmed' or 'aborted'
     * @talk.preconditions.client none
     * @talk.behavior.server update delivery and notify sender via outgoingDelivery notification
     * @talk.behavior.client
     * @talk.statechanges.serverobjects update state in delivery object to 'aborted', update timeChanged
     * @talk.errors.server
     **/

    // abort a delivery as sender
    TalkDelivery outDeliveryAbort(String messageId, String recipientId);

    // inform the server that a delivery is unknown to you as sender
    void outDeliveryUnknown(String messageId, String recipientId);

    // inform the server that a delivery is unknown to you as receiver
    void inDeliveryUnknown(String messageId);

    // reject a delivery as receiver
    TalkDelivery inDeliveryReject(String messageId, String reason);

    /**
     * Create a new group on the server
     * @param groupPresence is a record describing the group parameters
     * @return the id assigned to the group
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server create a group and assign an id, add client as admin member
     * @talk.behavior.client
     * @talk.statechanges.serverobjects create and store a new group with new unique group id, add connected client as admin member
     * @talk.errors.server
     **/
    @Deprecated
    String createGroup(TalkGroupPresence groupPresence);

    TalkGroupPresence createGroupWithMembers(String groupType, String groupTag, String groupName, String[] members, String[] roles);

    /** Retrieve array of groups changed after given date
     * @param lastKnown is the date in milliseconds since start of year 1970
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server none
     * @talk.preconditions.client none
     * @talk.behavior.server query database for all groups changed after given date and return result
     * @talk.behavior.client call getGroups after and only after login once
     * @talk.statechanges.serverobjects none
     * @talk.errors.server
     */
    TalkGroupPresence[] getGroups(Date lastKnown);

    TalkGroupPresence getGroup(String groupId);

    TalkGroupMembership getGroupMember(String groupId, String clientId);

    /** Update group name and group avatar; this function is deprecated, use updateGroupAvatar and updateGroupName instead
     * @param groupPresence is the group to update
     * @talk.preconditions client must be logged in, connected client must be admin member of the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server
     * @talk.behavior.client
     * @talk.statechanges.serverobjects Update group name and group avatar
     * @talk.errors.server
     * @talk.todo remove from API
     */
    void updateGroup(TalkGroupPresence groupPresence);

    /** delete group with given id
     * @param groupId denotes the group to delete
     * @talk.preconditions client must be logged in, connected client must be admin member of the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server notify members via groupUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects Set group state to none, update timeChanged
     * @talk.errors.server
     */
    void deleteGroup(String groupId);

    /** invite a friend to become member of a group
     * @param groupId denotes the group to delete
     * @param clientId denotes the contact to invite
     * @talk.preconditions client must be logged in, connected client must be admin member of the group, clientId must be a friend
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server add clientId as group member, notify new member via groupUpdated and all members via groupMemberUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects add clientId as group member with state 'invited', update timeChanged
     * @talk.errors.server
     */
    void inviteGroupMember(String groupId, String clientId);

    /** join a group we have been invited to
     * @param groupId denotes the group to join
     * @talk.preconditions client must be logged in, connected client must be invited to the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server add clientId as group member, notify members and groupMemberUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects update member state to 'joined', update timeChanged
     * @talk.errors.server
     */
    void joinGroup(String groupId);

    /** join a group we have been invited to
     * @param groupId denotes the group to leave
     * @talk.preconditions client must be logged in, connected client must be 'invited' or 'joined' member
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server clear membership of clientId, notify members via and groupMemberUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects update member state to 'none', update timeChanged
     * @talk.errors.server
     * @talk.todo currently nothing on the server prevents the last admin from abandoning a group, client must ensure this
     */
    void leaveGroup(String groupId);

    /** set the notification preferences for my group membership
     * @param groupId denotes the group to set the connected client's notification preferences for
     * @param preference currently a string containing 'enabled' or 'disabled'
     * @talk.preconditions client must be logged in, connected client must be 'invited' or 'joined' member
     */
    void setGroupNotifications(String groupId, String preference);


    /** set the notification preferences for a contact
     * @param otherClientId denotes the contact to set the connected client's notification preferences for
     * @param preference currently a string containing 'enabled' or 'disabled'
     * @talk.preconditions client must be logged in, connected client must have a relationship with otherClientId
     */
    void setClientNotifications(String otherClientId, String preference);


    /** Change group name
     * @param groupId denotes the group to rename
     * @param name the new group name
     * @talk.preconditions client must be logged in, connected client must be admin member of the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server Update group name and notify all members via groupUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects Update group name
     * @talk.errors.server
     */
    void updateGroupName(String groupId, String name);

    /** Change group group avatar url
     * @param groupId denotes the group to rename
     * @param avatarUrl the new avatar URL; can be empty to set default avatar
     * @talk.preconditions client must be logged in, connected client must be admin member of the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server Update group avatar url and notify all members via groupUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects Update group avatar url
     * @talk.errors.server
     */
    void updateGroupAvatar(String groupId, String avatarUrl);

    /** Update role of client in group; currently only 'member' and 'admin' are define as role
     * @param groupId denotes the group to rename
     * @param clientId denotes the member to assign the role to
     * @param role the new role, either 'admin' or 'member'
     * @talk.preconditions client must be logged in, connected client must be admin member of the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server Update group member role and notify all members via groupUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects Update group role
     * @talk.errors.server
     */
    void updateGroupRole(String groupId, String clientId, String role);

    /** Retrieve members of a group changed after given date
     * @param groupId denotes the group to retrieve the members of
     * @param lastKnown is the date in milliseconds since start of year 1970
     * @return array of relationship objects that have a change date greater than the given date
     * @talk.preconditions client must be logged in, connected client must be member of the group or invited to join
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server query database for all group members changed after given date and return result
     * @talk.statechanges.serverobjects none
     * @talk.errors.server
     */
    TalkGroupMembership[] getGroupMembers(String groupId, Date lastKnown);

    /** remove (kick) a member from a group
     * @param groupId denotes the group from which the member is to be removed
     * @param clientId denotes the member to remove
     * @talk.preconditions client must be logged in, connected client must be admin member of the group
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server set denoted client's membership state to 'none', notify all members via groupMemberUpdated
     * @talk.behavior.client
     * @talk.statechanges.serverobjects sets denoted client's membership state to 'none', update timeChanged
     * @talk.errors.server
     */
    void removeGroupMember(String groupId, String clientId);

    /** get URLs for file storage, currently only used for avatars.
     Files created this way on a server are only removed when all references to it are removed, which right now does not happen.
     * @param contentLength denotes the size of the file to be transfered in bytes; must be exact
     * @return a structure containing a fileId for further reference and an uploadUrl and a downloadUrl
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server create storage and maintain lifecycle
     * @talk.behavior.client
     * @talk.statechanges.serverobjects add and store references to this file
     * @talk.errors.server
     */
    FileHandles createFileForStorage(int contentLength);

    /** get URLs for file transer, currently only used for attachments.
     Files created this way on a server are only removed after succesful transfer or an expiry time has lapsed.
     * @param contentLength denotes the size of the file to be transfered in bytes; must be exact
     * @return a structure containing a fileId for further reference and an uploadUrl and a downloadUrl
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server create storage and maintain lifecycle; remove file after transfer or expiry
     * @talk.behavior.client
     * @talk.statechanges.serverobjects add and store references to this file
     * @talk.errors.server
     */
    FileHandles createFileForTransfer(int contentLength);

    public static final class FileHandles {
        public String fileId;
        public String uploadUrl;
        public String downloadUrl;
    }

    // can be called by the creator/uploader to remove it in case the transfer was cancelled
    //void deleteFile(String fileId);

    // should be called by the receiver of an transfer file after download; the server can the delete the file in case
    String receivedFile(String fileId);

    // should be called by the receiver of an transfer file if the user has aborted the download
    String abortedFileDownload(String fileId);

    // should be called by the receiver of an transfer file if the client has exceeded the download retry count
    String failedFileDownload(String fileId);

    // should be called by the receiver of an transfer file when a final attachment sender set state has been seen
    String acknowledgeAbortedFileUpload(String fileId);
    String acknowledgeFailedFileUpload(String fileId);

    //------ sender attachment state indication methods
    // should be called by the sender of an transfer file after upload has been started
    String startedFileUpload(String fileId);

    // should be called by the sender of an transfer file when the upload has been paused
    String pausedFileUpload(String fileId);

    // should be called by the sender of an transfer file after upload has been finished
    String finishedFileUpload(String fileId);

    // should be called by the sender of an transfer file when the upload is aborted by the user
    String abortedFileUpload(String fileId);

    // should be called by the sender of an transfer file when upload retry count has been exceeded
    String failedFileUpload(String fileId);

    // should be called by the sender of an transfer file when a final attachment receiver set state has been seen
    String acknowledgeReceivedFile(String fileId, String receiverId);
    String acknowledgeAbortedFileDownload(String fileId, String receiverId);
    String acknowledgeFailedFileDownload(String fileId, String receiverId);

    /** provide environment record for location based grouping
     * @param environment denotes the geoposition and other environment data for grouping
     * @return a group id for a transient group id the client is in
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server store environment and group
     * @talk.behavior.client
     * @talk.statechanges.serverobjects changes environment, creates, destroys or modifies groups and adds or removes group members
     * @talk.errors.server
     */
    String updateEnvironment(TalkEnvironment environment);

    /** end participation in location based or worldwide grouping
     * @param type denotes the type of the environment; only one environment of each type can exist on the server
     * @talk.preconditions client must be logged in
     * @talk.preconditions.server
     * @talk.preconditions.client
     * @talk.behavior.server remove client from group and destroy group if client is last member
     * @talk.behavior.client
     * @talk.statechanges.serverobjects changes environment, creates, destroys or modifies groups and adds or removes group members
     * @talk.errors.server
     */
    void destroyEnvironment(String type);

    /** begin time to live expiry, used for worldwide grouping
     *
     * @param type denotes the type of the environment; only one environment of each type can exist on the server
     * @talk.preconditions client must be logged in
     */
    void releaseEnvironment(String type);
    void releaseEnvironmentUpdatingParameters(String type, long timeToLive, String notificationPreference);

    // returns true for each group where you are an active or invited group member
    Boolean[] isMemberInGroups(String[] groupIds);

    // return true if for each client the caller is related to by a relationsShip or by an active group membership
    Boolean[] isContactOf(String[] clientIds);

    // returns true for each client that is an active or invited group member
    // throws exception if you are not an active or invited group member
    Boolean[] areMembersOfGroup(String groupId, String[] clientIds);

    // return total number of undelivered and unfinished deliveries from a particular client
    // used by the client to decide when a contact can be finally deleted
    long unfinishedDeliveriesFrom(String senderId);
}

/**
 * Add the following command line arguments when calling javadoc
 -tag talk.preconditions:a:"Preconditions"
 -tag talk.preconditions.server:a:"Preconditions Server"
 -tag talk.preconditions.client:a:"Preconditions Client"
 -tag talk.behavior.server:a:"Expected Server Behavior"
 -tag talk.behavior:a:"Expected System Behavior"
 -tag talk.behavior.client:a:"Expected Client Behavior"
 -tag talk.statechanges.serverobjects:a:"Server Object State Changes"
 -tag talk.statechanges.clientobjects:a:"Client Object State Changes"
 -tag talk.ui.client:a:"Client User Interface Behavior"
 -tag talk.errors.server:a:"Server Side Errors"
 -tag talk.errors.client:a:"Client Side Errors"
 -tag talk.todo:a:"TODO"
 */
