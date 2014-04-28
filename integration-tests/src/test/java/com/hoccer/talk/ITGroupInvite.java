package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ITGroupInvite extends IntegrationTest {

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
    public void inviteGroupTest() throws Exception {
        // assigning clients
        XoClient invitingClient = clients.get("client1");
        XoClient invitedClient = clients.get("client2");

        // create group
        String groupId = TestHelper.createGroup(invitingClient);

        // invite group
        TestHelper.inviteToGroup(invitingClient, invitedClient, groupId);
    }

}
