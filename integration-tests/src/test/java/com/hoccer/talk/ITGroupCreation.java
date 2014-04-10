package com.hoccer.talk;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
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

import java.sql.SQLException;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ITGroupCreation extends IntegrationTest {

    private TestTalkServer firstServer;

    @Before
    public void setUp() throws Exception {
        firstServer = createTalkServer();
    }

    @After
    public void tearDown() throws Exception {
        firstServer.shutdown();
    }

    @Test
    public void createGroupTest() throws Exception {
        // create client
        final XoClient client = createTalkClient(firstServer);

        // test waking and connecting
        assertFalse(client.isAwake());
        client.wake();
        assertTrue(client.isAwake());

        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return XoClient.STATE_ACTIVE == client.getState();
            }
        }, Timeout.timeout(seconds(2)));

        /* TODO: ideally this new group and presence creation stuff and eventually calling createGroup should be more graceful in the clients and disappear form this test entirely */
        TalkClientContact newGroup = TalkClientContact.createGroupContact();
        final String groupTag = newGroup.getGroupTag();

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(newGroup.getGroupTag());
        newGroup.updateGroupPresence(groupPresence);

        client.createGroup(newGroup);

        waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    return client.getDatabase().findContactByGroupTag(groupTag) != null;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }, Timeout.timeout(seconds(2)));
    }

}
