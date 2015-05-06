package com.hoccer.talk.util;

import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.mongodb.MongoClient;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.websocket.WebSocketHandler;

/**
 * this class just wraps a mongodb, a talkserver and a jetty server in one convenient class for test purposes
 */
public class TestTalkServer {

    private final Server s;
    private final TalkServer talkServer;

    public Connector getServerConnector() {
        return serverConnector;
    }

    public TalkServer getTalkServer() {
        return talkServer;
    }

    @SuppressWarnings("FieldMayBeFinal")
    private Connector serverConnector;

    TestTalkServer(TalkServerConfiguration configuration, MongoClient mongo) throws Exception {

        ITalkServerDatabase db = new JongoDatabase(configuration, mongo);
        db.reportPing();
        talkServer = new TalkServer(configuration, db);
        // create jetty instance

        s = new Server();
        WebSocketHandler clientHandler = new TalkRpcConnectionHandler(talkServer);
        s.setHandler(clientHandler);
        // loop until we find a free port
        int port = 3000;
        while (true) {
            try {
                serverConnector = new SelectChannelConnector();
                serverConnector.setPort(port);
                s.setConnectors(new Connector[]{serverConnector});
                s.start();
                break;
            } catch (java.net.BindException ex) {
                port++;
            }
        }
    }

    public void shutdown() throws Exception {
        s.stop();
        s.join();
    }
}
