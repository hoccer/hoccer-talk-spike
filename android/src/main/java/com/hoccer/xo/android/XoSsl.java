package com.hoccer.xo.android;

import com.hoccer.talk.client.HttpClientWithKeyStore;
import com.hoccer.talk.client.XoClientSslConfiguration;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.security.KeyStore;

/**
 * Static SSL configuration
 * <p/>
 * This class takes care of our SSL initialization.
 */
public class XoSsl {

    private static Logger LOG = Logger.getLogger(XoSsl.class);

    private static KeyStore KEYSTORE = null;

    public static KeyStore getKeyStore() {
        if (KEYSTORE == null) {
            throw new RuntimeException("SSL KeyStore not initialized");
        }
        return KEYSTORE;
    }

    public static void initialize(XoApplication application) {
        // set up SSL
        LOG.info("Initializing ssl KeyStore");
        try {
            // get the KeyStore
            KeyStore ks = KeyStore.getInstance("BKS");
            // load our keys into it
            InputStream in = application.getResources().openRawResource(R.raw.ssl_bks);
            try {
                ks.load(in, "password".toCharArray());
            } finally {
                in.close();
            }
            // configure HttpClient
            HttpClientWithKeyStore.initializeSsl(ks);
            // remember the KeyStore
            KEYSTORE = ks;
        } catch (Exception e) {
            LOG.error("Error initializing SSL KeyStore: ", e);
        }
    }
}
