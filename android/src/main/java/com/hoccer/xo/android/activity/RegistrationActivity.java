package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.credentialtransfer.CredentialImporter;
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

        if (CredentialImporter.isCredentialImportPackageInstalled(this)) {
            startImportFragment();
        } else {
            startNewClientRegistration();
        }
    }

    private void startImportFragment() {
        if (CredentialImporter.isCredentialImportSupported(this)) {
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

    @Override
    public void onBackPressed() {
        // do nothing if back is pressed
    }
}
