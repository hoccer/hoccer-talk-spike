package com.hoccer.talk.server;

import com.hoccer.talk.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

/**
 * Describes the interface of Talk database backends
 * <p/>
 * There currently are two implementations:
 * <p/>
 * .database.JongoDatabase    -  Jongo-based persistent database
 * .database.OrmLiteDatabase  -  Classical Relational persistent database (e.g. Postgresql) - currently unfinished
 */
public interface ITalkServerDatabase {

    public List<TalkClient> findAllClients();

    public TalkClient findClientById(String clientId);

    public TalkClient findClientByApnsToken(String apnsToken);

    public void saveClient(TalkClient client);

    public TalkMessage findMessageById(String messageId);

    public List<TalkMessage> findMessagesWithAttachmentFileId(String fileId);

    public void deleteMessage(TalkMessage message);

    public void saveMessage(TalkMessage message);

    public TalkDelivery findDelivery(String messageId, String clientId);

    public List<TalkDelivery> findDeliveriesInState(String state);

    List<TalkDelivery> findAllDeliveries();

    public List<TalkDelivery> findDeliveriesInStates(String[] states);

    public List<TalkDelivery> findDeliveriesInStatesAndAttachmentStates(String[] states, String[] attachmentStates);

    public List<TalkDelivery> findDeliveriesForClient(String clientId);

    public List<TalkDelivery> findDeliveriesForClientInState(String clientId, String state);

    public List<TalkDelivery> findDeliveriesForClientInDeliveryAndAttachmentStates(String clientId, String[] deliveryStates, String[] attachmentStates);

    public List<TalkDelivery> findDeliveriesFromClient(String clientId);

    public List<TalkDelivery> findDeliveriesFromClientInState(String clientId, String state);

    public List<TalkDelivery> findDeliveriesFromClientInStates(String clientId, String[] states);

    public List<TalkDelivery> findDeliveriesFromClientInDeliveryAndAttachmentStates(String clientId, String[] deliveryStates, String[] attachmentStates);

    public List<TalkDelivery> findDeliveriesForMessage(String messageId);

    public void deleteDelivery(TalkDelivery delivery);

    public void saveDelivery(TalkDelivery delivery);

    public List<TalkToken> findTokensByClient(String clientId);

    public TalkToken findTokenByPurposeAndSecret(String purpose, String secret);

    public void deleteToken(TalkToken token);

    public void saveToken(TalkToken token);

    public TalkPresence findPresenceForClient(String clientId);

    public void savePresence(TalkPresence presence);

    public List<TalkPresence> findPresencesChangedAfter(String clientId, Date lastKnown);

    public TalkKey findKey(String clientId, String keyId);

    public List<TalkKey> findKeys(String clientId);

    public void deleteKey(TalkKey key);

    public void saveKey(TalkKey key);

    public List<TalkRelationship> findRelationships(String client);

    public List<TalkRelationship> findRelationshipsForClientInState(String clientId, String state);

    public List<TalkRelationship> findRelationshipsForClientInStates(String clientId, String[] states);

    public List<TalkRelationship> findRelationshipsByOtherClient(String other);

    public List<TalkRelationship> findRelationshipsChangedAfter(String client, Date lastKnown);

    @Nullable
    public TalkRelationship findRelationshipBetween(String client, String otherClient);

    public void deleteRelationship(TalkRelationship relationship);

    public void saveRelationship(TalkRelationship relationship);

    public TalkGroupPresence findGroupPresenceById(String groupId);

    public void deleteGroupPresence(TalkGroupPresence groupPresence);

    public List<TalkGroupPresence> findGroupPresencesByClientIdChangedAfter(String clientId, Date lastKnown);

    public List<TalkGroupMembership> findGroupMembershipsByIdWithStatesChangedAfter(String groupId, String[] states, Date lastKnown);

    public void saveGroupPresence(TalkGroupPresence groupPresence);

    public List<TalkGroupMembership> findGroupMembershipsById(String groupId);

    public List<TalkGroupMembership> findGroupMembershipsByIdWithStates(String groupId, String[] states);

    public List<TalkGroupMembership> findGroupMembershipsByIdWithStatesAndRoles(String groupId, String[] states, String[] roles);

    public List<TalkGroupMembership> findGroupMembershipsByIdChangedAfter(String groupId, Date lastKnown);

    public List<TalkGroupMembership> findGroupMembershipsForClient(String clientId);

    public List<TalkGroupMembership> findGroupMembershipsForClientWithStates(String clientId, String[] states);

    public TalkGroupMembership findGroupMembershipForClient(String groupId, String clientId);

    public void saveGroupMembership(TalkGroupMembership membership);

    public void saveEnvironment(TalkEnvironment environment);

    public TalkEnvironment findEnvironmentByClientId(String type, String clientId);

    public List<TalkEnvironment> findEnvironmentsForGroup(String groupId);

    public List<TalkEnvironment> findEnvironmentsMatching(TalkEnvironment environment);

    public void deleteEnvironment(TalkEnvironment environment);

    public boolean ping();

    public void reportPing();

    public TalkClientHostInfo findClientHostInfoForClient(String clientId);

    public List<TalkClientHostInfo> findClientHostInfoByClientLanguageAndClientName(String clientLanguage, String clientName);

    public void saveClientHostInfo(TalkClientHostInfo clientHostInfo);

    public List<TalkDatabaseMigration> findDatabaseMigrations();

    public void saveDatabaseMigration(TalkDatabaseMigration migration);

    public void changeDeliveryFieldValue(String fieldName, String oldFieldValue, String newFieldValue);
}
