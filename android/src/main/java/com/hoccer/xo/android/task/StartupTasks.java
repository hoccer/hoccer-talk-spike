package com.hoccer.xo.android.task;

import android.content.Context;
import android.content.SharedPreferences;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.util.Map;

/*
 * Registers IStartupTask implementations to run on next startup.
 */
public class StartupTasks {

    private final static Logger LOG = Logger.getLogger(StartupTasks.class);

    private static XoApplication mApplication;
    private static SharedPreferences mPreferences;
    private static final String PREFERENCE_NAMESPACE = "STARTUP_TASKS";


    public static void initialize(XoApplication application) {
        if(mApplication != null) {
            throw new IllegalStateException("StartupTasks already initialized");
        }

        mApplication = application;
        mPreferences = application.getSharedPreferences(PREFERENCE_NAMESPACE, Context.MODE_PRIVATE);
    }

    /*
     * Registers the given class to be executed once on next application start.
     * @param clazz Must implement IStartupTask
     */
    public static void registerForNextStart(Class clazz) throws IllegalArgumentException {
        if (!IStartupTask.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("The provided class does not implement IStartupTask");
        }

        mPreferences.edit().putBoolean(clazz.getName(), true).apply();
    }

    /*
     * Executes all registered tasks once and unregisters all afterwards.
     * @note Should be called once at startup.
     */
    public static void executeRegisteredTasks() {
        Map<String, ?> preferenceMap = mPreferences.getAll();
        for (String className : preferenceMap.keySet()) {
            try {
                IStartupTask task = (IStartupTask) Class.forName(className).newInstance();
                task.execute(mApplication);
            } catch (Exception e) {
                LOG.error("Could not execute startup task '" + className + "'", e);
            }
        }

        mPreferences.edit().clear().apply();
    }
}
