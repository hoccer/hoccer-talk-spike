package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.GroupProfileFragment;
import com.hoccer.xo.release.R;

/**
 * Activity wrapping a group profile fragment
 */
public class GroupProfileActivity extends XoActionbarActivity {

    /* use this extra to open in "client registration" mode */
    public static final String EXTRA_CLIENT_CREATE_GROUP = "clientCreateGroup";

    /* use this extra to show the given contact */
    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";
    public static final String EXTRA_MAKE_FROM_NEARBY = "fromNearby";

    GroupProfileFragment mGroupProfileFragment;

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
                showCreateGroupProfileFragment();
            } else if (intent.hasExtra(EXTRA_CLIENT_CONTACT_ID)) {
                int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);
                if (contactId == -1) {
                    LOG.error("invalid contact id");
                } else {
                    showGroupProfileFragment(contactId);
                }
            } else if (intent.hasExtra(EXTRA_MAKE_FROM_NEARBY)) {
                String[] clientIds = intent.getStringArrayExtra(EXTRA_MAKE_FROM_NEARBY);
                mGroupProfileFragment.createGroupFromNearby(clientIds);
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

    private void showGroupProfileFragment(int contactId) {
        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileFragment.ARG_CLIENT_CONTACT_ID, contactId);

        mGroupProfileFragment = new GroupProfileFragment();
        mGroupProfileFragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_group_profile_fragment_container, mGroupProfileFragment);
        ft.commit();
    }

    private void showCreateGroupProfileFragment() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(GroupProfileFragment.ARG_CREATE_GROUP, true);

        mGroupProfileFragment = new GroupProfileFragment();
        mGroupProfileFragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_group_profile_fragment_container, mGroupProfileFragment);
        ft.commit();
    }
}
