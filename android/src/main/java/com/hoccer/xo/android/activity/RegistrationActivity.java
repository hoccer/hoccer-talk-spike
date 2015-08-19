package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.credentialtransfer.CredentialImporter;
import com.hoccer.xo.android.fragment.ImportCredentialFragment;
import com.hoccer.xo.android.fragment.ImportCredentialUpdateFragment;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;

/**
 * Activity handles credential import procedure or starts the new client registration.
 */
public class RegistrationActivity extends BaseActivity {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
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
        startActivity(new Intent(this, ClientProfileActivity.class)
                .setAction(ClientProfileActivity.ACTION_CREATE_SELF));
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        // do nothing if back is pressed, because we want to avoid immediate client registration
    }
}
