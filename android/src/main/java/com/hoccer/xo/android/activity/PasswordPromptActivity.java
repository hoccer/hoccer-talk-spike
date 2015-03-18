package com.hoccer.xo.android.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.inputmethod.InputMethodManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.fragment.PasswordPromptFragment;

public class PasswordPromptActivity extends FragmentActivity implements PasswordPromptFragment.OnPasswordProtectionUnlockListener {

    public static final String EXTRA_ENABLE_BACK_NAVIGATION = "EXTRA_ENABLE_BACK_NAVIGATION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_framelayout);
        showPasswordPromptFragment();
    }

    private void showPasswordPromptFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, new PasswordPromptFragment());
        ft.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onPasswordProtectionUnlocked() {
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra(EXTRA_ENABLE_BACK_NAVIGATION, false)) {
            super.onBackPressed();
        }
    }
}
