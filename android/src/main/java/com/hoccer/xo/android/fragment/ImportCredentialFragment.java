package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.RegistrationActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.credentialtransfer.CredentialImporter;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

/**
 * Fragment shows the credential import information.
 */
public class ImportCredentialFragment extends XoFragment {

    private static final Logger LOG = Logger.getLogger(ImportCredentialFragment.class);
    private RegistrationActivity mRegistrationActivity;
    private Credentials mCredentials;
    private View mProgressOverlay;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRegistrationActivity = (RegistrationActivity) activity;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_credential, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgressOverlay = view.findViewById(R.id.rl_progress_overlay);
        mProgressOverlay.setVisibility(View.VISIBLE);

        importCredentials();

        final Button importButton = (Button) view.findViewById(R.id.btn_import_credentials);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mCredentials != null) {

                    // send disconnect request to import package client
                    CredentialImporter.sendDisconnectRequestToImportPackageClient(mRegistrationActivity);

                    // set flag to change the srp secret on next login
//                    CredentialImporter.setSrpChangeOnNextLoginFlag(mRegistrationActivity);

                    // import new credentials
                    XoApplication.getXoClient().importCredentials(mCredentials);

                    LOG.info("Credentials imported successfully");
                    getActivity().finish();
                }
            }
        });

        final Button newClientButton = (Button) getView().findViewById(R.id.btn_create_new_client);
        newClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                if (mRegistrationActivity != null) {
                    mRegistrationActivity.startNewClientRegistration();
                }
            }
        });

    }

    private void importCredentials() {
        CredentialImporter.importCredentials(mRegistrationActivity, new CredentialImporter.CredentialImportListener() {
            @Override
            public void onSuccess(final Credentials credentials, final int contactCount) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCredentials = credentials;
                        mProgressOverlay.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onFailure() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LOG.info("Credentials import failed");
                        mRegistrationActivity.startNewClientRegistration();
                        mProgressOverlay.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
}
