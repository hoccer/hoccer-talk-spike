package com.hoccer.xo.android.base;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TaskStackBuilder;
import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hoccer.talk.client.IXoAlertListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.*;
import com.hoccer.xo.android.fragment.DeviceContactsInvitationFragment;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.view.chat.attachments.TransferControlView;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.Strings;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class BaseActivity extends FragmentActivity {

    private static final Logger LOG = Logger.getLogger(BaseActivity.class);

    boolean mUpEnabled;

    private TransferControlView mSpinner;
    private Handler mDialogDismisser;
    private Dialog mDialog;
    private XoAlertListener mAlertListener;

    private boolean mOptionsMenuEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutResource());

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        mAlertListener = new XoAlertListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForCrashesIfEnabled();
        checkKeys();
        getClient().registerAlertListener(mAlertListener);
    }

    private void checkKeys() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        boolean needToRegenerateKey = preferences.getBoolean("NEED_TO_REGENERATE_KEYS", true);

        if (needToRegenerateKey) {
            createDialog();
            regenerateKeys();
        }
    }

    private void checkForCrashesIfEnabled() {
        if (XoApplication.getConfiguration().isCrashReportingEnabled()) {
            CrashManager.register(this, XoApplication.getConfiguration().getHockeyAppId(), new CrashManagerListener() {
                @Override
                public String getStringForResource(int resourceID) {
                    switch (resourceID) {
                        case Strings.CRASH_DIALOG_TITLE_ID:
                            return getString(R.string.dialog_report_crash_title);
                        case Strings.CRASH_DIALOG_MESSAGE_ID:
                            return getString(R.string.dialog_report_crash_message);
                        case Strings.CRASH_DIALOG_NEGATIVE_BUTTON_ID:
                            return getString(R.string.dialog_report_crash_negative);
                        case Strings.CRASH_DIALOG_POSITIVE_BUTTON_ID:
                            return getString(R.string.dialog_report_crash_positive);
                        default:
                            return super.getStringForResource(resourceID);
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        getClient().unregisterAlertListener(mAlertListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mOptionsMenuEnabled) {
            getMenuInflater().inflate(R.menu.common, menu);

            int activityMenu = getMenuResource();
            if (activityMenu >= 0) {
                getMenuInflater().inflate(activityMenu, menu);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                break;
            case R.id.menu_my_profile:
                try {
                    showContactProfile(getDatabase().findSelfContact(false));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.menu_pair:
                showPairing();
                break;
            case R.id.menu_new_group:
                showNewGroup();
                break;
            case R.id.menu_settings:
                showPreferences();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public XoClient getClient() {
        return XoApplication.get().getClient();
    }

    public XoClientDatabase getDatabase() {
        return XoApplication.get().getClient().getDatabase();
    }

    public void startExternalActivity(Intent intent) {
        if (!canStartActivity(intent)) {
            return;
        }

        try {
            startActivity(intent);
            BackgroundManager.get().ignoreNextBackgroundPhase();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_compatible_app_unavailable, Toast.LENGTH_LONG).show();
            LOG.error(e.getMessage());
        }
    }

    public boolean canStartActivity(Intent intent) {
        if (intent != null) {
            ComponentName componentName = intent.resolveActivity(getPackageManager());
            if (componentName != null) {
                String activityName = componentName.getClassName();

                // perform check on specified Activity classes.
                if (activityName != null && activityName.equals(MapsLocationActivity.class.getName())) {
                    int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
                    if (result > 0) {
                        LOG.warn(getClass() + " aborting start of external activity " + intent + " because Google Play Services returned code " + result);
                        showGooglePlayServicesErrorDialog(result);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void showGooglePlayServicesErrorDialog(int result) {
        Dialog googlePlayServicesErrorDialog = GooglePlayServicesUtil.getErrorDialog(result, this, 0);
        if (googlePlayServicesErrorDialog != null) {
            googlePlayServicesErrorDialog.show();
        }
    }

    private void regenerateKeys() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    XoApplication.get().getClient().regenerateKeyPair();

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("NEED_TO_REGENERATE_KEYS", false);
                    editor.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    mDialogDismisser.sendEmptyMessage(0);
                }
            }
        });
        t.start();
    }

    public void createDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.waiting_dialog, null);
        mSpinner = (TransferControlView) view.findViewById(R.id.content_progress);

        mDialog = new Dialog(this);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(view);
        mDialog.getWindow()
                .setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return true;
            }
        });

        Handler spinnerStarter = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mSpinner.prepareToUpload();
                mSpinner.spin();
            }
        };
        mDialogDismisser = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    mDialog.dismiss();
                    mSpinner.completeAndGone();
                } catch (IllegalArgumentException e) {
                    LOG.error("Dialog is not attached to current activity.");
                    e.printStackTrace();
                    //TODO: Once upon a time we will redesign all this stuff... Maybe.
                }
            }
        };
        spinnerStarter.sendEmptyMessageDelayed(0, 500);
    }

    public void setOptionsMenuEnabled(boolean optionsMenuEnabled) {
        mOptionsMenuEnabled = optionsMenuEnabled;
        invalidateOptionsMenu();
    }

    protected void enableUpNavigation() {
        LOG.debug("enableUpNavigation()");
        mUpEnabled = true;
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressLint("NewApi")
    private void navigateUp() {
        LOG.debug("navigateUp()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && mUpEnabled) {
            Intent upIntent = getParentActivityIntent();
            if (upIntent != null) {
                // we have a parent, navigate up
                if (shouldUpRecreateTask(upIntent)) {
                    // we are not on our own task stack, so create one
                    TaskStackBuilder.create(this)
                            // add parents to back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // navigate up to next parent
                            .startActivities();
                } else {
                    // we are on our own task stack, so navigate upwards
                    navigateUpTo(upIntent);
                }
            } else {
                // we don't have a parent, navigate back instead
                onBackPressed();
            }
        } else {
            onBackPressed();
        }
    }

    public void showContactProfile(TalkClientContact contact) {
        LOG.debug("showContactProfile(" + contact.getClientContactId() + ")");
        Intent intent;
        if (contact.isGroup()) {
            intent = new Intent(this, GroupProfileActivity.class)
                    .setAction(GroupProfileActivity.ACTION_SHOW)
                    .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        } else {
            intent = new Intent(this, ClientProfileActivity.class)
                    .setAction(ClientProfileActivity.ACTION_SHOW)
                    .putExtra(ClientProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        }
        startActivity(intent);
    }

    public void showNewGroup() {
        startActivity(new Intent(this, GroupProfileActivity.class)
                .setAction(GroupProfileActivity.ACTION_CREATE));
    }

    public void showPairing() {
        XoDialogs.showSingleChoiceDialog(
                "SelectPairingMethod",
                R.string.dialog_add_contact_type_title,
                new String[]{
                        getResources().getString(R.string.dialog_add_contact_type_sms_item),
                        getResources().getString(R.string.dialog_add_contact_type_mail_item),
                        getResources().getString(R.string.dialog_add_contact_type_code_item)
                },
                this,
                new XoDialogs.OnSingleSelectionFinishedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, int selectedItem) {
                        switch (selectedItem) {
                            case 0:
                                pairBySMS();
                                break;
                            case 1:
                                pairByMail();
                                break;
                            case 2:
                                pairByCode();
                                break;
                        }
                    }
                });
    }

    private void pairBySMS() {
        Intent intent = new Intent(this, DeviceContactsInvitationActivity.class);
        intent.putExtra(DeviceContactsInvitationFragment.EXTRA_IS_SMS_INVITATION, true);
        startActivity(intent);
    }

    private void pairByMail() {
        Intent intent = new Intent(this, DeviceContactsInvitationActivity.class);
        intent.putExtra(DeviceContactsInvitationFragment.EXTRA_IS_SMS_INVITATION, false);
        startActivity(intent);
    }

    public void pairByCode() {
        LOG.debug("pairByCode()");
        Intent intent = new Intent(this, QrCodeActivity.class);
        startActivity(intent);
    }

    public void showFullscreenPlayer() {
        LOG.debug("showFullscreenPlayer()");
        startActivity(new Intent(this, FullscreenPlayerActivity.class));
    }

    public void showPreferences() {
        LOG.debug("showPreferences()");
        startActivity(new Intent(this, XoPreferenceActivity.class));
    }

    /**
     * This class is an implementation of IXoAlertListener which displays alerts inside an AlertDialog.
     * Links and other data inside the message text are tappable.
     */
    public class XoAlertListener implements IXoAlertListener {

        private final Context mContext;

        XoAlertListener(Context context) {
            mContext = context;
        }

        @Override
        public void onInternalAlert(String title, String message) {
            final String alertTitle = title;
            final String alertMessage = message;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayAlert(alertTitle, alertMessage);
                }
            });
        }

        @Override
        public void onAlertMessageReceived(String message) {
            final String alertMessage = message;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayAlert(null, alertMessage);
                }
            });
        }

        /**
         * Displays an AlertDialog from a given title and message string.
         * The displayed message text is interactive: links etc. can be tapped.
         *
         * @param title   The given alert title
         * @param message The given alert message
         */
        private void displayAlert(String title, String message) {

            // Scan for urls other information
            final SpannableString interactiveMessage = new SpannableString(message);
            Linkify.addLinks(interactiveMessage, Linkify.ALL);

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            if (title != null) {
                builder.setTitle(title);
            }
            builder.setMessage(interactiveMessage);
            builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index) {
                    dialog.dismiss();
                }
            });

            Dialog dialog = builder.create();
            dialog.show();

            // Make message interactive
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    protected abstract int getLayoutResource();

    protected abstract int getMenuResource();
}