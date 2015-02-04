package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestFileCache;
import com.hoccer.talk.util.TestHelper;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(JUnit4.class)
public class ITSingleFileClient extends IntegrationTest {

    private TestTalkServer talkServer;
    private TestFileCache fileCache;

    @Before
    public void setUp() throws Exception {
        fileCache = createFileCache();
        talkServer = createTalkServer(fileCache);
    }

    @After
    public void tearDown() throws Exception {
        talkServer.shutdown();
        fileCache.shutdown();
    }

    @Test
    public void uploadAvatar() throws Exception {
        // create client
        final XoClient c = TestHelper.createTalkClient(talkServer);
        c.wake();
        await().untilCall(to(c).getState(), equalTo(XoClient.STATE_READY));

        // upload file
        final TalkClientUpload upload = new TalkClientUpload();
        URL r1 = getClass().getResource("/test.png");

        upload.initializeAsAvatar(r1.toString(), "image/png");
        c.setClientAvatar(upload);
        // wait for upload to start
        await().untilCall(to(c.getTransferAgent()).isUploadActive(upload), is(true));
        // wait for upload to end
        await().untilCall(to(c.getTransferAgent()).isUploadActive(upload), is(false));

        // test disconnecting
        c.disconnect();
        await().untilCall(to(c).getState(), equalTo(XoClient.STATE_DISCONNECTED));
    }
}

