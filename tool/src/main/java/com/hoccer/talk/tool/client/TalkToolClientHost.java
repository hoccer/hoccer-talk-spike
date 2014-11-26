package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.tool.TalkToolContext;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
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
    public KeyStore getKeyStore() {
        return TalkToolContext.getKeyStore();
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
    public InputStream openInputStreamForUrl(String url) throws IOException {
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        return conn.getInputStream();
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

}
