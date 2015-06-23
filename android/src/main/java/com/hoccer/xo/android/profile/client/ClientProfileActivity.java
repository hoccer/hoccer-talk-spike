package com.hoccer.xo.android.profile.client;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ComposableActivity;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.profile.ClientProfileCreationFragment;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Activity wrapping a single profile fragment
 */
public class ClientProfileActivity extends ComposableActivity {

    private static final Logger LOG = Logger.getLogger(ClientProfileActivity.class);

    public static final String ACTION_CREATE_SELF = "com.hoccer.xo.android.profile.SingleProfileActivity.CREATE_SELF";

    public static final String ACTION_SHOW = "com.hoccer.xo.android.profile.SingleProfileActivity.SHOW";
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

            showClientProfileFragment(contactId);
        } else {
            throw new RuntimeException("Unknown or missing action");
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void showClientProfileFragment(int contactId) {
        try {
            TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
            Fragment fragment;
            if (contact.isSelf()) {
                fragment = new SelfClientProfileFragment();
            } else {
                fragment = new ContactClientProfileFragment();
            }

            Bundle bundle = new Bundle();
            bundle.putInt(ContactClientProfileFragment.ARG_CLIENT_CONTACT_ID, contactId);
            fragment.setArguments(bundle);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fl_single_profile_fragment_container, fragment);
            ft.commit();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    private void showCreateSingleProfileFragment() {
        Fragment fragment = new ClientProfileCreationFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_single_profile_fragment_container, fragment, SINGLE_PROFILE_CREATION_FRAGMENT);
        ft.commit();
    }
}
