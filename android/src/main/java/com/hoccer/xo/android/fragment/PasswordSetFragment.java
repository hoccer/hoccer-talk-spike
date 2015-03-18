package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.passwordprotection.PasswordProtection;

public class PasswordSetFragment extends Fragment {

    public static final String PASSWORD_SET_FRAGMENT = "PASSWORD_SET_FRAGMENT";
    private EditText mEnterPasswordView;
    private EditText mConfirmPasswordView;
    private Button mSubmitButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_set, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSubmitButton = (Button) view.findViewById(R.id.btn_submit);
        mEnterPasswordView = (EditText) view.findViewById(R.id.et_enter_passcode);
        mConfirmPasswordView = (EditText) view.findViewById(R.id.et_confirm_passcode);

        registerTextChangeListenerOnEnterPasswordView();
        registerListenersOnConfirmPasswordView();
        registerClickListernOnSubmitButton();
    }

    private void registerTextChangeListenerOnEnterPasswordView() {
        mEnterPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateConfirmPasswordView(s);
            }
        });
    }

    private void registerListenersOnConfirmPasswordView() {
        mConfirmPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateSubmitButton(editable);
            }
        });

        mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    updatePasswordAndFinishIfCorrectlyConfirmed();
                    return true;
                }
                return false;
            }
        });
    }

    private void registerClickListernOnSubmitButton() {
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePasswordAndFinishIfCorrectlyConfirmed();
            }
        });
    }

    private void updateSubmitButton(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            mSubmitButton.setEnabled(false);
        } else {
            mSubmitButton.setEnabled(true);
        }
    }

    private void updateConfirmPasswordView(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            mConfirmPasswordView.setFocusableInTouchMode(false);
            mConfirmPasswordView.setFocusable(false);
            mConfirmPasswordView.setHint(null);
        } else {
            mConfirmPasswordView.setFocusableInTouchMode(true);
            mConfirmPasswordView.setFocusable(true);
            mConfirmPasswordView.setHint(R.string.confirm_password);
        }
    }

    private void updatePasswordAndFinishIfCorrectlyConfirmed() {
        if (correctlyConfirmed()) {
            updatePasswordAndFinish();
        } else {
            clearInputFields();
            Toast.makeText(getActivity(), getActivity().getString(R.string.passwords_not_matching), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean correctlyConfirmed() {
        String passwordEntered = mEnterPasswordView.getText().toString();
        String passwordConfirmed = mConfirmPasswordView.getText().toString();
        return passwordEntered.equals(passwordConfirmed);
    }

    private void updatePasswordAndFinish() {
        String passCode = mEnterPasswordView.getText().toString();
        getActivity().getSharedPreferences(PasswordProtection.PASSWORD_PROTECTION_PREFERENCES, Context.MODE_PRIVATE).edit().putString(PasswordProtection.PASSWORD_KEY, passCode).apply();
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    private void clearInputFields() {
        mEnterPasswordView.getText().clear();
        mConfirmPasswordView.getText().clear();
    }
}