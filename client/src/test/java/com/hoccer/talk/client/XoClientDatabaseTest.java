package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class XoClientDatabaseTest {

    private static final Logger LOG = Logger.getLogger(XoClientDatabaseTest.class);

    private XoClientDatabase mDatabase;

    private JdbcConnectionSource mConnectionSource;

    private int clientIdCounter;

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
    }

    @After
    public void testCleanup() throws SQLException {
        mConnectionSource.close();
    }

    @Test
    public void test_findMembersInGroup() throws SQLException {
        LOG.info("test_findMembersInGroup");

        TalkClientContact groupAdmin = createClientContact("adminClientId");
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // add invited clients
        int invitedContactsCount = 4;
        for (int i = 0; i < invitedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        int joinedContactsCount = 2;
        for (int i = 0; i < joinedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_JOINED);
        }

        // check invited contacts count
        int expectedMembersInGroup = invitedContactsCount + joinedContactsCount + 1;
        List<TalkGroupMember> membersInGroup = mDatabase.findMembersInGroup(group.getGroupId());
        assertEquals(expectedMembersInGroup, membersInGroup.size());
    }


    @Test
    public void test_findMembersInGroupWithClientId() throws SQLException {
        LOG.info("test_findMemberInGroupWithClientId");

        String clientId = "adminClientId";
        TalkClientContact groupAdmin = createClientContact(clientId);
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // check invited contacts count
        TalkGroupMember member = mDatabase.findMemberInGroupWithClientId(group.getGroupId(), clientId);
        assertEquals(clientId, member.getClientId());
    }

    @Test
    public void test_findMembersInGroupWithState() throws SQLException {
        LOG.info("test_findMembersInGroupWithState");

        TalkClientContact groupAdmin = createClientContact("adminClientId");
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // add invited clients
        int invitedContactsCount = 4;
        for (int i = 0; i < invitedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        int joinedContactsCount = 2;
        for (int i = 0; i < joinedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_JOINED);
        }

        // check invited contacts count
        List<TalkGroupMember> invitedMembersInGroup = mDatabase.findMembersInGroupWithState(group.getGroupId(), TalkGroupMember.STATE_INVITED);
        assertEquals(invitedContactsCount, invitedMembersInGroup.size());

        // check joined contacts count
        int expectedJoinedContactsCount = joinedContactsCount + 1;
        List<TalkGroupMember> joinedMembersInGroup = mDatabase.findMembersInGroupWithState(group.getGroupId(), TalkGroupMember.STATE_JOINED);
        assertEquals(expectedJoinedContactsCount, joinedMembersInGroup.size());
    }

    @Test
    public void test_findContactsInGroupWitState() throws SQLException {
        LOG.info("test_findContactsInGroupWitState");

        TalkClientContact groupAdmin = createClientContact("adminClientId");
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // add invited clients
        int invitedContactsCount = 4;
        for (int i = 0; i < invitedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        int joinedContactsCount = 2;
        for (int i = 0; i < joinedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_JOINED);
        }

        // check invited contacts count
        List<TalkClientContact> invitedContactsInGroup = mDatabase.findContactsInGroupWithState(group.getGroupId(), TalkGroupMember.STATE_INVITED);
        assertEquals(invitedContactsCount, invitedContactsInGroup.size());

        // check joined contacts count
        int expectedJoinedContactsCount = joinedContactsCount + 1;
        List<TalkClientContact> joinedContactsInGroup = mDatabase.findContactsInGroupWithState(group.getGroupId(), TalkGroupMember.STATE_JOINED);
        assertEquals(expectedJoinedContactsCount, joinedContactsInGroup.size());
    }

    @Test
    public void test_findContactsInGroup() throws SQLException {
        LOG.info("test_findContactsInGroup");

        TalkClientContact groupAdmin = createClientContact("adminClientId");
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // add invited clients
        int invitedContactsCount = 4;
        for (int i = 0; i < invitedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        int joinedContactsCount = 2;
        for (int i = 0; i < joinedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_JOINED);
        }

        // check joined contacts count
        int expectedContactsInGroup = invitedContactsCount + joinedContactsCount + 1;
        List<TalkClientContact> contactsInGroup = mDatabase.findContactsInGroup(group.getGroupId());
        assertEquals(expectedContactsInGroup, contactsInGroup.size());
    }

    @Test
    public void test_findContactsInGroupWitRole() throws SQLException {
        LOG.info("test_findContactsInGroupWitRole");

        TalkClientContact groupAdmin = createClientContact("adminClientId");
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // add invited clients
        int invitedContactsCount = 2;
        for (int i = 0; i < invitedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        int joinedContactsCount = 3;
        for (int i = 0; i < joinedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_JOINED);
        }

        // check admin contacts count
        List<TalkClientContact> adminContacts = mDatabase.findContactsInGroupWithRole(group.getGroupId(), TalkGroupMember.ROLE_ADMIN);
        assertEquals(1, adminContacts.size());
        assertEquals(groupAdmin.getClientId(), adminContacts.get(0).getClientId());

        // check member contacts count
        List<TalkClientContact> memberContacts = mDatabase.findContactsInGroupWithRole(group.getGroupId(), TalkGroupMember.ROLE_MEMBER);
        assertEquals(joinedContactsCount + invitedContactsCount, memberContacts.size());
    }

    @Test
    public void test_findAdminInGroup() throws SQLException {
        LOG.info("test_findAdminInGroup");

        TalkClientContact groupAdmin = createClientContact("adminClientId");
        TalkClientContact group = createGroupContact(groupAdmin, "TestGroupId");

        // add invited clients
        int invitedContactsCount = 4;
        for (int i = 0; i < invitedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        int joinedContactsCount = 2;
        for (int i = 0; i < joinedContactsCount; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), group.getGroupId(), TalkGroupMember.STATE_JOINED);
        }

        // check admin in group
        TalkClientContact returnedGroupAdmin = mDatabase.findAdminInGroup(group.getGroupId());
        assertEquals(groupAdmin.getClientId(), returnedGroupAdmin.getClientId());
    }

    //////// Helper Methods ////////

    private TalkClientContact createClientContact(String clientId) throws SQLException {
        TalkClientContact contact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, clientId);
        mDatabase.saveContact(contact);
        return contact;
    }

    private TalkClientContact createGroupContact(TalkClientContact admin, String groupId) throws SQLException {
        TalkClientContact group = TalkClientContact.createGroupContact();
        group.setCreatedTimeStamp(new Date());

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(group.getGroupTag());
        groupPresence.setGroupId(groupId);
        groupPresence.setGroupName("TestGroup");
        groupPresence.setState(TalkGroup.STATE_EXISTS);
        group.updateGroupPresence(groupPresence);

        TalkGroupMember member = new TalkGroupMember();
        member.setClientId(admin.getClientId());
        member.setRole(TalkGroupMember.ROLE_ADMIN);
        member.setState(TalkGroupMember.STATE_JOINED);
        member.setMemberKeyId("TestKey");
        member.setGroupId(groupId);
        group.updateGroupMember(member);

        mDatabase.saveGroupMember(member);
        mDatabase.saveGroup(groupPresence);
        mDatabase.saveContact(group);

        return group;
    }

    private void addClientContactToGroup(String clientId, String groupId, String state) throws SQLException {
        TalkGroupMember member = new TalkGroupMember();
        member.setClientId(clientId);
        member.setRole(TalkGroupMember.ROLE_MEMBER);
        member.setState(state);
        member.setGroupId(groupId);
        mDatabase.saveGroupMember(member);
    }
}
