package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClientConfiguration;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

public class TalkToolClientHost implements IXoClientHost {

    private static final Logger LOG = Logger.getLogger(TalkToolClientHost.class);
    TalkToolClient mClient;

    public TalkToolClientHost(TalkToolClient client) {
        mClient = client;
    }

    @Override
    public ScheduledExecutorService getBackgroundExecutor() {
        return mClient.getContext().getExecutor();
    }

    @Override
    public ScheduledExecutorService getIncomingBackgroundExecutor() {
        return null;
    }

    @Override
    public IXoClientDatabaseBackend getDatabaseBackend() {
        return mClient.getDatabaseBackend();
    }

    @Override
    public WebSocketClientFactory getWebSocketFactory() {
        return mClient.getContext().getWSClientFactory();
    }

    @Override
    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                LOG.error("uncaught exception on thread " + thread.toString(), ex);
            }
        };
    }

    @Override
    public String getServerUri() {
        return mClient.getContext().getApplication().getServer();
    }

    @Override
    public InputStream openInputStreamForUrl(String url) throws IOException {
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        return conn.getInputStream();
    }

    @Override
    public boolean isSupportModeEnabled() {
        return false;
    }

    @Override
    public String getSupportTag() {
        return null;
    }

    @Override
    public String getClientName() {
        return null;
    }

    @Override
    public String getClientLanguage() {
        return null;
    }

    @Override
    public String getClientVersion() {
        return null;
    }

    @Override
    public Date getClientTime() {
        return null;
    }

    @Override
    public String getDeviceModel() {
        return null;
    }

    @Override
    public String getSystemName() {
        return null;
    }

    @Override
    public String getSystemLanguage() {
        return null;
    }

    @Override
    public String getSystemVersion() {
        return null;
    }

}
