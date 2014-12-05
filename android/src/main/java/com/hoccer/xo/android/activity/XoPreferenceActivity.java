package com.hoccer.xo.android.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.*;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.backup.Backup;
import com.hoccer.xo.android.backup.BackupFactory;
import com.hoccer.xo.android.backup.BackupFileUtils;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferControlView;
import net.hockeyapp.android.CrashManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class XoPreferenceActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = Logger.getLogger(XoPreferenceActivity.class);

    private static final String CREDENTIALS_TRANSFER_FILE = "credentials.json";

    private AttachmentTransferControlView mSpinner;

    private Handler mDialogDismisser;

    private Dialog mWaitingDialog;

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item.toString() + ")");
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
        checkForCrashesIfEnabled();
    }

    private void checkForCrashesIfEnabled() {
        if (XoApplication.getConfiguration().isCrashReportingEnabled()) {
            CrashManager.register(this, XoApplication.getConfiguration().getHockeyAppId());
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

    private void regenerateKeys() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    XoApplication.getXoClient().regenerateKeyPair();
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
        } else if ("preference_chats_backup".equals(preference.getKey())) {
            showDatabaseBackupDialog();
            return true;
        } else if ("preference_complete_backup".equals(preference.getKey())) {
            showCompleteBackupDialog();
            return true;
        } else if ("preference_import_backup".equals(preference.getKey())) {
            showImportBackupDialog();
            return true;
        } else if ("preference_database_dump".equals(preference.getKey())) {
            dumpDatabase();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void showImportBackupDialog() {
        final List<Backup> backups = BackupFileUtils.getBackups(XoApplication.getBackupDirectory());

        List<String> items = new ArrayList<String>(backups.size());
        for (Backup backup : backups) {
            String timestamp = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss").format(backup.getCreationDate());
            items.add(timestamp);
        }

        XoDialogs.showSingleChoiceDialog("ImportBackupDialog",
                R.string.dialog_import_credentials_title,
                items.toArray(new String[items.size()]),
                this,
                new XoDialogs.OnSingleSelectionFinishedListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id, final int selectedItem) {
                        XoDialogs.showInputPasswordDialog("ImportBackupPasswordDialog",
                                R.string.dialog_import_credentials_title,
                                XoPreferenceActivity.this,
                                new XoDialogs.OnTextSubmittedListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int id, String password) {
                                        importBackup(backups.get(selectedItem), password);
                                    }
                                }
                        );
                    }
                });

    }

    private void importBackup(final Backup backup, final String password) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    if (BackupFileUtils.isEnoughDiskSpaceAvailable(backup.getFile())) {
                        backup.restore(password);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                if (success) {
                    Toast.makeText(getBaseContext(), "Data imported successfully. Please restart app.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "Data import failed", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void showDatabaseBackupDialog() {
        XoDialogs.showInputPasswordDialog("CreateDatabaseBackupDialog",
                R.string.dialog_export_credentials_title,
                this,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        createDatabaseBackup(password);
                    }
                }
        );
    }

    private void createDatabaseBackup(final String password) {
        new AsyncTask<Void, Void, Backup>() {
            @Override
            protected Backup doInBackground(Void... params) {
                try {
                    return BackupFactory.createDatabaseBackup(password);
                } catch (Exception e) {
                    LOG.error("Data export failed.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Backup backup) {
                super.onPostExecute(backup);
                if (backup != null) {
                    Toast.makeText(getBaseContext(), "Data exported to " + backup.getFile().getAbsolutePath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "Data export failed", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void showCompleteBackupDialog() {
        XoDialogs.showInputPasswordDialog("CreateDatabaseBackupDialog",
                R.string.dialog_export_credentials_title,
                this,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        createCompleteBackup(password);
                    }
                }
        );
    }

    private void createCompleteBackup(final String password) {
        new AsyncTask<Void, Void, Backup>() {
            @Override
            protected Backup doInBackground(Void... params) {
                try {
                    return BackupFactory.createCompleteBackup(password);
                } catch (Exception e) {
                    LOG.error("Data export failed.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Backup backup) {
                super.onPostExecute(backup);
                if (backup != null) {
                    Toast.makeText(getBaseContext(), "Data exported to " + backup.getFile().getAbsolutePath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "Data export failed", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
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
