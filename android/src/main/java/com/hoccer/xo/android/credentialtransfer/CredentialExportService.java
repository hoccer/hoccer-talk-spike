package com.hoccer.xo.android.credentialtransfer;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Exports
 */
public class CredentialExportService extends IntentService {

    public static final String EXTRA_CREDENTIAL_PROTOCOL_VERSION = "credential_protocol_version";
    private static final String EXTRA_RESULT_CREDENTIALS_JSON = "credentialsJson";

    private static final Logger LOG = Logger.getLogger(CredentialExportService.class);

    public CredentialExportService() {
        super("DataExportService");

    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        ResultReceiver resultReceiver = null;
        if (intent.hasExtra("receiver")) {
            resultReceiver = intent.getParcelableExtra("receiver");
        }

        if (resultReceiver != null) {
            final String transferProtocolVersion = XoApplication.getConfiguration().getCredentialProtocolVersion();
            if (transferProtocolVersion != null) {
                if (intent.hasExtra(EXTRA_CREDENTIAL_PROTOCOL_VERSION) && transferProtocolVersion.equals(intent.getStringExtra(EXTRA_CREDENTIAL_PROTOCOL_VERSION))) {
                    exportCredentials(resultReceiver);
                    return;
                }
            }
            resultReceiver.send(Activity.RESULT_CANCELED, null);
        }
    }

    private static void exportCredentials(final ResultReceiver resultReceiver) {
        try {
            final String credentialsJson = XoApplication.getXoClient().extractCredentialsAsJson();
            final Bundle bundle = new Bundle();
            bundle.putString(EXTRA_RESULT_CREDENTIALS_JSON, credentialsJson);

            LOG.info("Exporting credentials");
            resultReceiver.send(Activity.RESULT_OK, bundle);
        } catch (Exception e) {
            resultReceiver.send(Activity.RESULT_CANCELED, null);
        }
    }
}
