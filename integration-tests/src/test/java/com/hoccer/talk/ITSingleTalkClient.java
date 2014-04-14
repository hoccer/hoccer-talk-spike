package com.hoccer.talk;

// import junit stuff
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestTalkServer;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

// Import the classes we need for the tests
import com.hoccer.talk.client.XoClient;


@RunWith(JUnit4.class)
public class ITSingleTalkClient extends IntegrationTest {

    private TestTalkServer firstServer;

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

        await("client active").untilCall(to(c).getState(), equalTo(XoClient.STATE_ACTIVE));
        await("client has pubkey").untilCall(to(c.getDatabase().findSelfContact(false)).getPrivateKey(), notNullValue());

        // test disconnecting
        c.deactivate();
        await("client is inactive").untilCall(to(c).getState(), equalTo(XoClient.STATE_INACTIVE));
    }

}

