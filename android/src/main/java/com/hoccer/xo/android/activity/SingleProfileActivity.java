package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.fragment.SingleProfileCreationFragment;
import com.hoccer.xo.android.fragment.SingleProfileFragment;
import com.hoccer.xo.release.R;

/**
 * Activity wrapping a single profile fragment
 */
public class SingleProfileActivity extends ComposableActivity {

    /* use this extra to open in "client registration" mode */
    public static final String EXTRA_CLIENT_CREATE_SELF = "clientCreateSelf";

    /* use this extra to show the given contact */
    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";
    public static final String SINGLE_PROFILE_CREATION_FRAGMENT = "SINGLE_PROFILE_CREATION_FRAGMENT";

    ActionBar mActionBar;

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[] { new MediaPlayerActivityComponent(this) };
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_single_profile;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);

        enableUpNavigation();

        mActionBar = getActionBar();

        Intent intent = getIntent();

        if (intent != null) {
            if (intent.hasExtra(EXTRA_CLIENT_CREATE_SELF)) {
                showCreateSingleProfileFragment();
            } else if (intent.hasExtra(EXTRA_CLIENT_CONTACT_ID)) {
                int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);
                if (contactId == -1) {
                    LOG.error("invalid contact id");
                } else {
                    showSingleProfileFragment(contactId);
                }
            }
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void showSingleProfileFragment(int contactId) {
        Bundle bundle = new Bundle();
        bundle.putInt(SingleProfileFragment.ARG_CLIENT_CONTACT_ID, contactId);

        Fragment fragment = new SingleProfileFragment();
        fragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, fragment);
        ft.commit();
    }

    private void showCreateSingleProfileFragment() {
        Fragment fragment = new SingleProfileCreationFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, fragment, SINGLE_PROFILE_CREATION_FRAGMENT);
        ft.commit();
    }
}
