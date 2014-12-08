package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkGroupPresence;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class XoClientDatabaseTest {

    private XoClientDatabase mDatabase;
    private JdbcConnectionSource mConnectionSource;

    private TalkClientContact mGroup;
    private TalkClientContact mGroupAdmin;

    private int clientIdCounter;

    private static final int INVITED_CONTACTS = 4;
    private static final int JOINED_CONTACTS = 3;

    @Before
    public void testSetup() throws Exception {
        mConnectionSource = new JdbcConnectionSource("jdbc:h2:mem:account");
        mDatabase = new XoClientDatabase(new IXoClientDatabaseBackend() {

            @Override
            public ConnectionSource getConnectionSource() {
                return mConnectionSource;
            }

            @Override
            public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
                D dao = DaoManager.createDao(mConnectionSource, clazz);
                dao.setObjectCache(true);
                return dao;
            }
        });

        XoClientDatabase.createTables(mConnectionSource);
        mDatabase.initialize();

        mGroupAdmin = createClientContact("adminClientId");
        mGroup = createGroupContact(mGroupAdmin, "TestGroupId");

        // add invited clients
        for (int i = 0; i < INVITED_CONTACTS; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), mGroup.getGroupId(), TalkGroupMembership.STATE_INVITED);
        }

        // add joined clients
        for (int i = 0; i < JOINED_CONTACTS; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), mGroup.getGroupId(), TalkGroupMembership.STATE_JOINED);
        }
    }

    @After
    public void testCleanup() throws SQLException {
        mConnectionSource.close();
    }

    ///////////////////////
    //////// Tests ////////
    ///////////////////////

    @Test
    public void testFindMembershipsInGroup() throws SQLException {
        int expectedMembershipsInGroup = INVITED_CONTACTS + JOINED_CONTACTS + 1;
        List<TalkGroupMembership> membershipsInGroup = mDatabase.findMembershipsInGroup(mGroup.getGroupId());
        assertEquals(expectedMembershipsInGroup, membershipsInGroup.size());
    }

    @Test
    public void testFindMembershipsInGroupByClientId() throws SQLException {
        TalkGroupMembership membership = mDatabase.findMembershipInGroupByClientId(mGroup.getGroupId(), mGroupAdmin.getClientId());
        assertEquals(mGroupAdmin.getClientId(), membership.getClientId());
    }

    @Test
    public void testFindMembershipsInGroupByState() throws SQLException {
        // check invited contacts count
        List<TalkGroupMembership> invitedMembershipsInGroup = mDatabase.findMembershipsInGroupByState(mGroup.getGroupId(), TalkGroupMembership.STATE_INVITED);
        assertEquals(INVITED_CONTACTS, invitedMembershipsInGroup.size());

        // check joined contacts count
        int expectedJoinedContactsCount = JOINED_CONTACTS + 1;
        List<TalkGroupMembership> joinedMembershipsInGroup = mDatabase.findMembershipsInGroupByState(mGroup.getGroupId(), TalkGroupMembership.STATE_JOINED);
        assertEquals(expectedJoinedContactsCount, joinedMembershipsInGroup.size());
    }

    @Test
    public void testFindContactsInGroupByState() throws SQLException {
        // check invited contacts count
        List<TalkClientContact> invitedContactsInGroup = mDatabase.findContactsInGroupByState(mGroup.getGroupId(), TalkGroupMembership.STATE_INVITED);
        assertEquals(INVITED_CONTACTS, invitedContactsInGroup.size());

        // check joined contacts count
        int expectedJoinedContactsCount = JOINED_CONTACTS + 1;
        List<TalkClientContact> joinedContactsInGroup = mDatabase.findContactsInGroupByState(mGroup.getGroupId(), TalkGroupMembership.STATE_JOINED);
        assertEquals(expectedJoinedContactsCount, joinedContactsInGroup.size());
    }

    @Test
    public void testFindContactsInGroup() throws SQLException {
        // check joined contacts count
        int expectedContactsInGroup = INVITED_CONTACTS + JOINED_CONTACTS + 1;
        List<TalkClientContact> contactsInGroup = mDatabase.findContactsInGroup(mGroup.getGroupId());
        assertEquals(expectedContactsInGroup, contactsInGroup.size());
    }

    @Test
    public void testFindContactsInGroupByRole() throws SQLException {
        // check admin contacts count
        List<TalkClientContact> adminContacts = mDatabase.findContactsInGroupByRole(mGroup.getGroupId(), TalkGroupMembership.ROLE_ADMIN);
        assertEquals(1, adminContacts.size());
        assertEquals(mGroupAdmin.getClientId(), adminContacts.get(0).getClientId());

        // check member contacts count
        List<TalkClientContact> memberContacts = mDatabase.findContactsInGroupByRole(mGroup.getGroupId(), TalkGroupMembership.ROLE_MEMBER);
        assertEquals(JOINED_CONTACTS + INVITED_CONTACTS, memberContacts.size());
    }

    @Test
    public void testFindAdminInGroup() throws SQLException {
        // check admin in group
        TalkClientContact groupAdmin = mDatabase.findAdminInGroup(mGroup.getGroupId());
        assertEquals(mGroupAdmin.getClientId(), groupAdmin.getClientId());
    }

    @Test
    public void testCheckForCachedReference() throws SQLException {
        // check if the contact reference is cached
        TalkClientContact contact = mDatabase.findContactByClientId("testId", true);
        TalkClientContact cachedContact = mDatabase.findContactByClientId("testId", false);
        assertTrue(contact == cachedContact);
    }

    ////////////////////////////////
    //////// Helper Methods ////////
    ////////////////////////////////

    private TalkClientContact createClientContact(String clientId) throws SQLException {
        TalkClientContact contact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, clientId);
        mDatabase.saveContact(contact);
        return contact;
    }

    private TalkClientContact createGroupContact(TalkClientContact admin, String groupId) throws SQLException {
        TalkClientContact group = TalkClientContact.createGroupContact();
        group.setCreatedTimeStamp(new Date());

        TalkGroupPresence groupPresence = new TalkGroupPresence();
        groupPresence.setGroupTag(group.getGroupTag());
        groupPresence.setGroupId(groupId);
        groupPresence.setGroupName("TestGroup");
        groupPresence.setState(TalkGroupPresence.STATE_EXISTS);
        group.updateGroupPresence(groupPresence);

        TalkGroupMembership membership = new TalkGroupMembership();
        membership.setClientId(admin.getClientId());
        membership.setRole(TalkGroupMembership.ROLE_ADMIN);
        membership.setState(TalkGroupMembership.STATE_JOINED);
        membership.setMemberKeyId("TestKey");
        membership.setGroupId(groupId);
        group.updateGroupMembership(membership);

        mDatabase.saveGroupMembership(membership);
        mDatabase.saveGroupPresence(groupPresence);
        mDatabase.saveContact(group);

        return group;
    }

    private void addClientContactToGroup(String clientId, String groupId, String state) throws SQLException {
        TalkGroupMembership membership = new TalkGroupMembership();
        membership.setClientId(clientId);
        membership.setRole(TalkGroupMembership.ROLE_MEMBER);
        membership.setState(state);
        membership.setGroupId(groupId);
        mDatabase.saveGroupMembership(membership);
    }
}
