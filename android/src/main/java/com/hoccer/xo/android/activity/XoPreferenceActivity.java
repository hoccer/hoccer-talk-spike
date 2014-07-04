package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.view.*;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoConfiguration;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.util.XoImportExportUtils;
import com.hoccer.xo.android.view.chat.attachments.AttachmentTransferControlView;
import com.hoccer.xo.release.R;

import net.hockeyapp.android.CrashManager;

import org.apache.log4j.Logger;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XoPreferenceActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = Logger.getLogger(XoPreferenceActivity.class);

    private static final String CREDENTIALS_TRANSFER_FILE = "credentials.json";

    private AttachmentTransferControlView mSpinner;

    private Handler mDialogDismisser;

    private Dialog mWaitingDialog;

    private Menu mMenu;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (XoConfiguration.DEVELOPMENT_MODE_ENABLED) {
            addPreferencesFromResource(R.xml.development_preferences);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }
        getListView().setBackgroundColor(Color.WHITE);

        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector();
        mMediaPlayerServiceConnector.connect(this,
                MediaPlayerService.PLAYSTATE_CHANGED_ACTION,
                new MediaPlayerServiceConnector.Listener() {
                    @Override
                    public void onConnected(MediaPlayerService service) {
                        updateActionBarIcons();
                    }

                    @Override
                    public void onDisconnected() {
                    }

                    @Override
                    public void onAction(String action, MediaPlayerService service) {
                        updateActionBarIcons();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForCrashesIfEnabled();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.common, menu);

        mMenu = menu;
        updateActionBarIcons();

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item.toString() + ")");
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_media_player:
                openFullScreenPlayer();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkForCrashesIfEnabled() {
        if (XoConfiguration.reportingEnable()) {
            CrashManager.register(this, XoConfiguration.HOCKEYAPP_ID);
        }
    }

    public void createDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.waiting_dialog, null);
        mSpinner = (AttachmentTransferControlView) view.findViewById(R.id.content_progress);

        mWaitingDialog = new Dialog(this);
        mWaitingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mWaitingDialog.setContentView(view);
        mWaitingDialog.getWindow()
                .setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
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
        mMediaPlayerServiceConnector.disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("preference_export")) {
            doExport();
            return true;
        } else if (preference.getKey().equals("preference_import")) {
            doImport();
            return true;
        } else if (preference.getKey().equals("preference_data_export")) {
            exportData();
        } else if (preference.getKey().equals("preference_data_import")) {
//            importData();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void exportData() {
        try {
            XoImportExportUtils.exportData();
        } catch (IOException e) {
            LOG.error("Data export failed.", e);
            Toast.makeText(this, "Data export failed", Toast.LENGTH_LONG).show();
        }
    }

    private void doImport() {
        final File credentialsFile = new File(XoApplication.getExternalStorage() + File.separator + CREDENTIALS_TRANSFER_FILE);
        if (credentialsFile == null || !credentialsFile.exists()) {
            Toast.makeText(this, getString(R.string.cant_find_credentials), Toast.LENGTH_LONG).show();
            return;
        }

        XoDialogs.showPasswordDialog("ImportCredentialsDialog",
                R.string.dialog_import_credentials_title,
                this,
                new XoDialogs.OnPasswordClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        if (password != null && password.length() > 0) {
                            importCredentials(credentialsFile, password);
                        } else {
                            Toast.makeText(XoPreferenceActivity.this, R.string.no_password, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }
        );
    }

    private void importCredentials(File credentialsFile, String password) {

        try {
            FileInputStream fileInputStream = new FileInputStream(XoApplication.getExternalStorage() + File.separator + CREDENTIALS_TRANSFER_FILE);

            byte[] credentials = new byte[(int) credentialsFile.length()];
            fileInputStream.read(credentials);

            boolean result = XoApplication.getXoClient().setEncryptedCredentialsFromContainer(credentials, password);
            if (result) {
                Toast.makeText(this, R.string.import_credentials_success, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.import_credentials_failure, Toast.LENGTH_LONG).show();
            }

        } catch (FileNotFoundException e) {
            LOG.error("Error while importing credentials", e);
            Toast.makeText(this, R.string.cant_find_credentials, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            LOG.error("Error while importing credentials", e);
            Toast.makeText(this, R.string.import_credentials_failure, Toast.LENGTH_LONG).show();
        }
    }

    private void doExport() {
        XoDialogs.showPasswordDialog("ExportCredentialsDialog",
                R.string.dialog_export_credentials_title,
                this,
                new XoDialogs.OnPasswordClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        if (password != null && password.length() > 0) {
                            exportCredentials(password);
                        } else {
                            Toast.makeText(XoPreferenceActivity.this, R.string.no_password, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }
        );
    }

    private void exportCredentials(String password) {
        try {
            byte[] credentialsContainer = XoApplication.getXoClient()
                    .makeEncryptedCredentialsContainer(password);

            FileOutputStream fos = new FileOutputStream(
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

    private void openFullScreenPlayer() {
        Intent resultIntent = new Intent(this, FullscreenPlayerActivity.class);
        startActivity(resultIntent);
    }

    private void updateActionBarIcons() {
        if (mMediaPlayerServiceConnector.isConnected() && mMenu != null) {
            MenuItem mediaPlayerItem = mMenu.findItem(R.id.menu_media_player);

            MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            if (service.isStopped() || service.isPaused()) {
                mediaPlayerItem.setVisible(false);
            } else {
                mediaPlayerItem.setVisible(true);
            }
        }
    }
}
