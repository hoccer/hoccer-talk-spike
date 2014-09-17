package com.hoccer.xo.android.activity;

import android.test.ActivityInstrumentationTestCase2;
import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions;
import com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers;
import org.hamcrest.Matchers;


public class SingleProfileActivityTest extends ActivityInstrumentationTestCase2<SingleProfileActivity> {

    public SingleProfileActivityTest() {
        super(SingleProfileActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testSomething() throws Exception {
        Espresso.onView(ViewMatchers.withId(com.hoccer.xo.release.R.id.tv_profile_name_text)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("Name"))));
    }
}
