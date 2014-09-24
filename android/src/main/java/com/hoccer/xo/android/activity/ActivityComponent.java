package com.hoccer.xo.android.activity;

import android.os.Bundle;

/**
 * Base class for Activity components providing Activity callbacks.
 */
public abstract class ActivityComponent {

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
