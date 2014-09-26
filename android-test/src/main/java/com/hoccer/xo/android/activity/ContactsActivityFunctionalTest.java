package com.hoccer.xo.android.activity;

import android.app.ActionBar.Tab;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.ClientContactsAdapter;
import com.hoccer.xo.android.fragment.ClientListFragment;

import java.sql.SQLException;

public class ContactsActivityFunctionalTest extends ActivityInstrumentationTestCase2<ContactsActivity> {

    private ContactsActivity activity;

    public ContactsActivityFunctionalTest() {
        super(ContactsActivity.class);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
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

    public void testReceiveClientInvitation() throws InterruptedException {

        final String otherClientName = "otherClientName";

        final TalkClientContact otherContact = new TalkClientContact(TalkClientContact.TYPE_CLIENT);
        TalkPresence presence = new TalkPresence();
        presence.setClientName(otherClientName);
        otherContact.updatePresence(presence);

        String selfContactId = XoApplication.getXoClient().getSelfContact().getClientId();

        TalkRelationship relationship = new TalkRelationship();
        relationship.setClientId(selfContactId);
        relationship.setOtherClientId(otherContact.getClientId());
        relationship.setState(TalkRelationship.STATE_INVITED_ME);

        otherContact.updateRelationship(relationship);

        try {
            XoApplication.getXoClient().getDatabase().saveRelationship(otherContact.getClientRelationship());
            XoApplication.getXoClient().getDatabase().saveContact(otherContact);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ViewPager viewPager = (ViewPager) activity.findViewById(com.hoccer.xo.release.R.id.pager);
        FragmentPagerAdapter adapter = (FragmentPagerAdapter) viewPager.getAdapter();
        final ClientListFragment clientListFragment = (ClientListFragment) adapter.getItem(0);
        final ClientContactsAdapter clientContactsAdapter = (ClientContactsAdapter) clientListFragment.getListAdapter();

        clientContactsAdapter.onClientRelationshipChanged(otherContact);

        assertEquals(1, clientContactsAdapter.getCount());
        TalkClientContact expectedContact = (TalkClientContact) clientContactsAdapter.getItem(0);

        assertTrue(expectedContact.getClientRelationship().invitedMe());

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

        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View itemView = clientListFragment.getListView().getChildAt(0);
                    assertNotNull(itemView);

                    TextView contactNameTextView = (TextView) itemView.findViewById(com.hoccer.xo.release.R.id.contact_name);
                    assertEquals(otherClientName, contactNameTextView.getText());

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
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

}
