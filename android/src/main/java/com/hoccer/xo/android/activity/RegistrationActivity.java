package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.fragment.ImportCredentialFragment;
import com.hoccer.xo.android.fragment.ImportCredentialUpdateFragment;
import com.hoccer.xo.release.R;

/**
 * Activity handles credential import procedure or starts the new client registration.
 */
public class RegistrationActivity extends XoActivity {

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
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        final Intent intent = new Intent();
        final ComponentName component = new ComponentName(packageName, "com.hoccer.xo.android.activity.DataExportActivity");
        intent.setComponent(component);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                final String credentialsJson = data.getStringExtra("credentialsJson");
                LOG.info("got credentials: " + credentialsJson);
            } else {
                LOG.info("did not get credentials");
            }
        }
    }
}
