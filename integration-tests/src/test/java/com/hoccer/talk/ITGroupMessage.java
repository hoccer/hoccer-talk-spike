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
        XoClient client3 = clients.get("client3");

        // create group
        String groupId = TestHelper.createGroup(client1);

        // invite to group
        TestHelper.inviteToGroup(client1, client2, groupId);
        TestHelper.inviteToGroup(client1, client3, groupId);

        // join group
        TestHelper.joinGroup(client2, groupId);
        TestHelper.joinGroup(client3, groupId);

        // TODO: implement actual message sending
    }
}
