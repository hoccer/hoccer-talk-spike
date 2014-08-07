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
	
    /**
     * Background executor thread count
     *
     * AFAIK this must be at least 3 for RPC to work.
     */
    public static final int CLIENT_THREADS = 10;

    /** Whether to reconnect explicitly on connection changes */
    public static final boolean CONNECTIVITY_RECONNECT_ON_CHANGE = false;

    /** Delay after which new activities send their first keepalive (seconds) */
    public static final int SERVICE_KEEPALIVE_PING_DELAY    = 60;
    /** Interval at which activities send keepalives to the client service (seconds) */
    public static final int SERVICE_KEEPALIVE_PING_INTERVAL = 600;
    /** Timeout after which the client service terminates automatically (seconds) */
    public static final int SERVICE_KEEPALIVE_TIMEOUT       = 1800;

    private static SharedPreferences sPreferences;
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener;

    public static void initialize(XoApplication application) {
        sPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        sPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("preference_enable_server_side_support_mode")) {
                    XoApplication.getXoClient().hello();
                }
            }
        };
        sPreferences.registerOnSharedPreferenceChangeListener(sPreferencesListener);

        if(application.getConfiguration().isTestingModeEnabled()) {
            SharedPreferences.Editor editor = sPreferences.edit();
            editor.putString("preference_log_level", "DEBUG");
            editor.putBoolean("preference_log_sd", true);
            editor.commit();
        }
        if(application.getConfiguration().isDevelopmentModeEnabled()) {
            SharedPreferences.Editor editor = sPreferences.edit();
            editor.putString("preference_log_level", "DEBUG");
            editor.commit();
        }
    }

    public static final void shutdown() {
        if(sPreferencesListener != null) {
            sPreferences.unregisterOnSharedPreferenceChangeListener(sPreferencesListener);
            sPreferencesListener = null;
        }
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
