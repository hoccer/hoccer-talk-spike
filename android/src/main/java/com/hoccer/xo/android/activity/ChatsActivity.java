package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.xo.android.*;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.fragment.ChatListFragment;
import com.hoccer.xo.android.fragment.NearbyChatListFragment;
import com.hoccer.xo.android.fragment.WorldwideChatListFragment;
import com.hoccer.xo.android.passwordprotection.PasswordProtection;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.ContactsMenuItemActionProvider;
import org.apache.log4j.Logger;

public class ChatsActivity extends ComposableActivity implements IXoStateListener, IXoPairingListener, BackgroundManager.Listener {

    private static final Logger LOG = Logger.getLogger(ChatsActivity.class);

    public static final String INTENT_EXTRA_EXIT = "exit";

    private String mPairingToken;
    private ContactsMenuItemActionProvider mContactsMenuItemActionProvider;
    private PowerManager.WakeLock mWl;


    @Override
    protected ActivityComponent[] createComponents() {
        MediaPlayerActivityComponent mediaPlayerActivityComponent = new MediaPlayerActivityComponent(this);

        ViewPagerActivityComponent viewPagerActivityComponent = new ViewPagerActivityComponent(this,
                R.id.pager,
                new ChatListFragment(),
                new NearbyChatListFragment(),
                new WorldwideChatListFragment());

        return new ActivityComponent[]{mediaPlayerActivityComponent, viewPagerActivityComponent};
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

        mContactsMenuItemActionProvider = new ContactsMenuItemActionProvider(this);

        handleIntent(getIntent());

        BackgroundManager.get().registerListener(this);

        PasswordProtection.get();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showProfileIfClientIsNotRegistered();
        registerListeners();
        mContactsMenuItemActionProvider.updateNotificationBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayer.get().removeNotification();
        NearbyController.get().removeNotification();
        BackgroundManager.get().unregisterListener(this);
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
        } else if (intent.hasExtra(IntentHelper.EXTRA_CONTACT_ID)) {
            handleContactIdIntent(intent);
        } else if (intent.hasExtra(IntentHelper.EXTRA_PUSH_MESSAGE)) {
            handlePushMessageIntent(intent);
        } else if (intent.getBooleanExtra(INTENT_EXTRA_EXIT, false)) {
            finish();
            XoApplication.restartApplication();
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
    public void onClientStateChange(XoClient client) {
        if (mPairingToken != null && client.isReady()) {
            performTokenPairing(mPairingToken);
            mPairingToken = null;
        }
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

    public void showContactConversation(int contactId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contactId);
        startActivity(intent);
    }

    private void handlePushMessageIntent(Intent intent) {
        String message = intent.getStringExtra(IntentHelper.EXTRA_PUSH_MESSAGE);
        XoDialogs.showOkDialog("PushMessage", "", message, this);
    }

    private void handleTokenPairingIntent(Intent intent) {
        String token = intent.getData().getHost();

        if (getXoClient().isReady()) {
            performTokenPairing(token);
        } else {
            mPairingToken = token;
        }
    }

    private void performTokenPairing(final String token) {
        XoApplication.get().getExecutor().execute(new Runnable() {
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

    private void startMediaBrowserActivity() {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBecameForeground(Activity activity) {
        LOG.debug("onBecameForeground()");
        getXoClient().setPresenceStatus(TalkPresence.STATUS_ONLINE);

        if (getXoClient().isDisconnected()) {
            connectClientIfNetworkAvailable();
        }
    }

    public void connectClientIfNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected()) {
            getXoClient().connect();
        }
    }

    @Override
    public void onBecameBackground(Activity activity) {
        LOG.debug("onBecameBackground()");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Background disconnet");
        mWl.acquire(); //TODO release wake lock as soon as possible

        getXoClient().setPresenceStatus(TalkPresence.STATUS_BACKGROUND);
    }
}
