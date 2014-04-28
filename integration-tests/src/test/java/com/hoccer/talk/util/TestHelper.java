package com.hoccer.talk.util;


import com.hoccer.talk.client.XoClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;


public class TestHelper {

    public static XoClient createTalkClient(TestTalkServer server) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new XoClient(new TestClientHost(server));
    }

    public static HashMap<String, XoClient> initializeTalkClients(TestTalkServer server,
                                                           int amount) throws Exception {
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

        await("client 1 is paired with client 2").untilCall(to(client1.getDatabase()).findContactByClientId(client1Id, false), notNullValue());
        await("client 1 has client 2's pubkey").untilCall(to(client1.getDatabase().findContactByClientId(client1Id, false)).getPublicKey(), notNullValue());

        await("client 2 is paired with client 1").untilCall(to(client2.getDatabase()).findContactByClientId(client2Id, false), notNullValue());
        await("client 2 has client 1's pubkey").untilCall(to(client2.getDatabase().findContactByClientId(client2Id, false)).getPublicKey(), notNullValue());
    }
}
