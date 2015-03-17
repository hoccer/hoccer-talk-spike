package com.hoccer.xo.android.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.inputmethod.InputMethodManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.fragment.PasswordPromptFragment;

public class PasswordPromptActivity extends FragmentActivity implements PasswordPromptFragment.OnPasswordProtectionUnlockListener{

    public static final String EXTRA_ENABLE_BACK_NAVIGATION = "ENABLE_BACK";
    private static final String PASSWORD_PROMPT_FRAGMENT = "PASSWORD_PROMPT_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_framelayout);

        showPasswordPromptFragment();
    }

    private void showPasswordPromptFragment() {
        Fragment fragment = new PasswordPromptFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, fragment, PASSWORD_PROMPT_FRAGMENT);
        ft.commit();
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
        if (getIntent().getBooleanExtra(EXTRA_ENABLE_BACK_NAVIGATION, false)){
            super.onBackPressed();
        }
    }
}
