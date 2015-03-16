package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import com.artcom.hoccer.R;

public class PromptPasswordActivity extends Activity {

    private boolean mEnableBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEnableBack = getIntent().getBooleanExtra("ENABLE_BACK", false);

        setContentView(R.layout.activity_passcode_prompt);

        final Button button = (Button) findViewById(R.id.btn_unlock);
        final EditText passcodeInputView = (android.widget.EditText) findViewById(R.id.et_enter_passcode);

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
                String passcode = getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).getString(SetPasscodeActivity.PASSCODE, null);
                if (passcodeInput.equals(passcode)) {
                    setResult(RESULT_OK);
                    finish();
                }

                final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(passcodeInputView.getWindowToken(), 0);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mEnableBack) {
            super.onBackPressed();
        }
    }
}
