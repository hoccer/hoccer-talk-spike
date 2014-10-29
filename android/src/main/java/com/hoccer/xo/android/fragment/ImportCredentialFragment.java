package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.hoccer.xo.android.XoDialogs;
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

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_credential, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RegistrationActivity registrationActivity = (RegistrationActivity) getActivity();
        if (registrationActivity != null) {
            final Button importButton = (Button) view.findViewById(R.id.btn_import_credentials);
            importButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final View progressOverlay = view.findViewById(R.id.rl_progress_overlay);
                    progressOverlay.setVisibility(View.VISIBLE);

                    CredentialImporter.importCredentials(registrationActivity, new CredentialImporter.CredentialImportListener() {
                        @Override
                        public void onSuccess() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LOG.info("Credentials imported successfully");
                                    getActivity().finish();
                                }
                            });
                        }

                        @Override
                        public void onFailure() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LOG.info("Credentials import failed");
                                    progressOverlay.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
            });

            final Button newClientButton = (Button) getView().findViewById(R.id.btn_create_new_client);
            newClientButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    registrationActivity.startNewClientRegistration();
                }
            });
        }
    }
}
