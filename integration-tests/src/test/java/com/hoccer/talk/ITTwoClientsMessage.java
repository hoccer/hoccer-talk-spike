package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.jayway.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.List;


@RunWith(JUnit4.class)
public class ITTwoClientsMessage extends IntegrationTest {

    private TestTalkServer firstServer;

    private final static String messageText = "test";

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
    }

    @Test
    public void clientMessageTest() throws Exception {
        final XoClient c1 = createTalkClient(firstServer);
        c1.wake();
        final XoClient c2 = createTalkClient(firstServer);
        c2.wake();

        await("client 1 is active").untilCall(to(c1).getState(), equalTo(XoClient.STATE_ACTIVE));
        await("client 2 is active").untilCall(to(c2).getState(), equalTo(XoClient.STATE_ACTIVE));

        // STATE_ACTIVE does not necessarily mean we are finished with generating pub/private keys (dumb i know)
        await("client 1 has private key").untilCall(to(c1.getDatabase().findSelfContact(false)).getPrivateKey(), notNullValue());
        await("client 2 has private key").untilCall(to(c2.getDatabase().findSelfContact(false)).getPrivateKey(), notNullValue());

        String token = c1.generatePairingToken();
        c2.performTokenPairing(token);

        final String c1Id = c1.getSelfContact().getClientId();
        final String c2Id = c2.getSelfContact().getClientId();

        await("client 1 is paired with client 2").untilCall(to(c1.getDatabase()).findContactByClientId(c2Id, false), notNullValue());
        await("client 1 has client 2's pubkey").untilCall(to(c1.getDatabase().findContactByClientId(c2Id, false)).getPublicKey(), notNullValue());

        await("client 2 is paired with client 1").untilCall(to(c2.getDatabase()).findContactByClientId(c1Id, false), notNullValue());
        await("client 2 has client 1's pubkey").untilCall(to(c2.getDatabase().findContactByClientId(c1Id, false)).getPublicKey(), notNullValue());

        // c1 sends a messages to c2
        TalkClientContact recipient = c1.getDatabase().findContactByClientId(c2.getSelfContact().getClientId(), false);
        TalkClientMessage message = c1.composeClientMessage(recipient, messageText);
        c1.requestDelivery(message);

        await("receiver has unseen messages").untilCall(to(c2.getDatabase()).findUnseenMessages(), is(not(empty())));

        final List<TalkClientMessage> unseenMessages = c2.getDatabase().findUnseenMessages();
        assertEquals(1, unseenMessages.size());

        // wait until message is done downloading
        await("unseen message is done downloading").untilCall(to(unseenMessages.get(0)).isInProgress(), is(false));
        assertEquals("unseen message text matches", unseenMessages.get(0).getText(), messageText);

        // disconnect clients
        c1.deactivate();
        c2.deactivate();
        await("client 1 is inactive").untilCall(to(c1).getState(), equalTo(XoClient.STATE_INACTIVE));
        await("client 2 is inactive").untilCall(to(c2).getState(), equalTo(XoClient.STATE_INACTIVE));


    }
}
