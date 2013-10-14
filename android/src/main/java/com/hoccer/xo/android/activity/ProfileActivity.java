package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import com.actionbarsherlock.app.ActionBar;
import com.hoccer.xo.android.XoActivity;
import com.hoccer.xo.release.R;
import com.hoccer.xo.android.fragment.ProfileFragment;
import com.hoccer.talk.client.model.TalkClientContact;

import java.sql.SQLException;

/**
 * Activity wrapping a profile fragment
 */
public class ProfileActivity extends XoActivity {

    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";

    ActionBar mActionBar;

    ProfileFragment mFragment;

    TalkClientContact mContact;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_profile;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.fragment_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);

        enableUpNavigation();

        mActionBar = getSupportActionBar();

        FragmentManager fragmentManager = getSupportFragmentManager();
        mFragment = (ProfileFragment)fragmentManager.findFragmentById(R.id.activity_profile_fragment);
    }

    @Override
    protected void onResume() {
        LOG.debug("onResume()");
        super.onResume();

        Intent intent = getIntent();

        // handle show intent
        if(intent != null && intent.hasExtra(EXTRA_CLIENT_CONTACT_ID)) {
            int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);
            if(contactId == -1) {
                LOG.error("invalid contact id");
            } else {
                showProfile(refreshContact(contactId));
            }
        } else {
            if(mContact != null) {
                showProfile(refreshContact(mContact.getClientContactId()));
            }
        }
    }

    private TalkClientContact refreshContact(int contactId) {
        try {
            return getXoDatabase().findClientContactById(contactId);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
        return null;
    }

    public void showProfile(TalkClientContact contact) {
        LOG.debug("showProfile(" + contact.getClientContactId() + ")");
        mContact = contact;
        if(mContact != null) {
            mActionBar.setTitle(contact.getName());
            mFragment.showProfile(contact);
            if(contact.isDeleted()) {
                finish();
            }
        }
    }

    @Override
    public void hackReturnedFromDialog() {
        super.hackReturnedFromDialog();
        if(mContact != null) {
            showProfile(refreshContact(mContact.getClientContactId()));
        }
    }

    // XXX @Override
    public void onContactRemoved(int contactId) throws RemoteException {
        if(mContact != null && mContact.getClientContactId() == contactId) {
            finish();
        }
    }
}
