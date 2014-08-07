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

    /** If true, GCM registration will be performed forcibly on every connect */
    public static final boolean GCM_ALWAYS_REGISTER = false;
    /** If true, GCM registration should always be pushed to server */
    public static final boolean GCM_ALWAYS_UPDATE = true;
    /** GCM sender id for push notifications */
    public static final String GCM_SENDER_ID = "1894273085";
    /** GCM server registration expiration (seconds) */
    public static final long GCM_REGISTRATION_EXPIRATION = 24 * 3600;

    /** Log tag to use in logcat */
    public static final String LOG_LOGCAT_TAG = "HoccerXO";
    /** The layout for android logcat */
    public static final Layout LOG_LOGCAT_LAYOUT = new PatternLayout("[%t] %-5p %c - %m%n");
    /** Base name of log files */
    public static final String LOG_FILE_NAME = "hoccer-xo.log";
    /** The maximum number of log files to keep */
    public static final int LOG_FILE_COUNT = 10;
    /** The maximum size of each log file */
    public static final int LOG_FILE_SIZE = 1024 * 1024;
    /** The layout for log files */
    public static final Layout LOG_FILE_LAYOUT = new PatternLayout("[%t] %-5p %c - %m%n");

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
