package com.hoccer.xo.android.eulaprompt;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.passwordprotection.activity.PasswordPromptActivity;

public class EulaPrompt implements Application.ActivityLifecycleCallbacks, BackgroundManager.Listener {

    private static EulaPrompt sInstance;
    private boolean accepted;
    private int eulaVersion = 1;

    public static EulaPrompt get() {
        if (sInstance == null) {
            sInstance = new EulaPrompt();
        }
        return sInstance;
    }

    private EulaPrompt() {
        XoApplication.get().registerActivityLifecycleCallbacks(this);
        BackgroundManager.get().registerListener(this);
        accepted = isAccepted(XoApplication.get().getBaseContext());
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

    public void accept(Activity activity) {
        accepted = true;
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt(activity.getString(R.string.preference_key_eula_version), eulaVersion).apply();
    }

    private boolean isAccepted(Context context){
        int version = PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.preference_key_eula_version), 0);
        return eulaVersion == version;
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
