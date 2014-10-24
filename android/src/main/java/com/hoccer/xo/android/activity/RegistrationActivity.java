package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.credentialtransfer.CredentialExportReceiver;
import com.hoccer.xo.android.fragment.ImportCredentialFragment;
import com.hoccer.xo.android.fragment.ImportCredentialUpdateFragment;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

/**
 * Activity handles credential import procedure or starts the new client registration.
 */
public class RegistrationActivity extends XoActivity {

    private static final String EXTRA_CREDENTIAL_PROTOCOL_VERSION = "credential_protocol_version";

    private static final Logger LOG = Logger.getLogger(RegistrationActivity.class);

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_registration;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check if we can potentially import credentials
        final PackageInfo importPackageInfo = retrieveCredentialImportPackageInfo();
        if (importPackageInfo != null) {
            setupImportFragment(importPackageInfo);
        } else {
            startNewClientRegistration();
        }
    }

    private PackageInfo retrieveCredentialImportPackageInfo() {
        PackageInfo result = null;
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (packageName != null) {
            try {
                result = getPackageManager().getPackageInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                // package is not installed
            }
        }
        return result;
    }

    private static boolean doesPackageSupportTransfer(final PackageInfo packageInfo) {
        final int versionCode = packageInfo.versionCode;
        return versionCode >= 89;
    }

    private void setupImportFragment(final PackageInfo packageInfo) {
        if (doesPackageSupportTransfer(packageInfo)) {
            showCredentialImportFragment();
        } else {
            showCredentialImportUpdateFragment();
        }
    }

    private void showCredentialImportFragment() {
        final ImportCredentialFragment fragment = new ImportCredentialFragment();
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_registration_fragment_container, fragment);
        ft.commit();
    }

    private void showCredentialImportUpdateFragment() {
        final ImportCredentialUpdateFragment fragment = new ImportCredentialUpdateFragment();
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_registration_fragment_container, fragment);
        ft.commit();
    }

    public void startNewClientRegistration() {
        final Intent intent = new Intent(this, SingleProfileActivity.class);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CREATE_SELF, true);
        startActivity(intent);
        finish();
    }

    public void importCredentials() {
        LOG.info("Try to import credentials");
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (packageName != null) {
            final Intent intent = new Intent();
            final String className = CredentialExportReceiver.class.getName();
            intent.setClassName(packageName, className);
            intent.setAction("com.hoccer.xo.android.action.EXPORT_DATA");
            intent.putExtra("receiver", new CredentialResultReceiver(new Handler()));
            intent.putExtra(EXTRA_CREDENTIAL_PROTOCOL_VERSION, XoApplication.getConfiguration().getCredentialProtocolVersion());
            sendBroadcast(intent);
        }
    }

    private class CredentialResultReceiver extends ResultReceiver {
        public CredentialResultReceiver(final Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(final int resultCode, final Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if (resultCode == Activity.RESULT_OK && resultData != null) {
                final String credentialsJson = resultData.getString("credentialsJson");
                LOG.info("got credentials: " + credentialsJson);

                // TODO save new credentials, renew srp secret and restart

            } else {
                // TODO handle error case gracefully
            }
        }
    }
}
