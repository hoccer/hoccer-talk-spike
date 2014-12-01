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
public class ITGroupCreation extends IntegrationTest {

    private TestTalkServer firstServer;
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(firstServer, 1);
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
        TestHelper.shutdownClients(clients);
    }

    @Test
    public void createGroupTest() throws Exception {
        XoClient client = clients.get("client1");
        TestHelper.createGroup(client, "group");
    }
}
