package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.apache.log4j.Logger;


public class CrashMonitor implements Thread.UncaughtExceptionHandler {

    private static final Logger LOG = Logger.getLogger(CrashMonitor.class);

    private static CrashMonitor INSTANCE;
    private final Context mApplicationContext;
    private boolean mCrashedBefore = false;

    private Thread.UncaughtExceptionHandler mPreviousHandler;

    private CrashMonitor(Context applicationContext)  {
        mApplicationContext = applicationContext;
        mPreviousHandler = Thread.getDefaultUncaughtExceptionHandler();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        mCrashedBefore = sharedPreferences.getBoolean("crash", false);;
    }

    public static CrashMonitor get(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CrashMonitor(context.getApplicationContext());
        }
        return INSTANCE;
    }

    public void saveCrashState(boolean crashed) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("crash", crashed);
        editor.apply();
    }

    public boolean isCrashedBefore() {
        return mCrashedBefore;
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
