package com.hoccer.xo.android.eulaprompt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import com.artcom.hoccer.R;

public class EulaPromptActivity extends FragmentActivity implements EulaPromptFragment.EulaPromptListener {

    public static final String EXTRA_ENABLE_BACK_NAVIGATION = "EXTRA_ENABLE_BACK_NAVIGATION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_framelayout);
        showEulaPromptFragment();
    }

    private void showEulaPromptFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, new EulaPromptFragment());
        ft.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onEulaAccepted() {
        EulaPrompt.get().accept();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onEulaDeclined() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // avoid transition animation when the activity is resumed->paused->resumed, when it was already on top
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra(EXTRA_ENABLE_BACK_NAVIGATION, false)) {
            super.onBackPressed();
        }
    }
}
