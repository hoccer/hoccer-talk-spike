package com.hoccer.xo.android.util;


import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {

    public static final String REGISTRATION_PREFERENCES = "REGISTRATION_PREFERENCES";

    public static void saveUserHasConfirmedProfile(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SharedPreferencesUtils.REGISTRATION_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("userHasConfirmedProfile", true);
        editor.commit();
    }

    public static boolean hasUserConfirmedProfile(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("REGISTRATION_PREFERENCES", Context.MODE_PRIVATE);
        boolean result = preferences.getBoolean("userHasConfirmedProfile", false);
        return result;
    }
}
