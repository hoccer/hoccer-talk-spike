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

import java.util.HashMap;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(JUnit4.class)
public class ITTwoTalkClients extends IntegrationTest {

    private TestTalkServer firstServer;
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(firstServer, 2);
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
    }

    @Test
    public void clientPairTest() throws Exception {
        XoClient client1 = clients.get("client1");
        XoClient client2 = clients.get("client2");

        String token = client1.generatePairingToken();
        assertNotNull("Pairing token must not be null", token);
        client2.performTokenPairing(token);

        final String client1Id = client1.getSelfContact().getClientId();
        final String client2Id = client2.getSelfContact().getClientId();

        await("client1 is paired with client2").untilCall(to(client1.getDatabase()).findContactByClientId(client2Id, false), notNullValue());
        await("client2 is paired with client1").untilCall(to(client2.getDatabase()).findContactByClientId(client1Id, false), notNullValue());
    }

}
