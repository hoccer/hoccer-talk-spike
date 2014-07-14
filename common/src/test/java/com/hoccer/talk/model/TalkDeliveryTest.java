package com.hoccer.talk.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TalkDeliveryTest {

    @Test
    public void testCreate() throws Exception {
        TalkDelivery myEmptyDelivery = new TalkDelivery();
        assertNull(myEmptyDelivery.getState());
        assertNull(myEmptyDelivery.getTimeAccepted());
        assertNull(myEmptyDelivery.getTimeChanged());
        assertNull(myEmptyDelivery.getTimeUpdatedIn());
        assertNull(myEmptyDelivery.getTimeUpdatedOut());

        TalkDelivery myInitializedDelivery = new TalkDelivery(true);
        assertEquals(TalkDelivery.STATE_NEW, myInitializedDelivery.getState());
    }

    @Test(expected = RuntimeException.class)
    public void testSetInvalidStateOnInitializedTalkDelivery() {
        TalkDelivery myDelivery = new TalkDelivery(true);
        myDelivery.setState("foo");
    }

}