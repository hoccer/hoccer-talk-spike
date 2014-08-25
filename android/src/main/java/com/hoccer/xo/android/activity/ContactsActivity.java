package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.ContactsPageAdapter;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.ContactsFragment;
import com.hoccer.xo.android.fragment.NearbyContactsFragment;
import com.hoccer.xo.android.fragment.SearchableListFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class ContactsActivity extends XoActionbarActivity implements IXoStateListener {

    private final static Logger LOG = Logger.getLogger(ContactsActivity.class);

    private ViewPager mViewPager;
    private ActionBar mActionBar;
    private ContactsPageAdapter mAdapter;

    private boolean mEnvironmentUpdatesEnabled;
    private boolean mNoUserInput = false;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.fragment_contacts;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] tabs = getResources().getStringArray(R.array.tab_names);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ConversationsPageListener());

        mActionBar = getActionBar();
        mAdapter = new ContactsPageAdapter(getSupportFragmentManager(), tabs.length);
        mViewPager.setAdapter(mAdapter);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (String tabName : tabs) {
            mActionBar.addTab(mActionBar.newTab().setText(tabName).setTabListener(new ConversationsTabListener()));
        }

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // TODO: remove, was only for debug purposes to manually active environment updates before there was a UI for that
        SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("preference_environment_update")) {
                    mEnvironmentUpdatesEnabled = sharedPreferences.getBoolean("preference_environment_update", true);
                    refreshEnvironmentUpdater(false);
                }
            }
        };
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesListener);
        mEnvironmentUpdatesEnabled = mPreferences.getBoolean("preference_environment_update", true);

        getXoClient().registerStateListener(this);

        // if the client is not yet registered start the registration procedure
        if (!getXoClient().isRegistered()) {
            Intent intent = new Intent(this, SingleProfileActivity.class);
            intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CREATE_SELF, true);
            startActivity(intent);
        }

        // check wether we should immediatly open the conversation with a contact
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(IntentHelper.EXTRA_CONTACT_ID)) {
            int contactId = intent.getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
            showContactConversation(contactId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getXoClient().unregisterStateListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getXoClient().unregisterStateListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEnvironmentUpdater(false);
        getXoClient().registerStateListener(this);
    }

    private void refreshEnvironmentUpdater(boolean force) {
        LOG.debug("refreshEnvironmentUpdater");
        int position = mViewPager.getCurrentItem();
        Fragment fragment = mAdapter.getItem(position);
        if (fragment instanceof NearbyContactsFragment) {
            if (mEnvironmentUpdatesEnabled) {
                if (isLocationServiceEnabled()) {
                    LOG.debug("refreshEnvironmentUpdater:startNearbySession");
                    XoApplication.startNearbySession(force);
                }
            }
        } else {
            shutDownNearbySession();
        }
    }

    private void shutDownNearbySession() {
        LOG.debug("shutDownNearbySession");
        XoApplication.stopNearbySession();
        NearbyContactsFragment nearbyContactsFragment = (NearbyContactsFragment) mAdapter.getItem(2);
        nearbyContactsFragment.shutdownNearbyChat();
    }

    private boolean isLocationServiceEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            XoDialogs.showYesNoDialog("EnableLocationServiceDialog",
                    R.string.dialog_enable_location_service_title,
                    R.string.dialog_enable_location_service_message,
                    this,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    }
            );
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_audio_attachment_list:
                startMediaBrowserActivity();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startMediaBrowserActivity() {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        startActivity(intent);
    }

    private class ConversationsPageListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            refreshEnvironmentUpdater(false);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                mNoUserInput = true;
                mActionBar.setSelectedNavigationItem(mViewPager.getCurrentItem());
                mNoUserInput = false;
            }
        }
    }

    private class ConversationsTabListener implements ActionBar.TabListener {

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (!mNoUserInput) {
                mViewPager.setCurrentItem(tab.getPosition());
            }
            getXoClient().wake();
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            Fragment fragment = mAdapter.getItem(tab.getPosition());
            if (fragment instanceof SearchableListFragment) {
                ((SearchableListFragment) fragment).leaveSearchMode();
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }

    @Override
    public void onClientStateChange(XoClient client, int state) {
        LOG.debug("onClientStateChange:"+state);
        if (!client.isAwake()) {
            shutDownNearbySession();
        } else if (client.isActive()) {
            refreshEnvironmentUpdater(true);
        }
    }

}
