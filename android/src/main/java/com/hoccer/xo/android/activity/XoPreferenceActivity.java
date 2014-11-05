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
import android.preference.*;
import android.view.*;
import android.widget.Toast;
import com.hoccer.talk.client.exceptions.NoClientIdInPresenceException;
import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.util.XoImportExportUtils;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferControlView;
import com.hoccer.xo.release.R;
import net.hockeyapp.android.CrashManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.SQLException;

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
        initDataImportPreferences();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item.toString() + ")");
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForCrashesIfEnabled();
    }

    private void initDataImportPreferences() {
        final ListPreference listPreference = (ListPreference) findPreference("preference_data_import");
        if (listPreference != null) {
            File exportDir = new File(XoApplication.getAttachmentDirectory(), XoImportExportUtils.EXPORT_DIRECTORY);
            File[] exportFiles = exportDir.listFiles();
            if (exportFiles != null) {
                final String[] entries = new String[exportFiles.length];
                String[] entryValues = new String[exportFiles.length];
                int index = 0;
                for (File exportFile : exportDir.listFiles()) {
                    entries[index] = exportFile.getName();
                    entryValues[index] = Integer.toString(index);
                    index++;
                }
                listPreference.setEntries(entries);
                listPreference.setEntryValues(entryValues);
                listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        listPreference.setEnabled(false);
                        importData(entries[Integer.parseInt((String) newValue)]);
                        return true;
                    }
                });
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
        if (key.equals("preference_keysize")) {
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
        if (preference.getKey().equals("preference_export")) {
            doExportCredentials();
            return true;
        } else if (preference.getKey().equals("preference_import")) {
            doImportCredentials();
            return true;
        } else if (preference.getKey().equals("preference_data_export")) {
            preference.setEnabled(false);
            exportData();
            return true;
        } else if (preference.getKey().equals("preference_database_dump")) {
            XoImportExportUtils.getInstance().exportDatabaseToFile();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void importData(final String importFileName) {

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    File exportDir = new File(XoApplication.getAttachmentDirectory(), XoImportExportUtils.EXPORT_DIRECTORY);
                    File importFile = new File(exportDir, importFileName);
                    XoImportExportUtils.getInstance().importDatabaseAndAttachments(importFile);
                } catch (IOException e) {
                    LOG.error("Data import failed.", e);
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                if (success) {
                    Toast.makeText(getBaseContext(), "Data imported successfully", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "Data import failed", Toast.LENGTH_LONG).show();
                }
                findPreference("preference_data_import").setEnabled(true);
            }
        }.execute();
    }

    private void exportData() {
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                try {
                    return XoImportExportUtils.getInstance().exportDatabaseAndAttachments();
                } catch (IOException e) {
                    LOG.error("Data export failed.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File file) {
                super.onPostExecute(file);
                if (file != null) {
                    Toast.makeText(getBaseContext(), "Data exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), "Data export failed", Toast.LENGTH_LONG).show();
                }
                findPreference("preference_data_export").setEnabled(true);
            }
        }.execute();
    }

    private void doImportCredentials() {
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
                        if (password != null && !password.isEmpty()) {
                            importCredentials(credentialsFile, password);
                        } else {
                            Toast.makeText(XoPreferenceActivity.this, R.string.no_password, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void importCredentials(final File credentialsFile, final String password) {
        try {
            final Credentials credentials = readCredentialsTransferFile(credentialsFile, password);
            XoApplication.getXoClient().importCredentials(credentials);
            Toast.makeText(this, R.string.import_credentials_success, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            LOG.error("Error while importing credentials", e);
            Toast.makeText(this, R.string.cant_find_credentials, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            LOG.error("Error while importing credentials", e);
            Toast.makeText(this, R.string.import_credentials_failure, Toast.LENGTH_LONG).show();
        } catch (SQLException e) {
            LOG.error("Error while importing credentials", e);
            e.printStackTrace();
        } catch (NoClientIdInPresenceException e) {
            LOG.error("Error while importing credentials", e);
            e.printStackTrace();
        }
    }

    private Credentials readCredentialsTransferFile(File credentialsFile, String password) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(XoApplication.getExternalStorage() + File.separator + CREDENTIALS_TRANSFER_FILE);
        final byte[] credentialsData = new byte[(int) credentialsFile.length()];
        fileInputStream.read(credentialsData);
        return Credentials.fromEncryptedBytes(credentialsData, password);
    }

    private void doExportCredentials() {
        XoDialogs.showInputPasswordDialog("ExportCredentialsDialog",
                R.string.dialog_export_credentials_title,
                this,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        if (password != null && !password.isEmpty()) {
                            exportCredentials(password);
                        } else {
                            Toast.makeText(XoPreferenceActivity.this, R.string.no_password, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void exportCredentials(final String password) {
        try {
            final Credentials credentials = XoApplication.getXoClient().exportCredentials();
            final byte[] credentialsContainer = credentials.toEncryptedBytes(password);

            final FileOutputStream fos = new FileOutputStream(
                    XoApplication.getExternalStorage() + File.separator + CREDENTIALS_TRANSFER_FILE);
            fos.write(credentialsContainer);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            LOG.error("error while writing credentials container to filesystem.", e);
            Toast.makeText(this, R.string.export_credentials_failure, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            LOG.error("error while generating credentials container", e);
            Toast.makeText(this, R.string.export_credentials_failure, Toast.LENGTH_LONG).show();
        }
        Toast.makeText(this, R.string.export_credentials_success, Toast.LENGTH_LONG).show();
    }
}
