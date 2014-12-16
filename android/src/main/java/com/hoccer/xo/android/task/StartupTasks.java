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

    private static final String PREFERENCE_NAMESPACE = "STARTUP_TASKS";

    private final static Logger LOG = Logger.getLogger(StartupTasks.class);

    public static void registerForNextStart(Context context, Class clazz) throws IllegalArgumentException {
        if (!IStartupTask.class.equals(clazz)) {
            throw new IllegalArgumentException("The provided class does not derive from IStartupTask");
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAMESPACE, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(clazz.getName(), true).apply();
    }

    public static void runRegisteredTasks(XoApplication application) {
        SharedPreferences preferences = application.getSharedPreferences(PREFERENCE_NAMESPACE, Context.MODE_PRIVATE);
        Map<String, ?> preferenceMap = preferences.getAll();
        for (String className : preferenceMap.keySet()) {
            try {
                IStartupTask task = (IStartupTask) Class.forName(className).newInstance();
                task.execute(application);
            } catch (Exception e) {
                LOG.error("Could not execute startup task '" + className + "'", e);
            }
        }

        preferences.edit().clear().apply();
    }
}
