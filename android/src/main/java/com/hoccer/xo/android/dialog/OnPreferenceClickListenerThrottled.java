package com.hoccer.xo.android.dialog;

import android.preference.Preference;

public abstract class OnPreferenceClickListenerThrottled implements Preference.OnPreferenceClickListener{

    private final long mTimeDelta;

    long mLastClicked;

    protected OnPreferenceClickListenerThrottled(long timeDelta) {
        mTimeDelta = timeDelta;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        long delta = Math.abs(mLastClicked - System.currentTimeMillis());
        if (delta > mTimeDelta) {
            onPreferenceClickThrottled();
            mLastClicked = System.currentTimeMillis();
        }
        return true;
    }

    public abstract boolean onPreferenceClickThrottled();
}
