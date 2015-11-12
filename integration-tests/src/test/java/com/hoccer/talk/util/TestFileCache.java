package com.hoccer.talk.util;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.CacheMain;
import com.hoccer.talk.filecache.db.OrmliteBackend;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/* this class just wraps a mongodb, a talkserver and a jetty server in one convenient class for test purposes */

public class TestFileCache {

    private final Server s;

    @SuppressWarnings("FieldMayBeFinal")
    private Connector serverConnector;

    public Connector getServerConnector() {
        return serverConnector;
    }

    TestFileCache(CacheConfiguration configuration) throws Exception {
        CacheBackend db = new OrmliteBackend(configuration);
        db.start();
        // create jetty instance
        s = new Server();
        s.setThreadPool(new QueuedThreadPool(configuration.getServerThreads()));
        CacheMain.setupServer(s, db, configuration);
        // loop until we find a free port
        int port = 3010;
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
