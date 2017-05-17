package com.hoccer.xo.android.activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.*;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.fragment.ChatListFragment;
import com.hoccer.xo.android.fragment.NearbyChatListFragment;
import com.hoccer.xo.android.passwordprotection.PasswordProtection;
import com.hoccer.xo.android.service.XoClientService;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.ContactsMenuItemActionProvider;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.Strings;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class ChatsBaseActivity extends ComposableActivity implements IXoStateListener, IXoPairingListener {

    protected static final Logger LOG = Logger.getLogger(ChatsBaseActivity.class);

    private CrashManagerListener mCrashManagerListener;

    private ContactsMenuItemActionProvider mContactsMenuItemActionProvider;

    protected ViewPagerActivityComponent mViewPagerActivityComponent;

    private static final int REGISTER_REQUEST_CODE = 1;

    public static final String INTENT_EXTRA_EXIT = "exit";

    private String mPairingToken;

    @Override
    protected ActivityComponent[] createComponents() {
        MediaPlayerActivityComponent mediaPlayerActivityComponent = new MediaPlayerActivityComponent(this);
        mViewPagerActivityComponent = new ViewPagerActivityComponent(this,
                R.id.pager,
                new ChatListFragment(),
                new NearbyChatListFragment());

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

        registerCrashManager();

        startXoClientService();

        XoApplication.get().getClient().connect();

        mContactsMenuItemActionProvider = new ContactsMenuItemActionProvider(this);

        handleIntent(getIntent());

        BackgroundConnectionHandler.get();

        PasswordProtection.get();

        FeaturePromoter.cleanupForSelectWorldwidePageOnFirstStart(this);

        showGooglePlayServicesErrorDialog();

        checkDeniedPermssions();
    }

    private void checkDeniedPermssions() {
        List<String> deniedPermissions = getDeniedPermissions();

        if (deniedPermissions.size() > 0) {
            StringBuilder permissions = new StringBuilder();
            for (String permission:deniedPermissions) {
                String firstCapital = permission.substring(0, 1).toUpperCase() + permission.substring(1);
                permissions.append("- "+firstCapital+"\n");
            }
            XoDialogs.showOkDialog("ChatBaseActivity", getString(R.string.missing_permissions_title), getString(R.string.missing_permissions_message)+"\n\n"+permissions, this);
        }
    }

    List<String> getDeniedPermissions() {
        List<String> denied = new ArrayList<String>();
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if (PermissionChecker.checkSelfPermission(XoApplication.get(), pi.requestedPermissions[i]) != PermissionChecker.PERMISSION_GRANTED) {
                    denied.add(getPackageManager().getPermissionInfo(pi.requestedPermissions[i],0).loadLabel(getPackageManager()).toString());
                }
            }
        } catch (Exception e) {
            LOG.error("Could not get denied permissions.", e);
        }
        return denied;
    }

    private void registerCrashManager() {
        if (getConfiguration().isCrashReportingEnabled()) {
            mCrashManagerListener = new CrashManagerListener() {
                @Override
                public String getStringForResource(int resourceID) {
                    switch (resourceID) {
                        case Strings.CRASH_DIALOG_TITLE_ID:
                            return CrashMonitor.get(ChatsBaseActivity.this).isCrashedBefore() ? getString(R.string.dialog_report_crash_title) : getString(R.string.dialog_report_errors_title);
                        case Strings.CRASH_DIALOG_MESSAGE_ID:
                            return CrashMonitor.get(ChatsBaseActivity.this).isCrashedBefore() ? getString(R.string.dialog_report_crash_message) : getString(R.string.dialog_report_errors_message);
                        case Strings.CRASH_DIALOG_NEGATIVE_BUTTON_ID:
                            return getString(R.string.dialog_report_crash_negative);
                        case Strings.CRASH_DIALOG_POSITIVE_BUTTON_ID:
                            return getString(R.string.dialog_report_crash_positive);
                        default:
                            return super.getStringForResource(resourceID);
                    }
                }
            };

            CrashManager.initialize(this, getConfiguration().getHockeyAppId(), mCrashManagerListener);
        }
    }

    private void startXoClientService() {
        Intent intent = new Intent(this, XoClientService.class);
        intent.putExtra(XoClientService.EXTRA_CONNECT, true);
        startService(intent);
    }

    public int showGooglePlayServicesErrorDialog() {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (result != ConnectionResult.SUCCESS) {
            LOG.debug("showGooglePlayServicesErrorDialog:" + result);
            Dialog googlePlayServicesErrorDialog = GooglePlayServicesUtil.getErrorDialog(result, this, 0);
            if (googlePlayServicesErrorDialog != null) {
                googlePlayServicesErrorDialog.show();
            }
        }
        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();

        CrashManager.execute(ChatsBaseActivity.this, mCrashManagerListener);
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
        NearbyController.INSTANCE.removeNotification();
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
        return getClient().getSelfContact().getSelf().isRegistrationConfirmed();
    }

    private void registerListeners() {
        getClient().registerStateListener(this);
        if (mContactsMenuItemActionProvider != null) {
            getClient().registerContactListener(mContactsMenuItemActionProvider);
        }
    }

    private void unregisterListeners() {
        getClient().unregisterStateListener(this);
        if (mContactsMenuItemActionProvider != null) {
            getClient().unregisterContactListener(mContactsMenuItemActionProvider);
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

        if (getClient().isReady()) {
            performTokenPairing(token);
        } else {
            mPairingToken = token;
        }
    }

    private void performTokenPairing(final String token) {
        XoApplication.get().getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                getClient().performTokenPairing(token, ChatsBaseActivity.this);
            }
        });
    }

    @Override
    public void onTokenPairingSucceeded(String token) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatsBaseActivity.this, R.string.toast_pairing_successful, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onTokenPairingFailed(String token) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatsBaseActivity.this, R.string.toast_pairing_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startMediaBrowserActivity() {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        startActivity(intent);
    }
}
