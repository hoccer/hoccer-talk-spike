package com.hoccer.xo.android.passwordprotection;

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

public class PasswordProtection implements Application.ActivityLifecycleCallbacks, BackgroundManager.Listener {

    public static final String PASSWORD_PROTECTION_PREFERENCES = "password_protection_preferences";
    public static final String PASSWORD = "password";
    public static final String LOCKED = "is_locked";

    public PasswordProtection() {
        XoApplication.get().registerActivityLifecycleCallbacks(this);
        BackgroundManager.get().registerListener(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isActive(activity) && isLocked(activity)) {
            startPasswordPromptActivity(activity);
        }
    }

    private boolean isActive(Activity activity) {
        return PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(activity.getString(R.string.preference_key_activate_passcode), false);
    }

    private boolean isLocked(Activity activity) {
        return activity.getSharedPreferences(PasswordProtection.PASSWORD_PROTECTION_PREFERENCES, Context.MODE_PRIVATE).getBoolean(PasswordProtection.LOCKED, false);
    }

    private void startPasswordPromptActivity(Activity activity) {
        if (XoApplication.get().getXoClient().getSelfContact().getSelf().isRegistrationConfirmed() && !((XoApplication) activity.getApplication()).isActiveInBackground()) {
            Intent intent = new Intent(activity, PasswordPromptActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(PasswordPromptActivity.EXTRA_ENABLE_BACK_NAVIGATION, false);
            activity.startActivity(intent);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onBecameForeground(Activity activity) {
    }

    @Override
    public void onBecameBackground(Activity activity) {
        if (isActive(activity)) {
            activity.getSharedPreferences(PasswordProtection.PASSWORD_PROTECTION_PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(PasswordProtection.LOCKED, true).apply();
        }
    }
}
