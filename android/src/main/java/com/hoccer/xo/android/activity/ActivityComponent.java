package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Base class for Activity components providing Activity callbacks.
 */
public abstract class ActivityComponent {

    private final Activity mActivity;

    protected ActivityComponent(final Activity activity) {
        mActivity = activity;
    }

    public Activity getActivity() {
        return mActivity;
    }

    @SuppressWarnings("unused")
    public void onCreate(final Bundle savedInstanceState) {
    }

    public void onStart() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onStop() {
    }

    public void onDestroy() {
    }

    public boolean onCreateOptionsMenu(final Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        return false;
    }
}
