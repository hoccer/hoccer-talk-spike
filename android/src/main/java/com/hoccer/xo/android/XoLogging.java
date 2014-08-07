package com.hoccer.xo.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.io.IOException;

/**
 * Static logging routines
 *
 * This class initializes and terminates our logging system.
 */
public class XoLogging {

    private static final Layout LOG_FILE_LAYOUT = new PatternLayout("[%t] %-5p %c - %m%n");
    private static final int LOG_FILE_SIZE = 1024 * 1024;
    private static final int LOG_FILE_COUNT = 10;
    private static final String LOG_FILE_BASENAME = "hoccer-xo.log";
    private static final Layout LOG_LOGCAT_LAYOUT = new PatternLayout("[%t] %-5p %c - %m%n");
    private final static String LOG_TAG = "HoccerXO";

    /** SharedPreferences to listen on */
    private static SharedPreferences sPreferences;
    /** Listener watching preferences */
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener;

    /** The root logger */
    private static Logger sRootLogger;

    /** Log file appender */
    private static RollingFileAppender sFileAppender;
    /** Logcat appender */
    private static LogcatAppender      sLogcatAppender;

    /** @return directory for log files */
    private static File getLogDirectory() {
        return XoApplication.getExternalStorage();
    }

    /**
     * Initialize the logging system
     * @param application for context
     */
    public static void initialize(XoApplication application) {
        Log.i(LOG_TAG, "[logging] initializing logging");

        // get the root logger for configuration
        sRootLogger = Logger.getRootLogger();

        // create logcat appender
        sLogcatAppender = new LogcatAppender(LOG_LOGCAT_LAYOUT);

        // create file appender
        try {
            File file = new File(getLogDirectory(), LOG_FILE_BASENAME);
            sFileAppender = new RollingFileAppender(LOG_FILE_LAYOUT, file.toString());
            sFileAppender.setMaximumFileSize(LOG_FILE_SIZE);
            sFileAppender.setMaxBackupIndex(LOG_FILE_COUNT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // attach preference listener
        sPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        sPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("preference_log_logcat")) {
                    configureLogLogcat();
                }
                if(key.equals("preference_log_sd")) {
                    configureLogSd();
                }
                if(key.equals("preference_log_level")) {
                    configureLogLevel();
                }
            }
        };
        sPreferences.registerOnSharedPreferenceChangeListener(sPreferencesListener);

        // apply initial configuration
        configureLogLevel();
        configureLogLogcat();
        configureLogSd();
    }

    /**
     * Shut down the logging system
     */
    public static final void shutdown() {
        if(sPreferencesListener != null) {
            sPreferences.unregisterOnSharedPreferenceChangeListener(sPreferencesListener);
            sPreferencesListener = null;
        }
    }

    private static void configureLogLevel() {
        String levelString = sPreferences.getString("preference_log_level", "INFO");
        Log.i(LOG_TAG, "[logging] setting log level to " + levelString);
        Level level = Level.toLevel(levelString);
        sRootLogger.setLevel(level);
    }

    private static void configureLogSd() {
        boolean enabled = sPreferences.getBoolean("preference_log_sd", false);
        Log.i(LOG_TAG, "[logging] " + (enabled ? "enabling" : "disabling") + " logging to SD card");
        if(enabled) {
            XoApplication.ensureDirectory(getLogDirectory());
            sRootLogger.addAppender(sFileAppender);
        } else {
            sRootLogger.removeAppender(sFileAppender);
        }
    }

    private static void configureLogLogcat() {
        boolean enabled = sPreferences.getBoolean("preference_log_logcat", true);
        Log.i(LOG_TAG, "[logging] " + (enabled ? "enabling" : "disabling") + " logging to logcat");
        if(enabled) {
            sRootLogger.addAppender(sLogcatAppender);
        } else {
            sRootLogger.removeAppender(sLogcatAppender);
        }
    }

    private static class LogcatAppender extends AppenderSkeleton {

        public LogcatAppender(Layout layout) {
            super();
            setLayout(layout);
        }

        @Override
        protected void append(LoggingEvent event) {
            String message = getLayout().format(event);
            int level = event.getLevel().toInt();
            if(level == Level.WARN_INT) {
                if(event.getThrowableInformation() != null) {
                    Log.w(LOG_TAG, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.w(LOG_TAG, message);
                }
            } else if(level == Level.ERROR_INT) {
                if(event.getThrowableInformation() != null) {
                    Log.e(LOG_TAG, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.e(LOG_TAG, message);
                }
            } else if(level == Level.FATAL_INT) {
                if(event.getThrowableInformation() != null) {
                    Log.wtf(LOG_TAG, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.wtf(LOG_TAG, message);
                }
            } else {
                if(event.getThrowableInformation() != null) {
                    Log.i(LOG_TAG, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.i(LOG_TAG, message);
                }
            }
        }

        @Override
        public void close() {
            Log.v(LOG_TAG, "[logging] logcat appender closed");
        }

        @Override
        public boolean requiresLayout() {
            return true;
        }
    }

}
