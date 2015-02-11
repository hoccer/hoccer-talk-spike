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

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;

@RunWith(JUnit4.class)
public class ITGroupJoin extends IntegrationTest {

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
        TestHelper.shutdownClients(clients);
    }

    @Test
    public void joinGroupTest() throws Exception {
        // create clients
        XoClient invitingClient = clients.get("client1");
        XoClient invitedClient = clients.get("client2");

        // create group
        String groupId = TestHelper.createGroup(invitingClient, "group");

        // invite to group
        TestHelper.inviteToGroup(invitingClient, invitedClient, groupId);

        // join group
        TestHelper.joinGroup(invitedClient, groupId);
    }

    @Test
    public void joinGroupTestWhileAdminIsOffline() throws Exception {
        XoClient invitingClient = clients.get("client1");
        XoClient invitedClient = clients.get("client2");

        // create group
        String groupId = TestHelper.createGroup(invitingClient, "group");

        // invite to group
        TestHelper.inviteToGroup(invitingClient, invitedClient, groupId);

        // take inviting client offline
        invitingClient.disconnect();
        await("invitingClient is inactive").untilCall(to(invitingClient).getState(), equalTo(XoClient.STATE_DISCONNECTED));

        // invited client joins group
        TestHelper.joinGroup(invitedClient, groupId);
    }
}
