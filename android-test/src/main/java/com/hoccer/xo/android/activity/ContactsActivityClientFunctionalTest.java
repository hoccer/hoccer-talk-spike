package com.hoccer.xo.android.activity;

import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkKey;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.ClientContactListAdapter;
import com.hoccer.xo.android.fragment.ClientContactListFragment;

import java.sql.SQLException;

public class ContactsActivityClientFunctionalTest extends ActivityInstrumentationTestCase2<ContactsActivity> {

    public static final String OTHER_CLIENT_NAME_PREFIX = "other_client_name_";

    private ContactsActivity activity;
    private ClientContactListFragment mClientContactListFragment;
    private ClientContactListAdapter mClientContactListAdapter;

    public ContactsActivityClientFunctionalTest() {
        super(ContactsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();

        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        mClientContactListFragment = (ClientContactListFragment) adapter.getItem(0);
        mClientContactListAdapter = (ClientContactListAdapter) mClientContactListFragment.getListAdapter();

        XoApplication.getXoClient().getDatabase().eraseAllClientContacts();
        XoApplication.getXoClient().getDatabase().eraseAllRelationships();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        XoApplication.getXoClient().getDatabase().eraseAllClientContacts();
        XoApplication.getXoClient().getDatabase().eraseAllRelationships();
    }

    public void testPreconditions() {
        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        assertNotNull(viewPager);

        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        assertNotNull(adapter);

        assertNotNull(adapter.getItem(0));
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

        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 1);

        mClientContactListAdapter.onClientRelationshipChanged(null);

        assertEquals(1, mClientContactListAdapter.getCount());
        TalkClientContact contact = (TalkClientContact) mClientContactListAdapter.getItem(0);

        assertTrue(contact.getClientRelationship().invitedMe());
        assertTrue(mClientContactListAdapter.isEnabled(0));

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 1000) {
                    if (mClientContactListFragment.getListView().getCount() > 0) {
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
        assertEquals(1, mClientContactListFragment.getListView().getCount());

        getInstrumentation().waitForIdleSync();

        final View itemView = mClientContactListFragment.getListView().getChildAt(0);
        assertNotNull(itemView);

        TextView contactNameTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.contact_name);
        assertEquals(OTHER_CLIENT_NAME_PREFIX + 1, contactNameTextView.getText());

        LinearLayout invitedMeLayout = (LinearLayout) itemView.findViewById(com.hoccer.xo.release.R.id.ll_invited_me);
        assertEquals(ViewGroup.VISIBLE, invitedMeLayout.getVisibility());

        Button acceptButton = (Button) itemView.findViewById(com.hoccer.xo.release.R.id.btn_accept);
        assertEquals(View.VISIBLE, acceptButton.getVisibility());

        Button declineButton = (Button) itemView.findViewById(com.hoccer.xo.release.R.id.btn_decline);
        assertEquals(View.VISIBLE, declineButton.getVisibility());

        TextView isInvitedTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_invited);
        assertEquals(View.GONE, isInvitedTextView.getVisibility());

        TextView isFriendTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_friend);
        assertEquals(View.GONE, isFriendTextView.getVisibility());

        View tabView = getActivity().getActionBar().getTabAt(0).getCustomView();
        assertNotNull(tabView);

        TextView pendingInvitationsBadgeTextView = (TextView) tabView.findViewById(com.hoccer.xo.release.R.id.tv_contact_invite_notification_badge);
        assertNotNull(pendingInvitationsBadgeTextView);
        assertEquals("1", pendingInvitationsBadgeTextView.getText());
    }

    public void testInvitedRelationUI() throws InterruptedException {

        mockClientRelationship(TalkRelationship.STATE_INVITED, 1);

        mClientContactListAdapter.onClientRelationshipChanged(null);

        assertEquals(1, mClientContactListAdapter.getCount());
        TalkClientContact expectedContact = (TalkClientContact) mClientContactListAdapter.getItem(0);

        assertTrue(expectedContact.getClientRelationship().isInvited());
        assertTrue(mClientContactListAdapter.isEnabled(0));

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 1000) {
                    if (mClientContactListFragment.getListView().getCount() > 0) {
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
        assertEquals(1, mClientContactListFragment.getListView().getCount());

        getInstrumentation().waitForIdleSync();

        View itemView = mClientContactListFragment.getListView().getChildAt(0);

        assertNotNull(itemView);

        TextView contactNameTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.contact_name);
        assertEquals(OTHER_CLIENT_NAME_PREFIX + 1, contactNameTextView.getText());

        LinearLayout invitedMeLayout = (LinearLayout) itemView.findViewById(com.hoccer.xo.release.R.id.ll_invited_me);
        assertEquals(ViewGroup.GONE, invitedMeLayout.getVisibility());

        TextView isInvitedTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_invited);
        assertEquals(View.VISIBLE, isInvitedTextView.getVisibility());

        TextView isFriendTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_friend);
        assertEquals(View.GONE, isFriendTextView.getVisibility());
    }

    public void testFriendRelationUI() {

        ActivityMonitor activityMonitor = getInstrumentation().addMonitor(SingleProfileActivity.class.getName(), null, false);

        TalkClientContact relatedContact = mockClientRelationship(TalkRelationship.STATE_FRIEND, 1);

        mClientContactListAdapter.onClientRelationshipChanged(null);
        assertEquals(1, mClientContactListAdapter.getCount());

        TalkClientContact contact = (TalkClientContact) mClientContactListAdapter.getItem(0);
        assertTrue(contact.getClientRelationship().isFriend());
        assertTrue(mClientContactListAdapter.isEnabled(0));

        getInstrumentation().waitForIdleSync();

        final ListView listView = mClientContactListFragment.getListView();
        final View itemView = listView.getChildAt(0);
        assertNotNull(itemView);

        TextView isInvitedTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_friend);
        String expected = getActivity().getResources().getString(com.hoccer.xo.release.R.string.message_and_attachment_count_default, 0, 0);
        assertEquals(expected, isInvitedTextView.getText());

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                listView.performItemClick(itemView, 0, mClientContactListAdapter.getItemId(0));
            }
        });
        getInstrumentation().waitForIdleSync();

        Activity singleProfileActivity = activityMonitor.waitForActivity();
        assertNotNull(singleProfileActivity);

        assertEquals(relatedContact.getClientContactId(), singleProfileActivity.getIntent().getIntExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, -1));

        singleProfileActivity.finish();
    }

    public void testAcceptClientInvitation() throws InterruptedException {

        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 1);

        mClientContactListAdapter.onClientRelationshipChanged(null);

        getInstrumentation().waitForIdleSync();

        ListView listView = mClientContactListFragment.getListView();
        View itemView = listView.getChildAt(0);
        assertNotNull(itemView);

        final Button acceptButton = (Button) itemView.findViewById(com.hoccer.xo.release.R.id.btn_accept);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                acceptButton.performClick();
            }
        });
        getInstrumentation().waitForIdleSync();

//        TODO: get notified when list is updated to perform following asserts
//        assertEquals(1, clientsAdapter.getCount());
//        assertEquals(1, listView.getCount());
//        assertEquals(1, listView.getChildCount());

//        itemView = listView.getChildAt(0);
//        assertNotNull(itemView);
//
//        LinearLayout invitedMeLayout = (LinearLayout) itemView.findViewById(com.hoccer.xo.release.R.id.ll_invited_me);
//        assertEquals(ViewGroup.GONE, invitedMeLayout.getVisibility());
//
//        TextView isFriendTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_friend);
//        assertEquals(View.VISIBLE, isFriendTextView.getVisibility());
    }

    public void testDeclineClientInvitation() {
        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 1);

        mClientContactListAdapter.onClientRelationshipChanged(null);

        getInstrumentation().waitForIdleSync();

        View itemView = mClientContactListFragment.getListView().getChildAt(0);
        assertNotNull(itemView);

        final Button declineButton = (Button) itemView.findViewById(com.hoccer.xo.release.R.id.btn_decline);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                declineButton.performClick();
            }
        });
        getInstrumentation().waitForIdleSync();

//        TODO: get notified when list is updated to perform following asserts
//        assertEquals(0, clientsAdapter.getCount());
//        assertEquals(0, listView.getCount());
//        assertEquals(0, listView.getChildCount());
    }

    public void testClientListSorting() {
        mockClientRelationship(TalkRelationship.STATE_FRIEND, 1, "H");
        mockClientRelationship(TalkRelationship.STATE_INVITED, 2, "D");
        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 3, "B");
        mockClientRelationship(TalkRelationship.STATE_INVITED, 4, "E");
        mockClientRelationship(TalkRelationship.STATE_INVITED, 5, "C");
        mockClientRelationship(TalkRelationship.STATE_FRIEND, 6, "I");
        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 7, "A");
        mockClientRelationship(TalkRelationship.STATE_FRIEND, 8, "F");
        mockClientRelationship(TalkRelationship.STATE_FRIEND, 9, "G");

        mClientContactListAdapter.onClientRelationshipChanged(null);

        assertEquals(9, mClientContactListAdapter.getCount());

        // invited me contacts - ordered chronologically
        TalkClientContact contact1 = (TalkClientContact) mClientContactListAdapter.getItem(0);
        assertEquals("B", contact1.getName());

        TalkClientContact contact2 = (TalkClientContact) mClientContactListAdapter.getItem(1);
        assertEquals("A", contact2.getName());

        // invited others - ordered alphabetically
        TalkClientContact contact3 = (TalkClientContact) mClientContactListAdapter.getItem(2);
        assertEquals("C", contact3.getName());

        TalkClientContact contact4 = (TalkClientContact) mClientContactListAdapter.getItem(3);
        assertEquals("D", contact4.getName());

        TalkClientContact contact5 = (TalkClientContact) mClientContactListAdapter.getItem(4);
        assertEquals("E", contact5.getName());

        // friends - ordered alphabetically
        TalkClientContact contact6 = (TalkClientContact) mClientContactListAdapter.getItem(5);
        assertEquals("F", contact6.getName());

        TalkClientContact contact7 = (TalkClientContact) mClientContactListAdapter.getItem(6);
        assertEquals("G", contact7.getName());

        TalkClientContact contact8 = (TalkClientContact) mClientContactListAdapter.getItem(7);
        assertEquals("H", contact8.getName());

        TalkClientContact contact9 = (TalkClientContact) mClientContactListAdapter.getItem(8);
        assertEquals("I", contact9.getName());
    }

    private TalkClientContact mockClientRelationship(String state, int clientId) {
        return mockClientRelationship(state, clientId, OTHER_CLIENT_NAME_PREFIX + clientId);
    }

    private TalkClientContact mockClientRelationship(String state, int clientId, String clientName) {

        final TalkClientContact relatedContact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, Integer.toString(clientId));

        TalkPresence presence = new TalkPresence();
        presence.setClientName(clientName);
        presence.setClientId(Integer.toString(clientId));

        String selfContactId = XoApplication.getXoClient().getSelfContact().getClientId();

        TalkRelationship relationship = new TalkRelationship();
        relationship.setClientId(selfContactId);
        relationship.setOtherClientId(relatedContact.getClientId());
        relationship.setState(state);

        TalkKey publicKey = new TalkKey();
        publicKey.setKeyId(String.format("PUBLIC_KEY_", clientId));

        relatedContact.updatePresence(presence);
        relatedContact.updateRelationship(relationship);
        relatedContact.setPublicKey(publicKey);

        try {
            XoApplication.getXoClient().getDatabase().savePublicKey(publicKey);
            XoApplication.getXoClient().getDatabase().saveRelationship(relatedContact.getClientRelationship());
            XoApplication.getXoClient().getDatabase().savePresence(presence);
            XoApplication.getXoClient().getDatabase().saveContact(relatedContact);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return relatedContact;
    }

}
