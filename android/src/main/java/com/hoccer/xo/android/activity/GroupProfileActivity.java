package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.base.IProfileFragmentManager;
import com.hoccer.xo.android.fragment.GroupProfileCreationFragment;
import com.hoccer.xo.android.fragment.GroupProfileFragment;
import com.artcom.hoccer.R;

/**
 * Activity wrapping a group profile fragment
 */
public class GroupProfileActivity extends ComposableActivity implements IProfileFragmentManager {

    /* use this extra to open in "client registration" mode */
    public static final String EXTRA_CLIENT_CREATE_GROUP = "clientCreateGroup";

    /* use this extra to show the given contact */
    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";
    public static final String EXTRA_MAKE_FROM_NEARBY = "fromNearby";

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[] { new MediaPlayerActivityComponent(this) };
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_group_profile;
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

        Intent intent = getIntent();

        if (intent != null) {
            if (intent.hasExtra(EXTRA_CLIENT_CREATE_GROUP)) {
                showCreateGroupProfileFragment(null);
            } else if (intent.hasExtra(EXTRA_CLIENT_CONTACT_ID)) {
                int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);
                if (contactId == -1) {
                    LOG.error("invalid contact id");
                } else {
                    showGroupProfileFragment(contactId, false, false);
                }
            } else if (intent.hasExtra(EXTRA_MAKE_FROM_NEARBY)) {
                String[] clientIds = intent.getStringArrayExtra(EXTRA_MAKE_FROM_NEARBY);
                showCreateGroupProfileFragment(clientIds);
            }
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        LOG.debug("onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        LOG.debug("onPause()");
        super.onPause();
    }

    private void showCreateGroupProfileFragment(String[] clientIds) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(GroupProfileCreationFragment.ARG_CREATE_GROUP, true);

        GroupProfileCreationFragment groupProfileFragment = new GroupProfileCreationFragment();
        groupProfileFragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_group_profile_fragment_container, groupProfileFragment);
        ft.commit();
    }

    @Override
    public void showSingleProfileFragment(int clientContactId) {
    }

    @Override
    public void showGroupProfileFragment(int groupContactId, boolean startInActionMode, boolean addToBackStack) {
        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileFragment.ARG_CLIENT_CONTACT_ID, groupContactId);

        if(startInActionMode) {
            bundle.putBoolean(GroupProfileFragment.ARG_START_IN_ACTION_MODE, true);
        } else {
            bundle.putBoolean(GroupProfileFragment.ARG_START_IN_ACTION_MODE, false);
        }

        GroupProfileFragment groupProfileFragment = new GroupProfileFragment();
        groupProfileFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_group_profile_fragment_container, groupProfileFragment);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void showGroupProfileCreationFragment(int groupContactId, boolean cloneProfile) {
        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileCreationFragment.ARG_CLIENT_CONTACT_ID, groupContactId);

        if (cloneProfile) {
            bundle.putBoolean(GroupProfileCreationFragment.ARG_CLONE_CURRENT_GROUP, true);
        }

        GroupProfileCreationFragment groupProfileCreationFragment = new GroupProfileCreationFragment();
        groupProfileCreationFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_group_profile_fragment_container, groupProfileCreationFragment);
        fragmentTransaction.commit();
    }
}
