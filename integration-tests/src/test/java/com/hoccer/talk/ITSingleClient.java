package com.hoccer.talk;

import com.google.code.tempusfugit.temporal.*;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.mongodb.Mongo;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import junit.framework.Assert;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.InetSocketAddress;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ITSingleClient {

    private MongodForTestsFactory factory;
    private Mongo mongo;
    private TalkServer ts;
    private Server s;
    private Connector serverConnector;

    @Before
    public void setup() throws Exception {
        factory = MongodForTestsFactory.with(Version.Main.PRODUCTION);
        mongo = factory.newMongo();
        TalkServerConfiguration configuration = new TalkServerConfiguration();

        ITalkServerDatabase db = new JongoDatabase(configuration,mongo);
        ts = new TalkServer(configuration, db);
        // create jetty instance

        s = new Server();
        WebSocketHandler clientHandler = new TalkRpcConnectionHandler(ts);
        s.setHandler(clientHandler);
        // loop until we find a free port
        int port = 3000;
        while (true) {
            try {
                serverConnector=new SelectChannelConnector();
                serverConnector.setPort(port);
                s.setConnectors(new Connector[]{serverConnector});
                s.start();
                break;
            } catch (java.net.BindException ex) {
                port++;
            }
        }


    }

    @After
    public void teardown() throws Exception {
        if (s != null) {
            s.stop();
        }
        if (factory != null)
            factory.shutdown();
    }
  @Test
  public void clientConnectAndDisconnectTest() throws Exception {
      // create client
      IXoClientHost host = new TestClientHost(serverConnector);
      final XoClient c = new XoClient(host);
      assertFalse(c.isAwake());
      c.wake();
      assertTrue(c.isAwake());

      waitOrTimeout(new Condition() {
          @Override
          public boolean isSatisfied() {
              return XoClient.STATE_ACTIVE == c.getState();
          }
      }, Timeout.timeout(seconds(2)));

      c.deactivate();
      waitOrTimeout(new Condition() {
          @Override
          public boolean isSatisfied() {
              return XoClient.STATE_INACTIVE == c.getState();
          }
      }, Timeout.timeout(seconds(2)));
  }

}

