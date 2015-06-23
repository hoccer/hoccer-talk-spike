package com.hoccer.talk;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.model.TalkGroupMembership;
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
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class ITWorldWideManyClients extends IntegrationTest {

    private TestTalkServer talkServer;
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        talkServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(talkServer, 30);
    }

    @After
    public void tearDown() throws Exception {
        TestHelper.shutdownClients(clients);
        talkServer.shutdown();
    }

    @Test
    public void threeGroupsAreMergedDownToTwo() throws Exception {
        activateWorldwideFor(new String[]{
            "client1", "client2", "client3", "client4", "client5", "client6", "client7", "client8", "client9", "client10",
            "client11", "client12", "client13", "client14", "client15", "client16", "client17", "client18", "client19", "client20",
            "client21", "client22", "client23", "client24", "client25", "client26", "client27", "client28", "client29", "client30"
        });

        final XoClient client9 = clients.get("client9");
        final XoClient client19 = clients.get("client19");
        final XoClient client28 = clients.get("client28");
        final XoClient client29 = clients.get("client29");
        final XoClient client30 = clients.get("client30");
        final ITalkServerDatabase serverDb = talkServer.getTalkServer().getDatabase();

        assertEquals(
                10,
                serverDb.findGroupMembershipsByIdWithStates(
                        client9.getCurrentWorldwideGroup().getGroupId(),
                        TalkGroupMembership.ACTIVE_STATES
                ).size()
        );

        assertEquals(
                10,
                serverDb.findGroupMembershipsByIdWithStates(
                        client19.getCurrentWorldwideGroup().getGroupId(),
                        TalkGroupMembership.ACTIVE_STATES
                ).size()
        );

        assertEquals(
                10,
                serverDb.findGroupMembershipsByIdWithStates(
                        client29.getCurrentWorldwideGroup().getGroupId(),
                        TalkGroupMembership.ACTIVE_STATES
                ).size()
        );

        // Now deactivate worldwide for 1-3, 11-15 & 21-27
        deactivateWorldwideFor(new String[]{
                "client1", "client2", "client3",
                "client11", "client12", "client13", "client14", "client15",
                "client21", "client22", "client23", "client24", "client25", "client26", "client27"
        });

        assertEquals(
                7,
                serverDb.findGroupMembershipsByIdWithStates(
                        client9.getCurrentWorldwideGroup().getGroupId(),
                        TalkGroupMembership.ACTIVE_STATES
                ).size()
        );

        assertEquals(
                5,
                serverDb.findGroupMembershipsByIdWithStates(
                        client19.getCurrentWorldwideGroup().getGroupId(),
                        TalkGroupMembership.ACTIVE_STATES
                ).size()
        );

        assertEquals(
                3,
                serverDb.findGroupMembershipsByIdWithStates(
                        client29.getCurrentWorldwideGroup().getGroupId(),
                        TalkGroupMembership.ACTIVE_STATES
                ).size()
        );

        final String oldGroupIdForClient29 = client29.getCurrentWorldwideGroup().getGroupId();
        final String oldGroupIdForClient28 = client28.getCurrentWorldwideGroup().getGroupId();
        final String oldGroupIdForClient30 = client30.getCurrentWorldwideGroup().getGroupId();
        /*
        System.out.print("OLD --------------\n\n\n");
        System.out.println(oldGroupIdForClient28);
        System.out.println(oldGroupIdForClient29);
        System.out.println(oldGroupIdForClient30);
        System.out.print("\n\n\n--------------");
        */

        // reupdate worldwide for the three remaining clients in the smallest group
        // this leads to the reevaluation of their memberships.
        activateWorldwideFor(new String[]{
                "client28", "client29", "client30",
        });

        await("client28 is now in a different worldwide group").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TalkClientContact newGroup = client28.getCurrentWorldwideGroup();
                return !newGroup.getGroupId().equals(oldGroupIdForClient28);
            }
        });
        await("client29 is now in a different worldwide group").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TalkClientContact newGroup = client29.getCurrentWorldwideGroup();
                return !newGroup.getGroupId().equals(oldGroupIdForClient29);
            }
        });
        await("client30 is now in a different worldwide group").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TalkClientContact newGroup = client30.getCurrentWorldwideGroup();
                return !newGroup.getGroupId().equals(oldGroupIdForClient30);
            }
        });

        /*
        System.out.print("NEW --------------\n\n\n");
        System.out.println("client28: " + client28.getCurrentWorldwideGroup().getGroupId());
        System.out.println("client29: " + client29.getCurrentWorldwideGroup().getGroupId());
        System.out.println("client30: " + client30.getCurrentWorldwideGroup().getGroupId());
        System.out.println("client19: " + client19.getCurrentWorldwideGroup().getGroupId());
        System.out.println("client9: " + client9.getCurrentWorldwideGroup().getGroupId());
        System.out.print("\n\n\n--------------");
        */

        assertEquals(
                client19.getCurrentWorldwideGroup().getGroupId(),
                client28.getCurrentWorldwideGroup().getGroupId()
        );
        assertEquals(
                client19.getCurrentWorldwideGroup().getGroupId(),
                client29.getCurrentWorldwideGroup().getGroupId()
        );


        // TODO: Actually it is random into which of the two remaining groups this client get put since they both now have the same size
        /*
        assertEquals(
                client19.getCurrentWorldwideGroup().getGroupId(),
                client30.getCurrentWorldwideGroup().getGroupId()
        );
        */
        // Now we only have 2 worldwide groups
    }

    private void deactivateWorldwideFor(final String[] clientIds) {
        XoClient client;
        for (final String clientId : clientIds) {
            client = clients.get(clientId);
            TestHelper.leaveEnvironmentWorldwide(client);
        }
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
