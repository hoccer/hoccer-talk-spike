package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestHelper;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;

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

        TestHelper.sendMessage(sendingClient, receivingClient, messageText);
    }

    @Test
    public void clientMessageTestOfflineRecipient() throws Exception {
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");
        TestHelper.pairClients(sendingClient, receivingClient);

        // Taking recipient offline
        receivingClient.disconnect();
        await("receivingClient is inactive").untilCall(to(receivingClient).getState(), equalTo(XoClient.STATE_DISCONNECTED));

        try {
            TestHelper.sendMessage(sendingClient, receivingClient, messageText);
        } catch (Exception e) {
            // expected to fail (with ConditionTimeoutException)

            // TODO: check that the recipient does not receive the message rather then wait 10secs

            // Rejected messages are not persisted on the server. Due to the nature of the blocking mechanism,
            // that the sender doesn't know whether a message was blocked, we can not test for rejected messages.
            // TODO: We need a callback from the TalkRpcHandler when a message was rejected
        }

        // Taking recipient online again
        receivingClient.connect();
        await("receivingClient reaches active state").untilCall(to(receivingClient).getState(), equalTo(XoClient.STATE_READY));

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
    public void clientMessageTestRecipientBlockedSender() throws SQLException, InterruptedException {
        final XoClient sendingClient = clients.get("client1");
        final XoClient receivingClient = clients.get("client2");
        TestHelper.pairClients(sendingClient, receivingClient);

        // Recipient blocks sender
        TestHelper.blockClient(receivingClient, sendingClient);

        try {
            TestHelper.sendMessage(sendingClient, receivingClient, "blocked");
        } catch (Exception e) {
            // expected to fail (with ConditionTimeoutException)

            // TODO: check that the recipient does not receive the message rather then wait 10secs

            // Rejected messages are not persisted on the server. Due to the nature of the blocking mechanism,
            // that the sender doesn't know whether a message was blocked, we can not test for rejected messages.
            // TODO: We need a callback from the TalkRpcHandler when a message was rejected
        }

        // Recipient unblocks sender
        TestHelper.unblockClient(receivingClient, sendingClient);

        TestHelper.sendMessage(sendingClient, receivingClient, "unblocked");
    }
}
