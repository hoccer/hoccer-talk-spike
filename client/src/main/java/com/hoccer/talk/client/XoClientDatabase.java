package com.hoccer.talk.client;

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
import java.util.*;

public class XoClientDatabase implements IXoMediaCollectionDatabase {

    private static final Logger LOG = Logger.getLogger(XoClientDatabase.class);

    IXoClientDatabaseBackend mBackend;

    Dao<TalkClientContact, Integer> mClientContacts;
    Dao<TalkClientSelf, Integer> mClientSelfs;
    Dao<TalkPresence, String> mPresences;
    Dao<TalkRelationship, Long> mRelationships;
    Dao<TalkGroup, String> mGroups;
    Dao<TalkGroupMember, Long> mGroupMembers;

    Dao<TalkClientMembership, Integer> mClientMemberships;

    Dao<TalkClientMessage, Integer> mClientMessages;
    Dao<TalkMessage, String> mMessages;
    Dao<TalkDelivery, Long> mDeliveries;

    Dao<TalkKey, Long> mPublicKeys;
    Dao<TalkPrivateKey, Long> mPrivateKeys;

    Dao<TalkClientDownload, Integer> mClientDownloads;
    Dao<TalkClientUpload, Integer> mClientUploads;

    Dao<TalkClientSmsToken, Integer> mSmsTokens;

    Dao<TalkClientMediaCollection, Integer> mMediaCollections;
    Dao<TalkClientMediaCollectionRelation, Integer> mMediaCollectionRelations;

    private WeakListenerArray<IXoUploadListener> mUploadListeners = new WeakListenerArray<IXoUploadListener>();
    private WeakListenerArray<IXoDownloadListener> mDownloadListeners = new WeakListenerArray<IXoDownloadListener>();
    private WeakListenerArray<IXoMediaCollectionListener> mMediaCollectionListeners = new WeakListenerArray<IXoMediaCollectionListener>();


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

        TableUtils.createTable(cs, TalkClientSmsToken.class);

        TableUtils.createTable(cs, TalkClientMediaCollection.class);
        TableUtils.createTable(cs, TalkClientMediaCollectionRelation.class);
    }

    public XoClientDatabase(IXoClientDatabaseBackend backend) {
        mBackend = backend;
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

        mSmsTokens = mBackend.getDao(TalkClientSmsToken.class);

        mMediaCollections = mBackend.getDao(TalkClientMediaCollection.class);
        mMediaCollectionRelations = mBackend.getDao(TalkClientMediaCollectionRelation.class);
    }

    public void saveContact(TalkClientContact contact) throws SQLException {
        mClientContacts.createOrUpdate(contact);
    }

    public void saveCredentials(TalkClientSelf credentials) throws SQLException {
        mClientSelfs.createOrUpdate(credentials);
    }

    public void savePresence(TalkPresence presence) throws Exception {
        if (presence.getClientId() == null) {
            // TODO: create own exception!
            throw new Exception("null client id");
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
            throw new SQLException("cant find record for Delivery: " + delivery.getId().toString());
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

    public TalkClientContact findClientContactById(int clientContactId) throws SQLException {
        return mClientContacts.queryForId(clientContactId);
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
        List<TalkClientContact> result = new ArrayList<TalkClientContact>();
        Set<TalkClientContact> contacts = new HashSet<TalkClientContact>();
        List<TalkRelationship> relationships = mRelationships.queryBuilder()
                .where()
                .eq("state", state)
                .query();
        for (TalkRelationship relationship : relationships) {
            TalkClientContact contact = findContactByClientId(relationship.getOtherClientId(), false);
            if (contact != null) {
                contacts.add(contact);
            }
        }

        result.addAll(contacts);
        return result;
    }

    public List<TalkClientContact> findAllGroupContacts() throws SQLException {
        return mClientContacts.queryBuilder().where()
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .eq("deleted", false)
                .and(2)
                .query();
    }

    public List<TalkClientContact> findGroupContactsByState(String state) throws SQLException {

        List<TalkClientContact> result = new ArrayList<TalkClientContact>();

        List<TalkClientContact> groupContacts = mClientContacts.queryBuilder().where()
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .eq("deleted", false)
                .and(2)
                .query();
        for (TalkClientContact groupContact : groupContacts) {
            TalkGroupMember groupMember = groupContact.getGroupMember();
            if (groupMember != null && groupMember.getState().equals(state)) {
                    result.add(groupContact);
            }
        }

        return result;
    }

    public int findGroupMemberCountForGroup(TalkClientContact groupContact) throws SQLException {
        int count = 0;
        TalkClientContact contact = mClientContacts.queryBuilder()
                .where()
                .eq("clientContactId", groupContact.getClientContactId())
                .queryForFirst();
        if (contact != null && contact.isGroup() && contact.getGroupMemberships() != null) {
            for (TalkClientMembership memberShip : contact.getGroupMemberships()) {
                TalkGroupMember groupMember = memberShip.getMember();
                if (groupMember != null && groupMember.isNearby()) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<TalkClientContact> findClientsInGroup(TalkClientContact groupContact) throws SQLException {
        List<TalkClientContact> allGroupContacts = new ArrayList<TalkClientContact>();
        if (groupContact != null && groupContact.isGroup() && groupContact.getGroupMemberships() != null) {
            for (TalkClientMembership memberShip : groupContact.getGroupMemberships()) {
                TalkGroupMember groupMember = memberShip.getMember();
                if (groupMember != null && groupMember.isJoined()) {
                    allGroupContacts.add(memberShip.getClientContact());
                }
            }
        }
        return allGroupContacts;
    }

    public List<TalkClientSmsToken> findAllSmsTokens() throws SQLException {
        return mSmsTokens.queryForAll();
    }

    public TalkClientContact findSelfContact(boolean create) throws SQLException {
        TalkClientContact contact = null;

        contact = mClientContacts.queryBuilder()
                .where()
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
        TalkClientContact contact = null;

        contact = mClientContacts.queryBuilder()
                .where()
                .eq("clientId", clientId)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, clientId);
            mClientContacts.create(contact);
        }

        return contact;
    }

    public synchronized TalkClientContact findContactByGroupId(String groupId, boolean create) throws SQLException {
        TalkClientContact contact = null;

        contact = mClientContacts.queryBuilder()
                .where()
                .eq("groupId", groupId)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();

        if (create && contact == null) {
            contact = new TalkClientContact(TalkClientContact.TYPE_GROUP, groupId);
            mClientContacts.create(contact);
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
        TalkClientMessage message = null;

        message = mClientMessages.queryBuilder()
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
        TalkClientMessage message;

        message = mClientMessages.queryBuilder()
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

        List<TalkClientDownload> downloads = downloadQb.join(messageQb).query();

        return downloads;
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

        List<TalkClientDownload> downloads = downloadQb.join(messageQb).query();

        return downloads;
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
            TalkClientContact groupContact = findClientContactById(groupId);
            TalkClientContact clientContact = findClientContactById(clientId);
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

    public TalkClientSmsToken findSmsTokenById(int smsTokenId) throws SQLException {
        return mSmsTokens.queryForId(smsTokenId);
    }

    public void saveSmsToken(TalkClientSmsToken token) throws SQLException {
        mSmsTokens.createOrUpdate(token);
    }

    public void deleteSmsToken(TalkClientSmsToken token) throws SQLException {
        mSmsTokens.delete(token);
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

    public long getInvitedMeRequestsCount() throws SQLException {
        long invitedMeCount = mRelationships.queryBuilder()
                .where()
                .eq("state", TalkRelationship.STATE_INVITED_ME)
                .countOf();
        return invitedMeCount;
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
        TalkClientMessage messageForDelivery = mClientMessages.queryBuilder()
                .where()
                .eq("messageTag", delivery.getMessageTag())
                .eq("messageId", delivery.getMessageId())
                .or(2)
                .eq("deleted", false)
                .and(2)
                .queryForFirst();
        return messageForDelivery;
    }

    public TalkClientMessage getClientMessageForUpload(TalkClientUpload upload) throws SQLException {
        QueryBuilder<TalkClientUpload, Integer> clientUploads = mClientUploads.queryBuilder();
        clientUploads.where().eq("clientUploadId", upload.getClientUploadId());
        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        TalkClientMessage messageForUpload = clientMessages.join(clientUploads).where()
                .eq("deleted", false)
                .queryForFirst();
        return messageForUpload;
    }

    public TalkClientMessage getClientMessageForDownload(TalkClientDownload download) throws SQLException {
        QueryBuilder<TalkClientDownload, Integer> clientDownloads = mClientDownloads.queryBuilder();
        clientDownloads.where().eq("clientDownloadId", download.getClientDownloadId());
        QueryBuilder<TalkClientMessage, Integer> clientMessages = mClientMessages.queryBuilder();
        TalkClientMessage messageForUpload = clientMessages.join(clientDownloads).where()
                .eq("deleted", false)
                .queryForFirst();
        return messageForUpload;
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

    public void migrateAllFilecacheUris() throws SQLException {
        List<TalkClientMessage> messages = mClientMessages.queryBuilder().where()
                .isNotNull("attachmentUpload_id")
                .or()
                .isNotNull("attachmentDownload_id").query();
        for (TalkClientMessage message : messages) {
            TalkClientDownload download = message.getAttachmentDownload();
            TalkClientUpload upload = message.getAttachmentUpload();
            if (download != null) {
                download.setDownloadUrl(migrateFilecacheUrl(download.getDownloadUrl()));
                mClientDownloads.update(download);
            }
            if (upload != null) {
                upload.setUploadUrl(migrateFilecacheUrl(upload.getUploadUrl()));
                mClientUploads.update(upload);
            }
        }
    }

    private String migrateFilecacheUrl(String url) {
        if (url == null) {
            return null;
        }
        String migratedUrl = url.substring(url.indexOf("/", 8));
        migratedUrl = "https://filecache.talk.hoccer.de:8444" + migratedUrl;
        LOG.debug("migrated url: " + url + " to: " + migratedUrl);
        return migratedUrl;
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

    /* delivered -> deliveredPrivate
     * confirmed -> deliveredPrivateAcknowledged
     * aborted -> abortedAcknowledged
     * failed -> failedAcknowledged */
    public void migrateDeliveryStates() throws SQLException {
        List<TalkDelivery> talkDeliveries = mDeliveries.queryForAll();
        for (TalkDelivery delivery : talkDeliveries) {
            if (delivery.getState().equals(TalkDelivery.STATE_DELIVERED_OLD)) {
                delivery.setState(TalkDelivery.STATE_DELIVERED_PRIVATE);
            } else if (delivery.getState().equals(TalkDelivery.STATE_CONFIRMED_OLD)) {
                delivery.setState(TalkDelivery.STATE_DELIVERED_PRIVATE_ACKNOWLEDGED);
            } else if (delivery.getState().equals(TalkDelivery.STATE_ABORTED_OLD)) {
                delivery.setState(TalkDelivery.STATE_ABORTED_ACKNOWLEDGED);
            } else if (delivery.getState().equals(TalkDelivery.STATE_FAILED_OLD)) {
                delivery.setState(TalkDelivery.STATE_FAILED_ACKNOWLEDGED);
            }

            if (delivery.getMessageId() == null) {
                saveDelivery(delivery);
                continue;
            }
            TalkClientMessage message = findMessageByMessageId(delivery.getMessageId(), false);
            if (message != null) {
                TalkClientUpload upload = message.getAttachmentUpload();
                TalkClientDownload download;
                if (upload == null) {
                    download = message.getAttachmentDownload();
                    if (download != null) {
                        migrateTalkClientDownload(delivery, download);
                    } else { // there is no Attachment in this delivery
                        delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_NONE);
                    }
                } else {
                    migrateTalkClientUpload(delivery, upload);
                }
            }

            saveDelivery(delivery);
        }
    }

    private void migrateTalkClientUpload(TalkDelivery delivery, TalkClientUpload upload) {
        switch (upload.getState()) {
            case COMPLETE:
                delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED);
                break;
            case FAILED:
                delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED);
                break;
            default:
                delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_NEW);
                break;
        }
    }

    private void migrateTalkClientDownload(TalkDelivery delivery, TalkClientDownload download) {
        switch (download.getState()) {
            case COMPLETE:
                delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED);
                break;
            case FAILED:
                delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED);
                break;
            default:
                delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_NEW);
                break;
        }
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
        mMediaCollections.createIfNotExists(collection);
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
