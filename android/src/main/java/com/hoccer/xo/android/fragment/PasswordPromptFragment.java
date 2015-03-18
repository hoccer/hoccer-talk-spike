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

public class PasswordPromptFragment extends Fragment {

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
        registerListenersOnPasswordInputView();
        registerClickListenerOnUnlockButton();
    }

    private void registerClickListenerOnUnlockButton() {
        mUnlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyIfUnlocked();
            }
        });
    }

    private void registerListenersOnPasswordInputView() {
        mPasswordInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable charSequence) {
                updateUnlockButton(charSequence);
            }
        });
        mPasswordInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    notifyIfUnlocked();
                    return true;
                }
                return false;
            }
        });
    }

    private void notifyIfUnlocked() {
        String passcodeInput = mPasswordInputView.getText().toString();
        String passcode = getActivity().getSharedPreferences(PasswordProtection.PASSWORD_PROTECTION_PREFERENCES, Context.MODE_PRIVATE).getString(PasswordProtection.PASSWORD_KEY, null);
        if (passcodeInput.equals(passcode)) {
            mListener.onPasswordProtectionUnlocked();
        } else {
            Toast.makeText(getActivity(), getActivity().getString(R.string.password_prompt_retry), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUnlockButton(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            mUnlockButton.setEnabled(false);
        } else {
            mUnlockButton.setEnabled(true);
        }
    }
}
