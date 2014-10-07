package com.hoccer.xo.android.activity;

import android.app.ActionBar.Tab;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.Suppress;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.*;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.fragment.GroupListFragment;
import com.hoccer.xo.android.adapter.GroupsAdapter;

import java.sql.SQLException;

public class ContactsActivityGroupFunctionalTest extends ActivityInstrumentationTestCase2<ContactsActivity> {

    public static final String GROUP_NAME_PREFIX = "group_name_";

    private ContactsActivity activity;
    private GroupListFragment groupListFragment;
    private GroupsAdapter groupsAdapter;

    public ContactsActivityGroupFunctionalTest() {
        super(ContactsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();

        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        groupListFragment = (GroupListFragment) adapter.getItem(1);
        groupsAdapter = (GroupsAdapter) groupListFragment.getListAdapter();

        XoApplication.getXoClient().getDatabase().eraseAllGroupContacts();
        XoApplication.getXoClient().getDatabase().eraseAllGroupMemberships();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        XoApplication.getXoClient().getDatabase().eraseAllGroupContacts();
        XoApplication.getXoClient().getDatabase().eraseAllGroupMemberships();
    }

    public void testPreconditions() {
        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        assertNotNull(viewPager);

        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        assertNotNull(adapter);

        assertNotNull(adapter.getItem(1));
    }

    public void testActivityTitle() {
        assertEquals("Contacts", getActivity().getTitle());
    }

    public void testActionBarMenuItems() {
        assertTrue(activity.getActionBar().isShowing());

        View pairMenuItem = activity.findViewById(com.hoccer.xo.release.R.id.menu_pair);
        assertNotNull(pairMenuItem);

        View newGroupMenuItem = activity.findViewById(com.hoccer.xo.release.R.id.menu_new_group);
        assertNotNull(newGroupMenuItem);
    }

    @Suppress
    public void testViewPagerUI() {
        assertEquals(0, activity.getActionBar().getSelectedTab().getPosition());
        assertEquals(2, activity.getActionBar().getTabCount());

        Tab tabFirst = activity.getActionBar().getTabAt(0);
        assertEquals("FRIENDS", tabFirst.getText());

        Tab tabSecond = activity.getActionBar().getTabAt(1);
        assertEquals("GROUPS", tabSecond.getText());
    }

    boolean ready = false;

    public void testInvitedMeRelationUI() throws InterruptedException {

        mockGroupRelationship(TalkGroupMember.STATE_INVITED, "1");

        assertEquals(1, groupsAdapter.getCount());

        TalkClientContact group = (TalkClientContact) groupsAdapter.getItem(0);

        assertTrue(group.getGroupMember().isInvited());

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 1000) {
                    if (groupListFragment.getListView().getCount() > 0) {
                        ready = true;
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                }
            }
        }).start();

        Thread.sleep(1100);
        assertTrue(ready);
        assertEquals(1, groupListFragment.getListView().getCount());

        getInstrumentation().waitForIdleSync();

        View itemView = groupListFragment.getListView().getChildAt(0);
        assertNotNull(itemView);

        TextView contactNameTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.contact_name);
        assertEquals(GROUP_NAME_PREFIX + 1, contactNameTextView.getText());

        LinearLayout invitedMeLayout = (LinearLayout) itemView.findViewById(com.hoccer.xo.release.R.id.ll_invited_me);
        assertEquals(ViewGroup.VISIBLE, invitedMeLayout.getVisibility());

        Button declineButton = (Button) itemView.findViewById(com.hoccer.xo.release.R.id.btn_decline);
        assertEquals(View.VISIBLE, declineButton.getVisibility());

        Button acceptButton = (Button) itemView.findViewById(com.hoccer.xo.release.R.id.btn_accept);
        assertEquals(View.VISIBLE, acceptButton.getVisibility());

        TextView isJoinedTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.is_joined);
        assertEquals(View.GONE, isJoinedTextView.getVisibility());
    }

    public void testIsJoinedRelationUI() throws InterruptedException {

        mockGroupRelationship(TalkGroupMember.STATE_JOINED, "1");

        assertEquals(1, groupsAdapter.getCount());

        TalkClientContact group = (TalkClientContact) groupsAdapter.getItem(0);

        assertTrue(group.getGroupMember().isJoined());

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 1000) {
                    if (groupListFragment.getListView().getCount() > 0) {
                        ready = true;
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                }
            }
        }).start();

        Thread.sleep(1100);
        assertTrue(ready);
        assertEquals(1, groupListFragment.getListView().getCount());

        getInstrumentation().waitForIdleSync();

        View itemView = groupListFragment.getListView().getChildAt(0);
        assertNotNull(itemView);

        TextView contactNameTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.contact_name);
        assertEquals(GROUP_NAME_PREFIX + 1, contactNameTextView.getText());

        LinearLayout invitedMeLayout = (LinearLayout) itemView.findViewById(com.hoccer.xo.release.R.id.ll_invited_me);
        assertEquals(ViewGroup.GONE, invitedMeLayout.getVisibility());

        TextView isJoinedTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.is_joined);
        assertEquals(View.VISIBLE, isJoinedTextView.getVisibility());
    }

    public void testAcceptInvitation() {

    }

    public void testDeclineInvitation() {

    }

    @UiThreadTest
    public void testGroupListSorting() {
        mockGroupRelationship(TalkGroupMember.STATE_JOINED, "1", "J");
        mockGroupRelationship(TalkGroupMember.STATE_INVITED, "2", "A");
        mockGroupRelationship(TalkGroupMember.STATE_JOINED, "3", "H");
        mockGroupRelationship(TalkGroupMember.STATE_INVITED, "4", "B");
        mockGroupRelationship(TalkGroupMember.STATE_JOINED, "5", "I");
        mockGroupRelationship(TalkGroupMember.STATE_INVITED, "6", "C");
        mockGroupRelationship(TalkGroupMember.STATE_JOINED, "7", "G");
        mockGroupRelationship(TalkGroupMember.STATE_INVITED, "8", "D");
        mockGroupRelationship(TalkGroupMember.STATE_JOINED, "9", "F");
        mockGroupRelationship(TalkGroupMember.STATE_INVITED, "10", "E");

        assertEquals(10, groupsAdapter.getCount());

        TalkClientContact group1 = (TalkClientContact) groupsAdapter.getItem(0);
        TalkClientContact group2 = (TalkClientContact) groupsAdapter.getItem(1);
        TalkClientContact group3 = (TalkClientContact) groupsAdapter.getItem(2);
        TalkClientContact group4 = (TalkClientContact) groupsAdapter.getItem(3);
        TalkClientContact group5 = (TalkClientContact) groupsAdapter.getItem(4);
        TalkClientContact group6 = (TalkClientContact) groupsAdapter.getItem(5);
        TalkClientContact group7 = (TalkClientContact) groupsAdapter.getItem(6);
        TalkClientContact group8 = (TalkClientContact) groupsAdapter.getItem(7);
        TalkClientContact group9 = (TalkClientContact) groupsAdapter.getItem(8);
        TalkClientContact group10 = (TalkClientContact) groupsAdapter.getItem(9);

        // invited to group
        assertEquals("A", group1.getNickname());
        assertEquals("B", group2.getNickname());
        assertEquals("C", group3.getNickname());
        assertEquals("D", group4.getNickname());
        assertEquals("E", group5.getNickname());

        // joined group
        assertEquals("F", group6.getNickname());
        assertEquals("G", group7.getNickname());
        assertEquals("H", group8.getNickname());
        assertEquals("I", group9.getNickname());
        assertEquals("J", group10.getNickname());
    }

    private void mockGroupRelationship(String state, String groupId) {
        mockGroupRelationship(state, groupId, GROUP_NAME_PREFIX + groupId);
    }

    private void mockGroupRelationship(String state, String groupId, String groudName) {

        TalkClientContact groupContact = TalkClientContact.createGroupContact();
        groupContact.updateGroupId(groupId);

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupId(groupContact.getGroupId());
        groupPresence.setGroupTag(groupContact.getGroupTag());
        groupPresence.setGroupName(groudName);
        groupContact.updateGroupPresence(groupPresence);

        TalkGroupMember groupMember = new TalkGroupMember();
        groupMember.setClientId(XoApplication.getXoClient().getSelfContact().getClientId());
        groupMember.setGroupId(groupPresence.getGroupId());
        groupMember.setState(state);

        try {
            XoApplication.getXoClient().getDatabase().saveContact(groupContact);
            XoApplication.getXoClient().getDatabase().saveGroup(groupPresence);
            XoApplication.getXoClient().getDatabase().saveGroupMember(groupMember);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        XoApplication.getXoClient().updateGroupMember(groupMember);
    }

}
