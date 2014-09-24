package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;


/**
 * Base class for activities composed of ActivityComponents
 * This class is part of the component based design pattern to favor composition over inheritance in some cases.
 */
public abstract class ComposableActivity extends FragmentActivity{

    private ActivityComponent[] mComponents;

    /*
     * Needs to be implemented in a derived class.
     * Returns an array of all ActivityComponents used by this Activity.
     */
    protected abstract ActivityComponent[] createComponents();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set list of components
        mComponents = createComponents();

        for(final ActivityComponent component : mComponents) {
            component.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        for(final ActivityComponent component : mComponents) {
            component.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for(final ActivityComponent component : mComponents) {
            component.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        for(int i = mComponents.length - 1; i >= 0; i--) {
            mComponents[i].onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for(int i = mComponents.length - 1; i >= 0; i--) {
            mComponents[i].onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for(int i = mComponents.length - 1; i >= 0; i--) {
            mComponents[i].onDestroy();
        }
    }
}
