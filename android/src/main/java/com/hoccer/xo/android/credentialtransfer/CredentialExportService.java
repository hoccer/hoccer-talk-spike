package com.hoccer.xo.android.credentialtransfer;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Exports the credentials.
 */
public class CredentialExportService extends IntentService {

    private static final Logger LOG = Logger.getLogger(CredentialExportService.class);

    public static final String EXTRA_RESULT_CREDENTIALS_JSON = "credentialsJson";

    public CredentialExportService() {
        super("DataExportService");

    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (!intent.hasExtra("receiver")) {
            LOG.warn("Export request received from '" + intent.getPackage() + "' but no receiver provided.");
            return;
        }

        final ResultReceiver resultReceiver = intent.getParcelableExtra("receiver");

        if (resultReceiver == null) {
            LOG.warn("Export request received from '" + intent.getPackage() + "' but receiver provided is null.");
            return;
        }

        exportCredentials(resultReceiver);
    }

    private static void exportCredentials(final ResultReceiver resultReceiver) {
        try {
            final String credentialsJson = XoApplication.getXoClient().extractCredentialsAsJson();
            final byte[] encryptedCredentials = XoApplication.getXoClient().makeEncryptedCredentialsContainer(credentialsJson, )
            final Bundle bundle = new Bundle();
            bundle.putString(EXTRA_RESULT_CREDENTIALS_JSON, credentialsJson);

            LOG.info("Exporting credentials");
            resultReceiver.send(Activity.RESULT_OK, bundle);
        } catch (final Exception e) {
            resultReceiver.send(Activity.RESULT_CANCELED, null);
        }
    }
}
