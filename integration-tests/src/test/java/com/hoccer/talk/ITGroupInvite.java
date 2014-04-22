package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.util.IntegrationTest;
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
        clients = initializeTalkClients(firstServer, 2);
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
        shutdownClients(clients);
    }

    @Test
    public void inviteGroupTest() throws Exception {
        // create clients
        XoClient invitingClient = clients.get("client1");
        XoClient invitedClient = clients.get("client2");

        /* TODO: ideally this new group and presence creation stuff and eventually calling createGroup should be more graceful in the clients and disappear form this test entirely */
        TalkClientContact newGroup = TalkClientContact.createGroupContact();
        final String groupTag = newGroup.getGroupTag();

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(newGroup.getGroupTag());
        newGroup.updateGroupPresence(groupPresence);

        invitingClient.createGroup(newGroup);
        await("client knows about created group").untilCall(to(invitingClient.getDatabase()).findContactByGroupTag(groupTag), notNullValue());
        final String groupId = invitingClient.getDatabase().findContactByGroupTag(groupTag).getGroupId();
        assertNotNull(groupId);

        await("invitingClient knows group via groupId").untilCall(to(invitingClient.getDatabase()).findContactByGroupId(groupId, false), notNullValue());

        invitingClient.inviteClientToGroup(groupId, invitedClient.getSelfContact().getClientId());

        await("invitedClient knows group via groupId").untilCall(to(invitedClient.getDatabase()).findContactByGroupId(groupId, false), notNullValue());

        TalkClientContact groupContact = invitedClient.getDatabase().findContactByGroupId(groupId, false);
        assertTrue("invitedClient is invited to group", groupContact.getGroupMember().isInvited());
        assertEquals("invited client membership is actually the invitedClient", groupContact.getGroupMember().getClientId(), invitedClient.getSelfContact().getClientId());
    }

}
