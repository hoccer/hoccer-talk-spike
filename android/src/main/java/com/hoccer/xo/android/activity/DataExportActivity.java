package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class DataExportActivity extends Activity {

    private static final String EXTRA_RESULT_CREDENTIALS_JSON = "credentialsJson";
    public static final String CALLING_PACKAGE_NAME = "com.artcom.hoccer";

    private static final Logger LOG = Logger.getLogger(DataExportActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_export);
        exportCredentialsToCallingPackage();
        finish();
    }

    private void exportCredentialsToCallingPackage() {
        if (CALLING_PACKAGE_NAME.equals(getCallingPackage())) {
            exportCredentials();
        } else {
            setResult(Activity.RESULT_CANCELED);
        }
    }

    private void exportCredentials() {
        try {
            LOG.info("Exporting credentials to " + getCallingPackage());
            String credentialsJson = XoApplication.getXoClient().extractCredentialsAsJson();
            setResult(Activity.RESULT_OK, new Intent()
                    .putExtra(EXTRA_RESULT_CREDENTIALS_JSON, credentialsJson));
        } catch (Exception e) {
            setResult(Activity.RESULT_CANCELED);
        }
    }
}
