package com.hoccer.xo.android.profile.group;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.activity.ComposableActivity;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Activity wrapping a group profile fragment
 */
public class GroupProfileActivity extends ComposableActivity {

    private static final Logger LOG = Logger.getLogger(GroupProfileActivity.class);

    public static final String ACTION_CREATE = "com.hoccer.xo.android.profile.group.GroupProfileActivity.CREATE";

    public static final String ACTION_CLONE = "com.hoccer.xo.android.profile.group.GroupProfileActivity.CLONE";
    public static final String EXTRA_GROUP_ID = "clientCloneGroup";

    public static final String ACTION_SHOW = "com.hoccer.xo.android.profile.group.GroupProfileActivity.SHOW";
    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";

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
        String action = intent.getAction();

        if (ACTION_CREATE.equals(action)) {
            showGroupProfileCreationFragment(null);
        } else if (ACTION_CLONE.equals(action)) {
            String cloneGroupId = intent.getStringExtra(EXTRA_GROUP_ID);

            if (cloneGroupId == null) {
                throw new RuntimeException("Missing EXTRA_GROUP_ID");
            }

            showGroupProfileCreationFragment(cloneGroupId);
        } else if (ACTION_SHOW.equals(action)) {
            int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);

            if (contactId == -1) {
                throw new RuntimeException("Missing EXTRA_CLIENT_CONTACT_ID");
            }

            showGroupProfileFragment(contactId, false);
        } else {
            throw new RuntimeException("Unknown or missing action");
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void showGroupProfileCreationFragment(@Nullable String cloneGroupId) {
        GroupProfileCreationFragment groupProfileCreationFragment = new GroupProfileCreationFragment();

        if (cloneGroupId != null) {
            Bundle bundle = new Bundle();
            bundle.putString(GroupProfileCreationFragment.ARG_CLONE_GROUP_ID, cloneGroupId);
            groupProfileCreationFragment.setArguments(bundle);
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_group_profile_fragment_container, groupProfileCreationFragment);
        fragmentTransaction.commit();
    }

    public void showGroupProfileFragment(int groupContactId, boolean startInActionMode) {
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
        fragmentTransaction.commit();
    }
}
