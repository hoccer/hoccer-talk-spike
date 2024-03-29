package com.hoccer.xo.android.passwordprotection.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.passwordprotection.fragment.PasswordPromptFragment;
import com.hoccer.xo.android.passwordprotection.fragment.PasswordSetFragment;

public class PasswordChangeActivity extends FragmentActivity implements PasswordPromptFragment.OnPasswordProtectionUnlockListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_framelayout);
        showPasswordPromptFragment();
    }

    private void showPasswordPromptFragment() {
        PasswordPromptFragment fragment = new PasswordPromptFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(PasswordPromptFragment.ARG_HINT_STRING_ID, R.string.old_password);
        fragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, fragment);
        ft.commit();
    }

    @Override
    public void onPasswordProtectionUnlocked() {
        showPasswordSetFragment();
    }

    private void showPasswordSetFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, new PasswordSetFragment());
        ft.commit();
    }
}
