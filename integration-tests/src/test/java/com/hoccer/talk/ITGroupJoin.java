package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
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
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        /* TODO: ideally this new group and presence creation stuff and eventually calling createGroup should be more graceful in the clients and disappear form this test entirely */
        TalkClientContact newGroup = TalkClientContact.createGroupContact();
        final String groupTag = newGroup.getGroupTag();

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(newGroup.getGroupTag());
        newGroup.updateGroupPresence(groupPresence);

        invitingClient.createGroup(newGroup);
        await("invitingClient knows about created group").untilCall(to(invitingClient.getDatabase()).findContactByGroupTag(groupTag), notNullValue());
        final String groupId = invitingClient.getDatabase().findContactByGroupTag(groupTag).getGroupId();
        assertNotNull(groupId);

        // invite to group
        await("invitingClient knows group via groupId").untilCall(to(invitingClient.getDatabase()).findContactByGroupId(groupId, false), notNullValue());

        invitingClient.inviteClientToGroup(groupId, invitedClient.getSelfContact().getClientId());

        await("invitedClient knows group via groupId").untilCall(to(invitedClient.getDatabase()).findContactByGroupId(groupId, false), notNullValue());

        TalkClientContact groupContact = invitedClient.getDatabase().findContactByGroupId(groupId, false);
        assertTrue("invitedClient is invited to group", groupContact.getGroupMember().isInvited());
        assertEquals("invited client membership is actually the invitedClient", groupContact.getGroupMember().getClientId(), invitedClient.getSelfContact().getClientId());

        // join group
        invitedClient.joinGroup(groupId);
        await("invited client is joined").until(joinComplete(invitedClient, groupId));
    }

    private Callable<Boolean> joinComplete(final XoClient invitedClient, final String groupId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TalkGroupMember groupMember = invitedClient.getDatabase().findContactByGroupId(groupId, false).getGroupMember();
                return TalkGroupMember.STATE_JOINED.equals(groupMember.getState()) &&
                       groupMember.isJoined() &&
                       groupMember.getEncryptedGroupKey() != null &&
                       groupMember.getMemberKeyId() != null;
            }
        };
    }

}
