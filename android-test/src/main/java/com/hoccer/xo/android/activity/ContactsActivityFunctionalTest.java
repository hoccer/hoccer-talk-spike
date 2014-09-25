package com.hoccer.xo.android.activity;

import android.app.ActionBar.Tab;
import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.mock.MockContext;
import android.view.View;
import com.hoccer.talk.client.IXoClientConfiguration;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoAndroidClientConfiguration;
import com.hoccer.xo.android.XoAndroidClientHost;
import com.hoccer.xo.android.XoApplication;

public class ContactsActivityFunctionalTest extends ActivityInstrumentationTestCase2<ContactsActivity> {

    private ContactsActivity activity;

    public ContactsActivityFunctionalTest() {
        super(ContactsActivity.class);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
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

        View pairMenuItem = (View) activity.findViewById(com.hoccer.xo.release.R.id.menu_pair);
        assertNotNull(pairMenuItem);

        View newGroupMenuItem = (View) activity.findViewById(com.hoccer.xo.release.R.id.menu_new_group);
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

    public void testReceiveInvitation() {

        Context mockContext = new MockContext();
        IXoClientHost clientHost = new XoAndroidClientHost(mockContext);
        IXoClientConfiguration configuration = new XoAndroidClientConfiguration(mockContext);

        XoClient xoClient = new XoClient(clientHost, configuration);

        TalkClientContact selfContact = XoApplication.getXoClient().getSelfContact();
        xoClient.inviteFriend(selfContact);
    }
}
