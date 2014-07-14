package com.hoccer.talk.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

}