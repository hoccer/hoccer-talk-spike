package com.hoccer.xo.android.activity;

import android.test.ActivityUnitTestCase;
import junit.framework.Assert;

public class ContactsActivityUnitTest extends ActivityUnitTestCase<ContactsActivity> {

    public ContactsActivityUnitTest() {
        super(ContactsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSomething() {
        Assert.assertEquals(1,1);
    }
}
