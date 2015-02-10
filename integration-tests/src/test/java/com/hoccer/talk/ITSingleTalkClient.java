package com.hoccer.talk;

// import junit stuff

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestHelper;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


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
        final XoClient c = TestHelper.createTalkClient(firstServer);

        // test waking and connecting
        assertFalse(c.isAwake());
        c.connect();
        assertTrue(c.isAwake());

        await("client active").untilCall(to(c).getState(), equalTo(XoClient.STATE_READY));

        // test disconnecting
        c.disconnect();
        await("client is inactive").untilCall(to(c).getState(), equalTo(XoClient.STATE_DISCONNECTED));
    }

}

