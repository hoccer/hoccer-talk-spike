package com.hoccer.talk.server.update;

import com.hoccer.talk.model.*;
import com.hoccer.talk.rpc.ITalkRpcClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
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
    private final TalkServerConfiguration mConfig;

    public UpdateAgent(TalkServer server) {
        super(
            server.getConfiguration().getUpdateAgentThreadPoolSize(),
            "update-agent"
        );
        mServer = server;
        mConfig = mServer.getConfiguration();
        mDatabase = mServer.getDatabase();
    }

    private void updateConnectionStatus(TalkPresence presence) {
        // determine the connection status of the client
        boolean isConnected = mServer.isClientConnected(presence.getClientId());
        if (presence.getConnectionStatus() == null || isConnected != presence.isConnected()) {
            String connStatus = isConnected ? TalkPresence.CONN_STATUS_ONLINE
                    : TalkPresence.CONN_STATUS_OFFLINE;
            LOG.info("Persisting connection status '" + connStatus + "' for client's presence. ClientId: '" + presence.getClientId() + "'");
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
                        TalkGroupMember member = mDatabase.findGroupMemberForClient(groupId, clientId);
                        if (member.isInvited() || member.isJoined()) {
                            List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
                            for (TalkGroupMember otherMember : members) {
                                if (!clientId.equals(otherMember.getClientId())) {
                                    if (otherMember.isJoined() || otherMember.isInvited()) {
                                        String otherClientId = otherMember.getClientId();
                                        LOG.debug("RPUFG: delivering presence of " + otherClientId + " to "+clientId);
                                        TalkPresence presence = mDatabase.findPresenceForClient(otherClientId);
                                        if (presence.getConnectionStatus() == null) {
                                            updateConnectionStatus(presence);
                                        }
                                        // Calling Client via RPC
                                        rpc.presenceUpdated(presence);
                                    } else {
                                        LOG.debug("RPUFG: target " + otherMember.getClientId() + " is not invited or joined");
                                    }
                                } else {
                                    LOG.debug("RPUFG: not sending presence update for group " + member.getGroupId()+" to self "+clientId);
                                }
                            }
                        } else {
                            LOG.debug("RPUFG: not invited or joined in group " + member.getGroupId());
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
            // if the other clients relation is friendly to or has been invited by present client
            // Clients that have blocked present client therefore will not be included here
            if (relationship.isDirectlyRelated()) {
                LOG.trace(tag + "including friend " + relationship.getClientId());
                clientIds.add(relationship.getClientId());
            }
        }
        // collect clientIds known through groups
        List<TalkGroupMember> ownMembers = mDatabase.findGroupMembersForClient(selfClientId);
        for (TalkGroupMember ownMember : ownMembers) {
            String groupId = ownMember.getGroupId();
            if (ownMember.isJoined() || ownMember.isInvited()) {
                LOG.trace(tag + "scanning group " + groupId);
                List<TalkGroupMember> otherMembers = mDatabase.findGroupMembersById(groupId);
                for (TalkGroupMember otherMember : otherMembers) {
                    if (otherMember.isJoined() || otherMember.isInvited()) { // MARK
                        LOG.trace(tag + "including group member " + otherMember.getClientId());
                        clientIds.add(otherMember.getClientId());
                    } else {
                        LOG.trace(tag + "not including group member " + otherMember.getClientId() + " in state " + otherMember.getState());
                    }
                }
            }
        }
/*
        // I think it is better to distribute the presence even to blocking clients
        // because there are some corner cases like the connection flag and
        // possibly others that need to be handled; maybe we can better live
        // with updating the presence to blocking clients and request from
        // the user to delete a contact when he does not want to know anything
        // about it.

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
        //TODO: deal with blocked group members in conjunction with group key distribution
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
                    TalkGroup updatedGroup = mDatabase.findGroupById(groupId);
                    if (updatedGroup != null) {
                        TalkRpcConnection connection = mServer.getClientConnection(clientId);
                        if (connection == null || !connection.isConnected()) {
                            return;
                        }

                        // Calling Client via RPC
                        ITalkRpcClient rpc = connection.getClientRpc();
                        try {
                            rpc.groupUpdated(updatedGroup);
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
                    TalkGroup updatedGroup = mDatabase.findGroupById(groupId);
                    if (updatedGroup != null) {
                        List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
                        for (TalkGroupMember member : members) {
                            if (member.isJoined() || member.isInvited() || member.isGroupRemoved()) {
                                TalkRpcConnection connection = mServer.getClientConnection(member.getClientId());
                                if (connection == null || !connection.isConnected()) {
                                    continue;
                                }

                                // Calling Client via RPC
                                ITalkRpcClient rpc = connection.getClientRpc();
                                try {
                                    rpc.groupUpdated(updatedGroup);
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

    // TODO: optimize update calls based in isNew
    public void requestGroupMembershipUpdate(final String groupId, final String clientId, final boolean isNew) {
        LOG.debug("requestGroupMembershipUpdate for group " + groupId + " client " + clientId);
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkGroupMember updatedMember = mDatabase.findGroupMemberForClient(groupId, clientId);
                    if (updatedMember == null) {
                        LOG.debug("requestGroupMembershipUpdate updatedMember is null");
                        return;
                    }
                    TalkGroupMember foreignMember = new TalkGroupMember();
                    foreignMember.foreignUpdateWith(updatedMember);

                    List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
                    LOG.debug("requestGroupMembershipUpdate found " + members.size() + " members");
                    boolean someOneWasNotified = false;
                    for (TalkGroupMember member : members) {
                        if (member.isJoined() || member.isInvited() || member.isGroupRemoved() || member.getClientId().equals(clientId)) {
                            TalkRpcConnection connection = mServer.getClientConnection(member.getClientId());
                            if (connection == null || !connection.isConnected()) {
                                LOG.debug("requestGroupMembershipUpdate - refrain from updating not connected member client " + member.getClientId());
                                continue;
                            }

                            // Calling Client via RPC
                            ITalkRpcClient rpc = connection.getClientRpc();
                            try {
                                if (member.getClientId().equals(clientId)) {
                                    // is own membership
                                    rpc.groupMemberUpdated(updatedMember);
                                } else {
                                    rpc.groupMemberUpdated(foreignMember);
                                }
                                someOneWasNotified = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            LOG.debug("requestGroupMembershipUpdate - not updating client " + member.getClientId() + ", state=" + member.getState() + ", self=" + member.getClientId().equals(clientId));
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
                    TalkGroupMember newMember = mDatabase.findGroupMemberForClient(groupId, newMemberClientId);
                    if (newMember == null) {
                        LOG.debug("requestGroupMembershipUpdateForNewMember can't find newMember, is null");
                        return;
                    }
                    List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
                    LOG.debug("requestGroupMembershipUpdateForNewMember found " + members.size() + " members");
                    TalkRpcConnection connection = mServer.getClientConnection(newMember.getClientId());
                    if (connection == null || !connection.isConnected()) {
                        LOG.debug("requestGroupMembershipUpdateForNewMember - new client no longer connected " + newMember.getClientId());
                        return;
                    }
                    // Calling Client via RPC
                    ITalkRpcClient rpc = connection.getClientRpc();
                    for (TalkGroupMember member : members) {
                        // do not send out updates for own membership or dead members
                        if (!member.getClientId().equals(newMemberClientId) && (member.isJoined() || member.isInvited())) {
                            try {
                                member.setEncryptedGroupKey(null);
                                rpc.groupMemberUpdated(member);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            LOG.debug("requestGroupMembershipUpdateForNewMember - not updating with member " + member.getClientId() + ", state=" + member.getState());
                        }
                    }
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };
        queueOrExecute(context, notificationGenerator);
    }

    public ArrayList<Pair<TalkGroupMember, Long>> membersSortedByLatency(List<TalkGroupMember> members) {

        Map<TalkGroupMember, Long> membersByLatency = new HashMap<TalkGroupMember, Long>();
        for (TalkGroupMember m : members) {
            TalkRpcConnection connection = mServer.getClientConnection(m.getClientId());
            if (connection != null) {
                // if we dont have a latency, assume something bad
                Long latency = connection.getLastPingLatency();
                if (latency != null) {
                    membersByLatency.put(m, latency + connection.getCurrentPriorityPenalty());
                } else {
                    membersByLatency.put(m, 5000L + connection.getCurrentPriorityPenalty());
                }
            }
        }
        membersByLatency = MapUtil.sortByValue(membersByLatency);

        ArrayList<Pair<TalkGroupMember, Long>> result = new ArrayList<Pair<TalkGroupMember, Long>>();
        for (Map.Entry<TalkGroupMember, Long> entry : membersByLatency.entrySet()) {
            result.add(new ImmutablePair<TalkGroupMember, Long>(entry.getKey(), entry.getValue()));
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
        TalkGroup group = mDatabase.findGroupById(groupId);
        if (group != null && group.exists()) {

            List<TalkGroupMember> members = mDatabase.findGroupMembersByIdWithStates(group.getGroupId(), TalkGroupMember.ACTIVE_STATES);
            LOG.debug("  * members: " + members.size());
            if (!members.isEmpty()) {
                List<TalkGroupMember> outOfDateMembers = new ArrayList<TalkGroupMember>();
                List<TalkGroupMember> keyMasterCandidatesWithCurrentKey = new ArrayList<TalkGroupMember>();
                List<TalkGroupMember> keyMasterCandidatesWithoutCurrentKey = new ArrayList<TalkGroupMember>();

                String sharedKeyId = group.getSharedKeyId();
                String sharedKeyIdSalt = group.getSharedKeyIdSalt();
                if (sharedKeyId == null) {
                    // nobody has supplied a group key yet
                    LOG.debug("  * nobody has supplied a group key yet...");
                    for (TalkGroupMember m : members) {
                        TalkPresence presence = mDatabase.findPresenceForClient(m.getClientId());
                        if (presence != null && presence.getKeyId() != null) {
                            if (m.isMember() && mServer.isClientReady(m.getClientId())) {
                                keyMasterCandidatesWithoutCurrentKey.add(m);
                            }
                            outOfDateMembers.add(m);
                        } else {
                            LOG.error("checkAndRequestGroupMemberKeys: no presence for client " + m.getClientId() + ", member of group " + groupId);
                        }
                    }
                } else {
                    // there is a group key
                    LOG.debug("  * There is a group key...");
                    for (TalkGroupMember m : members) {
                        TalkPresence presence = mDatabase.findPresenceForClient(m.getClientId());
                        if (presence != null && presence.getKeyId() != null) {
                            if (!sharedKeyId.equals(m.getSharedKeyId()) || !m.getMemberKeyId().equals(presence.getKeyId())) {
                                // member has not the current key
                                LOG.debug("  * Member "+m.getClientId()+" has not the current group key");
                                outOfDateMembers.add(m);
                                if (m.isMember() && mServer.isClientReady(m.getClientId())) {
                                    LOG.debug("  * Member "+m.getClientId()+" is ready and active and added to keyMasterCandidatesWithoutCurrentKey");
                                    keyMasterCandidatesWithoutCurrentKey.add(m);
                                }
                            } else {
                                // member has the current key
                                LOG.debug("  * Member "+m.getClientId()+" has the current group key");
                                if (m.isMember() && mServer.isClientReady(m.getClientId())) {
                                    LOG.debug("  * Member "+m.getClientId()+" is ready and active and added to keyMasterCandidatesWithCurrentKey");
                                    keyMasterCandidatesWithCurrentKey.add(m);
                                }
                            }
                        } else {
                            LOG.error("checkAndRequestGroupMemberKeys:(2) no presence for client " + m.getClientId() + ", member of group " + groupId);
                        }
                    }
                }
                if (!outOfDateMembers.isEmpty()) {
                    LOG.debug("  * There are members (" + outOfDateMembers.size() + ") without a current group key - need to issue a rekeying...");
                    // we need request some keys
                    if (!keyMasterCandidatesWithCurrentKey.isEmpty()) {
                        // prefer candidates that already have a key
                        ArrayList<Pair<TalkGroupMember, Long>> candidatesByLatency = membersSortedByLatency(keyMasterCandidatesWithCurrentKey);
                        TalkGroupMember newKeymaster;
                        if (candidatesByLatency.get(0).getRight() < MAX_ALLOWED_KEY_REQUEST_LATENCY) {
                            newKeymaster = candidatesByLatency.get(0).getLeft(); // get the lowest latency candidate
                            requestGroupKeys(newKeymaster.getClientId(), group.getGroupId(), sharedKeyId, sharedKeyIdSalt, outOfDateMembers);
                            return;
                        }
                        // fall through to next block if best candidate does not meet MAX_ALLOWED_KEY_REQUEST_LATENCY
                    }
                    if (!keyMasterCandidatesWithoutCurrentKey.isEmpty()) {
                        // nobody with a current key is online, but we have other admins online, so purchase a new group key
                        ArrayList<Pair<TalkGroupMember, Long>> candidatesByLatency = membersSortedByLatency(keyMasterCandidatesWithoutCurrentKey);
                        TalkGroupMember newKeymaster;
                        if (candidatesByLatency.get(0).getRight() < MAX_ALLOWED_KEY_REQUEST_LATENCY) {
                            newKeymaster = candidatesByLatency.get(0).getLeft(); // get the lowest latency candidate
                            requestGroupKeys(newKeymaster.getClientId(), group.getGroupId(), null, null, outOfDateMembers);
                            return;
                        }
                        // fall through to next block if best candidate does not meet MAX_ALLOWED_KEY_REQUEST_LATENCY
                    }
                    // we have out of date key members, but no suitable candidate for group key generation
                    LOG.warn("performCheckAndRequestGroupMemberKeys:" + outOfDateMembers.size() + " members have no key in group " + groupId + ", but no suitable keymaster available");
                }
            }
        }
    }

    private void requestGroupKeys(String fromClientId, String forGroupId, String forSharedKeyId, String withSharedKeyIdSalt, List<TalkGroupMember> forOutOfDateMembers) {
        ArrayList<String> forClientIdsList = new ArrayList<String>();
        ArrayList<String> withPublicKeyIdsList = new ArrayList<String>();
        for (TalkGroupMember member : forOutOfDateMembers) {
            TalkPresence presence = mDatabase.findPresenceForClient(member.getClientId());
            if (presence != null && presence.getKeyId() != null) {
                withPublicKeyIdsList.add(presence.getKeyId());
                forClientIdsList.add(member.getClientId());
                LOG.info("requestGroupKeys, added client='" + member.getClientId() + "', keyId='" + presence.getKeyId() + "'");
            } else {
                LOG.error("requestGroupKeys, failed to add client='" + member.getClientId() + "'");
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
            LOG.info("requestGroupKeys, calling getEncryptedGroupKeys(" + forGroupId + ") on client for " + forClientIds.length + " client(s)");
            String[] newKeyBoxes = rpc.getEncryptedGroupKeys(forGroupId, forSharedKeyId, withSharedKeyIdSalt, forClientIds, withPublicKeyIds);
            LOG.info("requestGroupKeys, call of getEncryptedGroupKeys(" + forGroupId + ") returned " + newKeyBoxes.length + " items)");
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
                    connection.resetPriorityPenalty();
                    Date now = new Date();

                    TalkGroup group = mDatabase.findGroupById(forGroupId);
                    group.setSharedKeyId(forSharedKeyId);
                    group.setSharedKeyIdSalt(withSharedKeyIdSalt);
                    group.setLastChanged(now);
                    LOG.info("requestGroupKeys, for group '" + forGroupId + "' did set sharedKeyId " + group.getSharedKeyId() + ", salt="+group.getSharedKeyIdSalt());
                    mDatabase.saveGroup(group);

                    for (int i = 0; i < forClientIds.length; ++i) {
                        TalkGroupMember member = mDatabase.findGroupMemberForClient(forGroupId, forClientIds[i]);
                        member.setSharedKeyId(forSharedKeyId);
                        member.setSharedKeyIdSalt(withSharedKeyIdSalt);
                        LOG.info("requestGroupKeys, for member '" + member.getClientId() + "' did set sharedKeyId " + member.getSharedKeyId() + ", salt="+member.getSharedKeyIdSalt());
                        member.setMemberKeyId(withPublicKeyIds[i]);
                        member.setEncryptedGroupKey(newKeyBoxes[i]);
                        member.setKeySupplier(fromClientId);
                        member.setSharedKeyDate(now); // TODO: remove this and other fields no longer required
                        member.setLastChanged(now);
                        mDatabase.saveGroupMember(member);
                        // now perform a groupMemberUpdate for the affected client so he gets the new key
                        // but only if it is not the member we got the key from
                        if (!member.getClientId().equals(fromClientId)) {
                            // Note: we notify the client directly from this thread, we are the UpdateAgent anyway
                            // and we have all the information fresh and right here
                            TalkRpcConnection memberConnection = mServer.getClientConnection(forClientIds[i]);
                            if (memberConnection != null && memberConnection.isLoggedIn()) {
                                ITalkRpcClient mrpc = memberConnection.getClientRpc();
                                // we send updates only to those members whose key has changed, so we always send the full update
                                try {
                                    mrpc.groupMemberUpdated(member);
                                    mrpc.groupUpdated(group);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                } else {
                    LOG.error("requestGroupKeys, bad number of keys returned for group " + forGroupId);
                    connection.penalizePriorization(100L); // penalize this client in selection
                    sleepForMillis(1000); // TODO: schedule with delay instead of sleep
                    checkAndRequestGroupMemberKeys(forGroupId); // try again
                }
            } else {
                LOG.error("requestGroupKeys, no keys returned for group " + forGroupId);
                connection.penalizePriorization(100L); // penalize this client in selection
                sleepForMillis(1000);  // TODO: schedule with delay instead of sleep
                checkAndRequestGroupMemberKeys(forGroupId); // try again
            }
        } else {
            sleepForMillis(1000); // TODO: schedule with delay instead of sleep
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
        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    final TalkRpcConnection conn = mServer.getClientConnection(clientId);
                    if (conn == null || !conn.isConnected()) {
                        return;
                    }
                    TalkClient talkClient = mDatabase.findClientById(clientId);
                    if (talkClient == null) {
                        return;
                    }
                    TalkClientHostInfo clientHostInfo = mDatabase.findClientHostInfoForClient(talkClient.getClientId());
                    String messageString = new StaticSystemMessage(talkClient, clientHostInfo, message).generateMessage();
                    LOG.info("requestUserAlert");
                    conn.getClientRpc().alertUser(messageString);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };

        queueOrExecute(context, notificationGenerator);
    }

}
