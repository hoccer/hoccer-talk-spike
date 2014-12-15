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
    private static final Layout LOG_LOGCAT_LAYOUT = new PatternLayout("[%t] %-5p %c - %m%n");

    private static String sLogTag;
    private static Logger sRootLogger;
    private static RollingFileAppender sFileAppender;
    private static LogcatAppender      sLogcatAppender;

    /** @return directory for log files */
    private static File getLogDirectory() {
        return XoApplication.getExternalStorage();
    }

    /**
     * Initialize the logging system
     * @param application for context
     */
    public static void initialize(XoApplication application, String logTag) {
        sLogTag = logTag;
        Log.i(sLogTag, "[logging] initializing logging");

        // get the root logger for configuration
        sRootLogger = Logger.getRootLogger();

        // create logcat appender
        sLogcatAppender = new LogcatAppender(LOG_LOGCAT_LAYOUT);

        // create file appender
        try {
            File file = new File(getLogDirectory(), sLogTag + ".log");
            sFileAppender = new RollingFileAppender(LOG_FILE_LAYOUT, file.toString());
            sFileAppender.setMaximumFileSize(LOG_FILE_SIZE);
            sFileAppender.setMaxBackupIndex(LOG_FILE_COUNT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // apply initial configuration
        configureLogLevel(application.getConfiguration().getLogLevel());
        configureLogLogcat(application.getConfiguration().isLoggingToLogcatEnabled());
        configureLogSd(application.getConfiguration().isLoggingToSdEnabled());
        configureClassLogLevels();
    }

    private static void configureClassLogLevels() {
        // This noisy stuff we mostly dont't want
        Logger.getLogger(com.j256.ormlite.stmt.mapped.BaseMappedStatement.class).setLevel(Level.WARN);
        Logger.getLogger(com.j256.ormlite.stmt.SelectIterator.class).setLevel(Level.WARN);
        Logger.getLogger(com.j256.ormlite.stmt.StatementBuilder.class).setLevel(Level.WARN);
        Logger.getLogger(com.j256.ormlite.stmt.StatementExecutor.class).setLevel(Level.WARN);
    }

    private static void configureLogLevel(String levelString) {
        Log.i(sLogTag, "[logging] setting log level to " + levelString);
        Level level = Level.toLevel(levelString);
        sRootLogger.setLevel(level);
    }

    private static void configureLogSd(boolean enabled) {
        Log.i(sLogTag, "[logging] " + (enabled ? "enabling" : "disabling") + " logging to SD card");
        if(enabled) {
            XoApplication.ensureDirectory(getLogDirectory());
            sRootLogger.addAppender(sFileAppender);
        } else {
            sRootLogger.removeAppender(sFileAppender);
        }
    }

    private static void configureLogLogcat(boolean enabled) {
        Log.i(sLogTag, "[logging] " + (enabled ? "enabling" : "disabling") + " logging to logcat");
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
                    Log.w(sLogTag, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.w(sLogTag, message);
                }
            } else if(level == Level.ERROR_INT) {
                if(event.getThrowableInformation() != null) {
                    Log.e(sLogTag, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.e(sLogTag, message);
                }
            } else if(level == Level.FATAL_INT) {
                if(event.getThrowableInformation() != null) {
                    Log.wtf(sLogTag, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.wtf(sLogTag, message);
                }
            } else {
                if(event.getThrowableInformation() != null) {
                    Log.i(sLogTag, message, event.getThrowableInformation().getThrowable());
                } else {
                    Log.i(sLogTag, message);
                }
            }
        }

        @Override
        public void close() {
            Log.v(sLogTag, "[logging] logcat appender closed");
        }

        @Override
        public boolean requiresLayout() {
            return true;
        }
    }

}
