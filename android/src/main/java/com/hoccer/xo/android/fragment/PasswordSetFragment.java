package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.activity.PasswordSetActivity;

public class PasswordSetFragment extends Fragment {

    public static final String PASSWORD_SET_FRAGMENT = "PASSWORD_SET_FRAGMENT";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_set, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Button button = (Button) view.findViewById(R.id.btn_submit);
        final EditText enterPasswordView = (EditText) view.findViewById(R.id.et_enter_passcode);
        final EditText confirmPasswordView = (EditText) view.findViewById(R.id.et_confirm_passcode);

        enterPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    confirmPasswordView.setFocusableInTouchMode(false);
                    confirmPasswordView.setFocusable(false);
                } else {
                    confirmPasswordView.setFocusableInTouchMode(true);
                    confirmPasswordView.setFocusable(true);
                }
            }
        });

        confirmPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    button.setEnabled(false);
                } else {
                    button.setEnabled(true);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String passwordEntered = enterPasswordView.getText().toString();
                String passwordConfirmed = confirmPasswordView.getText().toString();

                if (passwordEntered.equals(passwordConfirmed)) {
                    String passCode = enterPasswordView.getText().toString();
                    getActivity().getSharedPreferences(PasswordSetActivity.PASSCODE_PREFERENCES, Context.MODE_PRIVATE).edit().putString(PasswordSetActivity.PASSCODE, passCode).apply();
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                } else {
                    enterPasswordView.getText().clear();
                    confirmPasswordView.getText().clear();
                    Toast.makeText(getActivity(), getActivity().getString(R.string.passwords_not_matching), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}