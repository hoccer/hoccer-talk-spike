package com.hoccer.talk;

// import junit stuff

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
import com.hoccer.talk.client.XoClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.SQLException;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;

@RunWith(JUnit4.class)
public class ITTwoClientsPairing extends IntegrationTest {

    private TestServer firstServer;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
    }

    @Test
    public void clientPairTest() throws Exception {
        // create two clients
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

        String token = c1.generatePairingToken();
        c2.performTokenPairing(token);

        final String c1Id = c1.getSelfContact().getClientId();
        final String c2Id = c2.getSelfContact().getClientId();

        // ensure c1 is paired with c2
        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return c1.getDatabase().findContactByClientId(c2Id, false) != null;
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
                    return c2.getDatabase().findContactByClientId(c1Id, false) != null;
                } catch (SQLException e) {
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));

    }

}

