package com.hoccer.xo.android.credentialtransfer;

import android.app.IntentService;
import android.content.Intent;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Exports the credentials.
 */
public class DisconnectService extends IntentService {

    private static final Logger LOG = Logger.getLogger(DisconnectService.class);

    public static final String INTENT_ACTION_FILTER = "com.hoccer.android.action.DISCONNECT";

    public DisconnectService() {
        super("DisconnectService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final XoClient client = XoApplication.get().getClient();
        if(!client.isDisconnected()) {
            LOG.info("Disconnecting client by intent.");
            client.disconnect();
        }
    }
}
