package com.hoccer.talk.util;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.CacheMain;
import com.hoccer.talk.filecache.db.MemoryBackend;
import com.hoccer.talk.filecache.db.OrmliteBackend;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.WebSocketHandler;

import java.net.InetSocketAddress;

/* this class just wraps a mongodb, a talkserver and a jetty server in one convenient class for test purposes */

public class TestFileCache {

    private Server s;

    private Connector serverConnector;
    private CacheBackend db;

    public Connector getServerConnector() {
        return serverConnector;
    }

    TestFileCache(CacheConfiguration configuration) throws Exception {

        CacheMain main = new CacheMain();

        db = new OrmliteBackend(configuration);
        db.start();
        // create jetty instance
        s = new Server();
        s.setThreadPool(new QueuedThreadPool(configuration.getServerThreads()));
        CacheMain.setupServer(s, db);
        // loop until we find a free port
        int port = 3010;
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

    public void shutdown() throws Exception {
        s.stop();
        s.join();
    }
}
