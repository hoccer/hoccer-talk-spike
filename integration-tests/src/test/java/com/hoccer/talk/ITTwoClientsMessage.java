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

        pairClients(sendingClient, receivingClient);

        // sendingClient sends a messages to receivingClient
        TalkClientContact recipientContact = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        TalkClientMessage message = sendingClient.composeClientMessage(recipientContact, messageText);
        sendingClient.requestDelivery(message);

        await("receivingClient has unseen messages").untilCall(to(receivingClient.getDatabase()).findUnseenMessages(), is(not(empty())));

        final List<TalkClientMessage> unseenMessages = receivingClient.getDatabase().findUnseenMessages();
        assertEquals(1, unseenMessages.size());

        // wait until message is done downloading
        await("unseen message is done downloading").untilCall(to(unseenMessages.get(0)).isInProgress(), is(false));
        assertEquals("unseen message text matches", messageText, unseenMessages.get(0).getText());
    }

    @Test
    public void clientMessageTestOfflineRecipient() throws Exception {
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");
        pairClients(sendingClient, receivingClient);

        // Taking recipient offline
        receivingClient.deactivate();
        await("receivingClient is inactive").untilCall(to(receivingClient).getState(), equalTo(XoClient.STATE_INACTIVE));

        TalkClientContact recipientContact = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        TalkClientMessage message = sendingClient.composeClientMessage(recipientContact, messageText);
        sendingClient.requestDelivery(message);

        // Taking recipient online again
        receivingClient.wake();

        await("receivingClient has unseen messages").untilCall(to(receivingClient.getDatabase()).findUnseenMessages(), is(not(empty())));

        final List<TalkClientMessage> unseenMessages = receivingClient.getDatabase().findUnseenMessages();
        assertEquals(1, unseenMessages.size());

        // wait until message is done downloading
        await("unseen message is done downloading").untilCall(to(unseenMessages.get(0)).isInProgress(), is(false));
        assertEquals("unseen message text matches", messageText, unseenMessages.get(0).getText() );
    }
}
