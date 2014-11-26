package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
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

public class XoClientDatabaseTest {

    private XoClientDatabase mDatabase;

    private JdbcConnectionSource mConnectionSource;

    private int clientIdCounter;

    private TalkClientContact mGroup;

    private TalkClientContact mGroupAdmin;

    private static final int mInvitedContacts = 4;

    private static final int mJoinedContacts = 3;

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
        for (int i = 0; i < mInvitedContacts; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), mGroup.getGroupId(), TalkGroupMember.STATE_INVITED);
        }

        // add joined clients
        for (int i = 0; i < mJoinedContacts; i++) {
            TalkClientContact contact = createClientContact(String.valueOf(clientIdCounter++));
            addClientContactToGroup(contact.getClientId(), mGroup.getGroupId(), TalkGroupMember.STATE_JOINED);
        }
    }

    @After
    public void testCleanup() throws SQLException {
        mConnectionSource.close();
    }

    @Test
    public void testFindMembersInGroup() throws SQLException {
        int expectedMembersInGroup = mInvitedContacts + mJoinedContacts + 1;
        List<TalkGroupMember> membersInGroup = mDatabase.findMembersInGroup(mGroup.getGroupId());
        assertEquals(expectedMembersInGroup, membersInGroup.size());
    }

    @Test
    public void testFindMembersInGroupWithClientId() throws SQLException {
        TalkGroupMember member = mDatabase.findMemberInGroupByClientId(mGroup.getGroupId(), mGroupAdmin.getClientId());
        assertEquals(mGroupAdmin.getClientId(), member.getClientId());
    }

    @Test
    public void testFindMembersInGroupWithState() throws SQLException {
        // check invited contacts count
        List<TalkGroupMember> invitedMembersInGroup = mDatabase.findMembersInGroupByState(mGroup.getGroupId(), TalkGroupMember.STATE_INVITED);
        assertEquals(mInvitedContacts, invitedMembersInGroup.size());

        // check joined contacts count
        int expectedJoinedContactsCount = mJoinedContacts + 1;
        List<TalkGroupMember> joinedMembersInGroup = mDatabase.findMembersInGroupByState(mGroup.getGroupId(), TalkGroupMember.STATE_JOINED);
        assertEquals(expectedJoinedContactsCount, joinedMembersInGroup.size());
    }

    @Test
    public void testFindContactsInGroupWitState() throws SQLException {
        // check invited contacts count
        List<TalkClientContact> invitedContactsInGroup = mDatabase.findContactsInGroupByState(mGroup.getGroupId(), TalkGroupMember.STATE_INVITED);
        assertEquals(mInvitedContacts, invitedContactsInGroup.size());

        // check joined contacts count
        int expectedJoinedContactsCount = mJoinedContacts + 1;
        List<TalkClientContact> joinedContactsInGroup = mDatabase.findContactsInGroupByState(mGroup.getGroupId(), TalkGroupMember.STATE_JOINED);
        assertEquals(expectedJoinedContactsCount, joinedContactsInGroup.size());
    }

    @Test
    public void testFindContactsInGroup() throws SQLException {
        // check joined contacts count
        int expectedContactsInGroup = mInvitedContacts + mJoinedContacts + 1;
        List<TalkClientContact> contactsInGroup = mDatabase.findContactsInGroup(mGroup.getGroupId());
        assertEquals(expectedContactsInGroup, contactsInGroup.size());
    }

    @Test
    public void testFindContactsInGroupWitRole() throws SQLException {
        // check admin contacts count
        List<TalkClientContact> adminContacts = mDatabase.findContactsInGroupByRole(mGroup.getGroupId(), TalkGroupMember.ROLE_ADMIN);
        assertEquals(1, adminContacts.size());
        assertEquals(mGroupAdmin.getClientId(), adminContacts.get(0).getClientId());

        // check member contacts count
        List<TalkClientContact> memberContacts = mDatabase.findContactsInGroupByRole(mGroup.getGroupId(), TalkGroupMember.ROLE_MEMBER);
        assertEquals(mJoinedContacts + mInvitedContacts, memberContacts.size());
    }

    @Test
    public void testFindAdminInGroup() throws SQLException {
        // check admin in group
        TalkClientContact groupAdmin = mDatabase.findAdminInGroup(mGroup.getGroupId());
        assertEquals(mGroupAdmin.getClientId(), groupAdmin.getClientId());
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
