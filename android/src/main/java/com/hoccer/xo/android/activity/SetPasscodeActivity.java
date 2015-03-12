package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
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
        setContentView(R.layout.activity_set_passcode);
        Button button = (Button) findViewById(R.id.btn_ok);

        final EditText passcodeInput = (EditText) findViewById(R.id.et_enter_passcode);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passcodeInput.getText().length() > 0) {
                    String passCode = passcodeInput.getText().toString();
                    getSharedPreferences(PASSCODE_PREFERENCES, MODE_PRIVATE).edit().putString(PASSCODE, passCode);
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }
}
