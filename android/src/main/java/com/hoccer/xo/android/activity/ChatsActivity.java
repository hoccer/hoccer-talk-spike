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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.adapter.ChatsPageAdapter;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.content.contentselectors.IContentSelector;
import com.hoccer.xo.android.content.contentselectors.ImageSelector;
import com.hoccer.xo.android.content.contentselectors.VideoSelector;
import com.hoccer.xo.android.fragment.NearbyChatListFragment;
import com.hoccer.xo.android.fragment.SearchableListFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.ContactsMenuItemActionProvider;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ChatsActivity extends ComposableActivity implements IXoStateListener, IXoPairingListener {

    private final static Logger LOG = Logger.getLogger(ChatsActivity.class);
    private static final String ACTION_ALREADY_HANDLED = "com.hoccer.xo.android.intent.action.ALREADY_HANDLED";

    private ViewPager mViewPager;

    private boolean mEnvironmentUpdatesEnabled;
    private boolean mNoUserInput = false;
    private String mPairingToken;
    private ContactsMenuItemActionProvider mContactsMenuItemActionProvider;
    private String[] mTabNames;

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener  = new SharedPreferences.OnSharedPreferenceChangeListener() {
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
        return new ActivityComponent[] { new MediaPlayerActivityComponent(this) };
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

        initTabs();
        initActionBar();
        determineRegistrationForEnvironmentUpdates();
        showProfileIfClientIsNotRegistered();

        // check whether we should immediately open the conversation with a contact
        handleIntentActions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEnvironmentUpdater(false);
        registerListeners();
        handleTokenPairingIntent(getIntent());
        mContactsMenuItemActionProvider.evaluateNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);
        menu.findItem(R.id.menu_contacts).setActionProvider(mContactsMenuItemActionProvider);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleTokenPairingIntent(intent);
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
    private void initTabs() {
        mTabNames = getResources().getStringArray(R.array.tab_names);
        initViewPager(new ChatsPageAdapter(getSupportFragmentManager(), mTabNames.length));
    }

    private void initViewPager(PagerAdapter adapter) {
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ConversationsPageListener());
        mViewPager.setAdapter(adapter);
    }

    private void initActionBar() {
        ActionBar ab = getActionBar();
        ab.setHomeButtonEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (String tabName : mTabNames) {
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
        if (!getXoClient().isRegistered()) {
            Intent intent = new Intent(this, SingleProfileActivity.class);
            intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CREATE_SELF, true);
            startActivity(intent);
        }
    }

    private IContentObject getContentObject(Uri contentUri, String type) {
        IContentSelector s = determineContentSelectorForType(type);
        // Factory method in IContentSelector expects content to  be in intent extra field 'data'
        return createContentObjectWithSelectorForIntent(s, createDataIntent(contentUri));
    }

    private Intent createDataIntent(Uri contentUri) {
        Intent i = new Intent();
        i.setData(contentUri);
        return i;
    }

    private IContentSelector determineContentSelectorForType(String type) {
        IContentSelector s = null;
        if (type.startsWith("image/")) {
            s = new ImageSelector(this);
        } else if (type.startsWith("video/")) {
            s = new VideoSelector(this);
        }

        return s;
    }

    private IContentObject createContentObjectWithSelectorForIntent(IContentSelector selector, Intent intent) {
        return selector.createObjectFromSelectionResult(this, intent);
    }

    private void handleIntentActions() {
        Intent i = getIntent();
        if (i != null) {
            if (i.getAction() == Intent.ACTION_SEND) {
                Uri contentUri = i.getParcelableExtra(Intent.EXTRA_STREAM);
                IContentObject co = getContentObject(contentUri, i.getType());
                addSharedContentToClipboard(co);
            }
            if (i.hasExtra(IntentHelper.EXTRA_CONTACT_ID)) {
                showContactConversation(i.getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1));
            }
        }
    }

    private void addSharedContentToClipboard(IContentObject contentObject) {
        if (contentObject != null) {
            // Clipboard only works with TalkClientUpload and TalkClientDownload so we have to create one
            // unfortunately this Upload object will be a dead entry in the database since the attachment selection recreates the Upoad Object
            // see CompositionFragment.validateAndSendComposedMessage()
            TalkClientUpload attachmentUpload = SelectedContent.createAttachmentUpload(contentObject);
            try {
                getXoDatabase().saveClientUpload(attachmentUpload);
                Clipboard clipboard = Clipboard.get(this);
                clipboard.storeAttachment(attachmentUpload);
                Toast.makeText(this, getString(R.string.toast_stored_external_file_to_clipboard), Toast.LENGTH_LONG).show();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

    private void handleTokenPairingIntent(Intent intent) {
        if (intent.getAction() == Intent.ACTION_VIEW) {
            String token = intent.getData().getHost();
            intent.setAction(ACTION_ALREADY_HANDLED);

            if (getXoClient().isActive()) {
                performTokenPairing(token);
            } else {
                mPairingToken = token;
            }
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

    private void refreshEnvironmentUpdater(boolean force) {
        LOG.debug("refreshEnvironmentUpdater");
        Fragment f = getFragmentAt(mViewPager.getCurrentItem());
        if (f instanceof NearbyChatListFragment) {
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
        NearbyChatListFragment f = (NearbyChatListFragment) getFragmentAt(2);
        f.shutdownNearbyChat();
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
            Fragment f = getFragmentAt(tab.getPosition());
            if (f instanceof SearchableListFragment) {
                ((SearchableListFragment) f).leaveSearchMode();
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }
}
