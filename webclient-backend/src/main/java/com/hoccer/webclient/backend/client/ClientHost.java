package com.hoccer.webclient.backend.client;

import com.hoccer.talk.client.HttpClientWithKeyStore;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.webclient.backend.Configuration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ClientHost implements IXoClientHost {

    private static final Logger LOG = Logger.getLogger(ClientHost.class);

    private IXoClientDatabaseBackend mClientDatabase;
    private KeyStore mKeyStore;

    private ScheduledExecutorService mExecutor;

    public ClientHost(IXoClientDatabaseBackend clientDatabase, Configuration configuration) {
        mClientDatabase = clientDatabase;
        mExecutor = Executors.newScheduledThreadPool(8);

        if(configuration.getTrustedCertFile() != null) {
            mKeyStore = loadKeyStore(configuration.getTrustedCertFile());
        }

        if (mKeyStore != null) {
            try {
                HttpClientWithKeyStore.initializeSsl(mKeyStore);
            } catch (GeneralSecurityException e) {
                LOG.error("Failed to configure HTTP client for SSL", e);
            }
        }
    }

    private KeyStore loadKeyStore(String certFile) {
        try {
            // get the key store
            KeyStore ks = KeyStore.getInstance("BKS");

            // load certificate from file
            InputStream input = ClientHost.class.getClassLoader().getResourceAsStream(certFile);
            try {
                ks.load(input, "password".toCharArray());
            } finally {
                input.close();
            }

            return ks;
        } catch (Exception e) {
            LOG.error("Could not load SSL key store '" + certFile + "'", e);
            return null;
        }
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
        return mClientDatabase;
    }

    @Override
    public KeyStore getKeyStore() {
        return mKeyStore;
    }

    @Override
    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                LOG.error("Uncaught exception on thread " + thread.toString());
                ex.printStackTrace();
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
        return "WebClientBackend";
    }

    @Override
    public String getClientLanguage() {
        return null;
    }

    @Override
    public String getClientVersionName() {
        return "0.1";
    }

    @Override
    public int getClientVersionCode() {
        return 1;
    }

    @Override
    public String getClientBuildVariant() {
        return "release";
    }

    @Override
    public Date getClientTime() {
        return new Date();
    }

    @Override
    public String getDeviceModel() {
        return null;
    }

    @Override
    public String getSystemName() {
        return System.getProperty("os.name");
    }

    @Override
    public String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    @Override
    public String getSystemVersion() {
        return System.getProperty("os.version");
    }
}
