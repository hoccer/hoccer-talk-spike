package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestHelper;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class ITWorldWide extends IntegrationTest {

    private TestTalkServer talkServer;
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        talkServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(talkServer, 11);
    }

    @After
    public void tearDown() throws Exception {
        TestHelper.shutdownClients(clients);
        talkServer.shutdown();
    }

    @Test
    public void activateWorldwideOneClient() throws Exception {
        final XoClient client1 = clients.get("client1");
        final ITalkServerDatabase serverDb = talkServer.getTalkServer().getDatabase();

        TestHelper.setEnvironmentToWorldwide(client1);

        assertNotNull(serverDb.findEnvironmentByClientId(
                TalkEnvironment.TYPE_WORLDWIDE,
                client1.getSelfContact().getClientId()
        ));

        assertEquals(
                1,
                serverDb.findEnvironmentsForGroup(
                        client1.getCurrentWorldwideGroup().getGroupId()
                ).size()
        );

        assertEquals(
                1,
                serverDb.findGroupMembershipsById(
                        client1.getCurrentWorldwideGroup().getGroupId()
                ).size()
        );
    }

    @Test
    public void activateWorldwideUntilTwoGroups() throws Exception {
        activateWorldwideFor(new String[]{"client1", "client2", "client3", "client4", "client5", "client6",
                "client7", "client8", "client9", "client10"});

        // The first client should be together in one group with the other nine.
        final XoClient client1 = clients.get("client1");
        final ITalkServerDatabase serverDb = talkServer.getTalkServer().getDatabase();

        assertEquals(
                10,
                serverDb.findEnvironmentsForGroup(
                        client1.getCurrentWorldwideGroup().getGroupId()
                ).size()
        );

        assertEquals(
                10,
                serverDb.findGroupMembershipsById(
                        client1.getCurrentWorldwideGroup().getGroupId()
                ).size()
        );

        // 11th client should be in a new group
        activateWorldwideFor(new String[]{"client11"});
        final XoClient client11 = clients.get("client11");
        assertEquals(
                1,
                serverDb.findEnvironmentsForGroup(
                        client11.getCurrentWorldwideGroup().getGroupId()
                ).size()
        );
    }

    private void activateWorldwideFor(final String[] clientIds) {
        XoClient client;
        for (final String clientId : clientIds) {
            client = clients.get(clientId);
            TestHelper.setEnvironmentToWorldwide(client);
        }
        for (final String clientId : clientIds) {
            client = clients.get(clientId);
            final TalkEnvironment clientEnv = talkServer.getTalkServer().getDatabase().findEnvironmentByClientId(
                    TalkEnvironment.TYPE_WORLDWIDE,
                    client.getSelfContact().getClientId()
            );
            //talkServer.getTalkServer().getDatabase().findGroupMembershipsById(client.getCurrentWorldwideGroup().getGroupId())
            assertNotNull(clientEnv);
        }
    }
}
