package com.hoccer.talk.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TalkDeliveryTest {

    @Test
    public void testCreate() throws Exception {
        final TalkDelivery myEmptyDelivery = new TalkDelivery();
        assertNull(myEmptyDelivery.getState());
        assertNull(myEmptyDelivery.getTimeAccepted());
        assertNull(myEmptyDelivery.getTimeChanged());
        assertNull(myEmptyDelivery.getTimeUpdatedIn());
        assertNull(myEmptyDelivery.getTimeUpdatedOut());

        final TalkDelivery myInitializedDelivery = new TalkDelivery(true);
        assertEquals(TalkDelivery.STATE_NEW, myInitializedDelivery.getState());
        assertNotNull(myInitializedDelivery.getTimeAccepted());
        assertNotNull(myInitializedDelivery.getTimeChanged());
        assertNotNull(myInitializedDelivery.getTimeUpdatedIn());
        assertNotNull(myInitializedDelivery.getTimeUpdatedOut());
    }

    @Test(expected = RuntimeException.class)
    public void testSetInvalidStateOnInitializedTalkDelivery() throws Exception {
        final TalkDelivery myDelivery = new TalkDelivery(true);
        myDelivery.setState("foo");
    }

    @Test
    public void testSetInvalidStateOnUninitializedTalkDelivery() throws Exception {
        final TalkDelivery myDelivery = new TalkDelivery();
        myDelivery.setState("foo");
        assertEquals("foo", myDelivery.getState());
    }

}