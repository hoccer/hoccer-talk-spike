package com.hoccer.xo.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.passwordprotection.PasswordProtection;
import com.hoccer.xo.android.passwordprotection.activity.PasswordPromptActivity;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


public class BackgroundManager implements Application.ActivityLifecycleCallbacks {

    private static final Logger LOG = Logger.getLogger(BackgroundManager.class);

    public static final long BACKGROUND_DELAY = 500;

    private static BackgroundManager sInstance;

    public interface Listener {
        public void onBecameForeground(Activity activity);

        public void onBecameBackground(Activity activity);
    }

    private boolean mInBackground = true;
    private final List<Listener> listeners = new ArrayList<Listener>();
    private final Handler mBackgroundDelayHandler = new Handler();
    private Runnable mBackgroundTransition;

    public static BackgroundManager get() {
        if (sInstance == null) {
            sInstance = new BackgroundManager();
        }
        return sInstance;
    }

    private BackgroundManager() {
        XoApplication.get().registerActivityLifecycleCallbacks(this);
    }

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isInBackground() {
        return mInBackground;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (mBackgroundTransition != null) {
            mBackgroundDelayHandler.removeCallbacks(mBackgroundTransition);
            mBackgroundTransition = null;
        }

        if (mInBackground) {
            mInBackground = false;
            notifyOnBecameForeground(activity);
            LOG.info("Application went to foreground");
        }
    }

    private void notifyOnBecameForeground(Activity activity) {
        for (Listener listener : listeners) {
            try {
                listener.onBecameForeground(activity);
            } catch (Exception e) {
                LOG.error("Listener threw exception!", e);
            }
        }
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        if (!mInBackground && mBackgroundTransition == null) {
            mBackgroundTransition = new Runnable() {
                @Override
                public void run() {
                    mInBackground = true;
                    mBackgroundTransition = null;
                    notifyOnBecameBackground(activity);
                    LOG.info("Application went to background");
                }
            };
            mBackgroundDelayHandler.postDelayed(mBackgroundTransition, BACKGROUND_DELAY);
        }
    }

    private void notifyOnBecameBackground(Activity activity) {
        for (Listener listener : listeners) {
            try {
                listener.onBecameBackground(activity);
            } catch (Exception e) {
                LOG.error("Listener threw exception!", e);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
