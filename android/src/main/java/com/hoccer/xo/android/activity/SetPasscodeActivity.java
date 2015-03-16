package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.artcom.hoccer.R;

public class SetPasscodeActivity extends Activity {

    public static final String PASSCODE_PREFERENCES = "com.artcom.hoccer._preferences";
    public static final String PASSCODE = "passcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_passcode_set);

        final Button button = (Button) findViewById(R.id.btn_ok);
        final EditText passcodeInputView = (EditText) findViewById(R.id.et_enter_passcode);

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
                String passCode = passcodeInputView.getText().toString();
                getSharedPreferences(PASSCODE_PREFERENCES, MODE_PRIVATE).edit().putString(PASSCODE, passCode).apply();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}
