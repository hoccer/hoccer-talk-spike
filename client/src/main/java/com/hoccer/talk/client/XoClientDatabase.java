package com.hoccer.talk.client;

import com.hoccer.talk.client.exceptions.NoClientIdInPresenceException;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.model.*;
import com.hoccer.talk.util.WeakListenerArray;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.stmt.*;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

public class XoClientDatabase implements IXoMediaCollectionDatabase {

    private static final Logger LOG = Logger.getLogger(XoClientDatabase.class);

    private final IXoClientDatabaseBackend mBackend;

    private Dao<TalkClientContact, Integer> mClientContacts;
    private Dao<TalkClientSelf, Integer> mClientSelfs;
    private Dao<TalkPresence, String> mPresences;
    private Dao<TalkRelationship, Long> mRelationships;
    private Dao<TalkGroupPresence, String> mGroupPresences;
    private Dao<TalkGroupMembership, Long> mGroupMemberships;
    private Dao<TalkClientMessage, Integer> mClientMessages;
    private Dao<TalkMessage, String> mMessages;
    private Dao<TalkDelivery, Long> mDeliveries;
    private Dao<TalkKey, Long> mPublicKeys;
    private Dao<TalkPrivateKey, Long> mPrivateKeys;
    private Dao<TalkClientDownload, Integer> mClientDownloads;
    private Dao<TalkClientUpload, Integer> mClientUploads;
    private Dao<TalkClientMediaCollection, Integer> mMediaCollections;
    private Dao<TalkClientMediaCollectionRelation, Integer> mMediaCollectionRelations;

    private final WeakListenerArray<IXoUploadListener> mUploadListeners = new WeakListenerArray<IXoUploadListener>();
    private final WeakListenerArray<IXoDownloadListener> mDownloadListeners = new WeakListenerArray<IXoDownloadListener>();
    private final WeakListenerArray<IXoMediaCollectionListener> mMediaCollectionListeners = new WeakListenerArray<IXoMediaCollectionListener>();

    public XoClientDatabase(IXoClientDatabaseBackend backend) {
        mBackend = backend;
    }

    public static void createTables(ConnectionSource cs) throws SQLException {
        TableUtils.createTable(cs, TalkClientContact.class);
        TableUtils.createTable(cs, TalkClientSelf.class);
        TableUtils.createTable(cs, TalkPresence.class);
        TableUtils.createTable(cs, TalkRelationship.class);
        TableUtils.createTable(cs, TalkGroupPresence.class);
        TableUtils.createTable(cs, TalkGroupMembership.class);
        TableUtils.createTable(cs, TalkClientMessage.class);
        TableUtils.createTable(cs, TalkMessage.class);
        TableUtils.createTable(cs, TalkDelivery.class);
        TableUtils.createTable(cs, TalkKey.class);
        TableUtils.createTable(cs, TalkPrivateKey.class);
        TableUtils.createTable(cs, TalkClientDownload.class);
        TableUtils.createTable(cs, TalkClientUpload.class);
        TableUtils.createTable(cs, TalkClientMediaCollection.class);
        TableUtils.createTable(cs, TalkClientMediaCollectionRelation.class);
    }

    public void initialize() throws SQLException {
        mClientContacts = mBackend.getDao(TalkClientContact.class);
        mClientSelfs = mBackend.getDao(TalkClientSelf.class);
        mPresences = mBackend.getDao(TalkPresence.class);
        mRelationships = mBackend.getDao(TalkRelationship.class);
        mGroupPresences = mBackend.getDao(TalkGroupPresence.class);
        mGroupMemberships = mBackend.getDao(TalkGroupMembership.class);
        mClientMessages = mBackend.getDao(TalkClientMessage.class);
        mMessages = mBackend.getDao(TalkMessage.class);
        mDeliveries = mBackend.getDao(TalkDelivery.class);
        mPublicKeys = mBackend.getDao(TalkKey.class);
        mPrivateKeys = mBackend.getDao(TalkPrivateKey.class);
        mClientDownloads = mBackend.getDao(TalkClientDownload.class);
        mClientUploads = mBackend.getDao(TalkClientUpload.class);
        mMediaCollections = mBackend.getDao(TalkClientMediaCollection.class);
        mMediaCollectionRelations = mBackend.getDao(TalkClientMediaCollectionRelation.class);
    }

    public synchronized void saveCredentials(TalkClientSelf credentials) throws SQLException {
        mClientSelfs.createOrUpdate(credentials);
    }

    public synchronized void savePublicKey(TalkKey publicKey) throws SQLException {
        mPublicKeys.createOrUpdate(publicKey);
    }

    public synchronized void savePrivateKey(TalkPrivateKey privateKey) throws SQLException {
        mPrivateKeys.createOrUpdate(privateKey);
    }

    ////////////////////////////////////
    //////// Contact Management ////////
    ////////////////////////////////////

    public synchronized void saveContact(TalkClientContact contact) throws SQLException {
        mClientContacts.createOrUpdate(contact);
    }

    public void refreshClientContact(TalkClientContact contact) throws SQLException {
        mClientContacts.refresh(contact);
    }

    public synchronized void savePresence(TalkPresence presence) throws NoClientIdInPresenceException, SQLException {
        if (presence.getClientId() == null) {
            throw new NoClientIdInPresenceException("Client id is null.");
        }
        mPresences.createOrUpdate(presence);
    }

    public synchronized void saveRelationship(TalkRelationship relationship) throws SQLException {
        mRelationships.createOrUpdate(relationship);
    }

    public TalkClientContact findSelfContact(boolean create) throws SQLException {
        TalkClientContact contact = mClientContacts.queryBuilder().where()
                .eq("contactType", TalkClientContact.TYPE_SELF)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_SELF);
            mClientContacts.create(contact);
        }

        return contact;
    }

    public TalkClientContact findContactById(int contactId) throws SQLException {
        return mClientContacts.queryForId(contactId);
    }

    public synchronized TalkClientContact findContactByClientId(String clientId, boolean create) throws SQLException {
        TalkClientContact contact = mClientContacts.queryBuilder().where()
                .eq("clientId", clientId)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, clientId);
            mClientContacts.create(contact);
        }

        return contact;
    }

    public List<TalkClientContact> findAllContacts() throws SQLException {
        return mClientContacts.queryForAll();
    }

    public List<TalkClientContact> findAllContactsExceptSelfOrderedByRecentMessage() throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> messageQuery = mClientMessages.queryBuilder();
        messageQuery.orderBy("timestamp", false);

        QueryBuilder<TalkClientContact, Integer> contactsQuery = mClientContacts.queryBuilder();
        contactsQuery.where().ne("contactType", TalkClientContact.TYPE_SELF);

        List<TalkClientContact> allContacts = contactsQuery.query();
        List<TalkClientContact> orderedContacts = contactsQuery.join(messageQuery).query();

        List<TalkClientContact> orderedDistinctContacts = new ArrayList<TalkClientContact>();
        for (TalkClientContact contact : orderedContacts) {
            if (!orderedDistinctContacts.contains(contact)) {
                orderedDistinctContacts.add(contact);
            }
        }
        // add contacts without message
        for (TalkClientContact contact : allContacts) {
            if (!orderedDistinctContacts.contains(contact)) {
                orderedDistinctContacts.add(contact);
            }
        }
        return orderedDistinctContacts;
    }

    public List<TalkClientContact> findAllClientContacts() throws SQLException {
        return mClientContacts.queryForEq("contactType", TalkClientContact.TYPE_CLIENT);
    }

    public List<TalkClientContact> findClientContactsByState(String state) throws SQLException {
        QueryBuilder<TalkRelationship, Long> relationships = mRelationships.queryBuilder();
        relationships.where().eq("state", state);

        QueryBuilder<TalkClientContact, Integer> contacts = mClientContacts.queryBuilder();
        return contacts.join(relationships).query();
    }

    public List<TalkClientContact> findClientContactsByState(String state, String unblockState) throws SQLException {
        QueryBuilder<TalkRelationship, Long> relationships = mRelationships.queryBuilder();
        relationships.where()
                .eq("state", state)
                .and()
                .eq("unblockState", unblockState);

        QueryBuilder<TalkClientContact, Integer> contacts = mClientContacts.queryBuilder();
        return contacts.join(relationships).query();
    }

    //////////////////////////////////
    //////// Group Management ////////
    //////////////////////////////////

    public synchronized void saveGroupPresence(TalkGroupPresence group) throws SQLException {
        mGroupPresences.createOrUpdate(group);
    }

    public TalkClientContact findContactByGroupTag(String groupTag) throws SQLException {
        return mClientContacts.queryBuilder().where()
                .eq("groupTag", groupTag)
                .queryForFirst();
    }

    public List<TalkClientContact> findAllGroupContacts() throws SQLException {
        return mClientContacts.queryForEq("contactType", TalkClientContact.TYPE_GROUP);
    }

    public boolean isClientContactInGroupOfType(String groupType, String clientId) throws SQLException {
        List<TalkGroupPresence> groupPresences = mGroupPresences.queryBuilder().where()
                .eq("groupType", groupType)
                .query();

        List<TalkGroupMembership> memberships = mGroupMemberships.queryBuilder().where()
                .eq("clientId", clientId)
                .query();

        for (TalkGroupMembership groupMembership : memberships) {
            for (TalkGroupPresence groupPresence : groupPresences) {
                if (groupMembership.getGroupId().equals(groupPresence.getGroupId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<TalkClientContact> findGroupContactsByMembershipState(String state) throws SQLException {
        QueryBuilder<TalkGroupMembership, Long> memberships = mGroupMemberships.queryBuilder();
        memberships.where()
                .eq("state", state);

        QueryBuilder<TalkClientContact, Integer> contacts = mClientContacts.queryBuilder();
        contacts.where().eq("contactType", TalkClientContact.TYPE_GROUP);

        return contacts.join(memberships).query();
    }

    public synchronized void saveGroupMembership(TalkGroupMembership membership) throws SQLException {
        mGroupMemberships.createOrUpdate(membership);
    }

    public List<TalkGroupMembership> findMembershipsInGroup(String groupId) throws SQLException {
        return mGroupMemberships.queryBuilder().where()
                .eq("groupId", groupId)
                .query();
    }

    public TalkGroupMembership findMembershipInGroupByClientId(String groupId, String clientId) throws SQLException {
        return mGroupMemberships.queryBuilder().where()
                .eq("groupId", groupId)
                .and()
                .eq("clientId", clientId)
                .queryForFirst();
    }

    public List<TalkGroupMembership> findMembershipsInGroupByState(String groupId, String state) throws SQLException {
        return mGroupMemberships.queryBuilder().where()
                .eq("groupId", groupId)
                .and()
                .eq("state", state)
                .query();
    }

    public List<TalkClientContact> findContactsInGroup(String groupId) throws SQLException {
        List<TalkGroupMembership> memberships = mGroupMemberships.queryForEq("groupId", groupId);
        return getContactsForMemberships(memberships);
    }

    public List<TalkClientContact> findContactsInGroupByState(String groupId, String state) throws SQLException {
        List<TalkGroupMembership> memberships = findMembershipsInGroupByState(groupId, state);

        return getContactsForMemberships(memberships);
    }

    public TalkClientContact findAdminInGroup(String groupId) throws SQLException {
        List<TalkClientContact> contacts = findContactsInGroupByRole(groupId, TalkGroupMembership.ROLE_ADMIN);

        if (contacts.size() == 1) {
            return contacts.get(0);
        } else {
            LOG.error("Found " + contacts.size() + " admin(s) in group contact with groupId " + groupId);
            return null;
        }
    }

    public List<TalkClientContact> findContactsInGroupByRole(String groupId, String role) throws SQLException {
        List<TalkGroupMembership> memberships = mGroupMemberships.queryBuilder().where()
                .eq("groupId", groupId)
                .and()
                .eq("role", role)
                .query();

        return getContactsForMemberships(memberships);
    }

    public synchronized TalkClientContact findGroupContactByGroupId(String groupId, boolean create) throws SQLException {
        TalkClientContact contact = mClientContacts.queryBuilder().where()
                .eq("groupId", groupId)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_GROUP, groupId);
            mClientContacts.create(contact);
        }

        return contact;
    }

    private List<TalkClientContact> getContactsForMemberships(final List<TalkGroupMembership> memberships) throws SQLException {
        Collection<String> clientIds = CollectionUtils.collect(memberships, new Transformer<TalkGroupMembership, String>() {
            @Override
            public String transform(TalkGroupMembership membership) {
                return membership.getClientId();
            }
        });
        return mClientContacts.queryBuilder().where().in("clientId", clientIds).query();
    }

    ////////////////////////////////////
    //////// Message Management ////////
    ////////////////////////////////////

    public synchronized void saveClientMessage(TalkClientMessage message) throws SQLException {
        mClientMessages.createOrUpdate(message);
    }

    public synchronized void saveMessage(TalkMessage message) throws SQLException {
        mMessages.createOrUpdate(message);
    }

    public synchronized List<TalkClientMessage> findMessagesForDelivery() throws SQLException {
        List<TalkDelivery> newDeliveries = mDeliveries.queryForEq(TalkDelivery.FIELD_STATE, TalkDelivery.STATE_NEW);
        LOG.debug("findMessagesForDelivery found "+newDeliveries.size()+" unsent deliveries");

        List<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
        try {
            for (TalkDelivery newDelivery : newDeliveries) {
                TalkClientMessage message = mClientMessages.queryBuilder().where()
                        .eq("deleted", false)
                        .eq("delivery_id", newDelivery)
                        .eq("direction", TalkClientMessage.TYPE_OUTGOING)
                        .and(3)
                        .queryForFirst();

                if (message != null) {

                    if (!message.isInProgress()) {
                        messages.add(message);
                    } else {
                        LOG.debug("findMessagesForDelivery: not adding message because in progress, tag "+message.getMessageTag());
                    }

                } else {
                    LOG.error("No outgoing delivery for message with tag '" + newDelivery.getMessageTag() + "'.");
                }
            }

        } catch (SQLException e) {
            LOG.error("Error while fetching messages for delivery: ", e);
        }

        return messages;
    }

    public synchronized TalkClientMessage findMessageById(int messageId) throws SQLException {
        return mClientMessages.queryForId(messageId);
    }

    public synchronized TalkClientMessage findMessageByMessageId(String messageId, boolean create) throws SQLException {
        TalkClientMessage message = mClientMessages.queryBuilder().where()
                .eq("messageId", messageId)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();

        if (create && message == null) {
            message = new TalkClientMessage();
            message.setMessageId(messageId);
            mClientMessages.create(message);
        }

        return message;
    }

    public synchronized TalkClientMessage findMessageByMessageTag(String messageTag, boolean create) throws SQLException {
        TalkClientMessage message = mClientMessages.queryBuilder().where()
                .eq("messageTag", messageTag)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();

        if (create && message == null) {
            message = new TalkClientMessage();
            mClientMessages.create(message);
        }

        return message;
    }

    public List<TalkClientMessage> findNearbyMessages(long count, long offset) throws SQLException {
        List<TalkClientMessage> result = getAllNearbyGroupMessages();
        if (offset + count > result.size()) {
            count = result.size() - offset;
        }
        ArrayList<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
        for (int i = (int) offset; i < offset + count; i++) {
            messages.add(result.get(i));
        }
        return messages;
    }

    public long getHistoryGroupMessageCount(String groupType) throws SQLException {
        QueryBuilder<TalkClientMessage, ?> clientMessages = mClientMessages.queryBuilder();
        clientMessages.where()
                .eq("deleted", false);

        QueryBuilder<TalkClientContact, ?> clientContacts = mClientContacts.queryBuilder();
        clientContacts.where()
                .eq("deleted", false);

        QueryBuilder<TalkGroupPresence, ?> groupPresences = mGroupPresences.queryBuilder();
        groupPresences.where()
                .eq("groupType", groupType);

        return clientMessages.join(
                clientContacts.join(groupPresences)).countOf();
    }

    public List<TalkClientMessage> getAllNearbyGroupMessages() throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryBuilder()
                .orderBy("timestamp", true).where()
                .eq("deleted", false)
                .query();

        ArrayList<TalkClientMessage> nearbyMessages = new ArrayList<TalkClientMessage>();
        for (TalkClientMessage message : messages) {
            TalkClientContact conversationContact = message.getConversationContact();
            if (conversationContact != null && conversationContact.getContactType() != null) {
                if (conversationContact.isGroup()) {
                    TalkGroupPresence groupPresence = conversationContact.getGroupPresence();
                    if (groupPresence != null && groupPresence.isTypeNearby()) {
                        nearbyMessages.add(message);
                    }
                }
            }
        }
        ArrayList<TalkClientContact> allNearbyGroupsOrdered = new ArrayList<TalkClientContact>();
        for (TalkClientMessage m : nearbyMessages) {
            if (!allNearbyGroupsOrdered.contains(m.getConversationContact())) {
                allNearbyGroupsOrdered.add(m.getConversationContact());
            }
        }
        ArrayList<TalkClientMessage> orderedMessages = new ArrayList<TalkClientMessage>();
        for (TalkClientContact c : allNearbyGroupsOrdered) {
            TalkClientMessage separator = new TalkClientMessage();
            separator.setConversationContact(c);
            separator.setMessageId(TalkClientMessage.TYPE_SEPARATOR);
            orderedMessages.add(separator);
            orderedMessages.addAll(findMessagesByContactId(c.getClientContactId(), nearbyMessages.size(), 0));
        }
        return orderedMessages;
    }

    public List<TalkClientMessage> getAllWorldwideGroupMessages() throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryBuilder()
                .orderBy("timestamp", true).where()
                .eq("deleted", false)
                .query();

        ArrayList<TalkClientMessage> worldwideMessages = new ArrayList<TalkClientMessage>();
        for (TalkClientMessage message : messages) {
            TalkClientContact conversationContact = message.getConversationContact();
            if (conversationContact != null && conversationContact.getContactType() != null) {
                if (conversationContact.isWorldwideGroup()) {
                    worldwideMessages.add(message);
                }
            }
        }
        ArrayList<TalkClientContact> allWorldwideGroupsOrdered = new ArrayList<TalkClientContact>();
        for (TalkClientMessage m : worldwideMessages) {
            if (!allWorldwideGroupsOrdered.contains(m.getConversationContact())) {
                allWorldwideGroupsOrdered.add(m.getConversationContact());
            }
        }
        ArrayList<TalkClientMessage> orderedMessages = new ArrayList<TalkClientMessage>();
        for (TalkClientContact c : allWorldwideGroupsOrdered) {
            TalkClientMessage separator = new TalkClientMessage();
            separator.setConversationContact(c);
            separator.setMessageId(TalkClientMessage.TYPE_SEPARATOR);
            orderedMessages.add(separator);
            orderedMessages.addAll(findMessagesByContactId(c.getClientContactId(), worldwideMessages.size(), 0));
        }
        return orderedMessages;
    }

    public List<TalkClientMessage> findMessagesByContactId(int contactId, long count, long offset) throws SQLException {

        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        if (count >= 0) {
            clientMessages.limit(count);
        }
        Where<TalkClientMessage, Integer> where = clientMessages.where()
                .eq("conversationContact_id", contactId)
                .eq("deleted", false)
                .and(2);
        clientMessages.setWhere(where);

        QueryBuilder<TalkDelivery, ?> deliveries = mDeliveries.queryBuilder().orderBy("timeAccepted", true);

        QueryBuilder<TalkClientMessage, ?> join = clientMessages.join(deliveries);
        if (offset >= 0) {
            join.offset(offset);
        }

        return join.query();
    }

    public Vector<Integer> findMessageIdsByContactId(int contactId) throws SQLException {
        GenericRawResults<Object[]> results = mClientMessages.queryRaw(
                "select clientMessageId from clientMessage where conversationContact_id = ?",
                new DataType[]{DataType.INTEGER}, Integer.toString(contactId));
        List<Object[]> rows = results.getResults();
        Vector<Integer> ret = new Vector<Integer>(rows.size());
        for (Object[] row : rows) {
            Integer r = (Integer) row[0];
            ret.add(r);
        }
        return ret;
    }

    public long getMessageCountByContactId(int contactId) throws SQLException {
        return mClientMessages.queryBuilder().where()
                .eq("conversationContact_id", contactId)
                .eq("deleted", false)
                .and(2)
                .countOf();
    }

    public TalkClientMessage findClientMessageByTalkClientDownloadId(int attachmentDownloadId) throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryForEq("attachmentDownload_id", attachmentDownloadId);
        int numberOfMessages = messages.size();

        if (numberOfMessages == 0) {
            return null;
        } else {
            return messages.get(0);
        }
    }

    public TalkClientMessage findClientMessageByTalkClientUploadId(int attachmentUploadId) throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryForEq("attachmentUpload_id", attachmentUploadId);
        int numberOfMessages = messages.size();

        if (numberOfMessages == 0) {
            return null;
        } else {
            return messages.get(0);
        }
    }

    public TalkClientMessage findClientMessageById(int clientMessageId) throws SQLException {
        return mClientMessages.queryForId(clientMessageId);
    }

    public long findUnseenMessageCountByContactId(int contactId) throws SQLException {
        return mClientMessages.queryBuilder().where()
                .eq("conversationContact_id", contactId)
                .eq("seen", false)
                .eq("deleted", false)
                .and(3)
                .countOf();
    }

    public TalkClientMessage findLatestMessageByContactId(int contactId) throws SQLException {
        return mClientMessages.queryBuilder()
                .orderBy("timestamp", false).where()
                .isNotNull("text")
                .eq("conversationContact_id", contactId)
                .eq("deleted", false)
                .and(3)
                .queryForFirst();
    }

    public TalkClientMessage findMessageByUploadId(int uploadId) throws SQLException {
        return mClientMessages.queryBuilder().where()
                .eq("attachmentUpload_id", uploadId)
                .queryForFirst();
    }

    public TalkClientMessage findMessageByDownloadId(int downloadId) throws SQLException {
        return mClientMessages.queryBuilder().where()
                .eq("attachmentDownload_id", downloadId)
                .queryForFirst();
    }

    public List<TalkClientMessage> findUnseenMessages() throws SQLException {
        return mClientMessages.queryBuilder().orderBy("timestamp", false).where()
                .eq("seen", false)
                .eq("deleted", false)
                .and(2)
                .query();
    }

    public TalkClientMessage getClientMessageForDelivery(TalkDelivery delivery) throws SQLException {
        return mClientMessages.queryBuilder().where()
                .eq("messageTag", delivery.getMessageTag())
                .eq("messageId", delivery.getMessageId())
                .or(2)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();
    }

    public TalkClientMessage getClientMessageForUpload(TalkClientUpload upload) throws SQLException {
        QueryBuilder<TalkClientUpload, Integer> clientUploads = mClientUploads.queryBuilder();
        clientUploads.where().eq("clientUploadId", upload.getClientUploadId());
        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        return clientMessages.join(clientUploads).where()
                .eq("deleted", false)
                .queryForFirst();
    }

    public TalkClientMessage getClientMessageForDownload(TalkClientDownload download) throws SQLException {
        QueryBuilder<TalkClientDownload, Integer> clientDownloads = mClientDownloads.queryBuilder();
        clientDownloads.where().eq("clientDownloadId", download.getClientDownloadId());
        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        return clientMessages.join(clientDownloads).where()
                .eq("deleted", false)
                .queryForFirst();
    }

    public List<TalkDelivery> getDeliveriesForMessage(TalkClientMessage message) throws SQLException {
        QueryBuilder<TalkDelivery, Long> deliveries = mDeliveries.queryBuilder();
        deliveries.where().eq(TalkDelivery.FIELD_MESSAGE_TAG, message.getMessageTag());
        return deliveries.query();
    }

    ////////////////////////////////////////////
    //////// Upload/Download Management ////////
    ////////////////////////////////////////////

    public void registerUploadListener(IXoUploadListener listener) {
        mUploadListeners.registerListener(listener);
    }

    public void unregisterUploadListener(IXoUploadListener listener) {
        mUploadListeners.unregisterListener(listener);
    }

    public void registerDownloadListener(IXoDownloadListener listener) {
        mDownloadListeners.registerListener(listener);
    }

    public void unregisterDownloadListener(IXoDownloadListener listener) {
        mDownloadListeners.unregisterListener(listener);
    }

    public List<TalkClientDownload> findAllClientDownloads() throws SQLException {
        return mClientDownloads.queryForAll();
    }

    public List<TalkClientDownload> findAllPendingDownloads() throws SQLException {
        QueryBuilder<TalkClientDownload, Integer> downloads = mClientDownloads.queryBuilder();
        downloads.where()
                .ne("state", TalkClientDownload.State.COMPLETE);
        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        clientMessages.where()
                .eq("deleted", false);
        return downloads.join(clientMessages).query();
    }

    public List<TalkClientUpload> findAllPendingUploads() throws SQLException {
        QueryBuilder<TalkClientUpload, Integer> uploads = mClientUploads.queryBuilder();
        uploads.where()
                .ne("state", TalkClientUpload.State.COMPLETE);
        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        clientMessages.where()
                .eq("deleted", false);
        return uploads.join(clientMessages).query();
    }

    public TalkClientUpload findClientUploadById(int clientUploadId) throws SQLException {
        return mClientUploads.queryForId(clientUploadId);
    }

    public TalkClientDownload findClientDownloadById(int clientDownloadId) throws SQLException {
        return mClientDownloads.queryForId(clientDownloadId);
    }

    public synchronized void saveDelivery(TalkDelivery delivery) throws SQLException {
        mDeliveries.createOrUpdate(delivery);
    }

    public synchronized void updateDelivery(TalkDelivery delivery) throws SQLException {
        int updatedRows = mDeliveries.update(delivery);
        if (updatedRows < 1) {
            throw new SQLException("cant find record for Delivery: " + delivery.getId());
        }
    }

    public synchronized void saveClientDownload(TalkClientDownload download) throws SQLException {
        Dao.CreateOrUpdateStatus result = mClientDownloads.createOrUpdate(download);

        if (result.isCreated()) {
            for (IXoDownloadListener listener : mDownloadListeners) {
                listener.onDownloadCreated(download);
            }
        } else {
            for (IXoDownloadListener listener : mDownloadListeners) {
                listener.onDownloadUpdated(download);
            }
        }
    }

    public synchronized void saveClientUpload(TalkClientUpload upload) throws SQLException {
        Dao.CreateOrUpdateStatus result = mClientUploads.createOrUpdate(upload);

        if (result.isCreated()) {
            for (IXoUploadListener listener : mUploadListeners) {
                listener.onUploadCreated(upload);
            }
        } else {
            for (IXoUploadListener listener : mUploadListeners) {
                listener.onUploadUpdated(upload);
            }
        }
    }

    public void refreshClientDownload(TalkClientDownload download) throws SQLException {
        mClientDownloads.refresh(download);
    }

    public void refreshClientUpload(TalkClientUpload upload) throws SQLException {
        mClientUploads.refresh(upload);
    }

    public List<? extends XoTransfer> findAllTransfers() throws SQLException {
        List<TalkClientUpload> uploads = mClientUploads.queryForAll();
        List<TalkClientDownload> downloads = mClientDownloads.queryForAll();

        return ListUtils.union(uploads, downloads);
    }

    public List<XoTransfer> findTransfersByMediaTypeDistinct(String mediaType) throws SQLException {
        List<TalkClientUpload> uploads = findClientUploadsByMediaType(mediaType);
        List<TalkClientDownload> downloads = findClientDownloadsByMediaType(mediaType);

        List<XoTransfer> transfers = mergeUploadsAndDownloadsByMessageTimestamp(uploads, downloads);
        return filterDuplicateFiles(transfers);
    }

    private static <T extends XoTransfer> List<T> filterDuplicateFiles(List<T> transfers) {
        List<T> filteredTransfers = new ArrayList<T>();
        HashSet<String> filePathes = new HashSet<String>();

        for (int i = transfers.size() - 1; i >= 0; i--) {
            T transfer = transfers.get(i);
            if (!filePathes.contains(transfer.getFilePath())) {
                filteredTransfers.add(0, transfer);
                filePathes.add(transfer.getFilePath());
            }
        }

        return filteredTransfers;
    }

    public List<TalkClientUpload> findClientUploadsByMediaType(String mediaType) throws SQLException {
        return mClientUploads.queryBuilder().where()
                .eq("mediaType", mediaType)
                .and()
                .eq("state", TalkClientUpload.State.COMPLETE)
                .query();
    }

    public List<TalkClientDownload> findClientDownloadsByMediaType(String mediaType) throws SQLException {
        return mClientDownloads.queryBuilder().where()
                .eq("mediaType", mediaType)
                .and()
                .eq("state", TalkClientDownload.State.COMPLETE)
                .query();
    }

    public List<? extends XoTransfer> findTransfersByFilePath(String filePath) throws SQLException {
        List<TalkClientUpload> uploads = findClientUploadsByFilePath(filePath);
        List<TalkClientDownload> downloads = findClientDownloadsByFilePath(filePath);

        return ListUtils.union(uploads, downloads);
    }

    public List<TalkClientUpload> findClientUploadsByFilePath(String filePath) throws SQLException {
        SelectArg filePathArg = new SelectArg();
        filePathArg.setValue("%" + filePath + "%");

        return mClientUploads.queryBuilder().where()
                .like("dataFile", filePathArg)
                .and()
                .eq("state", TalkClientDownload.State.COMPLETE)
                .query();
    }

    public List<TalkClientDownload> findClientDownloadsByFilePath(String filePath) throws SQLException {
        SelectArg filePathArg = new SelectArg();
        filePathArg.setValue("%" + filePath + "%");

        return mClientDownloads.queryBuilder().where()
                .like("dataFile", filePathArg)
                .and()
                .eq("state", TalkClientDownload.State.COMPLETE)
                .query();
    }

    public long getAttachmentCountByContactId(int contactId) throws SQLException {
        Where where = mClientMessages.queryBuilder().where();
        return where.and(where.and(where.eq("conversationContact_id", contactId), where.eq("deleted", false)), where.or(where.isNotNull("attachmentUpload_id"), where.isNotNull("attachmentDownload_id"))).countOf();
    }

    private List<XoTransfer> mergeUploadsAndDownloadsByMessageTimestamp(List<TalkClientUpload> uploads, List<TalkClientDownload> downloads) throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryBuilder()
                .orderBy("timestamp", false).where()
                .isNotNull("attachmentUpload_id")
                .isNotNull("attachmentDownload_id")
                .or(2)
                .query();

        List<XoTransfer> transfers = new ArrayList<XoTransfer>();

        for (TalkClientMessage message : messages) {
            TalkClientUpload upload = message.getAttachmentUpload();
            if (upload != null && uploads.contains(upload)) {
                transfers.add(upload);
            }

            TalkClientDownload download = message.getAttachmentDownload();
            if (download != null && download.getState() == TalkClientDownload.State.COMPLETE && downloads.contains(download)) {
                transfers.add(download);
            }
        }

        return transfers;
    }

    public List<TalkClientDownload> findClientDownloadsByMediaTypeAndContactId(String mediaType, int contactId) throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> messageQb = mClientMessages.queryBuilder();
        messageQb
                .orderBy("timestamp", false).where()
                .eq("senderContact_id", contactId)
                .or()
                .eq("conversationContact_id", contactId);

        QueryBuilder<TalkClientDownload, Integer> downloadQb = mClientDownloads.queryBuilder();
        downloadQb.where()
                .eq("mediaType", mediaType)
                .and()
                .eq("state", TalkClientDownload.State.COMPLETE);

        return downloadQb.join(messageQb).query();
    }

    public List<TalkClientDownload> findClientDownloadsByMediaTypeAndContactIdDistinct(String mediaType, int contactId) throws SQLException {
        List<TalkClientDownload> downloads = findClientDownloadsByMediaTypeAndContactId(mediaType, contactId);
        return filterDuplicateFiles(downloads);
    }

    public TalkPrivateKey findPrivateKeyByKeyId(String keyId) throws SQLException {
        return mPrivateKeys.queryBuilder().where()
                .eq("keyId", keyId)
                .queryForFirst();
    }

    public TalkClientUpload getClientUploadForDelivery(TalkDelivery delivery) throws SQLException {
        TalkClientMessage message = getClientMessageForDelivery(delivery);
        if (message != null) {
            return message.getAttachmentUpload();
        }
        return null;
    }

    public TalkClientDownload getClientDownloadForDelivery(TalkDelivery delivery) throws SQLException {
        TalkClientMessage message = getClientMessageForDelivery(delivery);
        if (message != null) {
            return message.getAttachmentDownload();
        }
        return null;
    }

    public TalkDelivery deliveryForUpload(TalkClientUpload upload) throws SQLException {
        TalkClientMessage message = getClientMessageForUpload(upload);
        if (message != null) {
            return message.getDelivery();
        }
        return null;
    }

    public TalkDelivery deliveryForDownload(TalkClientDownload download) throws SQLException {
        TalkClientMessage message = getClientMessageForDownload(download);
        if (message != null) {
            return message.getDelivery();
        }
        return null;
    }

    ///////////////////////////////////////
    //////// Invitation Management ////////
    ///////////////////////////////////////

    public List<TalkClientContact> findAllPendingFriendRequests() {
        try {
            List<TalkClientContact> contacts = new ArrayList<TalkClientContact>();
            List<TalkRelationship> relationshipsInvitedMe = mRelationships.queryBuilder().where()
                    .eq("state", TalkRelationship.STATE_INVITED_ME)
                    .query();
            for (TalkRelationship relationship : relationshipsInvitedMe) {
                TalkClientContact contact = findContactByClientId(relationship.getOtherClientId(), false);
                CollectionUtils.addIgnoreNull(contacts, contact);
            }

            List<TalkRelationship> relationshipsInvitedByMe = mRelationships.queryBuilder().where()
                    .eq("state", TalkRelationship.STATE_INVITED)
                    .query();
            for (TalkRelationship relationship : relationshipsInvitedByMe) {
                TalkClientContact contact = findContactByClientId(relationship.getOtherClientId(), false);
                CollectionUtils.addIgnoreNull(contacts, contact);
            }

            return contacts;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getTotalCountOfInvitations() throws SQLException {
        return (int) (getCountOfInvitedMeGroupContacts() + getCountOfInvitedMeClients());
    }

    public long getCountOfInvitedMeClients() throws SQLException {
        return mRelationships.queryBuilder().where()
                .eq("state", TalkRelationship.STATE_INVITED_ME)
                .countOf();
    }

    public int getCountOfInvitedMeGroupContacts() throws SQLException {
        return findGroupContactsByMembershipState(TalkGroupMembership.STATE_INVITED).size();
    }

    public boolean hasPendingFriendRequests() {
        try {
            List<TalkRelationship> invitedRelations = mRelationships.queryBuilder().where()
                    .eq("state", TalkRelationship.STATE_INVITED_ME)
                    .or()
                    .eq("state", TalkRelationship.STATE_INVITED)
                    .query();
            if (invitedRelations != null && !invitedRelations.isEmpty()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /////////////////////////////////////
    //////// Deletion Management ////////
    /////////////////////////////////////

    public void deleteMessageById(int messageId) throws SQLException {
        UpdateBuilder<TalkClientMessage, Integer> updateBuilder = mClientMessages.updateBuilder();
        updateBuilder.updateColumnValue("deleted", true).where()
                .eq("deleted", false)
                .eq("clientMessageId", messageId)
                .and(2);
        updateBuilder.update();
    }

    public void deleteAllMessagesFromContactId(int contactId) throws SQLException {
        UpdateBuilder<TalkClientMessage, Integer> updateBuilder = mClientMessages.updateBuilder();
        updateBuilder.updateColumnValue("deleted", true).where()
                .eq("deleted", false)
                .eq("conversationContact_id", contactId)
                .and(2);
        updateBuilder.update();
    }

    public void eraseAllClientContacts() throws SQLException {
        DeleteBuilder<TalkClientContact, Integer> deleteBuilder = mClientContacts.deleteBuilder();
        deleteBuilder.where().eq("contactType", TalkClientContact.TYPE_CLIENT);
        deleteBuilder.delete();
    }

    public void eraseAllGroupContacts() throws SQLException {
        DeleteBuilder<TalkClientContact, Integer> deleteBuilder = mClientContacts.deleteBuilder();
        deleteBuilder.where().eq("contactType", TalkClientContact.TYPE_GROUP);
        deleteBuilder.delete();
    }

    public void eraseAllRelationships() throws SQLException {
        DeleteBuilder<TalkRelationship, Long> deleteBuilder = mRelationships.deleteBuilder();
        deleteBuilder.delete();
    }

    public void eraseAllGroupMemberships() throws SQLException {
        DeleteBuilder<TalkGroupMembership, Long> deleteBuilder = mGroupMemberships.deleteBuilder();
        deleteBuilder.delete();
    }


    private void deleteClientUpload(TalkClientUpload upload) throws SQLException {
        int deletedRowsCount = mClientUploads.delete(upload);

        if (deletedRowsCount > 0) {
            // remove upload from all collections
            List<TalkClientMediaCollection> collections = findAllMediaCollectionsContainingItem(upload);
            for (TalkClientMediaCollection collection : collections) {
                collection.removeItem(upload);
            }

            for (IXoUploadListener listener : mUploadListeners) {
                listener.onUploadDeleted(upload);
            }
        }
    }

    public void deleteClientDownload(TalkClientDownload download) throws SQLException {
        int deletedRowsCount = mClientDownloads.delete(download);

        if (deletedRowsCount > 0) {
            // remove download from all collections
            List<TalkClientMediaCollection> collections = findAllMediaCollectionsContainingItem(download);
            for (TalkClientMediaCollection collection : collections) {
                collection.removeItem(download);
            }

            for (IXoDownloadListener listener : mDownloadListeners) {
                listener.onDownloadDeleted(download);
            }
        }
    }

    public void deleteTransferAndUpdateMessage(XoTransfer transfer, String messageTextPrefix) throws SQLException {
        switch (transfer.getDirection()) {
            case UPLOAD:
                deleteClientUploadAndUpdateMessage((TalkClientUpload) transfer, messageTextPrefix);
                break;
            case DOWNLOAD:
                deleteClientDownloadAndUpdateMessage((TalkClientDownload) transfer, messageTextPrefix);
                break;
        }
    }

    public void deleteClientUploadAndUpdateMessage(TalkClientUpload upload, String messageTextPrefix) throws SQLException {
        deleteClientUpload(upload);
        TalkClientMessage message = findMessageByUploadId(upload.getClientUploadId());

        if (message != null) {
            message.setAttachmentUpload(null);
            updateMessageTextForDeletedTransfer(message, messageTextPrefix);
            saveClientMessage(message);
        }
    }

    public void deleteClientDownloadAndUpdateMessage(TalkClientDownload download, String messageTextPrefix) throws SQLException {
        deleteClientDownload(download);
        TalkClientMessage message = findMessageByDownloadId(download.getClientDownloadId());

        if (message != null) {
            message.setAttachmentDownload(null);
            updateMessageTextForDeletedTransfer(message, messageTextPrefix);
            saveClientMessage(message);
        }
    }

    private static void updateMessageTextForDeletedTransfer(TalkClientMessage message, String textPrefix) throws SQLException {
        StringBuilder newText = new StringBuilder(textPrefix);
        String oldText = message.getText();

        if (oldText != null && !oldText.isEmpty()) {
            newText.append("\n\n").append(oldText);
        }

        message.setText(newText.toString());
    }

    ////////////////////////////////////////////
    //////// MediaCollection Management ////////
    ////////////////////////////////////////////

    @Override
    public TalkClientMediaCollection findMediaCollectionById(Integer id) throws SQLException {
        TalkClientMediaCollection collection = mMediaCollections.queryForId(id);
        return prepareMediaCollection(collection);
    }

    @Override
    public List<TalkClientMediaCollection> findMediaCollectionsByName(String name) throws SQLException {
        List<TalkClientMediaCollection> collections = mMediaCollections.queryBuilder().where()
                .eq("name", name)
                .query();

        for (int i = 0; i < collections.size(); i++) {
            TalkClientMediaCollection preparedCollection = prepareMediaCollection(collections.get(i));
            collections.set(i, preparedCollection);
        }
        return collections;
    }

    @Override
    public List<TalkClientMediaCollection> findAllMediaCollections() throws SQLException {
        List<TalkClientMediaCollection> collections = mMediaCollections.queryForAll();

        for (int i = 0; i < collections.size(); i++) {
            TalkClientMediaCollection preparedCollection = prepareMediaCollection(collections.get(i));
            collections.set(i, preparedCollection);
        }
        return collections;
    }

    @Override
    public List<TalkClientMediaCollection> findAllMediaCollectionsContainingItem(XoTransfer item) throws SQLException {
        String column = item.isUpload() ? "uploadItem" : "item";

        List<TalkClientMediaCollectionRelation> relations = mMediaCollectionRelations.queryBuilder().where()
                .eq(column, item.getUploadOrDownloadId())
                .query();

        List<TalkClientMediaCollection> collections = new ArrayList<TalkClientMediaCollection>();
        for (TalkClientMediaCollectionRelation relation : relations) {
            TalkClientMediaCollection preparedCollection = findMediaCollectionById(relation.getMediaCollectionId());
            collections.add(preparedCollection);
        }
        return collections;
    }

    // Creates a new MediaCollection instance with the given name.
    @Override
    public TalkClientMediaCollection createMediaCollection(String collectionName) throws SQLException {
        TalkClientMediaCollection collection = new TalkClientMediaCollection(collectionName);
        mMediaCollections.create(collection);
        collection = prepareMediaCollection(collection);
        for (IXoMediaCollectionListener listener : mMediaCollectionListeners) {
            listener.onMediaCollectionCreated(collection);
        }
        return collection;
    }

    // Deletes the given collection from database.
    // Note: The collection will be cleared. Do not use the collection instance after deletion.
    @Override
    public void deleteMediaCollectionById(int collectionId) throws SQLException {
        TalkClientMediaCollection collection = findMediaCollectionById(collectionId);
        deleteMediaCollection(collection);
    }

    // Deletes the given collection from database.
    // Note: The collection will be cleared. Do not use the collection instance after deletion.
    @Override
    public void deleteMediaCollection(TalkClientMediaCollection collection) throws SQLException {
        collection.clear();
        mMediaCollections.delete(collection);
        for (IXoMediaCollectionListener listener : mMediaCollectionListeners) {
            listener.onMediaCollectionDeleted(collection);
        }
    }

    @Override
    public void registerMediaCollectionListener(IXoMediaCollectionListener listener) {
        mMediaCollectionListeners.registerListener(listener);
    }

    @Override
    public void unregisterMediaCollectionListener(IXoMediaCollectionListener listener) {
        mMediaCollectionListeners.unregisterListener(listener);
    }

    // The returned Dao should not be used directly to alter the database, use TalkClientMediaCollection instead
    @Override
    public Dao<TalkClientMediaCollection, Integer> getMediaCollectionDao() {
        return mMediaCollections;
    }

    // The returned Dao should not be used directly to alter the database, use TalkClientMediaCollection instead
    @Override
    public Dao<TalkClientMediaCollectionRelation, Integer> getMediaCollectionRelationDao() {
        return mMediaCollectionRelations;
    }

    private TalkClientMediaCollection prepareMediaCollection(TalkClientMediaCollection collection) {
        collection.setDatabase(this);
        return collection;
    }

    public boolean isWorldwideDownload(int clientDownloadId) throws SQLException {
        QueryBuilder<TalkClientDownload, ?> dowloadQueryBuilder = mClientDownloads.queryBuilder();
        dowloadQueryBuilder.where()
                .eq("clientDownloadId", clientDownloadId);
        QueryBuilder<TalkClientMessage, ?> clientMessageQueryBuilder = mClientMessages.queryBuilder();

        TalkClientMessage clientMessage = clientMessageQueryBuilder.join(dowloadQueryBuilder).queryForFirst();
        TalkClientContact senderContact = clientMessage.getSenderContact();

        return senderContact.isWorldwide() && !senderContact.isNearby() && !senderContact.isClientFriend();
    }

    public TalkPresence getLatestPresence() throws SQLException {
        QueryBuilder<TalkPresence, ?> queryBuilder = mPresences.queryBuilder();
        queryBuilder.orderBy("timestamp", false);

        return queryBuilder.queryForFirst();
    }

    public TalkRelationship getLatestRelationship() throws SQLException {
        QueryBuilder<TalkRelationship, ?> queryBuilder = mRelationships.queryBuilder();
        queryBuilder.orderBy("lastChanged", false);

        return queryBuilder.queryForFirst();
    }

    public TalkGroupPresence getLatestGroupPresence() throws SQLException {
        QueryBuilder<TalkGroupPresence, ?> queryBuilder = mGroupPresences.queryBuilder();
        queryBuilder.orderBy("lastChanged", false);

        return queryBuilder.queryForFirst();
    }

    public TalkGroupMembership getLatestGroupMember() throws SQLException {
        QueryBuilder<TalkGroupMembership, ?> queryBuilder = mGroupMemberships.queryBuilder();
        queryBuilder.orderBy("lastChanged", false);

        return queryBuilder.queryForFirst();
    }
}
