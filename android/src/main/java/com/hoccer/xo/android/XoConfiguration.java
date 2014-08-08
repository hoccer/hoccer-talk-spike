package com.hoccer.xo.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;

/**
 * Static client configuration
 *
 * This class collects various android-specific settings for the XO client.
 */
public class XoConfiguration {

    private static SharedPreferences sPreferences;

    public static void initialize(XoApplication application) {
        sPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public static boolean needToRegenerateKey() {
        return sPreferences.getBoolean("NEED_TO_REGENERATE_KEYS", true);
    }

    public static void setRegenerationDone() {
        SharedPreferences.Editor editor = sPreferences.edit();
        editor.putBoolean("NEED_TO_REGENERATE_KEYS", false);
        editor.commit();
    }

}
