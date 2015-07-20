package com.hoccer.talk.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TalkDeliveryTest {

    /*
    * Note: the attributes are currently package local. Since the test suite is also in the same package as the tested class we could
    * use the attributes directly. We are using the getters/setters though for the most part.
    * */

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

    @Test
    public void testNextStateAllowed() throws Exception {
        // Allows any next state if current state is already invalid
        assertTrue(TalkDelivery.nextStateAllowed("foo", "bar"));
        assertTrue(TalkDelivery.nextStateAllowed("foo", TalkDelivery.STATE_ABORTED_ACKNOWLEDGED));

        // Only allows valid states to follow on valid states
        assertFalse(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_NEW, "foo"));
        assertTrue(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_NEW, TalkDelivery.STATE_DELIVERING));
        assertTrue(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_NEW, TalkDelivery.STATE_FAILED));

        // Allows reentering states
        assertTrue(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_NEW, TalkDelivery.STATE_NEW));

        // Does not allow a next state when a final state was reached
        /*for (String finalState : TalkDelivery.FINAL_STATES) {
            assertFalse(TalkDelivery.nextStateAllowed(finalState, TalkDelivery.STATE_NEW));
        }*/
        assertFalse(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_ABORTED_ACKNOWLEDGED, TalkDelivery.STATE_NEW));
        assertFalse(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_FAILED_ACKNOWLEDGED, TalkDelivery.STATE_NEW));
        assertFalse(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_DELIVERED_PRIVATE_ACKNOWLEDGED, TalkDelivery.STATE_NEW));
        assertFalse(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_DELIVERED_SEEN_ACKNOWLEDGED, TalkDelivery.STATE_NEW));
        assertFalse(TalkDelivery.nextStateAllowed(TalkDelivery.STATE_DELIVERED_UNSEEN_ACKNOWLEDGED, TalkDelivery.STATE_NEW));

        // Only allows next states to be reachable from the current state otherwise.
        //... see tests for statePathExists
    }

    private HashMap<String, Set<String>> simpleStateGraph() {
        /* Simple Graph that allows the following transitions/states...
        *
        * States: ON, OFF
        * Transitions:
        *    ON => OFF
        * */
        HashMap<String, Set<String>> simpleGraph = new HashMap<String, Set<String>>();

        // Initial State
        simpleGraph.put("ON",
                new HashSet<String>(Arrays.asList(new String[]{"OFF"}))
        );

        // Final State
        simpleGraph.put("OFF",
                new HashSet<String>()
        );
        return simpleGraph;
    }

    private HashMap<String, Set<String>> simpleCyclicStateGraph() {
        // Based on ON => OFF simple graph
        HashMap<String, Set<String>> cyclicGraph = simpleStateGraph();

        // also allowing OFF => ON, which makes it cyclic
        cyclicGraph.remove("OFF");
        cyclicGraph.put("OFF",
                new HashSet<String>(Arrays.asList(new String[]{"ON"}))
        );
        return cyclicGraph;
    }

    private HashMap<String, Set<String>> simpleReentrantGraph() {
        /* Simple Graph that allows the following transitions/states...
        *
        * States/Transistions: A => A
        * */
        HashMap<String, Set<String>> simpleReentrantGraph = new HashMap<String, Set<String>>();

        // Initial State
        simpleReentrantGraph.put("A", new HashSet<String>(Arrays.asList(new String[]{"A"})));
        return simpleReentrantGraph;
    }

    private HashMap<String, Set<String>> loopingGraph() {
        /* Simple Graph that allows the following transitions/states...
        *
        * States/Transistions: A => B => C => B
        * */
        HashMap<String, Set<String>> loopingGraph = new HashMap<String, Set<String>>();

        // Initial State
        loopingGraph.put("A", new HashSet<String>(Arrays.asList(new String[]{"B"})));
        loopingGraph.put("B", new HashSet<String>(Arrays.asList(new String[]{"C"})));
        loopingGraph.put("C", new HashSet<String>(Arrays.asList(new String[]{"B"})));
        return loopingGraph;
    }

    private HashMap<String, Set<String>> deepStateGraph() {
        /* Simple Graph that allows the following transitions/states...
        *
        * States/Transistions: A => B => C => D => E => F
        * */
        HashMap<String, Set<String>> simpleDeepGraph = new HashMap<String, Set<String>>();

        // Initial State
        simpleDeepGraph.put("A", new HashSet<String>(Arrays.asList(new String[]{"B"})));
        simpleDeepGraph.put("B", new HashSet<String>(Arrays.asList(new String[]{"C"})));
        simpleDeepGraph.put("C", new HashSet<String>(Arrays.asList(new String[]{"D"})));
        simpleDeepGraph.put("D", new HashSet<String>(Arrays.asList(new String[]{"E"})));
        simpleDeepGraph.put("E", new HashSet<String>(Arrays.asList(new String[]{"F"})));
        simpleDeepGraph.put("F", new HashSet<String>());
        return simpleDeepGraph;
    }

    @Test
    public void testStatePathExistsSimple() throws Exception {
        assertTrue(TalkDelivery.statePathExists(simpleStateGraph(), "ON", "OFF", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(simpleStateGraph(), "OFF", "ON", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(simpleStateGraph(), "OFF", "OFF", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(simpleStateGraph(), "ON", "ON", new HashSet<String>()));
    }

    @Test
    public void testStatePathExistsReentrant() throws Exception {
        assertTrue(TalkDelivery.statePathExists(simpleReentrantGraph(), "A", "A", new HashSet<String>()));
    }

    @Test
    public void testStatePathExistsCyclic() throws Exception {
        assertTrue(TalkDelivery.statePathExists(simpleCyclicStateGraph(), "ON", "OFF", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(simpleCyclicStateGraph(), "OFF", "ON", new HashSet<String>()));
    }

    @Test
    public void testStatePathExistsLooping() throws Exception {
        assertFalse(TalkDelivery.statePathExists(loopingGraph(), "B", "A", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(loopingGraph(), "C", "A", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(loopingGraph(), "B", "B", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(loopingGraph(), "C", "B", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(loopingGraph(), "B", "C", new HashSet<String>()));
    }

    @Test(expected = RuntimeException.class)
    public void testStatePathWithUnknownStartState() throws Exception {
        TalkDelivery.statePathExists(simpleStateGraph(), "BROKEN", "OFF", new HashSet<String>());
    }

    @Test(expected = RuntimeException.class)
    public void testStatePathWithUnknownEndState() throws Exception {
        TalkDelivery.statePathExists(simpleStateGraph(), "ON", "NIRVANA", new HashSet<String>());
    }

    @Test
    public void testStatePathExistsDeep() throws Exception {
        assertTrue(TalkDelivery.statePathExists(deepStateGraph(), "A", "F", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(deepStateGraph(), "B", "F", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(deepStateGraph(), "C", "F", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(deepStateGraph(), "D", "F", new HashSet<String>()));
        assertTrue(TalkDelivery.statePathExists(deepStateGraph(), "E", "F", new HashSet<String>()));

        assertFalse(TalkDelivery.statePathExists(deepStateGraph(), "F", "F", new HashSet<String>()));

        // does not follow the path backwards...
        assertFalse(TalkDelivery.statePathExists(deepStateGraph(), "F", "A", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(deepStateGraph(), "F", "B", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(deepStateGraph(), "F", "C", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(deepStateGraph(), "F", "D", new HashSet<String>()));
        assertFalse(TalkDelivery.statePathExists(deepStateGraph(), "F", "E", new HashSet<String>()));
    }

    @Test
    public void testUpdateWithOtherDelivery() throws Exception {
        final TalkDelivery firstDelivery = new TalkDelivery();
        final TalkDelivery secondDelivery = new TalkDelivery();

        firstDelivery.setMessageId("messageId");
        firstDelivery.setMessageTag("messageTag");
        firstDelivery.setSenderId("senderId");
        firstDelivery.setReceiverId("receiverId");
        firstDelivery.setGroupId("groupId");
        firstDelivery.setState("state");
        firstDelivery.setKeyId("keyId");
        firstDelivery.setKeyCiphertext("keyCypherText");
        firstDelivery.setTimeAccepted(new Date(0));
        firstDelivery.setTimeChanged(new Date(10000));
        firstDelivery.setTimeUpdatedIn(new Date(20000));
        firstDelivery.setTimeUpdatedOut(new Date(30000));
        firstDelivery.setTimeAttachmentReceived(new Date(40000));
        firstDelivery.setAttachmentState("attachmentState");
        firstDelivery.setReason("because");

        secondDelivery.updateWith(firstDelivery);

        assertEquals("messageId", secondDelivery.getMessageId());
        assertEquals("messageTag", secondDelivery.getMessageTag());
        assertEquals("senderId", secondDelivery.getSenderId());

        assertEquals("receiverId", secondDelivery.getReceiverId());
        assertEquals("groupId", secondDelivery.getGroupId());
        assertEquals("state", secondDelivery.getState());
        assertEquals("keyId", secondDelivery.getKeyId());

        assertEquals("keyCypherText", secondDelivery.getKeyCiphertext());
        assertEquals("attachmentState", secondDelivery.getAttachmentState());

        assertEquals(new Date(0), secondDelivery.getTimeAccepted());
        assertEquals(new Date(10000), secondDelivery.getTimeChanged());
        assertEquals(new Date(20000), secondDelivery.getTimeUpdatedIn());
        assertEquals(new Date(30000), secondDelivery.getTimeUpdatedOut());
        assertEquals(new Date(40000), secondDelivery.getTimeAttachmentReceived());

        assertEquals("because", secondDelivery.getReason());
    }

    @Test
    public void testUpdateWithOtherDeliveryAndNullFields() throws Exception {
        // updateWith(deliver, fields) should work exactly like updateWith(delivery) if fields is null
        final TalkDelivery firstDelivery = new TalkDelivery();
        final TalkDelivery secondDelivery = new TalkDelivery();

        firstDelivery.setMessageId("messageId");
        firstDelivery.setMessageTag("messageTag");
        firstDelivery.setSenderId("senderId");
        firstDelivery.setReceiverId("receiverId");
        firstDelivery.setGroupId("groupId");
        firstDelivery.setState("state");
        firstDelivery.setKeyId("keyId");
        firstDelivery.setKeyCiphertext("keyCypherText");
        firstDelivery.setTimeAccepted(new Date(0));
        firstDelivery.setTimeChanged(new Date(10000));
        firstDelivery.setTimeUpdatedIn(new Date(20000));
        firstDelivery.setTimeUpdatedOut(new Date(30000));
        firstDelivery.setTimeAttachmentReceived(new Date(40000));
        firstDelivery.setAttachmentState("attachmentState");
        firstDelivery.setReason("because");

        secondDelivery.updateWith(firstDelivery, null);

        assertEquals("messageId", secondDelivery.getMessageId());
        assertEquals("messageTag", secondDelivery.getMessageTag());
        assertEquals("senderId", secondDelivery.getSenderId());

        assertEquals("receiverId", secondDelivery.getReceiverId());
        assertEquals("groupId", secondDelivery.getGroupId());
        assertEquals("state", secondDelivery.getState());
        assertEquals("keyId", secondDelivery.getKeyId());

        assertEquals("keyCypherText", secondDelivery.getKeyCiphertext());
        assertEquals("attachmentState", secondDelivery.getAttachmentState());

        assertEquals(new Date(0), secondDelivery.getTimeAccepted());
        assertEquals(new Date(10000), secondDelivery.getTimeChanged());
        assertEquals(new Date(20000), secondDelivery.getTimeUpdatedIn());
        assertEquals(new Date(30000), secondDelivery.getTimeUpdatedOut());
        assertEquals(new Date(40000), secondDelivery.getTimeAttachmentReceived());

        assertEquals("because", secondDelivery.getReason());
    }

    @Test
    public void testUpdateWithOtherDeliveryAndFields() throws Exception {
        final TalkDelivery firstDelivery = new TalkDelivery();
        final TalkDelivery secondDelivery = new TalkDelivery();

        firstDelivery.setMessageId("messageId");
        firstDelivery.setMessageTag("messageTag");
        firstDelivery.setSenderId("senderId");
        firstDelivery.setReceiverId("receiverId");
        firstDelivery.setGroupId("groupId");
        firstDelivery.setState("state");
        firstDelivery.setKeyId("keyId");
        firstDelivery.setKeyCiphertext("keyCypherText");
        firstDelivery.setTimeAccepted(new Date(0));
        firstDelivery.setTimeChanged(new Date(10000));
        firstDelivery.setTimeUpdatedIn(new Date(20000));
        firstDelivery.setTimeUpdatedOut(new Date(30000));
        firstDelivery.setTimeAttachmentReceived(new Date(40000));
        firstDelivery.setAttachmentState("attachmentState");
        firstDelivery.setReason("because");

        secondDelivery.setMessageId("_messageId");
        secondDelivery.setMessageTag("_messageTag");
        secondDelivery.setSenderId("_senderId");
        secondDelivery.setReceiverId("_receiverId");
        secondDelivery.setGroupId("_groupId");
        secondDelivery.setState("_state");
        secondDelivery.setKeyId("_keyId");
        secondDelivery.setKeyCiphertext("_keyCypherText");
        secondDelivery.setTimeAccepted(new Date(1));
        secondDelivery.setTimeChanged(new Date(10001));
        secondDelivery.setTimeUpdatedIn(new Date(20001));
        secondDelivery.setTimeUpdatedOut(new Date(30001));
        secondDelivery.setTimeAttachmentReceived(new Date(40001));
        secondDelivery.setAttachmentState("_attachmentState");
        secondDelivery.setReason("_because");

        // copy only a few fields...
        secondDelivery.updateWith(firstDelivery,
                new HashSet<String>(
                        Arrays.asList(new String[]{
                                TalkDelivery.FIELD_ATTACHMENT_STATE,
                                TalkDelivery.FIELD_STATE,
                                TalkDelivery.FIELD_REASON,
                                TalkDelivery.FIELD_TIME_UPDATED_IN
                        })
                )
        );

        assertEquals("_messageId", secondDelivery.getMessageId());
        assertEquals("_messageTag", secondDelivery.getMessageTag());
        assertEquals("_senderId", secondDelivery.getSenderId());

        assertEquals("_receiverId", secondDelivery.getReceiverId());
        assertEquals("_groupId", secondDelivery.getGroupId());
        assertEquals("state", secondDelivery.getState());
        assertEquals("_keyId", secondDelivery.getKeyId());

        assertEquals("_keyCypherText", secondDelivery.getKeyCiphertext());
        assertEquals("attachmentState", secondDelivery.getAttachmentState());

        assertEquals(new Date(1), secondDelivery.getTimeAccepted());
        assertEquals(new Date(10001), secondDelivery.getTimeChanged());
        assertEquals(new Date(20000), secondDelivery.getTimeUpdatedIn());
        assertEquals(new Date(30001), secondDelivery.getTimeUpdatedOut());
        assertEquals(new Date(40001), secondDelivery.getTimeAttachmentReceived());

        // It seems only reason is not copied over - is this intentional?
        assertEquals("because", secondDelivery.getReason());
    }
}