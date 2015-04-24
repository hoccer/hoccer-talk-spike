package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestHelper;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ITWorldWide extends IntegrationTest {
    private TestTalkServer talkServer;
    private HashMap<String, XoClient> clients;

    @Before
    public void setUp() throws Exception {
        talkServer = createTalkServer();
        clients = TestHelper.initializeTalkClients(talkServer, 2);
    }

    @After
    public void tearDown() throws Exception {
        TestHelper.shutdownClients(clients);
        talkServer.shutdown();
    }

    @Test
    public void activateWorldwide() throws Exception {
        final XoClient client1 = clients.get("client1");
        final XoClient client2 = clients.get("client2");
        TestHelper.setEnvironmentToWorldwide(client1);

        TalkEnvironment client1Env = talkServer.getTalkServer().getDatabase().findEnvironmentByClientId(
                TalkEnvironment.TYPE_WORLDWIDE,
                client1.getSelfContact().getClientId()
        );
        assertNotNull(client1Env);

        assertEquals(1,talkServer.getTalkServer().getDatabase().findEnvironmentsForGroup(
                client1.getCurrentWorldwideGroup().getGroupId()
        ).size());

        TestHelper.setEnvironmentToWorldwide(client2);
        TalkEnvironment client2Env = talkServer.getTalkServer().getDatabase().findEnvironmentByClientId(
                TalkEnvironment.TYPE_WORLDWIDE,
                client1.getSelfContact().getClientId()
        );
        assertNotNull(client2Env);

        assertEquals(2,talkServer.getTalkServer().getDatabase().findEnvironmentsForGroup(
                client1.getCurrentWorldwideGroup().getGroupId()
        ).size());
    }
}
