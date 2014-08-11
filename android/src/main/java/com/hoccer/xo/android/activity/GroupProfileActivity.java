package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.GroupProfileFragment;
import com.hoccer.xo.release.R;

/**
 * Activity wrapping a single profile fragment
 */
public class GroupProfileActivity extends XoActionbarActivity
        implements IXoContactListener {

    /* use this extra to open in "client registration" mode */
    public static final String EXTRA_CLIENT_CREATE_GROUP = "clientCreateGroup";

    /* use this extra to show the given contact */
    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";
    public static final String EXTRA_MAKE_FROM_NEARBY = "fromNearby";

    ActionBar mActionBar;

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

        mActionBar = getActionBar();

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
                createGroupFromNearby(clientIds);
            }

        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        LOG.debug("onResume()");
        super.onResume();

        getXoClient().registerContactListener(this);
    }

    @Override
    protected void onPause() {
        LOG.debug("onPause()");
        super.onPause();

        getXoClient().unregisterContactListener(this);
    }

    private boolean isMyContact(TalkClientContact contact) {
        TalkClientContact myContact = mGroupProfileFragment.getContact();
        return myContact != null && myContact.getClientContactId() == contact.getClientContactId();
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        // we don't care
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        if (isMyContact(contact)) {
            finish();
        }
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        processContactUpdate(contact);
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        processContactUpdate(contact);
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        processContactUpdate(contact);
    }

    private void createGroupFromNearby(String[] clientIds) {
        LOG.debug("createGroupFromNearby()");
        mGroupProfileFragment.createGroupFromNearby(clientIds);
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        processContactUpdate(contact);
    }

    void processContactUpdate(TalkClientContact contact) {
        if (isMyContact(contact)) {
            if (contact.isDeleted()) {
                finish();
            } else {
                mGroupProfileFragment.updateActionBar();
            }
        }
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
