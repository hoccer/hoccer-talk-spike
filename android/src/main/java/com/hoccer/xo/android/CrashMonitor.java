package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.apache.log4j.Logger;


public class CrashMonitor implements Thread.UncaughtExceptionHandler {

    private static final Logger LOG = Logger.getLogger(CrashMonitor.class);

    private static CrashMonitor INSTANCE;
    private final Context mContext;

    private Thread.UncaughtExceptionHandler mPreviousHandler;

    private CrashMonitor(Context context) {
        mContext = context;
        mPreviousHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static CrashMonitor get(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CrashMonitor(context);
        }
        return INSTANCE;
    }

    public void saveCrashState(boolean crashed) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("crash", crashed);
        editor.apply();
    }

    public boolean isCrashedBefore() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sharedPreferences.getBoolean("crash", false);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LOG.error("uncaught exception on thread " + thread.getName(), ex);
        saveCrashState(true);
        if (mPreviousHandler != null) {
            mPreviousHandler.uncaughtException(thread, ex);
        }
    }
}
