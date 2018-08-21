package com.hoccer.xo.android.eulaprompt;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.passwordprotection.activity.PasswordPromptActivity;

public class EulaPrompt implements Application.ActivityLifecycleCallbacks, BackgroundManager.Listener {

    public static final String EULA_PREFERENCES = "eulaprompt_preferences";

    private static EulaPrompt sInstance;
    private boolean accepted;

    public static EulaPrompt get() {
        if (sInstance == null) {
            sInstance = new EulaPrompt();
        }
        return sInstance;
    }

    private EulaPrompt() {
        XoApplication.get().registerActivityLifecycleCallbacks(this);
        BackgroundManager.get().registerListener(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
        if (!accepted && !(activity instanceof EulaPromptActivity) && !(activity instanceof PasswordPromptActivity)) {
            startEulaPromptActivity(activity);
        }
    }

    private static void startEulaPromptActivity(Activity activity) {
        Intent intent = new Intent(activity, EulaPromptActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(EulaPromptActivity.EXTRA_ENABLE_BACK_NAVIGATION, false);
        activity.startActivity(intent);
    }

    public void accept() {
        accepted = true;    
    }
    
    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onBecameForeground(Activity activity) {}

    @Override
    public void onBecameBackground(Activity activity) {

    }
}
