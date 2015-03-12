package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.artcom.hoccer.R;

public class PasscodeInputActivity extends Activity {

    private boolean mEnableBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_passcode_input);
        Button button = (Button) findViewById(R.id.btn_unlock);
        final EditText passcodeInputView = (android.widget.EditText) findViewById(R.id.et_enter_passcode);

        mEnableBack = getIntent().getBooleanExtra("ENABLE_BACK", false);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passcodeInputView.getText().length() > 0) {
                    String passcodeInput = passcodeInputView.getText().toString();
                    String passcode = getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).getString(SetPasscodeActivity.PASSCODE, null);
                    if (passcodeInput.equals(passcode)) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
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
