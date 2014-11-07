package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.adapter.ChatsPageAdapter;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.content.contentselectors.IContentSelector;
import com.hoccer.xo.android.content.contentselectors.ImageSelector;
import com.hoccer.xo.android.content.contentselectors.VideoSelector;
import com.hoccer.xo.android.fragment.NearbyChatListFragment;
import com.hoccer.xo.android.fragment.SearchableListFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.ContactsMenuItemActionProvider;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class ChatsActivity extends ComposableActivity implements IXoStateListener, IXoPairingListener {

    private final static Logger LOG = Logger.getLogger(ChatsActivity.class);

    private ViewPager mViewPager;

    private boolean mEnvironmentUpdatesEnabled;
    private boolean mNoUserInput = false;
    private String mPairingToken;
    private ContactsMenuItemActionProvider mContactsMenuItemActionProvider;

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("preference_environment_update")) {
                mEnvironmentUpdatesEnabled = sharedPreferences.getBoolean("preference_environment_update", true);
                refreshEnvironmentUpdater(false);
            }
        }
    };

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{new MediaPlayerActivityComponent(this)};
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_activity_chats;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViewPager();
        initActionBar();
        determineRegistrationForEnvironmentUpdates();

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showProfileIfClientIsNotRegistered();
        refreshEnvironmentUpdater(false);
        registerListeners();
        mContactsMenuItemActionProvider.updateNotificationBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);
        menu.findItem(R.id.menu_contacts).setActionProvider(mContactsMenuItemActionProvider);
        menu.findItem(R.id.menu_my_profile).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            handleTokenPairingIntent(intent);
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            handleShareIntent(intent);
        } else if (intent.hasExtra(IntentHelper.EXTRA_CONTACT_ID)) {
            handleContactIdIntent(intent);
        } else if (intent.hasExtra(IntentHelper.EXTRA_PUSH_MESSAGE)) {
            handlePushMessageIntent(intent);
        }
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

    @Override
    public void onClientStateChange(XoClient client, int state) {
        LOG.debug("onClientStateChange:" + state);
        if (!client.isAwake()) {
            shutDownNearbySession();
        } else if (client.isActive()) {
            refreshEnvironmentUpdater(true);

            if (mPairingToken != null) {
                performTokenPairing(mPairingToken);
                mPairingToken = null;
            }
        }
    }

    private void initViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ConversationsPageListener());
        mViewPager.setAdapter(new ChatsPageAdapter(getSupportFragmentManager()));
    }

    private void initActionBar() {
        ActionBar ab = getActionBar();
        ab.setHomeButtonEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        String[] tabNames = getResources().getStringArray(R.array.tab_names);
        for (String tabName : tabNames) {
            ab.addTab(ab.newTab().setText(tabName).setTabListener(new ConversationsTabListener()));
        }

        mContactsMenuItemActionProvider = new ContactsMenuItemActionProvider(this);
    }

    private void determineRegistrationForEnvironmentUpdates() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        pref.registerOnSharedPreferenceChangeListener(mPreferencesListener);
        mEnvironmentUpdatesEnabled = pref.getBoolean("preference_environment_update", true);
    }

    private void showProfileIfClientIsNotRegistered() {
        if (!getXoClient().getSelfContact().getSelf().isRegistrationConfirmed()) {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
        }
    }

    private void registerListeners() {
        getXoClient().registerStateListener(this);
        if (mContactsMenuItemActionProvider != null) {
            getXoClient().registerContactListener(mContactsMenuItemActionProvider);
        }
    }

    private void unregisterListeners() {
        getXoClient().unregisterStateListener(this);
        if (mContactsMenuItemActionProvider != null) {
            getXoClient().unregisterContactListener(mContactsMenuItemActionProvider);
        }
    }

    private void handleContactIdIntent(Intent intent) {
        int contactId = intent.getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
        showContactConversation(contactId);
    }

    private void handlePushMessageIntent(Intent intent) {
        String message = intent.getStringExtra(IntentHelper.EXTRA_PUSH_MESSAGE);
        XoDialogs.showOkDialog("PushMessage", "", message, this);
    }

    private void handleShareIntent(Intent intent) {
        Uri contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        IContentObject contentObject = getContentObject(contentUri, intent.getType());
        addSharedContentToClipboard(contentObject);
    }

    private IContentObject getContentObject(Uri contentUri, String type) {
        IContentSelector selector = determineContentSelectorForType(type);

        // Factory method in IContentSelector expects content to  be in intent extra field 'data'
        Intent intent = new Intent();
        intent.setData(contentUri);

        return selector.createObjectFromSelectionResult(this, intent);
    }

    private IContentSelector determineContentSelectorForType(String type) {
        IContentSelector selector = null;
        if (type.startsWith("image/")) {
            selector = new ImageSelector(this);
        } else if (type.startsWith("video/")) {
            selector = new VideoSelector(this);
        }

        return selector;
    }

    private void addSharedContentToClipboard(IContentObject contentObject) {
        if (contentObject != null) {
            Clipboard.getInstance().setContent(contentObject);
            Toast.makeText(this, getString(R.string.toast_stored_file_in_clipboard), Toast.LENGTH_LONG).show();
        } else {
            Clipboard.getInstance().clearContent();
            Toast.makeText(this, R.string.toast_failed_to_store_file_in_clipboard, Toast.LENGTH_LONG).show();
        }
    }

    private void handleTokenPairingIntent(Intent intent) {
        String token = intent.getData().getHost();

        if (getXoClient().isActive()) {
            performTokenPairing(token);
        } else {
            mPairingToken = token;
        }
    }

    private void performTokenPairing(final String token) {
        getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                getXoClient().performTokenPairing(token, ChatsActivity.this);
            }
        });
    }

    @Override
    public void onTokenPairingSucceeded(String token) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatsActivity.this, R.string.toast_pairing_successful, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onTokenPairingFailed(String token) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatsActivity.this, R.string.toast_pairing_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshEnvironmentUpdater(boolean force) {
        LOG.debug("refreshEnvironmentUpdater");
        Fragment fragment = getFragmentAt(mViewPager.getCurrentItem());
        if (fragment instanceof NearbyChatListFragment) {
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

    private void startMediaBrowserActivity() {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        startActivity(intent);
    }

    private Fragment getFragmentAt(int position) {
        return ((FragmentPagerAdapter) mViewPager.getAdapter()).getItem(position);
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
                getActionBar().setSelectedNavigationItem(mViewPager.getCurrentItem());
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
            Fragment fragment = getFragmentAt(tab.getPosition());
            if (fragment instanceof SearchableListFragment) {
                ((SearchableListFragment) fragment).leaveSearchMode();
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }
}
