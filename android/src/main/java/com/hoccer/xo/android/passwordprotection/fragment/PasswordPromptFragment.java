package com.hoccer.xo.android.passwordprotection.fragment;

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

public class PasswordPromptFragment extends Fragment {

    public static final String ARG_HINT_STRING_ID = "ARG_HINT_STRING_ID";

    OnPasswordProtectionUnlockListener mListener;

    private Button mUnlockButton;
    private EditText mPasswordInputView;

    public interface OnPasswordProtectionUnlockListener {
        public void onPasswordProtectionUnlocked();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnPasswordProtectionUnlockListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_prompt, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPasswordInputView = (EditText) view.findViewById(R.id.et_enter_passcode);
        mUnlockButton = (Button) view.findViewById(R.id.btn_unlock);
        updatePasswordInputHint();
        registerListenersOnPasswordInputView();
        registerClickListenerOnUnlockButton();
    }

    private void updatePasswordInputHint() {
        if (getArguments() != null && getArguments().containsKey(ARG_HINT_STRING_ID)) {
            mPasswordInputView.setHint(getArguments().getInt(ARG_HINT_STRING_ID));
        }
    }

    private void registerClickListenerOnUnlockButton() {
        mUnlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPassword();
            }
        });
    }

    private void registerListenersOnPasswordInputView() {
        mPasswordInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable charSequence) {
                updateUnlockButton(charSequence);
            }
        });
        mPasswordInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (view.getText().length() > 0 && (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    verifyPassword();
                    return true;
                }
                return false;
            }
        });
    }

    private void verifyPassword() {
        if (isPasswordCorrect()) {
            PasswordProtection.get().unlock();
            mListener.onPasswordProtectionUnlocked();
        } else {
            mPasswordInputView.getText().clear();
            Toast.makeText(getActivity(), getActivity().getString(R.string.password_prompt_retry), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPasswordCorrect() {
        String passwordEntered = mPasswordInputView.getText().toString();
        String password = getActivity().getSharedPreferences(PasswordProtection.PASSWORD_PROTECTION_PREFERENCES, Context.MODE_PRIVATE).getString(PasswordProtection.PASSWORD, null);
        return passwordEntered.equals(password);
    }

    private void updateUnlockButton(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            mUnlockButton.setEnabled(false);
        } else {
            mUnlockButton.setEnabled(true);
        }
    }
}
