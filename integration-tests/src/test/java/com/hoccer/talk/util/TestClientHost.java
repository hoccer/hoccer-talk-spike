package com.hoccer.talk.util;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.ErrorManager;

public class TestClientHost implements IXoClientHost {

    private final ScheduledExecutorService mExecutor;
    private final IXoClientDatabaseBackend mDatabaseBackend;
    private final WebSocketClientFactory mWSClientFactory;
    private final TestTalkServer mServer;
    private ErrorManager LOG;

    public TestClientHost(TestTalkServer testServer) throws Exception {
        mExecutor = Executors.newScheduledThreadPool(10);
        mDatabaseBackend = new TestClientDatabaseBackend();
        mWSClientFactory = new WebSocketClientFactory();
        mWSClientFactory.start();
        mServer = testServer;
    }

    @Override
    public ScheduledExecutorService getBackgroundExecutor() {
        return mExecutor;
    }

    @Override
    public ScheduledExecutorService getIncomingBackgroundExecutor() {
        return null;
    }

    @Override
    public IXoClientDatabaseBackend getDatabaseBackend() {
        return mDatabaseBackend;
    }

    @Override
    public WebSocketClientFactory getWebSocketFactory() {
        return mWSClientFactory;
    }

    @Override
    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Assert.fail(ex.toString());
            }
        };
    }

    @Override
    public String getServerUri() {
        return "ws://127.0.0.1:" + mServer.getServerConnector().getPort();
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

    @Override
    public int getRSAKeysize() {
        return 1024;
    }
}
