package com.hoccer.xo.android.activity;

import android.test.ActivityInstrumentationTestCase2;
import junit.framework.Assert;

public class SingleProfileActivityTest extends ActivityInstrumentationTestCase2<SingleProfileActivity> {

    public SingleProfileActivityTest(Class<SingleProfileActivity> activityClass) {
        super(activityClass);
    }

    public void testSomething() throws Exception {

        SingleProfileActivity activity = getActivity();
        String title = (String) activity.getTitle();
        Assert.assertEquals("Hoccer", title);
    }
}
