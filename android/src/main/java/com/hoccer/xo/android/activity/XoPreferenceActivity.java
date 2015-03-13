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
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.backup.*;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferControlView;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class XoPreferenceActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = Logger.getLogger(XoPreferenceActivity.class);

    private static final String CREDENTIALS_TRANSFER_FILE = "credentials.json";

    private AttachmentTransferControlView mSpinner;

    private Handler mDialogDismisser;

    private Dialog mWaitingDialog;
    private BackupController mBackupController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (XoApplication.getConfiguration().isDevelopmentModeEnabled()) {
            addPreferencesFromResource(R.xml.development_preferences);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }
        getListView().setBackgroundColor(Color.WHITE);

        BackupPreference createBackupPreference = (BackupPreference) findPreference(getString(R.string.preference_key_create_backup));
        BackupPreference restoreBackupPreference = (BackupPreference) findPreference(getString(R.string.preference_key_restore_backup));

        SwitchPreference activatePasswordPreference = (SwitchPreference) findPreference("preference_activate_passcode");
        activatePasswordPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean active = (Boolean) newValue;
                if (active && isPasscodeActive()) {
                    startPassCodeInputActivityForResult(16);
                } else if (active && !isPasscodeActive()) {
                    startSetPasswordActivityForResult();
                } else if (!active && isPasscodeActive()) {
                    startPassCodeInputActivityForResult(18);
                }
                return false;
            }
        });

        mBackupController = new BackupController(this, createBackupPreference, restoreBackupPreference);
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

        if (getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).contains(SetPasscodeActivity.PASSCODE)) {
            findPreference("preference_change_passcode").setEnabled(true);
        }

        if (!getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).getBoolean(SetPasscodeActivity.PASSCODE_ACTIVE, false)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("preference_activate_passcode", false).apply();
            ((SwitchPreference) findPreference("preference_activate_passcode")).setChecked(false);
        } else {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("preference_activate_passcode", true).apply();
            ((SwitchPreference) findPreference("preference_activate_passcode")).setChecked(true);
        }

        mBackupController.handleIntent(getIntent());
        mBackupController.registerAndBind();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        mSpinner = (AttachmentTransferControlView) view.findViewById(R.id.content_progress);

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
        }
    }

    private void startSetPasswordActivityForResult() {
        Intent intent = new Intent(this, SetPasscodeActivity.class);
        startActivityForResult(intent, 15);
    }

    private void startPassCodeInputActivityForResult(int requestCode) {
        Intent intent = new Intent(this, PromptPasswordActivity.class);
        intent.putExtra("ENABLE_BACK", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 15 || requestCode == 16) {
            if (resultCode == RESULT_OK) {
                getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).edit().putBoolean(SetPasscodeActivity.PASSCODE_ACTIVE, true).commit();
            }
        }
        if (requestCode == 17 && resultCode == RESULT_OK) {
            startChangePasscodeActivity();
        }
        if (requestCode == 18 && resultCode == RESULT_OK) {
            getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).edit().putBoolean(SetPasscodeActivity.PASSCODE_ACTIVE, false).commit();
        }
    }

    private void startChangePasscodeActivity() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    private boolean isPasscodeActive() {
        return getSharedPreferences(SetPasscodeActivity.PASSCODE_PREFERENCES, MODE_PRIVATE).getBoolean(SetPasscodeActivity.PASSCODE_ACTIVE, false);
    }

    private void regenerateKeys() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    XoApplication.get().getXoClient().regenerateKeyPair();
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
        } else if ("preference_change_passcode".equals(preference.getKey())) {
            startPassCodeInputActivityForResult(17);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
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
            String filename = BackupFileUtils.createUniqueBackupFilename() + ".db";
            File target = new File(XoApplication.getExternalStorage(), filename);
            FileUtils.copyFile(database, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
