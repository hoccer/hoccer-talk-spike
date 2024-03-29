package com.hoccer.talk.server.database;

import com.hoccer.talk.model.*;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.mongodb.*;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.Update;

import java.net.UnknownHostException;
import java.util.*;

/**
 * Database implementation using the Jongo mapper to MongoDB
 * <p/>
 * This is intended as the production backend.
 * <p/>
 * TODO: this should use findOne() instead of find() where appropriate
 */
public class JongoDatabase implements ITalkServerDatabase {

    private static final Logger LOG = Logger.getLogger(JongoDatabase.class);

    /**
     * Mongo connection pool
     */
    private final Mongo mMongo;

    /**
     * Mongo database accessor
     */
    private DB mDb;

    /**
     * Jongo object mapper
     */
    private Jongo mJongo;

    private final List<MongoCollection> mCollections;
    private final Map<String, MongoCollection> mCollectionsByName = new HashMap<String, MongoCollection>();

    private MongoCollection mClients;
    private MongoCollection mMessages;
    private MongoCollection mDeliveries;
    private MongoCollection mTokens;
    private MongoCollection mRelationships;
    private MongoCollection mPresences;
    private MongoCollection mKeys;
    private MongoCollection mGroupPresences;
    private MongoCollection mGroupMemberships;
    private MongoCollection mEnvironments;
    private MongoCollection mClientHostInfos;
    private MongoCollection mMigrations;
    private MongoCollection mStatistics;

    public JongoDatabase(TalkServerConfiguration configuration) {
        mCollections = new ArrayList<MongoCollection>();
        mMongo = createMongoClient(configuration);
        initialize(configuration.getJongoDb());
    }

    public JongoDatabase(TalkServerConfiguration configuration, Mongo mongodb) {
        mCollections = new ArrayList<MongoCollection>();
        mMongo = mongodb;
        initialize(configuration.getJongoDb());
    }

    public Object getRawCollection(String name) {
        return mCollectionsByName.get(name);
    }

    private Mongo createMongoClient(TalkServerConfiguration configuration) {
        // create connection pool
        try {
            MongoOptions options = new MongoOptions();
            options.threadsAllowedToBlockForConnectionMultiplier = 1500;
            options.maxWaitTime = configuration.getJongoMaxWaitTime();
            options.connectionsPerHost = configuration.getJongoConnectionsPerHost();
            return new Mongo(configuration.getJongoHost(), options);
        } catch (UnknownHostException e) {
            LOG.error("unknown host: '" + configuration.getJongoHost() + "'", e);
            return null;
        }
    }

    private void initialize(String dbName) {
        LOG.info("Initializing jongo with database " + dbName);

        // create db accessor
        mDb = mMongo.getDB(dbName);
        // create object mapper
        mJongo = new Jongo(mDb);
        // create collection accessors
        mClients = getCollection("client");
        mMessages = getCollection("message");
        mDeliveries = getCollection("delivery");
        mTokens = getCollection("token");
        mRelationships = getCollection("relationship");
        mPresences = getCollection("presence");
        mKeys = getCollection("key");
        mGroupPresences = getCollection("group");
        mGroupMemberships = getCollection("groupMember");
        mEnvironments = getCollection("environment");
        mClientHostInfos = getCollection("clientHostInfo");
        mMigrations = getCollection("migrations");

        LOG.info("Ensuring database indices for database " + dbName);
        mClients.ensureIndex("{clientId:1}");
        mClients.ensureIndex("{apnsToken:1}");
        mClients.ensureIndex("{timeRegistered:1}"); // for external statistics gathering

        mTokens.ensureIndex("{clientId:1}");
        mTokens.ensureIndex("{clientId:1, secret:1}");
        mTokens.ensureIndex("{purpose:1, secret:1}");

        mRelationships.ensureIndex("{clientId:1, otherClientId:1}") ;
        mRelationships.ensureIndex("{clientId:1}") ;
        mRelationships.ensureIndex("{otherClientId:1}") ;
        mRelationships.ensureIndex("{clientId:1, state:1}") ;
        mRelationships.ensureIndex("{otherClientId:1, state:1}") ;
        mRelationships.ensureIndex("{clientId:1, lastChanged:1}") ;
        mRelationships.ensureIndex("{state:1, lastChanged:1}") ;
        mRelationships.ensureIndex("{notificationPreference:1}");

        mPresences.ensureIndex("{clientId:1}");
        mPresences.ensureIndex("{connectionStatus:1}");
        mPresences.ensureIndex("{apnsToken:1}");
        mPresences.ensureIndex("{gcmRegistration:1}");

        mKeys.ensureIndex("{clientId:1}");

        mGroupPresences.ensureIndex("{groupId:1}");
        mGroupPresences.ensureIndex("{state:1}");
        mGroupPresences.ensureIndex("{state:1, lastChanged:1}");
        mGroupPresences.ensureIndex("{groupType:1}");
        mGroupPresences.ensureIndex("{state:1, groupType:1}");

        mGroupMemberships.ensureIndex("{groupId:1, clientId:1}");
        mGroupMemberships.ensureIndex("{clientId:1}");
        mGroupMemberships.ensureIndex("{groupId:1, state:1, role:1}");
        mGroupMemberships.ensureIndex("{groupId:1, state:1, lastChanged:1}");
        mGroupMemberships.ensureIndex("{clientId:1, state:1}");
        mGroupMemberships.ensureIndex("{clientId:1, state:1, role:1}");
        mGroupMemberships.ensureIndex("{state:1, lastChanged:1}");
        mGroupMemberships.ensureIndex("{state:1, role:1}");
        mGroupMemberships.ensureIndex("{role:1}");
        mGroupMemberships.ensureIndex("{notificationPreference:1}");

        mMessages.ensureIndex("{messageId:1, senderId:1}");
        mMessages.ensureIndex("{messageId:1}");
        mMessages.ensureIndex("{senderId:1}");
        mMessages.ensureIndex("{attachmentFileId:1}");
        mMessages.ensureIndex("{attachment:1}");
        mMessages.ensureIndex("{attachmentUploadStarted:1}");
        mMessages.ensureIndex("{attachmentUploadFinished:1}");

        mDeliveries.ensureIndex("{messageId:1, senderId:1, receiverId:1}");
        mDeliveries.ensureIndex("{messageId:1}");
        mDeliveries.ensureIndex("{senderId:1}");
        mDeliveries.ensureIndex("{groupId:1}");
        mDeliveries.ensureIndex("{senderId:1, state:1}");
        mDeliveries.ensureIndex("{senderId:1, state:1, attachmentState:1}");
        mDeliveries.ensureIndex("{receiverId:1}");
        mDeliveries.ensureIndex("{receiverId:1, state:1}");
        mDeliveries.ensureIndex("{receiverId:1, groupId:1, state:1}");
        mDeliveries.ensureIndex("{receiverId:1, state:1, attachmentState:1}");
        mDeliveries.ensureIndex("{state:1}");
        mDeliveries.ensureIndex("{attachmentState:1}");
        mDeliveries.ensureIndex("{state:1, attachmentState:1}");
        mDeliveries.ensureIndex("{timeAccepted:1}");

        mEnvironments.ensureIndex("{geoLocation: '2dsphere'}");
        mEnvironments.ensureIndex("{groupId: 1}");
        mEnvironments.ensureIndex("{clientId: 1}");
        mEnvironments.ensureIndex("{groupId: 1, clientId: 1}");
        mEnvironments.ensureIndex("{type: 1, clientId: 1}");
        mEnvironments.ensureIndex("{type: 1}");

        mClientHostInfos.ensureIndex("{clientId:1}");
        mClientHostInfos.ensureIndex("{clientLanguage: 1, clientName:1}");
        mClientHostInfos.ensureIndex("{systemName:1}");
        mClientHostInfos.ensureIndex("{serverTime:1}");
        mClientHostInfos.ensureIndex("{systemName: 1, serverTime:1}");
        mClientHostInfos.ensureIndex("{clientName:1}");
        mClientHostInfos.ensureIndex("{clientVersion:1}");
        mClientHostInfos.ensureIndex("{systemLanguage:1}");

        LOG.info("Ensuring database indices done for database " + dbName);

        cleanupDatabaseStateOnStartup();

    }

    void cleanupDatabaseStateOnStartup() {
        LOG.info("Cleanup database state on startup:");

        // after restart, everyone is offline
        long notOffline = mPresences.count("{connectionStatus : {$ne: 'offline'}}");
        LOG.info("-- cleanupDatabaseState: Presences not set to offline: "+notOffline);
        long online = mPresences.count("{connectionStatus : 'online'}");
        LOG.info("---- cleanupDatabaseState: Presences online:"+online);
        long background = mPresences.count("{connectionStatus : 'background'}");
        LOG.info("---- cleanupDatabaseState: Presences background: "+background);
        long typing = mPresences.count("{connectionStatus : 'typing'}");
        LOG.info("---- cleanupDatabaseState: Presences typing: "+typing);

        Date cleanupDate = new Date();
        Update update = mPresences.update("{connectionStatus : {$ne: 'offline'}}");
        WriteResult result = update.multi().with("{ $set: {connectionStatus:'offline'} }, { $set: {timestamp:#} }", cleanupDate);
        LOG.info("-- cleanupDatabaseState: Updated " + result.getN() + " presences to state 'offline' and timestamp " + cleanupDate);

        LOG.info("Cleanup database state on startup done.");
    }

     private MongoCollection getCollection(String name) {
        MongoCollection res = mJongo.getCollection(name).withWriteConcern(WriteConcern.JOURNALED);
        mCollections.add(res);
        mCollectionsByName.put(name, res);
        return res;
    }

    @Override
    @NotNull
    public List<TalkClient> findAllClients() {
        Iterator<TalkClient> it = mClients
                .find()
                .as(TalkClient.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @Nullable // null if client for given id does not exist.
    public TalkClient findClientById(String clientId) {
        return mClients.findOne("{clientId:#}", clientId)
                .as(TalkClient.class);
    }

    @Override
    @Nullable // null if client for given id does not exist.
    public TalkClient findDeletedClientById(String clientId) {
        return findClientById(clientId+"-DELETED");
    }

    @Override
    public boolean isDeletedClient(String clientId) {
        return clientId.endsWith("-DELETED");
    }

    @Override
    public String beforeDeletedId(String clientId) {
        if (isDeletedClient(clientId)) {
            return clientId.substring(0, clientId.length() - "-DELETED".length());
        } else {
            return clientId;
        }
    }


    @Override
    @Nullable // null if client for given apns token does not exist.
    public TalkClient findClientByApnsToken(String apnsToken) {
        return mClients.findOne("{apnsToken:#}", apnsToken)
                .as(TalkClient.class);
    }

    @Override
    public void saveClient(@NotNull TalkClient client) {
        mClients.save(client);
    }

    @Override
    public void markClientDeleted(@NotNull TalkClient client, @NotNull String reason) {
        if (!isDeletedClient(client.getClientId())) {
            client.setSrpSavedVerifier(client.getSrpVerifier());
            client.setSrpVerifier("");
            client.setReasonDeleted(reason);
            client.setClientId(client.getClientId()+"-DELETED");
            client.setTimeDeleted(new Date());
            mClients.save(client);
        }
    }

    @Override
    public void unmarkClientDeleted(@NotNull TalkClient client) {
        if (isDeletedClient(client.getClientId()) && client.getSrpSavedVerifier() != null) {
            client.setSrpVerifier(client.getSrpSavedVerifier());
            client.setSrpSavedVerifier("");
            client.setReasonDeleted("");
            client.setClientId(client.getClientId().substring(0,36));
            client.setTimeDeleted(null);
            mClients.save(client);
        }
    }

    @Override
    public void suspendClient(@NotNull TalkClient client, @Nullable Date when, long duration) {
        client.setTimeSuspended(when);
        client.setDurationSuspended(duration);
        mClients.save(client);
    }

    @Override
    public void unsuspendClient(TalkClient client) {
        suspendClient(client, null, 0);
    }


    @Override
    public void deleteClient(@NotNull TalkClient client) {
        mClients.remove("{clientId:#}", client.getClientId());
    }

    @Override
    @Nullable // null if no message with given messageId exists.
    public TalkMessage findMessageById(String messageId) {
        return mMessages.findOne("{messageId:#}", messageId)
                .as(TalkMessage.class);
    }

    @Override
    @NotNull
    public List<TalkMessage> findMessagesWithAttachmentFileId(String fileId) {
        Iterator<TalkMessage> it = mMessages
                .find("{attachmentFileId:#}", fileId)
                .as(TalkMessage.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkMessage> findMessagesFromClient(String senderId) {
        Iterator<TalkMessage> it = mMessages
                .find("{senderId:#}", senderId)
                .as(TalkMessage.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public void deleteMessage(@Nullable TalkMessage message) {
        mMessages.remove("{messageId:#}", message.getMessageId());
    }

    @Override
    public void saveMessage(TalkMessage message) {
        mMessages.save(message);
    }

    @Override
    @Nullable
    public TalkDelivery findDelivery(String messageId, String receiverId) {
        return mDeliveries.findOne("{messageId:#,receiverId:#}", messageId, receiverId)
                .as(TalkDelivery.class);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesInState(String state) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{state:#}", state)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findAllDeliveries() {
        Iterator<TalkDelivery> it = mDeliveries
                .find()
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesInStates(String[] states) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{state: { $in: # } }", Arrays.asList(states))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesInStatesAndAttachmentStates(String[] deliveryStates, String[] attachmentStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{state: { $in: # }, attachmentState: {$in: # } }", Arrays.asList(deliveryStates), Arrays.asList(attachmentStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public long countDeliveriesInStatesAndAttachmentStates(String[] deliveryStates, String[] attachmentStates) {
        return  mDeliveries.count("{state: { $in: # }, attachmentState: {$in: # } }", Arrays.asList(deliveryStates), Arrays.asList(attachmentStates));
    }


    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForClient(String receiverId) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#}", receiverId)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClient(String senderId) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#}", senderId)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForClientInState(String receiverId, String state) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#,state:#}", receiverId, state)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForClientInGroupInState(String receiverId, String groupId, String state) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#, groupId:#, state:#}", receiverId, groupId, state)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public long countDeliveriesForClientInGroupInState(String receiverId, String groupId, String state) {
        return mDeliveries.count("{receiverId:#, groupId:#, state:#}", receiverId, groupId, state);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForClientInDeliveryAndAttachmentStates(String receiverId, String[] deliveryStates, String[] attachmentStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#, state: { $in: # }, attachmentState: {$in: # } }", receiverId, Arrays.asList(deliveryStates), Arrays.asList(attachmentStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientInState(String senderId, String state) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#,state:#}", senderId, state)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }
    @Override
    public long countDeliveriesFromClientInState(String senderId, String state) {
        return mDeliveries.count("{senderId:#,state:#}", senderId, state);
    }
    @Override
    public long countDeliveriesBetweenClientsInState(String senderId, String receiverId, String state) {
        return mDeliveries.count("{senderId:#, receiverId:#, state:#}", senderId, receiverId, state);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientInStates(String senderId, String[] deliveryStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#, state: { $in: # } }", senderId, Arrays.asList(deliveryStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientInDeliveryAndAttachmentStates(String senderId, String[] deliveryStates, String[] attachmentStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#, state: { $in: # }, attachmentState: {$in: #} }", senderId, Arrays.asList(deliveryStates), Arrays.asList(attachmentStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public long countDeliveriesFromClientInDeliveryAndAttachmentStates(String senderId, String[] deliveryStates, String[] attachmentStates) {
        return mDeliveries.count("{senderId:#, state: { $in: # }, attachmentState: {$in: #} }", senderId, Arrays.asList(deliveryStates), Arrays.asList(attachmentStates));
    }

    @Override
    public long countDeliveriesBetweenClientsInDeliveryAndAttachmentStates(String senderId, String receiverId, String[] deliveryStates, String[] attachmentStates) {
        return mDeliveries.count("{senderId:#, receiverId:#, state: { $in: # }, attachmentState: {$in: #} }",
                senderId, receiverId, Arrays.asList(deliveryStates), Arrays.asList(attachmentStates));
    }


        @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForMessage(String messageId) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{messageId:#}", messageId)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }
    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientForMessage(String senderId, String messageId) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#, messageId:#}", senderId, messageId)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesAcceptedBefore(Date limit) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{timeAccepted: { $lt:# } }", limit)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }


    @Override
    public void deleteDelivery(TalkDelivery delivery) {
        mDeliveries.remove("{messageId:#,receiverId:#}", delivery.getMessageId(), delivery.getReceiverId());
    }

    @Override
    public void saveDelivery(TalkDelivery delivery) {
        mDeliveries.save(delivery);
    }

    @Override
    public void updateDeliveryTimeClientNotified(TalkDelivery delivery) {
        mDeliveries.update("{ _id:# }", delivery.getId()).with("{$set: { timeClientNotified:# }}", delivery.getTimeClientNotified());
    }

    @Override
    @NotNull
    public List<TalkToken> findTokensByClient(String clientId) {
        Iterator<TalkToken> it = mTokens
                .find("{clientId:#}", clientId)
                .as(TalkToken.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @Nullable
    public TalkToken findTokenByPurposeAndSecret(String purpose, String secret) {
        TalkToken token = null;

        Iterator<TalkToken> it = mTokens
                .find("{purpose:#,secret:#}", purpose, secret)
                .as(TalkToken.class)
                .iterator();

        if (it.hasNext()) {
            token = it.next();
            if (it.hasNext()) {
                // TODO: Define and throw a dedicated exception instead of using a generic one (from sonarqube)
                throw new RuntimeException("Duplicate token");
            }
        }

        return token;
    }

    @Override
    public void deleteToken(TalkToken token) {
        mTokens.remove("{clientId:#,secret:#}", token.getClientId(), token.getSecret());
    }

    @Override
    public void saveToken(TalkToken token) {
        mTokens.save(token);
    }

    @Override
    public TalkPresence findPresenceForClient(String clientId) {
        return mPresences.findOne("{clientId:#}", clientId)
                .as(TalkPresence.class);
    }

    @Override
    @NotNull
    public List<TalkPresence> findPresencesChangedAfter(String clientId, Date lastKnown) {
        // result array
        List<TalkPresence> res = new ArrayList<TalkPresence>();

        // set to collect clients into
        Set<String> clients = new HashSet<String>();

        // set to collect clients which may have been added to the presence set
        // since the client has received presences the last time
        Set<String> mustInclude = new HashSet<String>();

        // collect clients known through relationships

        /*
        // With the following (former) variant in this comment we will also get presences
        // for clients that have (only) blocked us which is not what we want. Note that this is
        // however the same code used in performPresenceUpdate() to determine to which
        // clients a presence update should be sent, but this is a different case.

        List<TalkRelationship> relationships = findRelationshipsByOtherClient(clientId);
        for (TalkRelationship relationship : relationships) {
            if (relationship.isRelated()) {
                LOG.info("including "+relationship.getClientId()+" because related");
                clients.add(relationship.getClientId());
                if (relationship.getLastChanged().after(lastKnown)) {
                    LOG.info("must include "+relationship.getClientId()+" because related");
                    mustInclude.add(relationship.getClientId());
                }
            }
        }
        The following code however will also include presence updates for clients *we* (clientId) do block,
        which is fine because there are corner cases like online indication and group key management
        that we also have to perform for clients we block. Semantically it is also ok because
        we might want to unblock them, so we actually have a relationship.

        Note that this code needs to behave the same way like performPresenceUpdate() regarding
        the decision if a particular presence is sent to a particular client.

        It also has to play well with the isContactOf() call which will also return true for a
        contact we have blocked, but false for a contact that *only* has blocked us without
        us have a relationShip to this blocking client.

        */
        List<TalkRelationship> relationships = findRelationships(clientId);
        for (TalkRelationship relationship : relationships) {
            if (relationship.isRelated()) {
                LOG.trace("including "+relationship.getOtherClientId()+" because related");
                clients.add(relationship.getOtherClientId());
                if (relationship.getLastChanged() != null && relationship.getLastChanged().after(lastKnown)) {
                    LOG.trace("must include "+relationship.getOtherClientId()+" because relationship has changed since lastKnown");
                    mustInclude.add(relationship.getOtherClientId());
                }
            }
        }

        // collect clients known through groups
        List<TalkGroupMembership> ownMemberships = findGroupMembershipsForClient(clientId);
        for (TalkGroupMembership ownMembership : ownMemberships) {
            String groupId = ownMembership.getGroupId();
            if (ownMembership.isInvited() || ownMembership.isJoined() || ownMembership.isSuspended()) {
                List<TalkGroupMembership> otherMemberships = findGroupMembershipsById(groupId);
                for (TalkGroupMembership otherMembership : otherMemberships) {
                    if (otherMembership.isInvited() || otherMembership.isJoined() || otherMembership.isSuspended()) {
                        clients.add(otherMembership.getClientId());
                        LOG.trace("include "+otherMembership.getClientId()+" because common membership");
                        if ((otherMembership.getLastChanged() != null && otherMembership.getLastChanged().after(lastKnown)) ||
                                (ownMembership.getLastChanged() != null && ownMembership.getLastChanged().after(lastKnown))) {
                            LOG.trace("must include "+otherMembership.getClientId()+" because memberships changed");
                            mustInclude.add(otherMembership.getClientId());
                        }
                    }
                }
            }
        }

        // treat former senders with unfinished deliveries as contact
        final List<TalkDelivery> deliveries = findDeliveriesForClientInState(clientId, TalkDelivery.STATE_DELIVERING);
        for (TalkDelivery delivery : deliveries) {
            clients.add(delivery.getSenderId());
            if (delivery.getTimeChanged() != null && delivery.getTimeChanged().after(lastKnown)) {
                mustInclude.add(delivery.getSenderId());
            }
        }
        final List<TalkDelivery> attachmentDeliveries =
                findDeliveriesForClientInDeliveryAndAttachmentStates(clientId,
                        TalkDelivery.IN_ATTACHMENT_DELIVERY_STATES, TalkDelivery.IN_ATTACHMENT_STATES);
        for (TalkDelivery delivery : attachmentDeliveries) {
            clients.add(delivery.getSenderId());
            if (delivery.getTimeChanged() != null && delivery.getTimeChanged().after(lastKnown)) {
                mustInclude.add(delivery.getSenderId());
            }
        }

        // remove self
        clients.remove(clientId);
        // collect presences
        for (String client : clients) {
            TalkPresence pres = findPresenceForClient(client);
            if (pres != null && (pres.getTimestamp().after(lastKnown) || mustInclude.contains(client))) {
                res.add(pres);
            }
        }
        return res;
    }

    @Override
    public List<TalkPresence> findPresencesWithStates(String[] states) {
        Iterator<TalkPresence> it = mPresences
                .find("{connectionStatus: { $in: # }}", Arrays.asList(states))
                .as(TalkPresence.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public void savePresence(TalkPresence presence) {
        mPresences.save(presence);
    }

    @Override
    public void deletePresence(TalkPresence presence) {
        mPresences.remove("{clientId:#}", presence.getClientId());
    }

    @Override
    @Nullable
    public TalkKey findKey(String clientId, String keyId) {
        return mKeys.findOne("{clientId:#,keyId:#}", clientId, keyId)
                .as(TalkKey.class);
    }

    @Override
    @NotNull
    public List<TalkKey> findKeys(String clientId) {
        Iterator<TalkKey> it = mKeys
                .find("{clientId:#}", clientId)
                .as(TalkKey.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public void deleteKey(TalkKey key) {
        mKeys.remove("{clientId:#,keyId:#}", key.getClientId(), key.getKeyId());
    }

    @Override
    public void saveKey(TalkKey key) {
        mKeys.save(key);
    }

    @Override
    @NotNull
    public List<TalkRelationship> findRelationships(String client) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{clientId:#}", client)
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkRelationship> findRelationshipsForClientInState(String clientId, String state) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{clientId:#,state:#}", clientId, state)
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkRelationship> findRelationshipsForClientInStates(String clientId, String[] states) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{clientId:#,state:{ $in: # }}", clientId, Arrays.asList(states))
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkRelationship> findRelationshipsForOtherClientInStates(String clientId, String[] states) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{otherClientId:#,state:{ $in: # }}", clientId, Arrays.asList(states))
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkRelationship> findRelationshipsByOtherClient(String other) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{otherClientId:#}", other)
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @Nullable
    public TalkRelationship findRelationshipBetween(String client, String otherClient) {
        return mRelationships.findOne("{clientId:#,otherClientId:#}", client, otherClient)
                .as(TalkRelationship.class);
    }

    @Override
    public List<TalkRelationship> findRelationshipsChangedAfter(String client, Date lastKnown) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{clientId:#,lastChanged: {$gt:#}}", client, lastKnown)
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkRelationship> findRelationshipsWithStatesChangedBefore(String[] states, Date lastChanged) {
        Iterator<TalkRelationship> it = mRelationships
                .find("{state: { $in: # }, lastChanged: { $lt:# } }", Arrays.asList(states), lastChanged)
                .as(TalkRelationship.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public int deleteRelationshipsWithStatesChangedBefore(String[] states, Date lastChanged) {
        WriteResult result = mRelationships.remove("{state: { $in: # }, lastChanged: { $lt:# } }", Arrays.asList(states), lastChanged);
        return result.getN();
    }

    @Override
    public int deleteRelationshipsWithStatesAndNotNotificationsDisabledChangedBefore(String[] states, Date lastChanged) {
        WriteResult result = mRelationships.remove("{state: { $in: # }, lastChanged: { $lt:# } , notificationPreference: { $ne:#} }", Arrays.asList(states), lastChanged, TalkRelationship.NOTIFICATIONS_DISABLED);
        return result.getN();
    }

    @Override
    public void deleteRelationship(TalkRelationship relationship) {
        mRelationships.remove("{clientId:#,otherClientId:#}",
                relationship.getClientId(), relationship.getOtherClientId());
    }

    @Override
    public void saveRelationship(TalkRelationship relationship) {
        mRelationships.save(relationship);
    }

    @Override
    public TalkGroupPresence findGroupPresenceById(String groupId) {
        return mGroupPresences.findOne("{groupId:#}", groupId).as(TalkGroupPresence.class);
    }

    @Override
    public void deleteGroupPresence(TalkGroupPresence groupPresence) {
        mGroupPresences.remove("{groupId:#}", groupPresence.getGroupId());
    }


    @Override
    public List<TalkGroupPresence> findGroupPresencesByClientIdChangedAfter(String clientId, Date lastKnown) {
        return findGroupsByClientIdChangedAfterV1(clientId,lastKnown);
    }


    private List<TalkGroupPresence> findGroupsByClientIdChangedAfterV1(String clientId, Date lastKnown) {
        // indirect query
        List<TalkGroupPresence> res = new ArrayList<TalkGroupPresence>();
        List<TalkGroupMembership> memberships = findGroupMembershipsForClient(clientId);
        for (TalkGroupMembership membership : memberships) {
            if (membership.isMember() || membership.isInvited() || membership.isSuspended()) {
                TalkGroupPresence groupPresence = findGroupPresenceById(membership.getGroupId());
                if (groupPresence == null) {
                    // TODO: Define and throw a dedicated exception instead of using a generic one (from sonarqube)
                    throw new RuntimeException("Internal inconsistency, could not find group "+membership.getGroupId()+ "for member client "+clientId);
                }
                if(groupPresence.getLastChanged() == null || lastKnown == null || lastKnown.getTime() == 0 || groupPresence.getLastChanged().after(lastKnown)) {
                    res.add(groupPresence);
                }
             }
        }
        return res;
    }

    private List<TalkGroupPresence> findGroupsByClientIdChangedAfterV2(String clientId, Date lastKnown) {
        // indirect query
        List<TalkGroupPresence> res = new ArrayList<TalkGroupPresence>();
        List<TalkGroupMembership> memberships = findGroupMembershipsByIdWithStates(clientId, new String[]{TalkGroupMembership.STATE_JOINED, TalkGroupMembership.STATE_INVITED, TalkGroupMembership.STATE_SUSPENDED});
        for (TalkGroupMembership membership : memberships) {
                TalkGroupPresence groupPresence = findGroupPresenceById(membership.getGroupId());
                if (groupPresence == null) {
                    // TODO: Define and throw a dedicated exception instead of using a generic one (from sonarqube)
                    throw new RuntimeException("Internal inconsistency, could not find group "+membership.getGroupId()+ "for member client "+clientId);
                }
                if(groupPresence.getLastChanged() == null || lastKnown == null || lastKnown.getTime() == 0 || groupPresence.getLastChanged().after(lastKnown)) {
                    res.add(groupPresence);
                }
        }
        return res;
    }

    @Override
    public List<TalkGroupPresence> findGroupPresencesWithState(String state) {
        Iterator<TalkGroupPresence> it = mGroupPresences
                .find("{state:#}", state)
                .as(TalkGroupPresence.class)
                .iterator();

        return IteratorUtils.toList(it);
    }
    @Override
    public List<TalkGroupPresence> findGroupPresencesWithTypeAndState(String groupType, String state) {
        Iterator<TalkGroupPresence> it = mGroupPresences
                .find("{state: # , groupType: # }", state, groupType)
                .as(TalkGroupPresence.class)
                .iterator();

        return IteratorUtils.toList(it);
    }
    @Override
    public List<TalkGroupPresence> findGroupPresencesWithStateChangedBefore(String state, Date changedDate) {
        Iterator<TalkGroupPresence> it = mGroupPresences
                .find("{state:#, lastChanged: {$lt:#} }", state, changedDate)
                .as(TalkGroupPresence.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public int deleteGroupPresencesWithStateChangedBefore(String state, Date changedDate) {
        WriteResult result = mGroupPresences.remove("{state:#, lastChanged: {$lt:#} }", state, changedDate);
        return result.getN();
    }

    @Override
    public int deleteGroupPresencesWithStateAndTypeChangedBefore(String state, String groupType, Date changedDate) {
        WriteResult result = mGroupPresences.remove("{state:#, groupType:#, lastChanged: {$lt:#} }", state, groupType, changedDate);
        return result.getN();
    }

    @Override
    public void saveGroupPresence(TalkGroupPresence groupPresence) {
        mGroupPresences.save(groupPresence);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsById(String groupId) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{groupId:#}", groupId)
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsByIdWithStates(String groupId, String[] states) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{groupId:#, state: { $in: # }}", groupId, Arrays.asList(states))
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsByIdWithStatesAndRoles(String groupId, String[] states, String[] roles) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{groupId:#, state: { $in: # }, role: {$in: #} }", groupId, Arrays.asList(states), Arrays.asList(roles))
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsWithStatesAndRoles(String[] states, String[] roles) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{state: { $in: # }, role: {$in: #} }", Arrays.asList(states), Arrays.asList(roles))
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }


    @Override
    public List<TalkGroupMembership> findGroupMembershipsByIdChangedAfter(String groupId, Date lastKnown) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{groupId:#,lastChanged: {$gt:#}}", groupId, lastKnown)
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsByIdWithStatesChangedAfter(String groupId, String[] states, Date lastKnown) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{groupId:#, state: { $in: # }, lastChanged: { $gt:# } }", groupId, Arrays.asList(states), lastKnown)
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsWithStatesChangedBefore(String[] states, Date lastChanged) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{state: { $in: # }, lastChanged: { $lt:# } }", Arrays.asList(states), lastChanged)
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public int deleteGroupMembershipsWithStatesChangedBefore(String[] states, Date lastChanged) {
        WriteResult result = mGroupMemberships.remove("{state: { $in: # }, lastChanged: { $lt:# } }", Arrays.asList(states), lastChanged);
        return result.getN();
    }

    @Override
    public int deleteGroupMembershipsWithStatesAndRolesChangedBefore(String[] states, String[] roles, Date lastChanged) {
        WriteResult result = mGroupMemberships.remove("{state: { $in: # }, role: { $in: # }, lastChanged: { $lt:# } }", Arrays.asList(states), Arrays.asList(roles), lastChanged);
        return result.getN();
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsForClient(String clientId) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{clientId:#}", clientId)
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsForClientWithStates(String clientId, String[] states) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{clientId:#, state: { $in: # }}", clientId, Arrays.asList(states))
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkGroupMembership> findGroupMembershipsForClientWithStatesAndRoles(String clientId, String[] states, String[] roles) {
        Iterator<TalkGroupMembership> it = mGroupMemberships
                .find("{clientId:#, state: { $in: # }, role: { $in: # }}", clientId, Arrays.asList(states),  Arrays.asList(roles))
                .as(TalkGroupMembership.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public TalkGroupMembership findGroupMembershipForClient(String groupId, String clientId) {
        return mGroupMemberships.findOne("{groupId:#,clientId:#}", groupId, clientId)
                .as(TalkGroupMembership.class);
    }

    @Override
    public void saveGroupMembership(TalkGroupMembership membership) {
        mGroupMemberships.save(membership);
    }

    @Override
    public void deleteGroupMembership(TalkGroupMembership membership) {
        mGroupMemberships.remove("{groupId:#,clientId:#}", membership.getGroupId(), membership.getClientId());
    }

    @Override
    public void saveEnvironment(TalkEnvironment environment) {
        if (environment.getClientId() != null) {
            mEnvironments.save(environment);
        } else {
            LOG.warn("Not saving environment without clientId (groupId == " + environment.getGroupId() + ")");
        }
    }

    @Override
    public TalkEnvironment findEnvironmentByClientId(String type, String clientId) {
        return mEnvironments.findOne("{type:#, clientId:#}", type, clientId)
                .as(TalkEnvironment.class);
    }

    @Override
    public TalkEnvironment findEnvironmentByClientIdForGroup(String clientId, String groupId) {
        return mEnvironments.findOne("{clientId:#, groupId:#}", clientId, groupId)
                .as(TalkEnvironment.class);
    }

    @Override
    public List<TalkEnvironment> findEnvironmentsForGroup(String groupId) {
        Iterator<TalkEnvironment> it = mEnvironments
                .find("{groupId:#}", groupId)
                .as(TalkEnvironment.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkEnvironment> findEnvironmentsForClient(String clientId) {
        Iterator<TalkEnvironment> it = mEnvironments
                .find("{clientId:#}", clientId)
                .as(TalkEnvironment.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkEnvironment> findEnvironmentsByType(String type) {
        Iterator<TalkEnvironment> it = mEnvironments
                .find("{type:#}", type)
                .as(TalkEnvironment.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public List<TalkEnvironment> findEnvironmentsForClient(String clientId, String type) {
        Iterator<TalkEnvironment> it = mEnvironments
                .find("{clientId:#, type:#}", clientId, type)
                .as(TalkEnvironment.class)
                .iterator();

        return IteratorUtils.toList(it);
    }


    private List<TalkEnvironment> findEnvironmentsMatchingNearby(TalkEnvironment environment) {
        List<TalkEnvironment> res = new ArrayList<TalkEnvironment>();

        // do geospatial search
        Double[] searchCenter = environment.getGeoLocation();
        Float accuracy = environment.getAccuracy();
        if (searchCenter != null) {
            Float searchRadius = accuracy;
            if (searchRadius > 200.f) {
                searchRadius = 200.f;
            }
            if (searchRadius < 100.f) {
                searchRadius = 100.f;
            }
            Double EARTH_RADIUS = 1000.0 * 6371.0;
            Double searchRadiusRad = searchRadius / EARTH_RADIUS;
            Iterator<TalkEnvironment> it = mEnvironments.find("{type:#, geoLocation : { $geoWithin : { $centerSphere : [ [# , #] , # ] } } }", environment.getType(), searchCenter[0], searchCenter[1], searchRadiusRad)
                    .as(TalkEnvironment.class).iterator();
            while (it.hasNext()) {
                res.add(it.next());
            }
            LOG.debug("found " + res.size() + " environments by geolocation");
        }

        // do bssid search
        if (environment.getBssids() != null) {
            List<String> bssids = Arrays.asList(environment.getBssids());
            Iterator<TalkEnvironment> it =
                    mEnvironments.find("{type:#, bssids :{ $in: # } }", environment.getType(), bssids)
                            .as(TalkEnvironment.class).iterator();
            int totalFound = 0;
            int newFound = 0;
            while (it.hasNext()) {
                TalkEnvironment te = it.next();
                ++totalFound;
                boolean found = false;
                for (TalkEnvironment rte : res) {
                    if (rte.getGroupId().equals(te.getGroupId()) && rte.getClientId().equals(te.getClientId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    res.add(te);
                    ++newFound;
                }
            }
            LOG.debug("found " + totalFound + " environments by bssid, " + newFound + " of them are new");
        }

        return res;

    }

    private List<TalkEnvironment> findEnvironmentsMatchingWorldwide(TalkEnvironment environment) {

        List<TalkEnvironment> res = new ArrayList<TalkEnvironment>();

        // do identifiers search first
        if (environment.getTag() != null) {
            Iterator<TalkEnvironment> it =
                    mEnvironments.find("{ type:#, tag :# }", environment.getType(), environment.getTag())
                            .as(TalkEnvironment.class).iterator();
            while (it.hasNext()) {
                TalkEnvironment te = it.next();
                res.add(te);
            }
            LOG.debug("found " + res.size() + " worldwide environments for tag "+environment.getTag());
        }
        LOG.debug("findEnvironmentsMatchingWorldwide: returning "+res.size()+ "environments");
        return res;
    }


    @Override
    public List<TalkEnvironment> findEnvironmentsMatching(TalkEnvironment environment) {
        if (TalkEnvironment.TYPE_NEARBY.equals(environment.getType())) {
            return findEnvironmentsMatchingNearby(environment);
        } else if (TalkEnvironment.TYPE_WORLDWIDE.equals(environment.getType())) {
            return findEnvironmentsMatchingWorldwide(environment);
        }
        throw new RuntimeException("findEnvironmentsMatching: unknown environment type "+environment.getType());
    }

    @Override
    public void deleteEnvironment(TalkEnvironment environment) {
        mEnvironments.remove("{type:#, clientId:#}", environment.getType(), environment.getClientId());
    }

    @Override
    public boolean ping() {
        return mDb.command("ping").ok();
    }

    @Override
    public void reportPing() {
        try {
            ping();
            LOG.info("Database is online");
        } catch (Exception e) {
            LOG.error("Database is not online:", e);
        }
    }

    @Override
    public TalkClientHostInfo findClientHostInfoForClient(String clientId) {
        return mClientHostInfos.findOne("{clientId: #}", clientId)
                .as(TalkClientHostInfo.class);
    }

    @Override
    public List<TalkClientHostInfo> findClientHostInfoByClientLanguageAndClientName(String clientLanguage, String clientName) {
        Iterator<TalkClientHostInfo> it = mClientHostInfos
                .find("{clientLanguage: #, clientName: #}", clientLanguage, clientName)
                .as(TalkClientHostInfo.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    public void saveClientHostInfo(TalkClientHostInfo clientHostInfo) {
        mClientHostInfos.save(clientHostInfo);
    }

    @Override
    public void deleteClientHostInfo(TalkClientHostInfo clientHostInfo) {
        mClientHostInfos.remove("{clientId:#}", clientHostInfo.getClientId());
    }

    @Override
    public List<TalkDatabaseMigration> findDatabaseMigrations() {
        Iterator<TalkDatabaseMigration> it = mMigrations
                .find()
                .sort("{position: 1}") // retrieve migration sorted
                .as(TalkDatabaseMigration.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    // Used for migrations - it bulk updates all specific field values encountered of the field specified with the given new value
    // *Note:* This is brutally fast
    public void changeDeliveryFieldValue(String fieldName, String oldFieldValue, String newFieldValue) {
        mDeliveries.update("{" + fieldName + ": '" + oldFieldValue + "'}")
                .multi()
                .with("{$set: {" + fieldName + ": '" + newFieldValue + "'}}");
        mDeliveries.findAndModify();
    }

    @Override
    public void saveDatabaseMigration(TalkDatabaseMigration migration) {
        mMigrations.save(migration);
    }
}
