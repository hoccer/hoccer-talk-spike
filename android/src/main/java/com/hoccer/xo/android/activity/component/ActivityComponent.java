package com.hoccer.xo.android.activity.component;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Base class for Activity components providing Activity callbacks.
 */
public abstract class ActivityComponent {

    private final FragmentActivity mActivity;

    protected ActivityComponent(FragmentActivity activity) {
        mActivity = activity;
    }

    public FragmentActivity getActivity() {
        return mActivity;
    }

    @SuppressWarnings("unused")
    public void onCreate(Bundle savedInstanceState) {}

    public void onStart() {}

    public void onResume() {}

    public void onPause() {}

    public void onStop() {}

    public void onDestroy() {}

    @SuppressWarnings("unused")
    public void onSaveInstanceState(Bundle instanceState) {}

    @SuppressWarnings("unused")
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @SuppressWarnings("unused")
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }
}
