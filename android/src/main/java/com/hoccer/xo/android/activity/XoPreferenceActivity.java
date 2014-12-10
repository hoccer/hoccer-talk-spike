package com.hoccer.xo.android.activity;

import android.app.Dialog;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.preference.*;
import android.support.v4.content.LocalBroadcastManager;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.backup.*;
import com.hoccer.xo.android.util.IntentHelper;
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
    private boolean mBackupServiceBound;
    private BackupService mBackupService;

    private BroadcastReceiver mBroadcastReceiver;
    private ServiceConnection mServiceConnection;

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

        createBroadcastReceiver();
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getListView().findViewById(R.id.rl_default_preference).setVisibility(View.VISIBLE);
                getListView().findViewById(R.id.rl_in_progress).setVisibility(View.GONE);
                getListView().findViewById(R.id.rl_in_progress).setVisibility(View.GONE);

                if (intent.getAction().equals(IntentHelper.ACTION_BACKUP_SUCCEEDED)) {
                    Backup backup = intent.getParcelableExtra("result");

                    String date = String.format("Date: %s", new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(backup.getCreationDate()));
                    String user = String.format("User: %s", backup.getClientName());
                    String path = String.format("Path: %s", backup.getFile().getAbsolutePath());
                    String size = String.format("Size: %s", FileUtils.byteCountToDisplaySize(backup.getSize()));

                    String message = String.format("%s\n%s\n%s\n%s", date, user, path, size);
                    XoDialogs.showOkDialog("BackupCreatedDialog", "Backup created", message, XoPreferenceActivity.this);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IntentHelper.ACTION_BACKUP_SUCCEEDED);
        intentFilter.addAction(IntentHelper.ACTION_BACKUP_FAILED);
        intentFilter.addAction(IntentHelper.ACTION_BACKUP_CANCELED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
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

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Intent intent = getIntent();
        if (intent != null) {
            boolean showBackupPreferences = intent.getBooleanExtra("show_backup_preferences", false);
            if (showBackupPreferences) {
                for (int i = 0; i < getListView().getAdapter().getCount(); i++) {
                    Preference preference = (Preference) getListView().getAdapter().getItem(i);
                    if ("backup_category".equals(preference.getKey())) {
                        getListView().smoothScrollToPosition(i);
                        getListView().setSelection(i);
                        break;
                    }
                }
            }
        }
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
            String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(backup.getCreationDate());
            items.add(timestamp + " " + FileUtils.byteCountToDisplaySize(backup.getSize()));
        }

        XoDialogs.showSingleChoiceDialog("ImportBackupDialog",
                R.string.dialog_import_credentials_title,
                items.toArray(new String[items.size()]),
                this,
                new XoDialogs.OnSingleSelectionFinishedListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id, final int selectedItem) {

                        Backup backup = backups.get(selectedItem);
                        try {
                            if (BackupFileUtils.isEnoughDiskSpaceAvailable(backup.getFile())) {
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
                            } else {
                                XoDialogs.showOkDialog("NotEnoughDiskSpaceAvailableDialog",
                                        R.string.dialog_import_credentials_title,
                                        R.string.dialog_not_enough_disk_space_dialog, XoPreferenceActivity.this);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
                        createBackup(password, BackupType.DATABASE);
                    }
                }
        );
    }

    private void showCompleteBackupDialog() {
        XoDialogs.showInputPasswordDialog("CreateDatabaseBackupDialog",
                R.string.dialog_export_credentials_title,
                this,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        createBackup(password, BackupType.COMPLETE);
                    }
                }
        );
    }

    private void createBackup(final String password, BackupType type) {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                BackupService.BackupServiceBinder binder = (BackupService.BackupServiceBinder) service;
                mBackupService = binder.getService();
                mBackupServiceBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBackupServiceBound = false;
            }
        };

        Bundle bundle = new Bundle();
        bundle.putString("type", type.toString());
        bundle.putString("password", password);

        Intent intent = new Intent(this, BackupService.class);
        intent.putExtras(bundle);

        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        getListView().findViewById(R.id.rl_default_preference).setVisibility(View.GONE);
        getListView().findViewById(R.id.rl_in_progress).setVisibility(View.VISIBLE);

        Button cancelBtn = (Button) getListView().findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBackupServiceBound) {
                    mBackupService.cancel();
                }
            }
        });
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
