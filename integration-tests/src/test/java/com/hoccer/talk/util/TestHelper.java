package com.hoccer.talk.util;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;


public class TestHelper {

    public static XoClient createTalkClient(TestTalkServer server) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new XoClient(new TestClientHost(), new TestClientConfiguration(server));
    }

    public static HashMap<String, XoClient> initializeTalkClients(TestTalkServer server, int amount) throws Exception {
        final HashMap<String, XoClient> clients = new HashMap<String, XoClient>();

        for (int i = 0; i < amount; i++) {
            XoClient client = createTalkClient(server);
            String clientName = "client" + (i + 1);

            client.wake();

            await(clientName + " reaches active state").untilCall(to(client).getState(), equalTo(XoClient.STATE_ACTIVE));
            clients.put(clientName, client);
        }

        return clients;
    }

    public static void shutdownClients(HashMap<String, XoClient> clients) {
        for (Map.Entry<String, XoClient> entry : clients.entrySet()) {
            XoClient client = entry.getValue();
            assertNotNull(client);
            client.deactivate();
            await(entry.getKey() + " is inactive").untilCall(to(client).getState(), equalTo(XoClient.STATE_INACTIVE));
        }
    }

    public static void pairClients(XoClient client1, XoClient client2) throws SQLException {
        final String token = client1.generatePairingToken();
        client2.performTokenPairing(token);

        final String client1Id = client1.getSelfContact().getClientId();
        final String client2Id = client2.getSelfContact().getClientId();

        await("client 1 is paired with client 2").untilCall(to(client1.getDatabase()).findContactByClientId(client2Id, false), notNullValue());
        await("client 1 has client 2's pubkey").untilCall(to(client1.getDatabase().findContactByClientId(client2Id, false)).getPublicKey(), notNullValue());

        await("client 2 is paired with client 1").untilCall(to(client2.getDatabase()).findContactByClientId(client1Id, false), notNullValue());
        await("client 2 has client 1's pubkey").untilCall(to(client2.getDatabase().findContactByClientId(client1Id, false)).getPublicKey(), notNullValue());
    }

    public static String createGroup(XoClient client) throws SQLException {
        /* TODO: ideally this new group and presence creation stuff and eventually calling createGroup should be more graceful in the clients and disappear form this test entirely */
        TalkClientContact newGroup = TalkClientContact.createGroupContact();
        final String groupTag = newGroup.getGroupTag();

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(newGroup.getGroupTag());
        newGroup.updateGroupPresence(groupPresence);

        client.createGroup(newGroup);
        await("client knows about created group").untilCall(to(client.getDatabase()).findContactByGroupTag(groupTag), notNullValue());
        await("created group has a group id").untilCall(to(client.getDatabase().findContactByGroupTag(groupTag)).getGroupId(), notNullValue());

        final String groupId = client.getDatabase().findContactByGroupTag(groupTag).getGroupId();
        return groupId;
    }

    public static void inviteToGroup(XoClient invitingClient, XoClient invitedClient, String groupId) throws SQLException, InterruptedException {
        await("invitingClient knows group via groupId").untilCall(to(invitingClient.getDatabase()).findGroupContactByGroupId(groupId, false), notNullValue());
        invitingClient.inviteClientToGroup(groupId, invitedClient.getSelfContact().getClientId());

        await("invitedClient knows group via groupId").untilCall(to(invitedClient.getDatabase()).findGroupContactByGroupId(groupId, false), notNullValue());
        TalkClientContact groupContactOfInvitedClient = invitedClient.getDatabase().findGroupContactByGroupId(groupId, false);

        await("invitedClient has received group member update").untilCall(to(groupContactOfInvitedClient).getGroupMember(), notNullValue());
        assertTrue("invitedClient is invited to group", groupContactOfInvitedClient.getGroupMember().isInvited());
        assertEquals("invitedClient membership is actually the invitedClient", groupContactOfInvitedClient.getGroupMember().getClientId(), invitedClient.getSelfContact().getClientId());

        // only when the invited client has received the shared group key, it is safe to proceed, e.g. to join the group
        await("invitedClient has a group key").untilCall(to(groupContactOfInvitedClient.getGroupMember()).getSharedKeyId(), notNullValue());
    }

    public static void joinGroup(final XoClient joiningClient, final String groupId) {
        joiningClient.joinGroup(groupId);

        await("client is joined").until(
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        TalkGroupMember groupMember = joiningClient.getDatabase().findGroupContactByGroupId(groupId, false).getGroupMember();
                        return TalkGroupMember.STATE_JOINED.equals(groupMember.getState()) &&
                                groupMember.isJoined() &&
                                groupMember.getEncryptedGroupKey() != null &&
                                groupMember.getMemberKeyId() != null;
                    }
                }
        );
    }

    public static void blockClient(final XoClient blockingClient, final XoClient blockedClient) throws SQLException {
        final String blockedClientId = blockedClient.getSelfContact().getClientId();
        blockingClient.blockContact(blockingClient.getDatabase().findContactByClientId(blockedClientId, false));

        await("client is blocked").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return blockingClient.getDatabase().findContactByClientId(blockedClientId, false).getClientRelationship().isBlocked();
            }
        });
    }

    public static void unblockClient(final XoClient client, final XoClient clientToUnblock) throws SQLException {
        final String clientId = clientToUnblock.getSelfContact().getClientId();
        client.unblockContact(client.getDatabase().findContactByClientId(clientId, false));

        await("client is unblocked").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !client.getDatabase().findContactByClientId(clientId, false).getClientRelationship().isBlocked();
            }
        });
    }

    public static void sendMessage(final XoClient sendingClient, final XoClient receivingClient, final String messageText) throws SQLException {
        assertNotNull(sendingClient);
        assertNotNull(receivingClient);
        assertNotNull(messageText);

        final int previousMsgCount = receivingClient.getDatabase().findUnseenMessages().size();

        // sendingClient sends a messages to receivingClient
        TalkClientContact recipientContact = sendingClient.getDatabase().findContactByClientId(receivingClient.getSelfContact().getClientId(), false);
        assertNotNull(recipientContact);
        TalkClientMessage message = sendingClient.composeClientMessage(recipientContact, messageText);
        sendingClient.sendMessage(message.getMessageTag());

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<TalkClientMessage> unseenMessages = receivingClient.getDatabase().findUnseenMessages();
                return unseenMessages != null &&
                        unseenMessages.size() == (previousMsgCount + 1) &&
                        !unseenMessages.get(0).isInProgress() &&
                        messageText.equals(unseenMessages.get(0).getText());
            }
        });
    }
}
