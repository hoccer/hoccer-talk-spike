package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.release.R;

/**
 * Fragment handles the credential import from the supported package.
 */
public class CredentialTransferFragment extends XoFragment {

    public static final String ARG_PACKAGE_VERSION_CODE = "ARG_PACKAGE_VERSION_CODE";

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credential_import, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PackageInfo importPackageInfo = null;
        final String importPackageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (importPackageName != null) {
            importPackageInfo = getPackageInfoByName(importPackageName);
        }

        if (importPackageInfo != null) {
            setupTransferLayout(importPackageInfo);
        } else {
            showCreateSingleProfileFragment();
        }
    }

    private void setupTransferLayout(final PackageInfo packageInfo) {
        if (doesPackageSupportTransfer(packageInfo)) {
            setupImportBlock();
        } else {
            setupUpdateBlock();
        }

        setupNewClientBlock();
    }

    private void setupImportBlock() {
        final View view = getView().findViewById(R.id.ll_import_credentials);
        view.setVisibility(View.VISIBLE);
    }

    private void setupUpdateBlock() {
        final View view = getView().findViewById(R.id.ll_update_information);
        view.setVisibility(View.VISIBLE);

        final Button playstoreButton = (Button) getView().findViewById(R.id.btn_open_playstore);
        playstoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.hoccer.xo.release"));
                startActivity(intent);
            }
        });
    }

    private void setupNewClientBlock() {
        final View view = getView().findViewById(R.id.ll_create_new_client);
        view.setVisibility(View.VISIBLE);

        final Button newClientButton = (Button) getView().findViewById(R.id.btn_create_new_client);
        newClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateSingleProfileFragment();
            }
        });
    }

    private static boolean doesPackageSupportTransfer(final PackageInfo packageInfo) {
        final int versionCode = packageInfo.versionCode;
        return versionCode >= 92;
    }

    private PackageInfo getPackageInfoByName(final String packageName) {
        PackageInfo result = null;
        try {
            result = getActivity().getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // package is not installed, case already handled above
        }
        return result;
    }

    private void showCreateSingleProfileFragment() {
        final Fragment fragment = new SingleProfileCreationFragment();

        final FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, fragment);
        ft.commit();
    }
}
