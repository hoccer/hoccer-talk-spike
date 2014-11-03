package com.hoccer.xo.android.credentialtransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.apache.log4j.Logger;

/**
 * Based on the given intent action starts the CredentialExportService or DisconnectService and forwards the intent.
 */
public class CredentialTransferReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logger.getLogger(CredentialTransferReceiver.class);

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Intent serviceIntent = new Intent(intent);

        if (CredentialExportService.INTENT_ACTION_FILTER.equals(intent.getAction())) {
            serviceIntent.setClassName(context, CredentialExportService.class.getName());
        } else if (DisconnectService.INTENT_ACTION_FILTER.equals(intent.getAction())) {
            serviceIntent.setClassName(context, DisconnectService.class.getName());
        } else {
            LOG.error("Intent with unknown action '" + intent.getAction() + "' received.");
        }
        context.startService(serviceIntent);
    }
}
