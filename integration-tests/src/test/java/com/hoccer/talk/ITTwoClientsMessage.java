package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestHelper;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ITTwoClientsMessage extends IntegrationTest {

    private TestTalkServer firstServer;

    private final static String messageText = "test";
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(firstServer, 2);
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
        TestHelper.shutdownClients(clients);
    }

    @Test
    public void clientMessageTest() throws Exception {
        // assigning clients
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");

        TestHelper.pairClients(sendingClient, receivingClient);

        // sendingClient sends a messages to receivingClient
        TalkClientContact recipientContact = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        TalkClientMessage message = sendingClient.composeClientMessage(recipientContact, messageText);
        sendingClient.requestDelivery(message);

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<TalkClientMessage> unseenMessages = receivingClient.getDatabase().findUnseenMessages();
                return unseenMessages != null &&
                        unseenMessages.size() == 1 &&
                        !unseenMessages.get(0).isInProgress() &&
                        messageText.equals(unseenMessages.get(0).getText());
            }
        });
    }

    @Test
    public void clientMessageTestOfflineRecipient() throws Exception {
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");
        TestHelper.pairClients(sendingClient, receivingClient);

        // Taking recipient offline
        receivingClient.deactivate();
        await("receivingClient is inactive").untilCall(to(receivingClient).getState(), equalTo(XoClient.STATE_INACTIVE));

        TalkClientContact recipientContact = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        TalkClientMessage message = sendingClient.composeClientMessage(recipientContact, messageText);
        sendingClient.requestDelivery(message);

        // Taking recipient online again
        receivingClient.wake();
        await("receivingClient reaches active state").untilCall(to(receivingClient).getState(), equalTo(XoClient.STATE_ACTIVE));

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<TalkClientMessage> unseenMessages = receivingClient.getDatabase().findUnseenMessages();
                return unseenMessages != null &&
                       unseenMessages.size() == 1 &&
                       !unseenMessages.get(0).isInProgress() &&
                       messageText.equals(unseenMessages.get(0).getText());
            }
        });
    }

    @Ignore
    @Test
    public void clientMessageTestRecipientBlockedSender() throws SQLException, InterruptedException {
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");
        TestHelper.pairClients(sendingClient, receivingClient);

        // Recipient blocks sender
        final String sendingClientId = sendingClient.getSelfContact().getClientId();
        receivingClient.blockContact(receivingClient.getDatabase().findContactByClientId(sendingClientId, false));

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return receivingClient.getDatabase().findContactByClientId(sendingClientId, false).getClientRelationship().isBlocked();
            }
        });

        TalkClientContact recipientContact = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        TalkClientMessage message = sendingClient.composeClientMessage(recipientContact, messageText);
        sendingClient.requestDelivery(message);

        // TODO: Now check that the recipient does not receive the message.
        // maybe ask the server? Does the server persist this?
    }
}
