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

    public Object getRawCollection(String name);

    public List<TalkClient> findAllClients();

    public TalkClient findClientById(String clientId);

    public TalkClient findDeletedClientById(String clientId);

    public boolean isDeletedClient(String clientId);

    public String beforeDeletedId(String clientId);

    public TalkClient findClientByApnsToken(String apnsToken);

    public void saveClient(TalkClient client);

    public void markClientDeleted(TalkClient client, String reason);

    public void unmarkClientDeleted(TalkClient client);

    public void suspendClient(TalkClient client, Date when, long duration);

    public void unsuspendClient(TalkClient client);

    public void deleteClient(TalkClient client);

    public TalkMessage findMessageById(String messageId);

    public List<TalkMessage> findMessagesWithAttachmentFileId(String fileId);

    public List<TalkMessage> findMessagesFromClient(String senderId);

    public void deleteMessage(TalkMessage message);

    public void saveMessage(TalkMessage message);

    public TalkDelivery findDelivery(String messageId, String receiverId);

    public List<TalkDelivery> findDeliveriesInState(String state);

    List<TalkDelivery> findAllDeliveries();

    public List<TalkDelivery> findDeliveriesInStates(String[] states);

    public List<TalkDelivery> findDeliveriesInStatesAndAttachmentStates(String[] states, String[] attachmentStates);
    public long countDeliveriesInStatesAndAttachmentStates(String[] deliveryStates, String[] attachmentStates);

    public List<TalkDelivery> findDeliveriesForClient(String receiverId);

    public List<TalkDelivery> findDeliveriesForClientInState(String receiverId, String state);

    public List<TalkDelivery> findDeliveriesForClientInGroupInState(String receiverId, String groupId, String state);

    public long countDeliveriesForClientInGroupInState(String receiverId, String groupId, String state);

    public List<TalkDelivery> findDeliveriesForClientInDeliveryAndAttachmentStates(String receiverId, String[] deliveryStates, String[] attachmentStates);

    public List<TalkDelivery> findDeliveriesFromClient(String senderId);

    public List<TalkDelivery> findDeliveriesFromClientInState(String senderId, String state);
    public long countDeliveriesFromClientInState(String senderId, String state);
    public long countDeliveriesBetweenClientsInState(String senderId, String receiverId, String state);

    public List<TalkDelivery> findDeliveriesFromClientInStates(String senderId, String[] states);

    public List<TalkDelivery> findDeliveriesFromClientInDeliveryAndAttachmentStates(String senderId, String[] deliveryStates, String[] attachmentStates);
    public long countDeliveriesFromClientInDeliveryAndAttachmentStates(String senderId, String[] deliveryStates, String[] attachmentStates);
    public long countDeliveriesBetweenClientsInDeliveryAndAttachmentStates(String senderId, String receiverId, String[] deliveryStates, String[] attachmentStates);

    public List<TalkDelivery> findDeliveriesForMessage(String messageId);

    public List<TalkDelivery> findDeliveriesFromClientForMessage(String senderId, String messageId);

    public List<TalkDelivery> findDeliveriesAcceptedBefore(Date limit);

    public void deleteDelivery(TalkDelivery delivery);

    public void saveDelivery(TalkDelivery delivery);

    public void updateDeliveryTimeClientNotified(TalkDelivery delivery);

    public List<TalkToken> findTokensByClient(String clientId);

    public TalkToken findTokenByPurposeAndSecret(String purpose, String secret);

    public void deleteToken(TalkToken token);

    public void saveToken(TalkToken token);

    public TalkPresence findPresenceForClient(String clientId);

    public void savePresence(TalkPresence presence);

    public void deletePresence(TalkPresence presence);

    public List<TalkPresence> findPresencesChangedAfter(String clientId, Date lastKnown);

    public List<TalkPresence> findPresencesWithStates(String[] states);

    public TalkKey findKey(String clientId, String keyId);

    public List<TalkKey> findKeys(String clientId);

    public void deleteKey(TalkKey key);

    public void saveKey(TalkKey key);

    public List<TalkRelationship> findRelationships(String client);

    public List<TalkRelationship> findRelationshipsForClientInState(String clientId, String state);

    public List<TalkRelationship> findRelationshipsForClientInStates(String clientId, String[] states);

    public List<TalkRelationship> findRelationshipsForOtherClientInStates(String clientId, String[] states);

    public List<TalkRelationship> findRelationshipsByOtherClient(String other);

    public List<TalkRelationship> findRelationshipsChangedAfter(String client, Date lastKnown);

    public List<TalkRelationship> findRelationshipsWithStatesChangedBefore(String[] states, Date lastChanged);

    public int deleteRelationshipsWithStatesChangedBefore(String[] states, Date lastChanged);
    public int deleteRelationshipsWithStatesAndNotNotificationsDisabledChangedBefore(String[] states, Date lastChanged);

    @Nullable
    public TalkRelationship findRelationshipBetween(String client, String otherClient);

    public void deleteRelationship(TalkRelationship relationship);

    public void saveRelationship(TalkRelationship relationship);

    public TalkGroupPresence findGroupPresenceById(String groupId);

    public void deleteGroupPresence(TalkGroupPresence groupPresence);

    public List<TalkGroupPresence> findGroupPresencesByClientIdChangedAfter(String clientId, Date lastKnown);

    public List<TalkGroupMembership> findGroupMembershipsByIdWithStatesChangedAfter(String groupId, String[] states, Date lastKnown);

    public List<TalkGroupMembership> findGroupMembershipsWithStatesChangedBefore(String[] states, Date lastChanged);

    public int deleteGroupMembershipsWithStatesChangedBefore(String[] states, Date lastChanged);

    public int deleteGroupMembershipsWithStatesAndRolesChangedBefore(String[] states, String[] roles, Date lastChanged);

    public List<TalkGroupPresence> findGroupPresencesWithState(String state);

    public List<TalkGroupPresence> findGroupPresencesWithTypeAndState(String groupType, String state);

    public List<TalkGroupPresence> findGroupPresencesWithStateChangedBefore(String state, Date changedDate);

    public int deleteGroupPresencesWithStateChangedBefore(String state, Date changedDate);

    public int deleteGroupPresencesWithStateAndTypeChangedBefore(String state, String groupType, Date changedDate);

    public void saveGroupPresence(TalkGroupPresence groupPresence);

    public List<TalkGroupMembership> findGroupMembershipsById(String groupId);

    public List<TalkGroupMembership> findGroupMembershipsByIdWithStates(String groupId, String[] states);

    public List<TalkGroupMembership> findGroupMembershipsByIdWithStatesAndRoles(String groupId, String[] states, String[] roles);

    public List<TalkGroupMembership> findGroupMembershipsWithStatesAndRoles(String[] states, String[] roles);

    public List<TalkGroupMembership> findGroupMembershipsByIdChangedAfter(String groupId, Date lastKnown);

    public List<TalkGroupMembership> findGroupMembershipsForClient(String clientId);

    public List<TalkGroupMembership> findGroupMembershipsForClientWithStates(String clientId, String[] states);

    public List<TalkGroupMembership> findGroupMembershipsForClientWithStatesAndRoles(String clientId, String[] states, String[] roles);

    public TalkGroupMembership findGroupMembershipForClient(String groupId, String clientId);

    public void saveGroupMembership(TalkGroupMembership membership);

    public void deleteGroupMembership(TalkGroupMembership membership);

    public void saveEnvironment(TalkEnvironment environment);

    public TalkEnvironment findEnvironmentByClientId(String type, String clientId);

    public TalkEnvironment findEnvironmentByClientIdForGroup(String clientId, String groupId);

    public List<TalkEnvironment> findEnvironmentsForGroup(String groupId);

    public List<TalkEnvironment> findEnvironmentsForClient(String clientId);

    public List<TalkEnvironment> findEnvironmentsByType(String type);

    public List<TalkEnvironment> findEnvironmentsForClient(String clientId, String type);

    public List<TalkEnvironment> findEnvironmentsMatching(TalkEnvironment environment);

    public void deleteEnvironment(TalkEnvironment environment);

    public boolean ping();

    public void reportPing();

    public TalkClientHostInfo findClientHostInfoForClient(String clientId);

    public List<TalkClientHostInfo> findClientHostInfoByClientLanguageAndClientName(String clientLanguage, String clientName);

    public void saveClientHostInfo(TalkClientHostInfo clientHostInfo);

    public void deleteClientHostInfo(TalkClientHostInfo clientHostInfo);

    public List<TalkDatabaseMigration> findDatabaseMigrations();

    public void saveDatabaseMigration(TalkDatabaseMigration migration);

    public void changeDeliveryFieldValue(String fieldName, String oldFieldValue, String newFieldValue);
}
