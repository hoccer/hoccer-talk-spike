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

public class PasswordPromptFragment extends Fragment {

    OnPasswordProtectionUnlockListener mListener;

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

        final Button button = (Button) view.findViewById(R.id.btn_unlock);
        final EditText passcodeInputView = (android.widget.EditText) view.findViewById(R.id.et_enter_passcode);

        passcodeInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
                String passcodeInput = passcodeInputView.getText().toString();
                String passcode = getActivity().getSharedPreferences(PasswordSetActivity.PASSCODE_PREFERENCES, Context.MODE_PRIVATE).getString(PasswordSetActivity.PASSCODE, null);
                if (passcodeInput.equals(passcode)) {
                    mListener.onPasswordProtectionUnlocked();
                } else {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.password_prompt_retry), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
