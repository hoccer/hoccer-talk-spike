package com.hoccer.talk.client;

import com.hoccer.talk.client.exceptions.NoClientIdInPresenceException;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.model.*;
import com.hoccer.talk.util.WeakListenerArray;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class XoClientDatabase implements IXoMediaCollectionDatabase {

    private static final Logger LOG = Logger.getLogger(XoClientDatabase.class);

    private final IXoClientDatabaseBackend mBackend;

    private Dao<TalkClientContact, Integer> mClientContacts;
    private Dao<TalkClientSelf, Integer> mClientSelfs;
    private Dao<TalkPresence, String> mPresences;
    private Dao<TalkRelationship, Long> mRelationships;
    private Dao<TalkGroup, String> mGroups;
    private Dao<TalkGroupMember, Long> mGroupMembers;
    private Dao<TalkClientMembership, Integer> mClientMemberships;
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
        TableUtils.createTable(cs, TalkGroup.class);
        TableUtils.createTable(cs, TalkGroupMember.class);
        TableUtils.createTable(cs, TalkClientMembership.class);
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
        mGroups = mBackend.getDao(TalkGroup.class);
        mGroupMembers = mBackend.getDao(TalkGroupMember.class);
        mClientMemberships = mBackend.getDao(TalkClientMembership.class);
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

    public void saveContact(TalkClientContact contact) throws SQLException {
        mClientContacts.createOrUpdate(contact);
    }

    public void saveCredentials(TalkClientSelf credentials) throws SQLException {
        mClientSelfs.createOrUpdate(credentials);
    }

    public void savePresence(TalkPresence presence) throws NoClientIdInPresenceException, SQLException {
        if (presence.getClientId() == null) {
            throw new NoClientIdInPresenceException("Client id is null for " + presence);
        }
        mPresences.createOrUpdate(presence);
    }

    public void saveRelationship(TalkRelationship relationship) throws SQLException {
        mRelationships.createOrUpdate(relationship);
    }

    public void saveGroup(TalkGroup group) throws SQLException {
        mGroups.createOrUpdate(group);
    }

    public void saveGroupMember(TalkGroupMember member) throws SQLException {
        mGroupMembers.createOrUpdate(member);
    }

    public synchronized void saveClientMessage(TalkClientMessage message) throws SQLException {
        Dao.CreateOrUpdateStatus result = mClientMessages.createOrUpdate(message);
    }

    public void saveMessage(TalkMessage message) throws SQLException {
        mMessages.createOrUpdate(message);
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

    public void savePublicKey(TalkKey publicKey) throws SQLException {
        mPublicKeys.createOrUpdate(publicKey);
    }

    public void savePrivateKey(TalkPrivateKey privateKey) throws SQLException {
        mPrivateKeys.createOrUpdate(privateKey);
    }

    public void saveClientDownload(TalkClientDownload download) throws SQLException {
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

    public void saveClientUpload(TalkClientUpload upload) throws SQLException {
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

    public void refreshClientContact(TalkClientContact contact) throws SQLException {
        mClientContacts.refresh(contact);
    }

    public void refreshClientDownload(TalkClientDownload download) throws SQLException {
        mClientDownloads.refresh(download);
    }

    public void refreshClientUpload(TalkClientUpload upload) throws SQLException {
        mClientUploads.refresh(upload);
    }

    public List<TalkClientContact> findAllContacts() throws SQLException {
        return mClientContacts.queryBuilder().where()
                .eq("deleted", false)
                .query();
    }

    public List<TalkClientContact> findAllClientContactsOrderedByRecentMessage() throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> recentUnreadMessages = mClientMessages.queryBuilder();
        QueryBuilder<TalkClientContact, Integer> recentSenders = mClientContacts.queryBuilder();
        recentUnreadMessages.orderBy("timestamp", false);
        List<TalkClientContact> orderedListOfSenders = recentSenders.join(recentUnreadMessages).where()
                .eq("deleted", false)
                .query();
        List<TalkClientContact> allContacts = mClientContacts.queryBuilder().where()
                .eq("deleted", false)
                .query();
        ArrayList<TalkClientContact> orderedListOfDistinctSenders = new ArrayList<TalkClientContact>();
        for (int i = 0; i < orderedListOfSenders.size(); i++) {
            if (!orderedListOfDistinctSenders.contains(orderedListOfSenders.get(i))) {
                orderedListOfDistinctSenders.add(orderedListOfSenders.get(i));
            }
        }
        for (int i = 0; i < allContacts.size(); i++) {
            if (!orderedListOfDistinctSenders.contains(allContacts.get(i))) {
                orderedListOfDistinctSenders.add(allContacts.get(i));
            }
        }
        return orderedListOfDistinctSenders;
    }

    public List<TalkClientContact> findAllClientContacts() throws SQLException {
        return mClientContacts.queryBuilder().where()
                .eq("contactType", TalkClientContact.TYPE_CLIENT)
                .eq("deleted", false)
                .and(2)
                .query();
    }

    public List<TalkClientContact> findClientContactsByState(String state) throws SQLException {
        QueryBuilder<TalkRelationship, Long> relationships = mRelationships.queryBuilder();
        relationships.where()
                .eq("state", state);

        QueryBuilder<TalkClientContact, Integer> contacts = mClientContacts.queryBuilder();
        contacts.where()
                .eq("deleted", false);

        return contacts.join(relationships).query();
    }

    public List<TalkClientContact> findAllGroupContacts() throws SQLException {
        return mClientContacts.queryBuilder().where()
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .eq("deleted", false)
                .and(2)
                .query();
    }

    public List<TalkClientContact> findGroupContactsByState(String state) throws SQLException {
        QueryBuilder<TalkGroupMember, Long> groupMembers = mGroupMembers.queryBuilder();
        groupMembers.where()
                .eq("state", state);

        QueryBuilder<TalkClientContact, Integer> contacts = mClientContacts.queryBuilder();
        contacts.where()
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .isNotNull("groupPresence_id")
                .eq("deleted", false)
                .and(3);

        return contacts.join(groupMembers).query();
    }

    public List<TalkClientContact> findContactsInGroup(String groupId) throws SQLException {
        return findContactsInGroupWithState(groupId, TalkGroupMember.STATE_JOINED);
    }

    public List<TalkClientContact> findContactsInGroupWithState(String groupId, String state) throws SQLException {
        QueryBuilder<TalkGroupMember, Long> groupMemberQuery = mGroupMembers.queryBuilder();
        groupMemberQuery.where()
                .eq("groupId", groupId)
                .and()
                .eq("state", state);
        groupMemberQuery.selectColumns("clientId");

        List<TalkGroupMember> groupMembers = groupMemberQuery.query();
        List<TalkClientContact> contacts = new ArrayList<TalkClientContact>(groupMembers.size());
        for (TalkGroupMember member : groupMembers) {
            TalkClientContact contact = findContactByClientId(member.getClientId(), false);
            if (contact != null) {
                contacts.add(contact);
            }
        }

        return contacts;
    }

    public TalkClientContact findSelfContact(boolean create) throws SQLException {
        TalkClientContact contact = mClientContacts.queryBuilder()
                .where()
                .eq("contactType", TalkClientContact.TYPE_SELF)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_SELF);
            mClientContacts.create(contact);
            contact = findContactById(contact.getClientContactId());
        }

        return contact;
    }

    public TalkClientContact findContactById(int contactId) throws SQLException {
        return mClientContacts.queryForId(contactId);
    }

    public synchronized TalkClientContact findContactByClientId(String clientId, boolean create) throws SQLException {
        TalkClientContact contact = mClientContacts.queryBuilder()
                .where()
                .eq("clientId", clientId)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, clientId);
            mClientContacts.create(contact);
            contact = findContactById(contact.getClientContactId());
        }

        return contact;
    }

    public TalkClientContact findDeletedContactByClientId(String clientId) throws SQLException {
        return mClientContacts.queryBuilder()
                .where()
                .eq("clientId", clientId)
                .eq("deleted", true)
                .and(2)
                .queryForFirst();
    }

    public synchronized TalkClientContact findContactByGroupId(String groupId, boolean create) throws SQLException {
        TalkClientContact contact = mClientContacts.queryBuilder()
                .where()
                .eq("groupId", groupId)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_GROUP, groupId);
            mClientContacts.create(contact);
            contact = findContactById(contact.getClientContactId());
        }

        return contact;
    }

    public TalkClientContact findContactByGroupTag(String groupTag) throws SQLException {
        return mClientContacts.queryBuilder()
                .where()
                .eq("groupTag", groupTag)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();
    }

    public synchronized List<TalkClientMessage> findMessagesForDelivery() throws SQLException {
        List<TalkDelivery> newDeliveries = mDeliveries.queryForEq(TalkDelivery.FIELD_STATE, TalkDelivery.STATE_NEW);

        List<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
        try {
            for (TalkDelivery newDelivery : newDeliveries) {
                TalkClientMessage message = mClientMessages.queryBuilder()
                        .where()
                        .eq("deleted", false)
                        .eq("outgoingDelivery" + "_id", newDelivery)
                        .and(2)
                        .queryForFirst();

                if (message != null) {

                    if (!message.isInProgress()) {
                        messages.add(message);
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
        TalkClientMessage message = mClientMessages.queryBuilder()
                .where()
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
        TalkClientMessage message = mClientMessages.queryBuilder()
                .where()
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

    public long getNearbyMessageCount() throws SQLException {
        return getAllNearbyGroupMessages().size();
    }

    public List<TalkClientMessage> getAllNearbyGroupMessages() throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> builder = mClientMessages.queryBuilder();
        builder.where().eq("deleted", false);
        builder.orderBy("timestamp", true);
        List<TalkClientMessage> messages = builder.query();

        ArrayList<TalkClientMessage> nearbyMessages = new ArrayList<TalkClientMessage>();
        for (TalkClientMessage message : messages) {
            TalkClientContact conversationContact = message.getConversationContact();
            if (conversationContact != null && conversationContact.getContactType() != null) {
                if (conversationContact.isGroup()) {
                    TalkGroup groupPresence = conversationContact.getGroupPresence();
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
            separator.setText("Nearby: " + c.getNickname());
            separator.setMessageId(TalkClientMessage.TYPE_SEPARATOR);
            orderedMessages.add(separator);
            orderedMessages.addAll(findMessagesByContactId(c.getClientContactId(), nearbyMessages.size(), 0));
        }
        return orderedMessages;
    }

    public List<TalkClientMessage> findMessagesByContactId(int contactId, long count, long offset) throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> builder = mClientMessages.queryBuilder();
        if (count >= 0) {
            builder.limit(count);
        }
        builder.orderBy("timestamp", true);
        if (offset >= 0) {
            builder.offset(offset);
        }
        Where<TalkClientMessage, Integer> where = builder.where()
                .eq("conversationContact_id", contactId)
                .eq("deleted", false)
                .and(2);
        builder.setWhere(where);
        List<TalkClientMessage> messages = mClientMessages.query(builder.prepare());
        return messages;
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
        return mClientMessages.queryBuilder()
                .where()
                .eq("conversationContact_id", contactId)
                .eq("deleted", false)
                .and(2)
                .countOf();
    }

    public long getAttachmentCountByContactId(int contactId) throws SQLException {
        Where where = mClientMessages.queryBuilder().where();
        return where.and(where.and(where.eq("conversationContact_id", contactId), where.eq("deleted", false)), where.or(where.isNotNull("attachmentUpload_id"), where.isNotNull("attachmentDownload_id"))).countOf();
    }

    public TalkPrivateKey findPrivateKeyByKeyId(String keyId) throws SQLException {
        return mPrivateKeys.queryBuilder().where().eq("keyId", keyId).queryForFirst();
    }

    public TalkClientUpload findClientUploadById(int clientUploadId) throws SQLException {
        return mClientUploads.queryForId(clientUploadId);
    }

    public TalkClientDownload findClientDownloadById(int clientDownloadId) throws SQLException {
        return mClientDownloads.queryForId(clientDownloadId);
    }

    public List<XoTransfer> findTransfersByMediaType(String mediaType) throws SQLException {
        List<TalkClientUpload> uploads = mClientUploads.queryBuilder()
                .where()
                .eq("mediaType", mediaType)
                .isNotNull("contentUrl")
                .and(2)
                .query();

        List<TalkClientDownload> downloads = mClientDownloads.queryForEq("mediaType", mediaType);

        return mergeUploadsAndDownloadsByMessageTimestamp(uploads, downloads);
    }

    private List<XoTransfer> mergeUploadsAndDownloadsByMessageTimestamp(List<TalkClientUpload> uploads, List<TalkClientDownload> downloads) throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryBuilder()
                .orderBy("timestamp", false)
                .where()
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

    public List<TalkClientDownload> findClientDownloadsByContactId(int contactId) throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> messageQb = mClientMessages.queryBuilder();
        messageQb
                .orderBy("timestamp", false)
                .where()
                .eq("senderContact_id", contactId)
                .or()
                .eq("conversationContact_id", contactId);

        QueryBuilder<TalkClientDownload, Integer> downloadQb = mClientDownloads.queryBuilder();
        downloadQb.where()
                .eq("state", TalkClientDownload.State.COMPLETE);

        return downloadQb.join(messageQb).query();
    }

    public List<TalkClientDownload> findClientDownloadsByMediaTypeAndContactId(String mediaType, int contactId) throws SQLException {
        QueryBuilder<TalkClientMessage, Integer> messageQb = mClientMessages.queryBuilder();
        messageQb
                .orderBy("timestamp", false)
                .where()
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

    public List<XoTransfer> findAllTransfers() throws SQLException {
        List<TalkClientUpload> uploads = mClientUploads.queryBuilder()
                .where()
                .isNotNull("contentUrl")
                .query();

        List<TalkClientDownload> downloads = mClientDownloads.queryForAll();

        return mergeUploadsAndDownloadsByMessageTimestamp(uploads, downloads);
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
                .orderBy("timestamp", false)
                .where()
                .isNotNull("text")
                .eq("conversationContact_id", contactId)
                .eq("deleted", false)
                .and(3)
                .queryForFirst();
    }

    public TalkClientMessage findMessageByUploadId(int uploadId) throws SQLException {
        return mClientMessages.queryBuilder()
                .where()
                .eq("attachmentUpload_id", uploadId)
                .queryForFirst();
    }

    public TalkClientMessage findMessageByDownloadId(int downloadId) throws SQLException {
        return mClientMessages.queryBuilder()
                .where()
                .eq("attachmentDownload_id", downloadId)
                .queryForFirst();
    }

    public List<TalkClientMessage> findUnseenMessages() throws SQLException {
        return mClientMessages.queryBuilder().orderBy("timestamp", false).
                where()
                .eq("seen", false)
                .eq("deleted", false)
                .and(2)
                .query();
    }


    public TalkClientMembership findMembershipByContacts(int groupId, int clientId, boolean create) throws SQLException {
        TalkClientMembership res = mClientMemberships.queryBuilder().where()
                .eq("groupContact_id", groupId)
                .eq("clientContact_id", clientId)
                .and(2)
                .queryForFirst();

        if (create && res == null) {
            TalkClientContact groupContact = findContactById(groupId);
            TalkClientContact clientContact = findContactById(clientId);
            res = new TalkClientMembership();
            res.setGroupContact(groupContact);
            res.setClientContact(clientContact);
            mClientMemberships.create(res);
        }

        return res;
    }

    public void saveClientMembership(TalkClientMembership membership) throws SQLException {
        mClientMemberships.createOrUpdate(membership);
    }

    public List<TalkClientContact> findAllPendingFriendRequests() {
        try {
            List<TalkClientContact> contacts = new ArrayList<TalkClientContact>();
            List<TalkRelationship> relationshipsInvitedMe = mRelationships.queryBuilder()
                    .where()
                    .eq("state", TalkRelationship.STATE_INVITED_ME)
                    .query();
            for (TalkRelationship relationship : relationshipsInvitedMe) {
                TalkClientContact contact = findContactByClientId(relationship.getOtherClientId(), false);
                if (contact != null) {
                    contacts.add(contact);
                }
            }

            List<TalkRelationship> relationshipsInvitedByMe = mRelationships.queryBuilder()
                    .where()
                    .eq("state", TalkRelationship.STATE_INVITED)
                    .query();
            for (TalkRelationship relationship : relationshipsInvitedByMe) {
                TalkClientContact contact = findContactByClientId(relationship.getOtherClientId(), false);
                if (contact != null) {
                    contacts.add(contact);
                }
            }

            return contacts;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getTotalCountOfInvitations() throws SQLException {
        return (int) (getCountOfInvitedMeGroups() + getCountOfInvitedMeClients());
    }

    public long getCountOfInvitedMeClients() throws SQLException {
        return mRelationships.queryBuilder()
                .where()
                .eq("state", TalkRelationship.STATE_INVITED_ME)
                .countOf();
    }

    public int getCountOfInvitedMeGroups() throws SQLException {
        return findGroupContactsByState(TalkGroupMember.STATE_INVITED).size();
    }

    public boolean hasPendingFriendRequests() {
        try {
            List<TalkRelationship> invitedRelations = mRelationships.queryBuilder()
                    .where()
                    .eq("state", TalkRelationship.STATE_INVITED_ME)
                    .or()
                    .eq("state", TalkRelationship.STATE_INVITED)
                    .query();
            if (invitedRelations != null && invitedRelations.size() > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public TalkClientMessage getClientMessageForDelivery(TalkDelivery delivery) throws SQLException {
        return mClientMessages.queryBuilder()
                .where()
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
            return message.getOutgoingDelivery();
        }
        return null;
    }

    public TalkDelivery deliveryForDownload(TalkClientDownload download) throws SQLException {
        TalkClientMessage message = getClientMessageForDownload(download);
        if (message != null) {
            return message.getIncomingDelivery();
        }
        return null;
    }

    public void deleteAllClientContacts() throws SQLException {
        UpdateBuilder<TalkClientContact, Integer> updateBuilder = mClientContacts.updateBuilder();
        updateBuilder.updateColumnValue("deleted", true).where()
                .eq("deleted", false)
                .eq("contactType", TalkClientContact.TYPE_CLIENT)
                .and(2);
        updateBuilder.update();
    }

    public void deleteAllGroupContacts() throws SQLException {
        UpdateBuilder<TalkClientContact, Integer> updateBuilder = mClientContacts.updateBuilder();
        updateBuilder.updateColumnValue("deleted", true).where()
                .eq("deleted", false)
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .and(2);
        updateBuilder.update();
    }

    public void deleteMessageById(int messageId) throws SQLException {
        UpdateBuilder<TalkClientMessage, Integer> updateBuilder = mClientMessages.updateBuilder();
        updateBuilder.updateColumnValue("deleted", true).where()
                .eq("deleted", false)
                .eq("clientMessageId", messageId)
                .and(2);
        updateBuilder.update();

        TalkClientMessage message = mClientMessages.queryForId(messageId);
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
        deleteBuilder.where()
                .eq("deleted", false)
                .eq("contactType", TalkClientContact.TYPE_CLIENT)
                .and(2);
        deleteBuilder.delete();
    }

    public void eraseAllGroupContacts() throws SQLException {
        DeleteBuilder<TalkClientContact, Integer> deleteBuilder = mClientContacts.deleteBuilder();
        deleteBuilder.where()
                .eq("deleted", false)
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .and(2);
        deleteBuilder.delete();
    }

    public void eraseAllRelationships() throws SQLException {
        DeleteBuilder<TalkRelationship, Long> deleteBuilder = mRelationships.deleteBuilder();
        deleteBuilder.delete();
    }

    public void eraseAllGroupMemberships() throws SQLException {
        DeleteBuilder<TalkGroupMember, Long> deleteBuilder = mGroupMembers.deleteBuilder();
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

    public void deleteTransferAndMessage(XoTransfer transfer) throws SQLException {
        switch (transfer.getDirection()) {
            case UPLOAD:
                deleteClientUploadAndMessage((TalkClientUpload) transfer);
                break;
            case DOWNLOAD:
                deleteClientDownloadAndMessage((TalkClientDownload) transfer);
                break;
        }
    }

    public void deleteClientUploadAndMessage(TalkClientUpload upload) throws SQLException {
        deleteClientUpload(upload);
        int messageId = findMessageByUploadId(upload.getClientUploadId()).getClientMessageId();
        deleteMessageById(messageId);
    }

    public void deleteClientDownloadAndMessage(TalkClientDownload download) throws SQLException {
        deleteClientDownload(download);
        int messageId = findMessageByDownloadId(download.getClientDownloadId()).getClientMessageId();
        deleteMessageById(messageId);
    }

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

    //////// MediaCollection Management ////////

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

        List<TalkClientMediaCollectionRelation> relations = mMediaCollectionRelations.queryBuilder()
                .where()
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

    // Returns an already cached collection with the same id or the prepared collection
    private TalkClientMediaCollection prepareMediaCollection(TalkClientMediaCollection collection) {
        collection.setDatabase(this);
        return collection;
    }
}
