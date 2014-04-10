package com.hoccer.talk;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
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

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;

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

        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return XoClient.STATE_ACTIVE == c1.getState();
            }
        }, Timeout.timeout(seconds(2)));
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return XoClient.STATE_ACTIVE == c2.getState();
            }
        }, Timeout.timeout(seconds(2)));

        // STATE_ACTIVE does not necessarily mean we are finished with generating pub/private keys (dumb i know)
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return c2.getDatabase().findSelfContact(false).getPrivateKey() != null;
                } catch (SQLException e) {
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return c1.getDatabase().findSelfContact(false).getPrivateKey() != null;
                } catch (SQLException e) {
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));

        String token = c1.generatePairingToken();
        c2.performTokenPairing(token);

        final String c1Id = c1.getSelfContact().getClientId();
        final String c2Id = c2.getSelfContact().getClientId();

        // ensure c1 is paired with c2
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return c1.getDatabase().findContactByClientId(c2Id, false) != null &&
                           c1.getDatabase().findContactByClientId(c2Id, false).getPublicKey() != null;
                } catch (SQLException e) {
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));

        // ensure c2 is paired with c1
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return c2.getDatabase().findContactByClientId(c1Id, false) != null &&
                           c2.getDatabase().findContactByClientId(c1Id, false).getPublicKey() != null;
                } catch (SQLException e) {
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));

        // c1 sends a messages to c2
        TalkClientContact recipient = c1.getDatabase().findContactByClientId(c2.getSelfContact().getClientId(), false);
        TalkClientMessage message = c1.composeClientMessage(recipient, messageText);
        c1.requestDelivery(message);

        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return c2.getDatabase().findUnseenMessages().size() != 0;
                } catch (SQLException e) {
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));

        final List<TalkClientMessage> unseenMessages = c2.getDatabase().findUnseenMessages();

        assertTrue(unseenMessages.size() == 1);
        assertTrue(unseenMessages.get(0).getText() == messageText);
    }
}
