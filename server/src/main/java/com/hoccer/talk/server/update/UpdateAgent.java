package com.hoccer.talk.server.update;

import com.hoccer.talk.model.*;
import com.hoccer.talk.rpc.ITalkRpcClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.agents.NotificationDeferrer;
import com.hoccer.talk.server.message.StaticSystemMessage;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import com.hoccer.talk.util.MapUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Agent for simple updates (presence, group presence, relationship)
 */
public class UpdateAgent extends NotificationDeferrer {
    private final TalkServer mServer;

    private final ITalkServerDatabase mDatabase;

    private static final ThreadLocal<ArrayList<Runnable>> context = new ThreadLocal<ArrayList<Runnable>>();

    private final static Long MAX_ALLOWED_KEY_REQUEST_LATENCY = 10000L;

    public UpdateAgent(TalkServer server) {
        super(
            server.getConfiguration().getUpdateAgentThreadPoolSize(),
            "update-agent"
        );
        mServer = server;
        mDatabase = mServer.getDatabase();
    }

    private void updateConnectionStatus(TalkPresence presence) {
        // determine the connection status of the client
        boolean isConnected = mServer.isClientConnected(presence.getClientId());
        if (presence.getConnectionStatus() == null || isConnected != presence.isConnected()) {
            String connStatus = isConnected ? TalkPresence.STATUS_ONLINE
                    : TalkPresence.STATUS_OFFLINE;
            LOG.debug("Persisting connection status '" + connStatus + "' for client's presence. ClientId: '" + presence.getClientId() + "'");
            presence.setConnectionStatus(connStatus);
            mDatabase.savePresence(presence);
        }
    }

    // send the presence of all other members of <groupId> to the group member <clientId>
    public void requestPresenceUpdateForClientOfMembersOfGroup(final String clientId, final String groupId) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug("RPUFG: update " + clientId + " for group " + groupId);
                    TalkRpcConnection conn = mServer.getClientConnection(clientId);
                    if (conn == null || !conn.isConnected()) {
                        return;
                    }
                    ITalkRpcClient rpc = conn.getClientRpc();
                    try {
                        TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, clientId);
                        if (membership.isInvited() || membership.isJoined() || membership.isSuspended()) {
                            List<TalkGroupMembership> otherMemberships = mDatabase.findGroupMembershipsById(groupId);
                            for (TalkGroupMembership otherMembership : otherMemberships) {
                                if (!clientId.equals(otherMembership.getClientId())) {
                                    if (otherMembership.isJoined() || otherMembership.isInvited() || otherMembership.isSuspended()) {
                                        String otherClientId = otherMembership.getClientId();
                                        LOG.trace("RPUFG: delivering presence of " + otherClientId + " to "+clientId);
                                        TalkPresence presence = mDatabase.findPresenceForClient(otherClientId);
                                        if (presence.getConnectionStatus() == null) {
                                            updateConnectionStatus(presence);
                                        }
                                        // Calling Client via RPC
                                        rpc.presenceUpdated(presence);
                                    } else {
                                        LOG.trace("RPUFG: target " + otherMembership.getClientId() + " is not invited or joined");
                                    }
                                } else {
                                    LOG.trace("RPUFG: not sending presence update for group " + membership.getGroupId()+" to self "+clientId);
                                }
                            }
                        } else {
                            LOG.debug("RPUFG: not invited or joined in group " + membership.getGroupId());
                        }
                    } catch (Throwable t) {
                        LOG.error("exception in runnable", t);
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    // send presence updates of <clientId>  to <targetClientId>
    public void requestPresenceUpdateForClient(final String clientId, final String targetClientId) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug("RPUC: updating " + targetClientId + " with presence of " + clientId);
                    TalkPresence presence = mDatabase.findPresenceForClient(clientId);
                    TalkRpcConnection targetConnection = mServer.getClientConnection(targetClientId);
                    if (targetConnection == null || !targetConnection.isConnected()) {
                        LOG.debug("RPUC: target not connected");
                        return;
                    }
                    updateConnectionStatus(presence);
                    try {
                        // Calling Client via RPC
                        targetConnection.getClientRpc().presenceUpdated(presence);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    // send presence updates to all related clients of <clientId>
    public void requestPresenceUpdate(final String clientId, final Set<String> fields) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    // retrieve the current presence of the client
                    TalkPresence presence = mDatabase.findPresenceForClient(clientId);
                    // if we actually have a presence
                    if (presence != null) {
                        if (fields == null || fields.contains(TalkPresence.FIELD_CONNECTION_STATUS)) {
                            updateConnectionStatus(presence);
                        }
                        // propagate the presence to all friends
                        performPresenceUpdate(presence, fields);
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    // send presence updates to all related clients of the client denoted by <presence>
    private void performPresenceUpdate(TalkPresence presence, final Set<String> fields) {
        String tag = "RPU-" + presence.getClientId() + ": ";
        LOG.trace(tag + "commencing");

        // own client id
        String selfClientId = presence.getClientId();

        // set to collect clientIds into
        Set<String> clientIds = new HashSet<String>();

        // collect clientIds known through relationships (have a relationship to me)
        List<TalkRelationship> relationships = mDatabase.findRelationshipsByOtherClient(selfClientId);
        for (TalkRelationship relationship : relationships) {
            if (relationship.isRelated()) {
                // if the other clients relation is anything but 'none'
                // Clients that have blocked present client therefore will also be included here
                LOG.trace(tag + "including friend " + relationship.getClientId());
                clientIds.add(relationship.getClientId());
            }
        }
        // collect clientIds known through groups
        List<TalkGroupMembership> ownMemberships = mDatabase.findGroupMembershipsForClient(selfClientId);
        for (TalkGroupMembership ownMembership : ownMemberships) {
            String groupId = ownMembership.getGroupId();
            if (ownMembership.isJoined() || ownMembership.isInvited() || ownMembership.isSuspended()) {
                LOG.trace(tag + "scanning group " + groupId);
                List<TalkGroupMembership> otherMemberships = mDatabase.findGroupMembershipsById(groupId);
                for (TalkGroupMembership otherMembership : otherMemberships) {
                    if (otherMembership.isJoined() || otherMembership.isInvited() || otherMembership.isSuspended()) {
                        LOG.trace(tag + "including group member " + otherMembership.getClientId());
                        clientIds.add(otherMembership.getClientId());
                    } else {
                        LOG.trace(tag + "not including group member " + otherMembership.getClientId() + " in state " + otherMembership.getState());
                    }
                }
            }
        }
/*
        // It is better to distribute the presence even to blocking clients
        // because there are some corner cases like the connection flag and
        // possibly others that need to be handled; we can better live
        // with updating the presence to blocking clients and request from
        // the user to delete a contact when he does not want to know anything
        // about it.

        // The main hazard is that a client might change it's avatar frequently
        // causing a blocker to unnecessarily download the avatar.
        // TODO: implement some countermeasures against this (this might as well be done on the client side by not retrieving avatars of blocked clients)

        // do not send presence to clients the present client has blocked
        List<TalkRelationship> my_relationships = mDatabase.findRelationships(selfClientId);
        for (TalkRelationship relationship : my_relationships) {
            if (relationship.isBlocked()) {
                if (clientIds.contains(relationship.getClientId())) {
                    LOG.trace(tag + "excluding blocked contact " + relationship.getClientId());
                    clientIds.remove(relationship.getClientId());
                }
            }
        }
*/
        // remove self
        LOG.trace(tag + "excluding self " + selfClientId);
        clientIds.remove(selfClientId);

        TalkPresence modifiedPresence = null;
        if (fields != null) {
            modifiedPresence = new TalkPresence();
            modifiedPresence.updateWith(presence, fields);
        }

        // send presence updates
        for (String clientId : clientIds) {
            // look for a connection by the other clientId
            TalkRpcConnection connection = mServer.getClientConnection(clientId);
            // and if the corresponding clientId is online
            if (connection != null && connection.isLoggedIn()) {
                LOG.trace(tag + "clientId " + clientId + " is connected");
                try {
                    // Calling Client via RPC
                    // tell the clientId about the new presence
                    if (fields == null) {
                        connection.getClientRpc().presenceUpdated(presence);
                    } else {
                        connection.getClientRpc().presenceModified(modifiedPresence);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } else {
                LOG.trace(tag + "clientId " + clientId + " is disconnected");
            }
        }
        LOG.trace(tag + "complete");
    }

    public void requestRelationshipUpdate(final TalkRelationship relationship) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkRpcConnection clientConnection = mServer.getClientConnection(relationship.getClientId());
                    if (clientConnection != null && clientConnection.isLoggedIn()) {
                        // Calling Client via RPC
                        clientConnection.getClientRpc().relationshipUpdated(relationship);
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    public void requestGroupUpdate(final String groupId, final String clientId) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkGroupPresence updatedGroupPresence = mDatabase.findGroupPresenceById(groupId);
                    if (updatedGroupPresence != null) {
                        TalkRpcConnection connection = mServer.getClientConnection(clientId);
                        if (connection == null || !connection.isConnected()) {
                            return;
                        }

                        // Calling Client via RPC
                        ITalkRpcClient rpc = connection.getClientRpc();
                        try {
                            rpc.groupUpdated(updatedGroupPresence);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    public void requestGroupUpdate(final String groupId) {
        Runnable notification = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkGroupPresence updatedGroupPresence = mDatabase.findGroupPresenceById(groupId);
                    if (updatedGroupPresence != null) {
                        List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsById(groupId);
                        for (TalkGroupMembership membership : memberships) {
                            if (membership.isJoined() || membership.isInvited() || membership.isSuspended() || membership.isGroupRemoved()) {
                                if (membership.getClientId() == null) {
                                    LOG.error("requestGroupUpdate for group " + groupId + ", no clientID for a membership record");
                                    continue;
                                }
                                TalkRpcConnection connection = mServer.getClientConnection(membership.getClientId());
                                if (connection == null || !connection.isConnected()) {
                                    continue;
                                }

                                // Calling Client via RPC
                                ITalkRpcClient rpc = connection.getClientRpc();
                                try {
                                    rpc.groupUpdated(updatedGroupPresence);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notification);
    }

    // will just send out a change to the single client the membership belongs to
    public void requestGroupMembershipUpdate(final TalkGroupMembership membership) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkRpcConnection clientConnection = mServer.getClientConnection(membership.getClientId());
                    if (clientConnection != null && clientConnection.isLoggedIn()) {
                        // Calling Client via RPC
                        clientConnection.getClientRpc().groupMemberUpdated(membership);
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }


    public void requestGroupMembershipUpdate(final String groupId, final String clientId) {
        LOG.debug("requestGroupMembershipUpdate for group " + groupId + " client " + clientId);
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkGroupMembership updatedMembership = mDatabase.findGroupMembershipForClient(groupId, clientId);
                    if (updatedMembership == null) {
                        LOG.debug("requestGroupMembershipUpdate updatedMember is null");
                        return;
                    }
                    TalkGroupMembership foreignMembership = new TalkGroupMembership();
                    foreignMembership.foreignUpdateWith(updatedMembership);

                    List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsById(groupId);
                    LOG.debug("requestGroupMembershipUpdate found " + memberships.size() + " members");
                    boolean someOneWasNotified = false;
                    for (TalkGroupMembership membership : memberships) {
                        if (membership.isJoined() || membership.isInvited() || membership.isSuspended() || membership.isGroupRemoved() || membership.getClientId().equals(clientId)) {
                            TalkRpcConnection connection = mServer.getClientConnection(membership.getClientId());
                            if (connection == null || !connection.isConnected()) {
                                LOG.trace("requestGroupMembershipUpdate - refrain from updating not connected member client " + membership.getClientId());
                                continue;
                            }

                            // Calling Client via RPC
                            ITalkRpcClient rpc = connection.getClientRpc();
                            try {
                                if (membership.getClientId().equals(clientId)) {
                                    // is own membership
                                    rpc.groupMemberUpdated(updatedMembership);
                                } else {
                                    rpc.groupMemberUpdated(foreignMembership);
                                }
                                someOneWasNotified = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            LOG.trace("requestGroupMembershipUpdate - not updating client " + membership.getClientId() + ", state=" + membership.getState() + ", self=" + membership.getClientId().equals(clientId));
                        }
                    }
                    if (someOneWasNotified) {
                        checkAndRequestGroupMemberKeys(groupId);
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    // call once for a new group member, will send out groupMemberUpdated-Notifications to new member with all other group members
    public void requestGroupMembershipUpdatesForNewMember(final String groupId, final String newMemberClientId) {
        LOG.debug("requestGroupMembershipUpdateForNewMember for group " + groupId + " newMemberClientId " + newMemberClientId);
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkGroupMembership newMembership = mDatabase.findGroupMembershipForClient(groupId, newMemberClientId);
                    if (newMembership == null) {
                        LOG.debug("requestGroupMembershipUpdateForNewMember can't find newMember, is null");
                        return;
                    }
                    List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsById(groupId);
                    LOG.debug("requestGroupMembershipUpdateForNewMember found " + memberships.size() + " members");
                    TalkRpcConnection connection = mServer.getClientConnection(newMembership.getClientId());
                    if (connection == null || !connection.isConnected()) {
                        LOG.debug("requestGroupMembershipUpdateForNewMember - new client no longer connected " + newMembership.getClientId());
                        return;
                    }
                    // Calling Client via RPC
                    ITalkRpcClient rpc = connection.getClientRpc();
                    for (TalkGroupMembership membership : memberships) {
                        // do not send out updates for own membership or dead members
                        if (!membership.getClientId().equals(newMemberClientId) && (membership.isJoined() || membership.isInvited() || membership.isSuspended())) {
                            try {
                                membership.setEncryptedGroupKey(null);
                                rpc.groupMemberUpdated(membership);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            LOG.debug("requestGroupMembershipUpdateForNewMember - not updating with member " + membership.getClientId() + ", state=" + membership.getState());
                        }
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    public ArrayList<Pair<TalkGroupMembership, Long>> membershipsSortedByLatency(List<TalkGroupMembership> memberships) {

        Map<TalkGroupMembership, Long> membershipsByLatency = new HashMap<TalkGroupMembership, Long>();
        for (TalkGroupMembership membership : memberships) {
            TalkRpcConnection connection = mServer.getClientConnection(membership.getClientId());
            if (connection != null) {
                // if we dont have a latency, assume something bad
                Long latency = connection.getLastPingLatency();
                if (latency != null) {
                    membershipsByLatency.put(membership, latency + connection.getCurrentPriorityPenalty());
                } else {
                    membershipsByLatency.put(membership, 5000L + connection.getCurrentPriorityPenalty());
                }
            }
        }
        membershipsByLatency = MapUtil.sortByValue(membershipsByLatency);

        ArrayList<Pair<TalkGroupMembership, Long>> result = new ArrayList<Pair<TalkGroupMembership, Long>>();
        for (Map.Entry<TalkGroupMembership, Long> entry : membershipsByLatency.entrySet()) {
            result.add(new ImmutablePair<TalkGroupMembership, Long>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public void checkAndRequestGroupMemberKeys(final String groupId) {
        LOG.debug("checkAndRequestGroupMemberKeys for group " + groupId);
        Runnable checker = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkServer.NonReentrantLock lock = mServer.idLockNonReentrant("groupKeyCheck-"+groupId);

                    boolean acquired = lock.tryLock();
                    LOG.debug("checkAndRequestGroupMemberKeys trylock groupId: '" + groupId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+"acquired="+acquired+" waiting="+lock.getWaiting());

                    if (!acquired && lock.getWaiting() > 0) {
                        // we are sure that that there are other threads waiting to be performed
                        // so we can just throw away this request
                        LOG.debug("checkAndRequestGroupMemberKeys enough waiters, throwing away request: '" + groupId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+"acquired="+acquired+" waiting="+lock.getWaiting());
                        return;
                    }

                    try {
                        LOG.debug("checkAndRequestGroupMemberKeys will lock groupId: '" + groupId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread());
                        if (!acquired) {
                            lock.lock();
                        }
                        LOG.debug("checkAndRequestGroupMemberKeys acquired lock for groupId: '" + groupId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread());
                        performCheckAndRequestGroupMemberKeys(groupId);
                        LOG.debug("checkAndRequestGroupMemberKeys ready for groupId: '" + groupId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread());
                    } catch (InterruptedException e) {
                        LOG.debug("checkAndRequestGroupMemberKeys: interrupted" + e);
                    } finally {
                        LOG.debug("checkAndRequestGroupMemberKeys releasing lock for groupId: '" + groupId + "' with id "+lock+", hash="+lock.hashCode()+",thread="+Thread.currentThread());
                        lock.unlock();
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
               }
            }
        };
        queueOrExecute(context, checker);
    }

    private void performCheckAndRequestGroupMemberKeys(String groupId) {
        LOG.debug("performCheckAndRequestGroupMemberKeys for groupId: '" + groupId + "'");
        TalkGroupPresence groupPresence = mDatabase.findGroupPresenceById(groupId);
        if (groupPresence != null && groupPresence.exists()) {

            List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsByIdWithStates(groupPresence.getGroupId(), TalkGroupMembership.ACTIVE_STATES);
            LOG.debug("  * members: " + memberships.size());
            if (!memberships.isEmpty()) {
                List<TalkGroupMembership> outOfDateMemberships = new ArrayList<TalkGroupMembership>();
                List<TalkGroupMembership> keyMasterCandidatesWithCurrentKey = new ArrayList<TalkGroupMembership>();
                List<TalkGroupMembership> keyMasterCandidatesWithoutCurrentKey = new ArrayList<TalkGroupMembership>();

                String sharedKeyId = groupPresence.getSharedKeyId();
                String sharedKeyIdSalt = groupPresence.getSharedKeyIdSalt();
                if (sharedKeyId == null) {
                    // nobody has supplied a group key yet
                    LOG.debug("  * nobody has supplied a group key yet...");
                    for (TalkGroupMembership membership : memberships) {
                        TalkPresence presence = mDatabase.findPresenceForClient(membership.getClientId());
                        if (presence != null && presence.getKeyId() != null) {
                            if (membership.isMember() && mServer.isClientReady(membership.getClientId())) {
                                keyMasterCandidatesWithoutCurrentKey.add(membership);
                            }
                            outOfDateMemberships.add(membership);
                        } else {
                            LOG.error("checkAndRequestGroupMemberKeys: no presence for client " + membership.getClientId() + ", member of group " + groupId);
                        }
                    }
                } else {
                    // there is a group key
                    LOG.debug("  * There is a group key...");
                    for (TalkGroupMembership membership : memberships) {
                        TalkPresence presence = mDatabase.findPresenceForClient(membership.getClientId());
                        if (presence != null && presence.getKeyId() != null) {
                            if (!sharedKeyId.equals(membership.getSharedKeyId()) || !membership.getMemberKeyId().equals(presence.getKeyId())) {
                                // member has not the current key
                                LOG.debug("  * Member "+membership.getClientId()+" has not the current group key");
                                outOfDateMemberships.add(membership);
                                if (membership.isMember() && mServer.isClientReady(membership.getClientId())) {
                                    LOG.debug("  * Member "+membership.getClientId()+" is ready and active and added to keyMasterCandidatesWithoutCurrentKey");
                                    keyMasterCandidatesWithoutCurrentKey.add(membership);
                                }
                            } else {
                                // member has the current key
                                LOG.debug("  * Member "+membership.getClientId()+" has the current group key");
                                if (membership.isMember() && mServer.isClientReady(membership.getClientId())) {
                                    LOG.debug("  * Member "+membership.getClientId()+" is ready and active and added to keyMasterCandidatesWithCurrentKey");
                                    keyMasterCandidatesWithCurrentKey.add(membership);
                                }
                            }
                        } else {
                            LOG.error("checkAndRequestGroupMemberKeys:(2) no presence for client " + membership.getClientId() + ", member of group " + groupId);
                        }
                    }
                }
                if (!outOfDateMemberships.isEmpty()) {
                    LOG.debug("  * There are members (" + outOfDateMemberships.size() + ") without a current group key - need to issue a rekeying...");
                    // we need request some keys
                    if (!keyMasterCandidatesWithCurrentKey.isEmpty()) {
                        // prefer candidates that already have a key
                        ArrayList<Pair<TalkGroupMembership, Long>> candidatesByLatency = membershipsSortedByLatency(keyMasterCandidatesWithCurrentKey);
                        TalkGroupMembership newKeymaster;
                        if (candidatesByLatency.get(0).getRight() < MAX_ALLOWED_KEY_REQUEST_LATENCY) {
                            newKeymaster = candidatesByLatency.get(0).getLeft(); // get the lowest latency candidate
                            requestGroupKeys(newKeymaster.getClientId(), groupPresence.getGroupId(), sharedKeyId, sharedKeyIdSalt, outOfDateMemberships);
                            return;
                        }
                        // fall through to next block if best candidate does not meet MAX_ALLOWED_KEY_REQUEST_LATENCY
                    }
                    if (!keyMasterCandidatesWithoutCurrentKey.isEmpty()) {
                        // nobody with a current key is online, but we have other admins online, so purchase a new group key
                        ArrayList<Pair<TalkGroupMembership, Long>> candidatesByLatency = membershipsSortedByLatency(keyMasterCandidatesWithoutCurrentKey);
                        TalkGroupMembership newKeymaster;
                        if (candidatesByLatency.get(0).getRight() < MAX_ALLOWED_KEY_REQUEST_LATENCY) {
                            newKeymaster = candidatesByLatency.get(0).getLeft(); // get the lowest latency candidate
                            requestGroupKeys(newKeymaster.getClientId(), groupPresence.getGroupId(), null, null, outOfDateMemberships);
                            return;
                        }
                        // fall through to next block if best candidate does not meet MAX_ALLOWED_KEY_REQUEST_LATENCY
                    }
                    // we have out of date key members, but no suitable candidate for group key generation
                    LOG.warn("performCheckAndRequestGroupMemberKeys:" + outOfDateMemberships.size() + " members have no key in group " + groupId + ", but no suitable keymaster available");
                }
            }
        }
    }

    private void requestGroupKeys(String fromClientId, String forGroupId, String forSharedKeyId, String withSharedKeyIdSalt, List<TalkGroupMembership> forOutOfDateMemberships) {
        ArrayList<String> forClientIdsList = new ArrayList<String>();
        ArrayList<String> withPublicKeyIdsList = new ArrayList<String>();
        for (TalkGroupMembership membership : forOutOfDateMemberships) {
            TalkPresence presence = mDatabase.findPresenceForClient(membership.getClientId());
            if (presence != null && presence.getKeyId() != null) {
                withPublicKeyIdsList.add(presence.getKeyId());
                forClientIdsList.add(membership.getClientId());
                LOG.debug("requestGroupKeys, added client='" + membership.getClientId() + "', keyId='" + presence.getKeyId() + "'");
            } else {
                LOG.error("requestGroupKeys, failed to add client='" + membership.getClientId() + "'");
            }
        }
        if (!withPublicKeyIdsList.isEmpty() && withPublicKeyIdsList.size() == forClientIdsList.size()) {
            String[] forClientIds = forClientIdsList.toArray(new String[forClientIdsList.size()]);
            String[] withPublicKeyIds = withPublicKeyIdsList.toArray(new String[withPublicKeyIdsList.size()]);
            TalkRpcConnection connection = mServer.getClientConnection(fromClientId);
            if (forSharedKeyId == null) {
                forSharedKeyId = "RENEW";
                withSharedKeyIdSalt = "RENEW";
            }
            ITalkRpcClient rpc = connection.getClientRpc();
            LOG.debug("requestGroupKeys, acquiring lock for calling getEncryptedGroupKeys(" + forGroupId + ") on client for " + forClientIds.length + " client(s)");
            String[] newKeyBoxes = null;
            // serialize encrypted key request for one client
            synchronized (connection.keyRequestLock) {
                LOG.debug("requestGroupKeys, calling getEncryptedGroupKeys(" + forGroupId + ") on client for " + forClientIds.length + " client(s)");
                // temporarily add penalty so this client won't be selected again unless there is no other who can do the work
                connection.penalizePriorization(1000);
                try {
                    newKeyBoxes = rpc.getEncryptedGroupKeys(forGroupId, forSharedKeyId, withSharedKeyIdSalt, forClientIds, withPublicKeyIds);
                    connection.penalizePriorization(-1000);
                    LOG.debug("requestGroupKeys, call of getEncryptedGroupKeys(" + forGroupId + ") returned " + newKeyBoxes.length + " items)");
                } catch (Exception e) {
                    LOG.error("An error occured for calling getEncryptedGroupKeys -> most proably because the connection is actually not valid anymore.");
                    e.printStackTrace();
                }
            }
            if (newKeyBoxes != null) {
                boolean responseLengthOk;
                if ("RENEW".equals(forSharedKeyId)) {
                    // call return array with two additional
                    responseLengthOk = newKeyBoxes.length == forClientIds.length + 2;
                    if (responseLengthOk) {
                        forSharedKeyId = newKeyBoxes[forClientIds.length];
                        withSharedKeyIdSalt = newKeyBoxes[forClientIds.length + 1];
                    }
                } else {
                    responseLengthOk = newKeyBoxes.length == forClientIds.length;
                }
                if (responseLengthOk) {
                    connection.resetPriorityPenalty(0L);
                    Date now = new Date();

                    TalkGroupPresence groupPresence = mDatabase.findGroupPresenceById(forGroupId);
                    groupPresence.setSharedKeyId(forSharedKeyId);
                    groupPresence.setSharedKeyIdSalt(withSharedKeyIdSalt);
                    groupPresence.setLastChanged(now);
                    LOG.debug("requestGroupKeys, for group '" + forGroupId + "' did set sharedKeyId " + groupPresence.getSharedKeyId() + ", salt="+groupPresence.getSharedKeyIdSalt());
                    mDatabase.saveGroupPresence(groupPresence);

                    for (int i = 0; i < forClientIds.length; ++i) {
                        TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(forGroupId, forClientIds[i]);
                        membership.setSharedKeyId(forSharedKeyId);
                        membership.setSharedKeyIdSalt(withSharedKeyIdSalt);
                        LOG.debug("requestGroupKeys, for member '" + membership.getClientId() + "' did set sharedKeyId " + membership.getSharedKeyId() + ", salt="+membership.getSharedKeyIdSalt());
                        membership.setMemberKeyId(withPublicKeyIds[i]);
                        membership.setEncryptedGroupKey(newKeyBoxes[i]);
                        membership.setKeySupplier(fromClientId);
                        membership.setSharedKeyDate(now); // TODO: remove this and other fields no longer required
                        membership.setLastChanged(now);
                        mDatabase.saveGroupMembership(membership);
                        // now perform a groupMemberUpdate for the affected client so he gets the new key
                        // but only if it is not the member we got the key from
                        if (!membership.getClientId().equals(fromClientId)) {
                            // Note: we notify the client directly from this thread, we are the UpdateAgent anyway
                            // and we have all the information fresh and right here
                            TalkRpcConnection memberConnection = mServer.getClientConnection(forClientIds[i]);
                            if (memberConnection != null && memberConnection.isLoggedIn()) {
                                ITalkRpcClient mrpc = memberConnection.getClientRpc();
                                // we send updates only to those members whose key has changed, so we always send the full update
                                try {
                                    mrpc.groupMemberUpdated(membership);
                                    mrpc.groupUpdated(groupPresence);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                } else {
                    LOG.error("requestGroupKeys, bad number of keys returned for group " + forGroupId);
                    connection.penalizePriorization(300L); // penalize this client in selection
                    sleepForMillis(2000); // TODO: schedule with delay instead of sleep
                    checkAndRequestGroupMemberKeys(forGroupId); // try again
                }
            } else {
                LOG.error("requestGroupKeys, no keys returned for group " + forGroupId);
                connection.penalizePriorization(300L); // penalize this client in selection
                sleepForMillis(2000);  // TODO: schedule with delay instead of sleep
                checkAndRequestGroupMemberKeys(forGroupId); // try again
            }
        } else {
            sleepForMillis(2000); // TODO: schedule with delay instead of sleep
            LOG.error("requestGroupKeys, no presence for any outdated member of group " + forGroupId);
            checkAndRequestGroupMemberKeys(forGroupId); // try again
        }
    }

    private void sleepForMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void setRequestContext() {
        setRequestContext(context);
    }

    public void clearRequestContext() {
        clearRequestContext(context);
    }

    public void requestUserAlert(final String clientId, final StaticSystemMessage.Message message) {
        TalkClientHostInfo clientHostInfo = mDatabase.findClientHostInfoForClient(clientId);
        String messageString = new StaticSystemMessage(clientId, clientHostInfo, message).generateMessage();
        requestUserAlert(clientId, messageString);
    }

    public void requestUserAlert(final String clientId, final String message) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    final TalkRpcConnection conn = mServer.getClientConnection(clientId);
                    if (conn == null || !conn.isConnected()) {
                        return;
                    }

                    LOG.debug("requestUserAlert");
                    conn.getClientRpc().alertUser(message);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };

        queueOrExecute(context, notificationGenerator);
    }

    public void requestSettingUpdate(final String clientId, final String setting, final String value, final StaticSystemMessage.Message message) {
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    final TalkRpcConnection conn = mServer.getClientConnection(clientId);
                    if (conn == null || !conn.isConnected()) {
                        return;
                    }

                    TalkClientHostInfo clientHostInfo = mDatabase.findClientHostInfoForClient(clientId);
                    String messageString = new StaticSystemMessage(clientId, clientHostInfo, message).generateMessage();

                    LOG.debug("requestSettingUpdate");
                    conn.getClientRpc().settingsChanged(setting, value, messageString);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };

        queueOrExecute(context, notificationGenerator);
    }

    public void removeMembership(TalkGroupMembership membership, Date changedDate, String removalState) {
        LOG.debug("removeMembership group "+membership.getGroupId()+" removing membership for client "+membership.getClientId());
        if (!(removalState.equals(membership.getState()) &&
                TalkGroupMembership.ROLE_NONE.equals(membership.getRole())))
        {
            // set membership state to removalState
            membership.setState(removalState);
            // degrade anyone who leaves to member
            membership.setRole(TalkGroupMembership.ROLE_NONE);
            // trash keys
            membership.trashPrivate();
            membership.setLastChanged(changedDate);
            mDatabase.saveGroupMembership(membership);
            requestGroupMembershipUpdate(membership.getGroupId(), membership.getClientId());
        }
    }

    public void nullifyRelationship(TalkRelationship relationship, Date changedDate) {
        if (!(TalkRelationship.STATE_NONE.equals(relationship.getState()) &&
                TalkRelationship.STATE_NONE.equals(relationship.getUnblockState()) &&
                relationship.getNotificationPreference() != null))
        {
            relationship.setState(TalkRelationship.STATE_NONE);
            relationship.setUnblockState(TalkRelationship.STATE_NONE);
            relationship.setNotificationPreference(null);
            relationship.setLastChanged(changedDate);
            mDatabase.saveRelationship(relationship);
            requestRelationshipUpdate(relationship);
        }
    }

    public void nullifyRelationships(List<TalkRelationship> relationships, List<TalkRelationship> otherRelationships) {
        LOG.debug("nullifyRelationships: count:"+relationships.size()+", otherCount:"+otherRelationships.size());

        int count = 0;
        int otherCount = 0;
        for (TalkRelationship relationship : relationships) {
            synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, relationship.getClientId(), relationship.getOtherClientId())) {
                Date deletionMarkDate = new Date();
                nullifyRelationship(relationship, deletionMarkDate);
                ++count;
                for (TalkRelationship otherRelationship : otherRelationships) {
                    if (otherRelationship.getOtherClientId().equals(relationship.getClientId()) &&
                        otherRelationship.getClientId().equals(relationship.getOtherClientId())) {
                        nullifyRelationship(otherRelationship, deletionMarkDate);
                        ++otherCount;
                        break;
                    }
                }
            }
        }
        if (count != otherCount) {
            LOG.warn("nullifyRelationships: counts differ, done count:"+count+", done otherCount:"+otherCount);
        } else {
            LOG.debug("nullifyRelationships: done count:"+count+", done otherCount:"+otherCount);
        }
    }

    public void performAccountDeletion(final String clientId) {
        LOG.debug("performAccountDeletion running for client " + clientId);

        // make sure client is disconnected
        final TalkRpcConnection conn = mServer.getClientConnection(clientId);
        if (conn != null) {
            LOG.debug("performAccountDeletion closing connection for client " + clientId);
            conn.disconnect();
        }

        long acquaintances = 0;

        // remove membership from all groups and close groups where I am admin
        List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsForClient(clientId);
        LOG.debug("performAccountDeletion found " + memberships.size() + " group memberships for client " + clientId);
        for (int i = 0; i < memberships.size(); i++) {
            TalkGroupMembership membership = memberships.get(i);

            if (membership != null) {
                if (membership.isAdmin()) {
                    // delete group
                    LOG.debug("performAccountDeletion closing group " + membership.getGroupId() + " for client " + clientId);
                    TalkGroupPresence groupPresence = mDatabase.findGroupPresenceById(membership.getGroupId());
                    if (groupPresence != null) {
                        // mark the group as deleted
                        if (!TalkGroupPresence.STATE_DELETED.equals(groupPresence.getState())) {
                            groupPresence.setState(TalkGroupPresence.STATE_DELETED);
                            groupPresence.setLastChanged(new Date());
                            mDatabase.saveGroupPresence(groupPresence);
                            requestGroupUpdate(groupPresence.getGroupId());
                        }

                        // walk the group and make everyone have a "none" relationship to it
                        List<TalkGroupMembership> otherMemberships = mDatabase.findGroupMembershipsById(groupPresence.getGroupId());
                        for (TalkGroupMembership otherMembership : otherMemberships) {
                            if (otherMembership.isInvited() || otherMembership.isJoined() || otherMembership.isSuspended()) {
                                removeMembership(otherMembership, groupPresence.getLastChanged(), TalkGroupMembership.STATE_GROUP_REMOVED);
                                ++acquaintances;
                            }
                        }
                    } else {
                        LOG.warn("performAccountDeletion not presence for group " + membership.getGroupId() + " for client " + clientId);
                    }
                } else if (membership.isInvited() || membership.isMember() || membership.isSuspended()) {
                    removeMembership(membership, new Date(), TalkGroupMembership.STATE_NONE);
                    ++acquaintances;
                }
            }
        }

        // remove all relationsships
        final List<TalkRelationship> relationships =
                mDatabase.findRelationships(clientId);
                // mDatabase.findRelationshipsForClientInStates(clientId, TalkRelationship.STATES_RELATED);

        final List<TalkRelationship> otherRelationships =
            mDatabase.findRelationshipsByOtherClient(clientId);
            // mDatabase.findRelationshipsForOtherClientInStates(clientId, TalkRelationship.STATES_RELATED);

        if (otherRelationships.size() > 0 || relationships.size() > 0) {
            nullifyRelationships(relationships, otherRelationships);
            acquaintances += relationships.size() + otherRelationships.size();
        }

        // cleanup presence
        TalkPresence presence = mDatabase.findPresenceForClient(clientId);
        if (presence != null) {
            presence.setAvatarUrl("");
            presence.setClientStatus("Account deleted");
            mDatabase.savePresence(presence);
        } else {
            LOG.warn("performAccountDeletion no presence for clientId " + clientId);
        }

        // expire outgoing deliveries
        final List<TalkDelivery> outDeliveries = mDatabase.findDeliveriesFromClient(clientId);
        for (TalkDelivery delivery : outDeliveries) {
            if (!delivery.isFinished()) {
                delivery.expireDelivery();
                mDatabase.saveDelivery(delivery);
            }
        }

        // reject or expire undelivered incoming deliveries
        final List<TalkDelivery> inDeliveries = mDatabase.findDeliveriesForClient(clientId);
        for (TalkDelivery delivery : inDeliveries) {
            if (!delivery.isFinished()) {
                if (TalkDelivery.STATE_DELIVERING.equals(delivery.getState())) {
                    delivery.setState(TalkDelivery.STATE_REJECTED);
                } else {
                    delivery.expireDelivery();
                }
            }
            mDatabase.saveDelivery(delivery);
        }

        // delete messages from client immediately
        final List<TalkMessage> messages = mDatabase.findMessagesFromClient(clientId);
        for (TalkMessage message : messages) {
            mDatabase.deleteMessage(message);
        }

        // throw away the keys now
        final List<TalkKey> keys = mDatabase.findKeys(clientId);
        for (TalkKey key : keys) {
            mDatabase.deleteKey(key);
        }

        // throw away all tokens
        final List<TalkToken> tokens = mDatabase.findTokensByClient(clientId);
        for (TalkToken token : tokens) {
            mDatabase.deleteToken(token);
        }

        // throw away all tokens
        final List<TalkEnvironment> environments = mDatabase.findEnvironmentsForClient(clientId);
        for (TalkEnvironment environment : environments) {
            mDatabase.deleteEnvironment(environment);
        }

        if (acquaintances == 0) {
            LOG.debug("performAccountDeletion: no acquaintances, deleting " + clientId + " immediately");
            // delete all other stuff immediately as well because nobody knows us
            if (presence != null) {
                mDatabase.deletePresence(presence);
            }

            TalkClient client = mDatabase.findDeletedClientById(clientId);
            if (client != null) {
                mDatabase.deleteClient(client);
            }

            TalkClientHostInfo clientHostInfo = mDatabase.findClientHostInfoForClient(clientId);
            if (clientHostInfo != null) {
                mDatabase.deleteClientHostInfo(clientHostInfo);
            }
            mServer.getFilecacheClient().deleteAccount(clientId);
        } else {
            LOG.debug("performAccountDeletion: " + acquaintances + ", just marked " + clientId + " for deletion");
        }

        //@DatabaseTable(tableName = "groupPresence")
        //public class TalkGroupPresence {
    }

    public void requestAccountDeletion(final String clientId) {
        Runnable accountDeleter = new Runnable() {
            @Override
            public void run() {
                try {
                    performAccountDeletion(clientId);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };

        queueOrExecute(context, accountDeleter);
    }

}
