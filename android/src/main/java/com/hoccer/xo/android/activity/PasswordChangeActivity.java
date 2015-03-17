package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.fragment.PasswordPromptFragment;
import com.hoccer.xo.android.fragment.PasswordSetFragment;

public class PasswordChangeActivity extends FragmentActivity implements PasswordPromptFragment.OnPasswordProtectionUnlockListener {

    private static final String PASSWORD_PROMPT_FRAGMENT = "PASSWORD_PROMPT_FRAGMENT";
    private static final String PASSWORD_SET_FRAGMENT = "PASSWORD_SET_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_framelayout);
        showPasswordPromptFragment();
    }

    private void showPasswordPromptFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, new PasswordPromptFragment(), PASSWORD_PROMPT_FRAGMENT);
        ft.commit();
    }

    @Override
    public void onPasswordProtectionUnlocked() {
        showPasswordSetFragment();
    }

    private void showPasswordSetFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, new PasswordSetFragment(), PASSWORD_SET_FRAGMENT);
        ft.commit();
    }
}
