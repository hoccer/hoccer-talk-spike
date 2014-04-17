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

import static com.jayway.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

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
        client.wake();

        await("client is active").untilCall(to(client).getState(), equalTo(XoClient.STATE_ACTIVE));

        /* TODO: ideally this new group and presence creation stuff and eventually calling createGroup should be more graceful in the clients and disappear form this test entirely */
        TalkClientContact newGroup = TalkClientContact.createGroupContact();
        final String groupTag = newGroup.getGroupTag();

        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(newGroup.getGroupTag());
        newGroup.updateGroupPresence(groupPresence);

        client.createGroup(newGroup);
        await("client knows about created group").untilCall(to(client.getDatabase()).findContactByGroupTag(groupTag), notNullValue());
        final String groupId = client.getDatabase().findContactByGroupTag(groupTag).getGroupId();
        assertNotNull(groupId);

        // test disconnecting
        client.deactivate();
        await("client is inactive").untilCall(to(client).getState(), equalTo(XoClient.STATE_INACTIVE));
    }

}
