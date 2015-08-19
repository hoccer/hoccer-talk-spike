package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.base.BaseActivity;


/**
 * Base class for activities composed of ActivityComponents
 * This class is part of the component based design pattern to favor composition over inheritance in some cases.
 */
public abstract class ComposableActivity extends BaseActivity {

    private final ActivityComponent[] mComponents;

    /*
     * Needs to be implemented in a derived class.
     * Returns an array of all ActivityComponents used by this Activity.
     */
    protected abstract ActivityComponent[] createComponents();

    protected ComposableActivity() {
        super();

        mComponents = createComponents();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (ActivityComponent component : mComponents) {
            component.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (ActivityComponent component : mComponents) {
            component.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (ActivityComponent component : mComponents) {
            component.onResume();
        }
    }

    /*
     * Returns false if one of the components returns false.
     * Returns base class return value otherwise.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        if (result) {
            for (ActivityComponent component : mComponents) {
                if (!component.onCreateOptionsMenu(menu)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isConsumed = super.onOptionsItemSelected(item);

        if (!isConsumed) {
            for (ActivityComponent component : mComponents) {
                if (component.onOptionsItemSelected(item)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        for (int i = mComponents.length - 1; i >= 0; i--) {
            mComponents[i].onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (int i = mComponents.length - 1; i >= 0; i--) {
            mComponents[i].onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (int i = mComponents.length - 1; i >= 0; i--) {
            mComponents[i].onDestroy();
        }
    }
}
