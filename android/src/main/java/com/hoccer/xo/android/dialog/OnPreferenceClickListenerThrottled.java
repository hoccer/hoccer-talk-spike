package com.hoccer.xo.android.dialog;

import android.preference.Preference;

public abstract class OnPreferenceClickListenerThrottled implements Preference.OnPreferenceClickListener{

    private final long mTimeDelta;

    private long mLastClicked;

    protected OnPreferenceClickListenerThrottled(long timeDelta) {
        mTimeDelta = timeDelta;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        long delta = System.currentTimeMillis() - mLastClicked;

        if (delta > mTimeDelta) {
            onPreferenceClickThrottled();
            mLastClicked = System.currentTimeMillis();
        }

        return true;
    }

    public abstract boolean onPreferenceClickThrottled();
}
