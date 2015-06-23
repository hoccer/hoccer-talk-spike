package com.hoccer.talk.util;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.model.TalkGroupMembership;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.sql.SQLException;
import java.util.Date;
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

            client.connect();

            await(clientName + " reaches active state").untilCall(to(client).getState(), equalTo(XoClient.State.READY));
            clients.put(clientName, client);
        }

        return clients;
    }

    public static void shutdownClients(HashMap<String, XoClient> clients) {
        for (Map.Entry<String, XoClient> entry : clients.entrySet()) {
            XoClient client = entry.getValue();
            assertNotNull(client);
            client.disconnect();
            await(entry.getKey() + " is inactive").untilCall(to(client).getState(), equalTo(XoClient.State.DISCONNECTED));
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

    public static String createGroup(XoClient client, String name) throws SQLException {
        String groupTag = client.createGroup(name);
        await("client knows about created group").untilCall(to(client.getDatabase()).findContactByGroupTag(groupTag), notNullValue());

        String groupId = client.getDatabase().findContactByGroupTag(groupTag).getGroupId();
        return groupId;
    }

    public static void inviteToGroup(XoClient invitingClient, XoClient invitedClient, String groupId) throws SQLException, InterruptedException {
        await("invitingClient knows group via groupId").untilCall(to(invitingClient.getDatabase()).findGroupContactByGroupId(groupId, false), notNullValue());
        invitingClient.inviteClientToGroup(groupId, invitedClient.getSelfContact().getClientId());

        await("invitedClient knows group via groupId").untilCall(to(invitedClient.getDatabase()).findGroupContactByGroupId(groupId, false), notNullValue());
        TalkClientContact groupContactOfInvitedClient = invitedClient.getDatabase().findGroupContactByGroupId(groupId, false);

        await("invitedClient has received group member update").untilCall(to(groupContactOfInvitedClient).getGroupMembership(), notNullValue());
        assertTrue("invitedClient is invited to group", groupContactOfInvitedClient.getGroupMembership().isInvited());
        assertEquals("invitedClient membership is actually the invitedClient", groupContactOfInvitedClient.getGroupMembership().getClientId(), invitedClient.getSelfContact().getClientId());

        // only when the invited client has received the shared group key, it is safe to proceed, e.g. to join the group
        await("invitedClient has a group key").untilCall(to(groupContactOfInvitedClient.getGroupMembership()).getSharedKeyId(), notNullValue());
    }

    public static void joinGroup(final XoClient joiningClient, final String groupId) {
        joiningClient.joinGroup(groupId);

        await("client is joined").until(
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        TalkGroupMembership membership = joiningClient.getDatabase().findGroupContactByGroupId(groupId, false).getGroupMembership();
                        return membership.isJoined() &&
                                membership.getEncryptedGroupKey() != null &&
                                membership.getMemberKeyId() != null;
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

    public static void leaveEnvironmentWorldwide(final XoClient theClient) {
        assertNotNull(theClient);
        theClient.sendDestroyEnvironment(TalkEnvironment.TYPE_WORLDWIDE);
        await("client is not in worldwide group anymore").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TalkClientContact group = theClient.getCurrentWorldwideGroup();
                return group == null;
            }
        });
    }

    public static void setEnvironmentToWorldwide(final XoClient theClient) {
        setEnvironmentToWorldwide(theClient, "*");
    }

    public static void setEnvironmentToWorldwide(final XoClient theClient, final String tag) {
        assertNotNull(theClient);
        assertNotNull(tag);
        final TalkEnvironment environment = new TalkEnvironment();
        environment.setTimestamp(new Date());
        environment.setType(TalkEnvironment.TYPE_WORLDWIDE);
        environment.setTag(tag);
        theClient.sendEnvironmentUpdate(environment);

        await("client has received a worldwide group").until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                TalkClientContact group = theClient.getCurrentWorldwideGroup();
                return group != null;
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
