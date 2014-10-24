package com.hoccer.xo.android.credentialtransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Starts the CredentialExportService and forwards the intent.
 */
public class CredentialExportReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Intent serviceIntent = new Intent(intent);
        serviceIntent.setClassName(context, CredentialExportService.class.getName());
        context.startService(serviceIntent);
    }
}
