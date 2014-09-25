package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.view.MenuItem;


/* Cannot run this test until the XoApplication doen't use static variables */
@Suppress
public class ChatsActivityUnitTest extends ActivityUnitTestCase<ChatsActivity> {

    private ChatsActivity activity;

    public ChatsActivityUnitTest() {
        super(ChatsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Intent intent = new Intent(getInstrumentation().getTargetContext(), ChatsActivity.class);
        startActivity(intent, null, null);
        activity = getActivity();
    }


    public void testActionBarMenuLayout() {
        MenuItem contactsMenuItem = (MenuItem) activity.findViewById(com.hoccer.xo.release.R.id.menu_contacts);
        assertNotNull(contactsMenuItem);
        assertEquals(true, contactsMenuItem.isVisible());

        MenuItem myProfileMenuItem = (MenuItem) activity.findViewById(com.hoccer.xo.release.R.id.menu_my_profile);
        assertNotNull(myProfileMenuItem);
        assertEquals(true, myProfileMenuItem.isVisible());
    }

    public void testContactsIntentTriggerViaOnClick() {
        MenuItem contactsMenuItem = (MenuItem) activity.findViewById(com.hoccer.xo.release.R.id.menu_contacts);

        activity.onOptionsItemSelected(contactsMenuItem);

        Intent triggeredIntent = getStartedActivityIntent();
        assertNotNull(triggeredIntent);

    }



}
