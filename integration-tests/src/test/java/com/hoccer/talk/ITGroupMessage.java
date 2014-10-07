package com.hoccer.talk;


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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ITGroupMessage extends IntegrationTest {

    private TestTalkServer firstServer;
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(firstServer, 3);
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
        TestHelper.shutdownClients(clients);
    }

    @Test
    public void groupMessageTest() throws Exception {
        // create clients
        XoClient client1 = clients.get("client1");
        XoClient client2 = clients.get("client2");

        // create group
        String groupId = TestHelper.createGroup(client1);

        assertNull("client2 is unknown for client1", client1.getDatabase().findContactByClientId(client2.getSelfContact().getClientId(), false));

        // invite to group
        TestHelper.inviteToGroup(client1, client2, groupId);

        // send message c1 -> c2
        try {
            TestHelper.sendMessage(client1, client2, "still no relationship");
        } catch (Exception e) {
            // expected to fail (with ConditionTimeoutException)

            // TODO: check that the recipient does not receive the message rather then wait 10secs

            // Rejected messages are not persisted on the server. Due to the nature of the blocking mechanism,
            // that the sender doesn't know whether a message was blocked, we can not test for rejected messages.
            // TODO: We need a callback from the TalkRpcHandler when a message was rejected
        }

        // c2 join group
        TestHelper.joinGroup(client2, groupId);

        assertTrue("no messages delivered so far", client2.getDatabase().findUnseenMessages().isEmpty());

        // send message c1 -> c2
        TestHelper.sendMessage(client1, client2, "relationship via group");

        // send message c2 -> c1
        TestHelper.sendMessage(client2, client1, "relationship via group");
    }

/*
    @Test
    public void groupMessageBlockTest() throws Exception {
        // create clients
        XoClient client1 = clients.get("client1");
        XoClient client2 = clients.get("client2");

        // create group
        String groupId = TestHelper.createGroup(client1);

        // invite to group
        TestHelper.inviteToGroup(client1, client2, groupId);

        // c2 join group
        TestHelper.joinGroup(client2, groupId);

        // c1 blocks c2
        TestHelper.blockClient(client1, client2);

        // send message c1 -> c2
        TestHelper.sendMessage(client1, client2, "relationship via group");

        // send message c2 -> c1
        try {
            TestHelper.sendMessage(client2, client1, "should be blocked");
        } catch (Exception e) {
            // expected to fail (with ConditionTimeoutException)

            // TODO: check that the recipient does not receive the message rather then wait 10secs

            // Rejected messages are not persisted on the server. Due to the nature of the blocking mechanism,
            // that the sender doesn't know whether a message was blocked, we can not test for rejected messages.
            // TODO: We need a callback from the TalkRpcHandler when a message was rejected
        }
    }
*/
}
