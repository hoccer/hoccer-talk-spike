package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
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
    public static final String SINGLE_PROFILE_FRAGMENT = "SINGLE_PROFILE_FRAGMENT";

    ActionBar mActionBar;

    SingleProfileFragment mSingleProfileFragment;

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

        mSingleProfileFragment = new SingleProfileFragment();
        mSingleProfileFragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, mSingleProfileFragment);
        ft.commit();
    }

    private void showCreateSingleProfileFragment() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(SingleProfileFragment.ARG_CREATE_SELF, true);

        mSingleProfileFragment = new SingleProfileFragment();
        mSingleProfileFragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, mSingleProfileFragment, SINGLE_PROFILE_FRAGMENT);
        ft.commit();
    }
}
