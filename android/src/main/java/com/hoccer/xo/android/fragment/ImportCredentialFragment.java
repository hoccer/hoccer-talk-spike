package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.hoccer.xo.android.activity.RegistrationActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.release.R;

/**
 * Fragment shows the credential import information.
 */
public class ImportCredentialFragment extends XoFragment {

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
                    registrationActivity.importCredentials();
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
