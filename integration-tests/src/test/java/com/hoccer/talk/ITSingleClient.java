package com.hoccer.talk;

// import junit stuff

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

// Utility classes for async tests
import com.google.code.tempusfugit.temporal.*;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;

// Import the classes we need for the tests
// import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;

@RunWith(JUnit4.class)
public class ITSingleClient extends IntegrationTest {

    private TestServer firstServer;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
    }

    @Test
    public void clientConnectAndDisconnectTest() throws Exception {
        // create client
        final XoClient c = createTalkClient(firstServer);

        // test waking and connecting
        assertFalse(c.isAwake());
        c.wake();
        assertTrue(c.isAwake());

        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return XoClient.STATE_ACTIVE == c.getState();
            }
        }, Timeout.timeout(seconds(2)));

        // test disconnecting
        c.deactivate();
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return XoClient.STATE_INACTIVE == c.getState();
            }
        }, Timeout.timeout(seconds(2)));
    }

}

