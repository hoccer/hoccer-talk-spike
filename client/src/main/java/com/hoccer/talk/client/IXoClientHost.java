package com.hoccer.talk.client;

import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Interface implemented by programs that host a XoClient
 */
public interface IXoClientHost {

    public ScheduledExecutorService getBackgroundExecutor();
    public ScheduledExecutorService getIncomingBackgroundExecutor();
    public IXoClientDatabaseBackend getDatabaseBackend();
    public WebSocketClientFactory   getWebSocketFactory();
    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler();
    public InputStream openInputStreamForUrl(String url) throws IOException;

    public String getClientName();
    public String getClientLanguage();
    public String getClientVersionName();
    public int getClientVersionCode();
    public String getClientBuildVariant();
    public Date getClientTime();

    public String getDeviceModel();

    public String getSystemName();
    public String getSystemLanguage();
    public String getSystemVersion();
}
