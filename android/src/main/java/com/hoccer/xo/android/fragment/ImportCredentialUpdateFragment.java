package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.hoccer.xo.android.activity.RegistrationActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.release.R;

/**
 * Fragment handles the credential import from the supported package.
 */
public class ImportCredentialUpdateFragment extends XoFragment {
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_credential_update, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Button playstoreButton = (Button) getView().findViewById(R.id.btn_open_playstore);
        playstoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.hoccer.xo.release"));
                startActivity(intent);
            }
        });

        final Button newClientButton = (Button) getView().findViewById(R.id.btn_create_new_client);
        newClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final RegistrationActivity registrationActivity = (RegistrationActivity) getActivity();
                if (registrationActivity != null) {
                    registrationActivity.startNewClientRegistration();
                }
            }
        });
    }
}
