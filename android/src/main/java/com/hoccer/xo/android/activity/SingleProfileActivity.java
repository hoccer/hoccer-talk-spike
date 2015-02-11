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
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

/**
 * Activity wrapping a single profile fragment
 */
public class SingleProfileActivity extends ComposableActivity {

    private static final Logger LOG = Logger.getLogger(SingleProfileActivity.class);

    public static final String ACTION_CREATE_SELF = "com.hoccer.xo.android.activity.SingleProfileActivity.CREATE_SELF";

    public static final String ACTION_SHOW = "com.hoccer.xo.android.activity.SingleProfileActivity.SHOW";
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
        String action = intent.getAction();

        if (ACTION_CREATE_SELF.equals(action)) {
            showCreateSingleProfileFragment();
        } else if (ACTION_SHOW.equals(action)) {
            int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);

            if (contactId == -1) {
                throw new RuntimeException("Missing EXTRA_CLIENT_CONTACT_ID");
            }

            showSingleProfileFragment(contactId);
        } else {
            throw new RuntimeException("Unknown or missing action");
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
