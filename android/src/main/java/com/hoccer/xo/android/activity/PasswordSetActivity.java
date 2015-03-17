package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.fragment.PasswordPromptFragment;
import com.hoccer.xo.android.fragment.PasswordSetFragment;

public class PasswordSetActivity extends FragmentActivity {

    public static final String PASSCODE_PREFERENCES = "com.artcom.hoccer._preferences";
    public static final String PASSCODE = "passcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_framelayout);
        showPasswordSetFragment();
    }

    private void showPasswordSetFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, new PasswordSetFragment(), PasswordSetFragment.PASSWORD_SET_FRAGMENT);
        ft.commit();
    }
}

