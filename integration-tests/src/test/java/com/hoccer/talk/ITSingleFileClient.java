package com.hoccer.talk;

// import junit stuff

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.util.IntegrationTest;
import com.hoccer.talk.util.TestFileCache;
import com.hoccer.talk.util.TestTalkServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;


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
  public void clientConnectAndDisconnectTest() throws Exception {
      // create client
      final XoClient c = createTalkClient(talkServer);
      c.wake();
      waitOrTimeout(new Condition() {
          @Override
          public boolean isSatisfied() {
              return XoClient.STATE_ACTIVE == c.getState();
          }
      }, Timeout.timeout(seconds(2)));

      // upload file
      final TalkClientUpload upload = new TalkClientUpload();
      URL r1 = getClass().getResource("/test.png");

      upload.initializeAsAvatar(r1.toString(), r1.toString(), "image/png", r1.getFile().length());
      c.setClientAvatar(upload);
      // wait for upload to start
      waitOrTimeout(new Condition() {
          @Override
          public boolean isSatisfied() {
              return c.getTransferAgent().isUploadActive(upload) == true;
          }
      }, Timeout.timeout(seconds(2)));
      // wait for upload to end
      waitOrTimeout(new Condition() {
          @Override
          public boolean isSatisfied() {
              return c.getTransferAgent().isUploadActive(upload) == false;
          }
      }, Timeout.timeout(seconds(2)));
      // test disconnecting
      c.deactivate();
      waitOrTimeout(new Condition() {
          @Override
          public boolean isSatisfied() {
              return XoClient.STATE_INACTIVE == c.getState();
          }
      }, Timeout.timeout(seconds(2)));
  }
}

