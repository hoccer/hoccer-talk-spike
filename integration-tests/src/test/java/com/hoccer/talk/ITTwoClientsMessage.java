package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ITTwoClientsMessage extends IntegrationTest {

    private TestTalkServer firstServer;

    private final static String messageText = "test";
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
        clients = initializeTalkClients(firstServer, 2);
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
        shutdownClients(clients);
    }

    @Test
    public void clientMessageTest() throws Exception {
        // assigning clients
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");

        String token = sendingClient.generatePairingToken();
        receivingClient.performTokenPairing(token);

        final String c1Id = sendingClient.getSelfContact().getClientId();
        final String c2Id = receivingClient.getSelfContact().getClientId();

        await("client 1 is paired with client 2").untilCall(to(sendingClient.getDatabase()).findContactByClientId(c2Id, false), notNullValue());
        await("client 1 has client 2's pubkey").untilCall(to(sendingClient.getDatabase().findContactByClientId(c2Id, false)).getPublicKey(), notNullValue());

        await("client 2 is paired with client 1").untilCall(to(receivingClient.getDatabase()).findContactByClientId(c1Id, false), notNullValue());
        await("client 2 has client 1's pubkey").untilCall(to(receivingClient.getDatabase().findContactByClientId(c1Id, false)).getPublicKey(), notNullValue());

        // sendingClient sends a messages to receivingClient
        TalkClientContact recipient = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        TalkClientMessage message = sendingClient.composeClientMessage(recipient, messageText);
        sendingClient.requestDelivery(message);

        await("receivingClient has unseen messages").untilCall(to(receivingClient.getDatabase()).findUnseenMessages(), is(not(empty())));

        final List<TalkClientMessage> unseenMessages = receivingClient.getDatabase().findUnseenMessages();
        assertEquals(1, unseenMessages.size());

        // wait until message is done downloading
        await("unseen message is done downloading").untilCall(to(unseenMessages.get(0)).isInProgress(), is(false));
        assertEquals("unseen message text matches", unseenMessages.get(0).getText(), messageText);
    }
}
