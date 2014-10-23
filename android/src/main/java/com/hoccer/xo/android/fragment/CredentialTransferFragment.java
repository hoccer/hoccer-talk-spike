package com.hoccer.xo.android.fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.release.R;

/**
 * Fragment handles the credential import from the supported package.
 */
public class CredentialTransferFragment extends XoFragment {

    public static final String SUPPORTED_CREDENTIAL_TRANSFER_PACKAGE_NAME = "com.hoccer.xo.release";

    public static final String ARG_PACKAGE_VERSION_CODE = "ARG_PACKAGE_VERSION_CODE";

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credential_import, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final PackageInfo packageInfo = getTransferCompatiblePackageInfo();
        if (packageInfo != null) {
            if (isTransferSupported(packageInfo)) {
                showImportButton();
            } else {
                showUpdateInformation();
            }
        } else {
            showCreateSingleProfileFragment();
        }
    }

    private boolean isTransferSupported(final PackageInfo packageInfo) {
        final int versionCode = packageInfo.versionCode;
        return versionCode >= 92;
    }

    private PackageInfo getTransferCompatiblePackageInfo() {
        PackageInfo result = null;
        try {
            result = getActivity().getPackageManager().getPackageInfo(CredentialTransferFragment.SUPPORTED_CREDENTIAL_TRANSFER_PACKAGE_NAME, 0);
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
