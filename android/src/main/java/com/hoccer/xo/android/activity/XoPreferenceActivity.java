package com.hoccer.xo.android.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.*;
import android.view.*;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.WorldwideController;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.backup.*;
import com.hoccer.xo.android.passwordprotection.PasswordProtection;
import com.hoccer.xo.android.passwordprotection.activity.PasswordChangeActivity;
import com.hoccer.xo.android.passwordprotection.activity.PasswordPromptActivity;
import com.hoccer.xo.android.passwordprotection.activity.PasswordSetActivity;
import com.hoccer.xo.android.view.chat.attachments.TransferControlView;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class XoPreferenceActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = Logger.getLogger(XoPreferenceActivity.class);

    private static final String CREDENTIALS_TRANSFER_FILE = "credentials.json";

    private static final int REQUEST_SET_AND_ACTIVATE_PASSWORD = 1;
    private static final int REQUEST_ACTIVATE_PASSWORD = 2;
    private static final int REQUEST_DEACTIVATE_PASSWORD = 3;

    private TransferControlView mSpinner;

    private Handler mDialogDismisser;

    private Dialog mWaitingDialog;
    private BackupController mBackupController;
    private SharedPreferences mDefaultSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (XoApplication.getConfiguration().isDevelopmentModeEnabled()) {
            addPreferencesFromResource(R.xml.development_preferences);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }
        getListView().setBackgroundColor(Color.WHITE);

        final BackupPreference createBackupPreference = (BackupPreference) findPreference(getString(R.string.preference_key_create_backup));
        final BackupPreference restoreBackupPreference = (BackupPreference) findPreference(getString(R.string.preference_key_restore_backup));

        SwitchPreference activatePasswordPreference = (SwitchPreference) findPreference(getString(R.string.preference_key_activate_passcode));
        activatePasswordPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean oldValue = mDefaultSharedPreferences.getBoolean(getString(R.string.preference_key_activate_passcode), false);
                if (oldValue != newValue) {
                    boolean activatePassword = (Boolean) newValue;
                    if (activatePassword && !isPasswordSet()) {
                        startSetPasswordActivityForResult();
                    } else if (activatePassword) {
                        startPasswordPromptActivityForResult(REQUEST_ACTIVATE_PASSWORD);
                    } else {
                        startPasswordPromptActivityForResult(REQUEST_DEACTIVATE_PASSWORD);
                    }
                }

                return false;
            }
        });

        mBackupController = new BackupController(this, createBackupPreference, restoreBackupPreference);
    }

    private boolean isPasswordSet() {
        return getSharedPreferences(PasswordProtection.PASSWORD_PROTECTION_PREFERENCES, MODE_PRIVATE).contains(PasswordProtection.PASSWORD);
    }

    private void startSetPasswordActivityForResult() {
        Intent intent = new Intent(this, PasswordSetActivity.class);
        startActivityForResult(intent, REQUEST_SET_AND_ACTIVATE_PASSWORD);
    }

    private void startPasswordPromptActivityForResult(int requestCode) {
        Intent intent = new Intent(this, PasswordPromptActivity.class);
        intent.putExtra(PasswordPromptActivity.EXTRA_ENABLE_BACK_NAVIGATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SET_AND_ACTIVATE_PASSWORD && resultCode == RESULT_OK) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(getString(R.string.preference_key_activate_passcode), true).apply();
            findPreference(getString(R.string.preference_key_change_passcode)).setEnabled(true);
            PasswordProtection.get().unlock();
            restartActivityWithoutAnimation();
        }
        if (requestCode == REQUEST_ACTIVATE_PASSWORD && resultCode == RESULT_OK) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(getString(R.string.preference_key_activate_passcode), true).apply();
            restartActivityWithoutAnimation();
        }
        if (requestCode == REQUEST_DEACTIVATE_PASSWORD && resultCode == RESULT_OK) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(getString(R.string.preference_key_activate_passcode), false).apply();
            restartActivityWithoutAnimation();
        }
    }

    /*
     * This is a workaround for some devices, e.g. HTC One, to update ui components respective to its preference state
     */
    private void restartActivityWithoutAnimation() {
        finish();
        overridePendingTransition(0, 0);
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item + ")");
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDefaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (isPasswordSet()) {
            findPreference(getString(R.string.preference_key_change_passcode)).setEnabled(true);
        }

        mBackupController.handleIntent(getIntent());
        mBackupController.registerAndBind();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mDefaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        mBackupController.unregisterAndUnbind();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Intent intent = getIntent();
        if (intent != null) {
            boolean selectBackupPreferences = intent.getBooleanExtra("select_backup_preferences", false);
            if (selectBackupPreferences) {
                ListAdapter adapter = getListView().getAdapter();
                if (adapter != null) {
                    for (int i = 0; i < adapter.getCount(); i++) {
                        Preference preference = (Preference) getListView().getAdapter().getItem(i);
                        if ("backup_category".equals(preference.getKey())) {
                            getListView().setSelection(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void createDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.waiting_dialog, null);
        mSpinner = (TransferControlView) view.findViewById(R.id.content_progress);

        mWaitingDialog = new Dialog(this);
        mWaitingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mWaitingDialog.setContentView(view);
        mWaitingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mWaitingDialog.setCanceledOnTouchOutside(false);
        if (!isFinishing()) {
            mWaitingDialog.show();
        }
        mWaitingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
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
                mWaitingDialog.dismiss();
                mSpinner.completeAndGone();
            }
        };
        spinnerStarter.sendEmptyMessageDelayed(0, 500);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("preference_keysize".equals(key)) {
            createDialog();
            regenerateKeys();
        } else if (getString(R.string.preference_key_worldwide_timetolive).equals(key)) {
            updateWorldwideTimeToLive(sharedPreferences);
        } else if (getString(R.string.preference_key_worldwide_enable_notifications).equals(key)) {
            updateWorldwideNotificationPreference(sharedPreferences);
        }
    }

    private void updateWorldwideTimeToLive(SharedPreferences sharedPreferences) {
        long timeToLive = Long.parseLong(sharedPreferences.getString("preference_key_worldwide_timetolive", "0"));
        WorldwideController.get().updateTimeToLive(timeToLive);
    }

    private void updateWorldwideNotificationPreference(SharedPreferences sharedPreferences) {
        Boolean notificationsEnabled = sharedPreferences.getBoolean(getString(R.string.preference_key_worldwide_enable_notifications), false);
        if (notificationsEnabled) {
            WorldwideController.get().updateNotificationPreference(TalkGroupMembership.NOTIFICATIONS_ENABLED);
        } else {
            WorldwideController.get().updateNotificationPreference(TalkGroupMembership.NOTIFICATIONS_DISABLED);
        }
    }

    private void regenerateKeys() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    XoApplication.get().getClient().regenerateKeyPair();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    mDialogDismisser.sendEmptyMessage(0);
                }
            }
        });
        t.start();
    }

    @Override
    protected void onDestroy() {
        if (mSpinner != null) {
            mSpinner.completeAndGone();
        }
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if ("preference_export".equals(preference.getKey())) {
            showExportCredentialsDialog();
            return true;
        } else if ("preference_import".equals(preference.getKey())) {
            showImportCredentialsDialog();
            return true;
        } else if ("preference_database_dump".equals(preference.getKey())) {
            dumpDatabase();
            return true;
        } else if (getString(R.string.preference_key_change_passcode).equals(preference.getKey())) {
            startChangePasscodeActivity();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void startChangePasscodeActivity() {
        Intent intent = new Intent(this, PasswordChangeActivity.class);
        startActivity(intent);
    }

    private void showImportCredentialsDialog() {
        final File credentialsFile = new File(XoApplication.getExternalStorage() + File.separator + CREDENTIALS_TRANSFER_FILE);
        if (credentialsFile == null || !credentialsFile.exists()) {
            Toast.makeText(this, getString(R.string.cant_find_credentials), Toast.LENGTH_LONG).show();
            return;
        }

        XoDialogs.showInputPasswordDialog("ImportCredentialsDialog",
                R.string.dialog_import_credentials_title,
                this,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        importCredentials(credentialsFile, password);
                    }
                }
        );
    }

    private void importCredentials(final File credentialsFile, final String password) {
        try {
            Backup backup = BackupFactory.readBackup(credentialsFile);
            backup.restore(password);
            Toast.makeText(this, R.string.import_credentials_success, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            LOG.error("Error while importing credentials", e);
            Toast.makeText(this, R.string.import_credentials_failure, Toast.LENGTH_LONG).show();
        }
    }

    private void showExportCredentialsDialog() {
        XoDialogs.showInputPasswordDialog("ExportCredentialsDialog",
                R.string.dialog_export_credentials_title,
                this,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        exportCredentials(password);
                    }
                }
        );
    }

    private void exportCredentials(final String password) {
        try {
            BackupFactory.createCredentialsBackup(password);
            Toast.makeText(this, R.string.export_credentials_success, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            LOG.error("error while writing credentials container to filesystem.", e);
            Toast.makeText(this, R.string.export_credentials_failure, Toast.LENGTH_LONG).show();
        }
    }

    private void dumpDatabase() {
        try {
            File database = new File("/data/data/" + getPackageName() + "/databases/hoccer-talk.db");
            String filename = BackupFileUtils.createUniqueBackupFilename(XoApplication.getConfiguration().getAppName()) + ".db";
            File target = new File(XoApplication.getExternalStorage(), filename);
            FileUtils.copyFile(database, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
