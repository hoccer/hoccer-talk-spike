package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
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

public class ChatsActivity extends ComposableActivity implements IXoStateListener, IXoPairingListener {

    private static final Logger LOG = Logger.getLogger(ChatsActivity.class);

    public static final String INTENT_EXTRA_EXIT = "exit";
    public static final int REGISTER_REQUEST_CODE = 1;

    private String mPairingToken;
    private ContactsMenuItemActionProvider mContactsMenuItemActionProvider;
    private ViewPagerActivityComponent mViewPagerActivityComponent;
    private WorldwideChatListFragment mWorldwideChatListFragment;

    @Override
    protected ActivityComponent[] createComponents() {
        MediaPlayerActivityComponent mediaPlayerActivityComponent = new MediaPlayerActivityComponent(this);

        mWorldwideChatListFragment = new WorldwideChatListFragment();
        mViewPagerActivityComponent = new ViewPagerActivityComponent(this,
                R.id.pager,
                new ChatListFragment(),
                new NearbyChatListFragment(),
                mWorldwideChatListFragment);

        return new ActivityComponent[]{mediaPlayerActivityComponent, mViewPagerActivityComponent};
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

        BackgroundConnectionHandler.get();

        PasswordProtection.get();

        FeaturePromoter.cleanupForSelectWorldwidePageOnFirstStart(this);
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
        if (!isRegistered()) {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivityForResult(intent, REGISTER_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK && requestCode == REGISTER_REQUEST_CODE) {
        }
    }

    private boolean isRegistered() {
        return getXoClient().getSelfContact().getSelf().isRegistrationConfirmed();
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
}
