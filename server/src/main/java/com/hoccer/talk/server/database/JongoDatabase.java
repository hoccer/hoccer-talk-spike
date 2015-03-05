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
    }

    private MongoCollection getCollection(String name) {
        MongoCollection res = mJongo.getCollection(name).withWriteConcern(WriteConcern.JOURNALED);
        mCollections.add(res);
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
    public void markClientDeleted(@NotNull TalkClient client) {
        if (!isDeletedClient(client.getClientId())) {
            client.setClientId(client.getClientId()+"-DELETED");
            client.setTimeDeleted(new Date());
            mClients.save(client);
        }
    }

    @Override
    public void deleteClient(@NotNull TalkClient client) {
        mMessages.remove("{clientId:#}", client.getClientId());
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
    public List<TalkMessage> findMessagesFromClient(String clientId) {
        Iterator<TalkMessage> it = mMessages
                .find("{clientId:#}", clientId)
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
    public TalkDelivery findDelivery(String messageId, String clientId) {
        return mDeliveries.findOne("{messageId:#,receiverId:#}", messageId, clientId)
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
    @NotNull
    public List<TalkDelivery> findDeliveriesForClient(String clientId) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#}", clientId)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClient(String clientId) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#}", clientId)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForClientInState(String clientId, String state) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#,state:#}", clientId, state)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesForClientInDeliveryAndAttachmentStates(String clientId, String[] deliveryStates, String[] attachmentStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{receiverId:#, state: { $in: # }, attachmentState: {$in: # } }", clientId, Arrays.asList(deliveryStates), Arrays.asList(attachmentStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientInState(String clientId, String state) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#,state:#}", clientId, state)
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientInStates(String clientId, String[] deliveryStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#, state: { $in: # } }", clientId, Arrays.asList(deliveryStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
    }

    @Override
    @NotNull
    public List<TalkDelivery> findDeliveriesFromClientInDeliveryAndAttachmentStates(String clientId, String[] deliveryStates, String[] attachmentStates) {
        Iterator<TalkDelivery> it = mDeliveries
                .find("{senderId:#, state: { $in: # }, attachmentState: {$in: #} }", clientId, Arrays.asList(deliveryStates), Arrays.asList(attachmentStates))
                .as(TalkDelivery.class)
                .iterator();

        return IteratorUtils.toList(it);
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
    public void deleteDelivery(TalkDelivery delivery) {
        mDeliveries.remove("{messageId:#,receiverId:#}", delivery.getMessageId(), delivery.getReceiverId());
    }

    @Override
    public void saveDelivery(TalkDelivery delivery) {
        mDeliveries.save(delivery);
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
        List<TalkRelationship> relationships = findRelationshipsByOtherClient(clientId);
        for (TalkRelationship relationship : relationships) {
            if (relationship.isDirectlyRelated()) {
                clients.add(relationship.getClientId());
                if (relationship.getLastChanged().after(lastKnown)) {
                    mustInclude.add(relationship.getClientId());
                }
            }
        }
        // collect clients known through groups
        List<TalkGroupMembership> ownMemberships = findGroupMembershipsForClient(clientId);
        for (TalkGroupMembership ownMembership : ownMemberships) {
            String groupId = ownMembership.getGroupId();
            if (ownMembership.isInvited() || ownMembership.isJoined()) {
                List<TalkGroupMembership> otherMemberships = findGroupMembershipsById(groupId);
                for (TalkGroupMembership otherMembership : otherMemberships) {
                    if (otherMembership.isInvited() || otherMembership.isJoined()) {
                        clients.add(otherMembership.getClientId());
                        if (otherMembership.getLastChanged().after(lastKnown) || ownMembership.getLastChanged().after(lastKnown)) {
                            mustInclude.add(otherMembership.getClientId());
                        }
                    }
                }
            }
        }
        // remove self
        clients.remove(clientId);
        // collect presences
        for (String client : clients) {
            TalkPresence pres = findPresenceForClient(client);
            if (pres != null &&
                    (pres.getTimestamp().after(lastKnown) ||
                            mustInclude.contains(client))) {
                res.add(pres);
            }
        }
        return res;
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
            if (membership.isMember() || membership.isInvited()) {
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
        List<TalkGroupMembership> memberships = findGroupMembershipsByIdWithStates(clientId, new String[]{TalkGroupMembership.STATE_JOINED, TalkGroupMembership.STATE_INVITED});
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
                .find("{groupId:#, state: { $in: # }, role: {$in: #}", groupId, Arrays.asList(states), Arrays.asList(roles))
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
    public TalkGroupMembership findGroupMembershipForClient(String groupId, String clientId) {
        return mGroupMemberships.findOne("{groupId:#,clientId:#}", groupId, clientId)
                .as(TalkGroupMembership.class);
    }

    @Override
    public void saveGroupMembership(TalkGroupMembership membership) {
        mGroupMemberships.save(membership);
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
    public List<TalkEnvironment> findEnvironmentsMatching(TalkEnvironment environment) {
        mEnvironments.ensureIndex("{geoLocation: '2dsphere'}");
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

        // do identifiers search
        if (environment.getIdentifiers() != null) {
            List<String> identifiers = Arrays.asList(environment.getIdentifiers());
            Iterator<TalkEnvironment> it =
                    mEnvironments.find("{ identifiers :{ $in: # } }", identifiers)
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

            LOG.debug("found " + totalFound + " environments by identifiers, " + newFound + " of them are new");
        }

        return res;
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
