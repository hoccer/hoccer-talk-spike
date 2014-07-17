package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TalkToolClientHost implements IXoClientHost {

    private static final Logger LOG = Logger.getLogger(TalkToolClientHost.class);
    private final ScheduledExecutorService mExecutor;
    TalkToolClient mClient;

    public TalkToolClientHost(TalkToolClient client) {
        mClient = client;
        mExecutor = Executors.newScheduledThreadPool(4);
    }

    @Override
    public ScheduledExecutorService getBackgroundExecutor() {
        return mExecutor; // mClient.getContext().getExecutor();
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
        return mClient.getSupportMode();
    }

    @Override
    public String getSupportTag() {
        return mClient.getSupportTag();
    }

    @Override
    public boolean getUseBsonProtocol() {
        return true;
    }

    @Override
    public String getBsonProtocolString() {
        return "com.hoccer.talk.v4.bson";
    }

    @Override
    public String getJsonProtocolString() {
        return "com.hoccer.talk.v4";
    }

    @Override
    public int getTransferThreads() {
        return 2;
    }

    @Override
    public int getConnectTimeout() {
        return 15;
    }

    @Override
    public int getIdleTimeout() {
        return 300;
    }

    @Override
    public boolean getKeepAliveEnabled() {
        return false;
    }

    @Override
    public int getKeepAliveInterval() {
        return 120;
    }

    @Override
    public int getConnectionIdleTimeout() {
        return 900000;
    }

    @Override
    public float getReconnectBackoffFixedDelay() {
        return 3;
    }

    @Override
    public float getReconnectBackoffVariableFactor() {
        return 1;
    }

    @Override
    public float getReconnectBackoffVariableMaximum() {
        return 120;
    }

    @Override
    public String getUrlScheme() {
        return "hxo://";
    }

    @Override
    public String getClientName() {
        return "Talk";
    }

    @Override
    public String getClientLanguage() {
        return "en";
    }

    @Override
    public String getClientVersionName() {
        return null;
    }

    @Override
    public int getClientVersionCode() {
        return 0;
    }

    @Override
    public String getClientBuildVariant() {
        return "release";
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
        return "TalkTool";
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

    @Override
    public boolean isSendDeliveryConfirmationEnabled() {
        return true;
    }
}
