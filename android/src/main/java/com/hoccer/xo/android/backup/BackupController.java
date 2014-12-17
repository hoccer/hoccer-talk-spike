package com.hoccer.xo.android.backup;

import android.app.Activity;
import android.content.*;
import android.os.IBinder;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.service.CancelableHandlerService;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.hoccer.xo.android.backup.BackupAndRestoreService.BackupAction;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.BACKUP;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.RESTORE;

public class BackupController {

    private final Activity mActivity;
    private final BackupPreference mCreateBackupPreference;
    private final BackupPreference mRestoreBackupPreference;

    private ServiceConnection mServiceConnection;
    private BackupAndRestoreService mBackupService;
    private BroadcastReceiver mBroadcastReceiver;

    public interface OnCancelListener {
        public void onCancel();
    }

    public BackupController(Activity activity, BackupPreference createBackupPreference, BackupPreference restoreBackupPreference) {
        mActivity = activity;
        mBroadcastReceiver = createBroadcastReceiver();
        mServiceConnection = createServiceConnection();

        View.OnClickListener cancelListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBackupService.cancel();
            }
        };

        mCreateBackupPreference = createBackupPreference;
        mCreateBackupPreference.setCancelListener(cancelListener);
        mCreateBackupPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showCreateBackupDialog();
                return true;
            }
        });

        mRestoreBackupPreference = restoreBackupPreference;
        mRestoreBackupPreference.setCancelListener(cancelListener);
        mRestoreBackupPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showRestoreBackupDialog();
                return true;
            }
        });
    }

    public void registerAndBind() {
        IntentFilter intentFilter = new IntentFilter();

        for (BackupAction action : BackupAction.values()) {
            intentFilter.addAction(action.toString());
        }

        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mBroadcastReceiver, intentFilter);
        mActivity.bindService(new Intent(mActivity, BackupAndRestoreService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unregisterAndUnbind() {
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mBroadcastReceiver);
        mActivity.unbindService(mServiceConnection);
    }

    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                CancelableHandlerService.ServiceBinder binder = (CancelableHandlerService.ServiceBinder) service;
                mBackupService = (BackupAndRestoreService) binder.getService();

                BackupAndRestoreService.OperationInProgress operation = mBackupService.getOperationInProgress();
                if (operation != null) {
                    setBackupInProgress(operation == BACKUP);
                    setRestoreInProgress(operation == RESTORE);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBackupService = null;
            }
        };
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BackupAction action = BackupAction.fromString(intent.getAction());

                switch (action) {
                    case ACTION_BACKUP_IN_PROGRESS:
                        handleBackupInProgress();
                        break;
                    case ACTION_BACKUP_SUCCEEDED:
                        handleBackupSucceeded(intent);
                        break;
                    case ACTION_BACKUP_CANCELED:
                        handleBackupCanceled();
                        break;
                    case ACTION_BACKUP_FAILED:
                        handleBackupFailed();
                        break;
                    case ACTION_RESTORE_IN_PROGRESS:
                        handleRestoreInProgress();
                        break;
                    case ACTION_RESTORE_SUCCEEDED:
                        handleRestoreSucceeded(intent);
                        break;
                    case ACTION_RESTORE_CANCELED:
                        handleRestoreCanceled();
                        break;
                    case ACTION_RESTORE_FAILED:
                        handleRestoreFailed();
                        break;
                }
            }
        };
    }

    private void showCreateBackupDialog() {
        XoDialogs.showInputPasswordDialog("BackupDialog",
                R.string.dialog_backup_title,
                mActivity,
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, String password) {
                        createBackup(password, BackupType.COMPLETE);
                        handleBackupInProgress();
                    }
                }
        );
    }

    private void createBackup(final String password, BackupType type) {
        mActivity.startService(new Intent(mActivity, BackupAndRestoreService.class)
                .putExtra(BackupAndRestoreService.EXTRA_CREATE_BACKUP_TYPE, type.toString())
                .putExtra(BackupAndRestoreService.EXTRA_PASSWORD, password));
    }

    private void showRestoreBackupDialog() {
        final List<Backup> backups = BackupFileUtils.getBackups(XoApplication.getBackupDirectory());

        List<String> items = new ArrayList<String>(backups.size());
        for (Backup backup : backups) {
            String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(backup.getCreationDate());
            items.add(timestamp + " " + FileUtils.byteCountToDisplaySize(backup.getSize()));
        }

        XoDialogs.showSingleChoiceDialog("RestoreDialog",
                R.string.dialog_import_credentials_title,
                items.toArray(new String[items.size()]),
                mActivity,
                new XoDialogs.OnSingleSelectionFinishedListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id, final int selectedItem) {

                        Backup backup = backups.get(selectedItem);
                        try {
                            if (BackupFileUtils.isEnoughDiskSpaceAvailable(backup.getFile())) {
                                XoDialogs.showInputPasswordDialog("ImportBackupPasswordDialog",
                                        R.string.dialog_import_credentials_title,
                                        mActivity,
                                        new XoDialogs.OnTextSubmittedListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int id, String password) {
                                                restoreBackup(backups.get(selectedItem), password);
                                                handleRestoreInProgress();
                                            }
                                        }
                                );
                            } else {
                                XoDialogs.showOkDialog("NotEnoughDiskSpaceAvailableDialog",
                                        R.string.dialog_import_credentials_title,
                                        R.string.dialog_not_enough_disk_space_dialog, mActivity);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void restoreBackup(final Backup backup, final String password) {
        mActivity.startService(new Intent(mActivity, BackupAndRestoreService.class)
                .putExtra(BackupAndRestoreService.EXTRA_RESTORE_BACKUP, backup)
                .putExtra(BackupAndRestoreService.EXTRA_PASSWORD, password));
    }

    private void handleBackupInProgress() {
        setBackupInProgress(true);
    }

    private void handleBackupSucceeded(Intent intent) {
        setBackupInProgress(false);
        Backup backup = intent.getParcelableExtra(BackupAndRestoreService.EXTRA_BACKUP);
        showBackupSuccessDialog(backup);
    }

    private void showBackupSuccessDialog(Backup backup) {
        String date = String.format("Date: %s", new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(backup.getCreationDate()));
        String user = String.format("User: %s", backup.getClientName());
        String path = String.format("Path: %s", backup.getFile().getAbsolutePath());
        String size = String.format("Size: %s", FileUtils.byteCountToDisplaySize(backup.getSize()));
        String message = String.format("%s\n%s\n%s\n%s", date, user, path, size);
        XoDialogs.showOkDialog("BackupCreatedDialog", "Backup created", message, mActivity);
    }

    private void handleBackupCanceled() {
        setBackupInProgress(false);
    }

    private void handleBackupFailed() {
        setBackupInProgress(false);
        showBackupFailedDialog();
    }

    private void showBackupFailedDialog() {
        XoDialogs.showOkDialog("BackupFailedDialog", "Creating backup failed.", "Creating backup failed.", mActivity);
    }

    private void setBackupInProgress(boolean inProgress) {
        mCreateBackupPreference.setInProgress(inProgress);
        mRestoreBackupPreference.setEnabled(!inProgress);
    }

    private void handleRestoreInProgress() {
        setRestoreInProgress(true);
    }

    private void handleRestoreSucceeded(Intent intent) {
        setRestoreInProgress(false);
        Backup backup = intent.getParcelableExtra(BackupAndRestoreService.EXTRA_BACKUP);
        showRestoreSuccessDialog(backup);
    }

    private void showRestoreSuccessDialog(Backup backup) {
        String date = String.format("Date: %s", new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(backup.getCreationDate()));
        String user = String.format("User: %s", backup.getClientName());
        String info = "Please restart Hoccer.";
        String message = String.format("%s\n%s\n\n%s", date, user, info);
        XoDialogs.showOkDialog("BackupRestoredDialog", "Backup restored", message, mActivity);
    }

    private void handleRestoreCanceled() {
        setRestoreInProgress(false);
    }

    private void handleRestoreFailed() {
        setRestoreInProgress(false);
        showRestoreFailedDialog();
    }

    private void showRestoreFailedDialog() {
        XoDialogs.showOkDialog("RestoreFailedDialog", "Restoring backup failed.", "Restoring backup failed.", mActivity);
    }

    private void setRestoreInProgress(boolean inProgress) {
        mCreateBackupPreference.setEnabled(!inProgress);
        mRestoreBackupPreference.setInProgress(inProgress);
    }
}
