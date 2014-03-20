package com.hoccer.talk;

import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.mongodb.Mongo;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
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

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ITDummy {

    private MongodForTestsFactory factory;
    private Mongo mongo;
    private TalkServer ts;
    private Server s;

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
                Connector connector=new SelectChannelConnector();
                connector.setPort(port);
                s.setConnectors(new Connector[]{connector});
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
  public void thisFirstTest() throws InterruptedException {
    assertEquals(2, 1+1);
      sleep(60000);
  }

  @Test
  public void thisfailTest() {
    assertEquals(2, 2);
  }
}

