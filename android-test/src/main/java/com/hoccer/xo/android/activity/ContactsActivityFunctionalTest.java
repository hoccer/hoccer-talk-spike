package com.hoccer.xo.android.activity;

import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
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
import com.hoccer.xo.android.adapter.ClientContactsAdapter;
import com.hoccer.xo.android.fragment.ClientListFragment;

import java.sql.SQLException;

public class ContactsActivityFunctionalTest extends ActivityInstrumentationTestCase2<ContactsActivity> {

    public static final String OTHER_CLIENT_NAME_PREFIX = "other_client_name_";

    private ContactsActivity activity;
    private ClientListFragment clientListFragment;
    private ClientContactsAdapter clientContactsAdapter;

    public ContactsActivityFunctionalTest() {
        super(ContactsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();

        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        clientListFragment = (ClientListFragment) adapter.getItem(0);
        clientContactsAdapter = (ClientContactsAdapter) clientListFragment.getListAdapter();

        XoApplication.getXoClient().getDatabase().eraseAllClientContacts();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        XoApplication.getXoClient().getDatabase().eraseAllClientContacts();
    }

    public void testPreconditions() {
        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        assertNotNull(viewPager);

        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        assertNotNull(adapter);

        assertNotNull(adapter.getItem(0));
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

        clientContactsAdapter.onClientRelationshipChanged(null);

        assertEquals(1, clientContactsAdapter.getCount());
        TalkClientContact expectedContact = (TalkClientContact) clientContactsAdapter.getItem(0);

        assertTrue(expectedContact.getClientRelationship().invitedMe());

        assertFalse(clientContactsAdapter.isEnabled(0));

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 1000) {
                    if (clientListFragment.getListView().getCount() > 0) {
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
        assertEquals(1, clientListFragment.getListView().getCount());

        getInstrumentation().waitForIdleSync();

        final View itemView = clientListFragment.getListView().getChildAt(0);
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
    }

    public void testInvitedRelationUI() throws InterruptedException {

        mockClientRelationship(TalkRelationship.STATE_INVITED, 1);

        clientContactsAdapter.onClientRelationshipChanged(null);

        assertEquals(1, clientContactsAdapter.getCount());
        TalkClientContact expectedContact = (TalkClientContact) clientContactsAdapter.getItem(0);

        assertTrue(expectedContact.getClientRelationship().isInvited());

        assertFalse(clientContactsAdapter.isEnabled(0));

        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 1000) {
                    if (clientListFragment.getListView().getCount() > 0) {
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
        assertEquals(1, clientListFragment.getListView().getCount());

        getInstrumentation().waitForIdleSync();

        View itemView = clientListFragment.getListView().getChildAt(0);

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

        clientContactsAdapter.onClientRelationshipChanged(null);
        assertEquals(1, clientContactsAdapter.getCount());

        TalkClientContact contact = (TalkClientContact) clientContactsAdapter.getItem(0);
        assertTrue(contact.getClientRelationship().isFriend());
        
        assertTrue(clientContactsAdapter.isEnabled(0));

        getInstrumentation().waitForIdleSync();

        final ListView listView = clientListFragment.getListView();
        final View itemView = listView.getChildAt(0);
        assertNotNull(itemView);

        TextView isInvitedTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.tv_is_friend);
        String expected = getActivity().getResources().getString(com.hoccer.xo.release.R.string.message_and_attachment_count_info, 0, 0);
        assertEquals(expected, isInvitedTextView.getText());

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                listView.performItemClick(itemView, 0, clientContactsAdapter.getItemId(0));
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

        clientContactsAdapter.onClientRelationshipChanged(null);

        getInstrumentation().waitForIdleSync();

        ListView listView = clientListFragment.getListView();
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
//        assertEquals(1, clientContactsAdapter.getCount());
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

        clientContactsAdapter.onClientRelationshipChanged(null);

        getInstrumentation().waitForIdleSync();

        View itemView = clientListFragment.getListView().getChildAt(0);
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
//        assertEquals(0, clientContactsAdapter.getCount());
//        assertEquals(0, listView.getCount());
//        assertEquals(0, listView.getChildCount());
    }

    public void testClientListSorting() {
        mockClientRelationship(TalkRelationship.STATE_FRIEND, 1);
        mockClientRelationship(TalkRelationship.STATE_INVITED, 2);
        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 3);
        mockClientRelationship(TalkRelationship.STATE_INVITED, 4);
        mockClientRelationship(TalkRelationship.STATE_INVITED, 5);
        mockClientRelationship(TalkRelationship.STATE_FRIEND, 6);
        mockClientRelationship(TalkRelationship.STATE_INVITED_ME, 7);

        clientContactsAdapter.onClientRelationshipChanged(null);

        assertEquals(7, clientContactsAdapter.getCount());

        // invited me contacts
        TalkClientContact expectedContact1 = (TalkClientContact) clientContactsAdapter.getItem(0);
        assertEquals("3", expectedContact1.getClientId());

        TalkClientContact expectedContact2 = (TalkClientContact) clientContactsAdapter.getItem(1);
        assertEquals("7", expectedContact2.getClientId());

        // invited others
        TalkClientContact expectedContact3 = (TalkClientContact) clientContactsAdapter.getItem(2);
        assertEquals("2", expectedContact3.getClientId());

        TalkClientContact expectedContact4 = (TalkClientContact) clientContactsAdapter.getItem(3);
        assertEquals("4", expectedContact4.getClientId());

        TalkClientContact expectedContact5 = (TalkClientContact) clientContactsAdapter.getItem(4);
        assertEquals("5", expectedContact5.getClientId());

        // friends
        TalkClientContact expectedContact6 = (TalkClientContact) clientContactsAdapter.getItem(5);
        assertEquals("1", expectedContact6.getClientId());

        TalkClientContact expectedContact7 = (TalkClientContact) clientContactsAdapter.getItem(6);
        assertEquals("6", expectedContact7.getClientId());
    }


    private TalkClientContact mockClientRelationship(String state, int clientId) {

        final TalkClientContact relatedContact = new TalkClientContact(TalkClientContact.TYPE_CLIENT, Integer.toString(clientId));

        TalkPresence presence = new TalkPresence();
        presence.setClientName(OTHER_CLIENT_NAME_PREFIX + clientId);
        presence.setClientId(Integer.toString(clientId));

        String selfContactId = XoApplication.getXoClient().getSelfContact().getClientId();

        TalkRelationship relationship = new TalkRelationship();
        relationship.setClientId(selfContactId);
        relationship.setOtherClientId(relatedContact.getClientId());
        relationship.setState(state);

        TalkKey publicKey = new TalkKey();
        publicKey.setKeyId(String.format("%%:%%:%%:%%:%%:%%:%%:%%", clientId));

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
