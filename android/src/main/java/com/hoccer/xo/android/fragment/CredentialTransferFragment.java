package com.hoccer.xo.android.fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if(packageName != null) {
            tryImportCredentialsFromPackage(packageName);
        } else {
            showCreateSingleProfileFragment();
        }
    }

    private void tryImportCredentialsFromPackage(final String packageName) {
        final PackageInfo packageInfo = getPackageInfoByName(packageName);
        if (packageInfo != null) {
            tryImportCredentialsFromPackage(packageInfo);
        } else {
            showCreateSingleProfileFragment();
        }
    }

    private void tryImportCredentialsFromPackage(final PackageInfo packageInfo) {
        if (doesPackageSupportTransfer(packageInfo)) {
            showImportButton();
        } else {
            showUpdateInformation();
        }
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

    private void showImportButton() {
        final View view = getView().findViewById(R.id.ll_import_credentials);
        view.setVisibility(View.VISIBLE);
    }

    private void showUpdateInformation() {
        final View view = getView().findViewById(R.id.ll_update_information);
        view.setVisibility(View.VISIBLE);
    }

    private void showCreateSingleProfileFragment() {
        final Fragment fragment = new SingleProfileCreationFragment();

        final FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, fragment);
        ft.commit();
    }
}
