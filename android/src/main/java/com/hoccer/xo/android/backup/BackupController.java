package com.hoccer.xo.android.backup;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.*;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.dialog.OnPreferenceClickListenerThrottled;
import com.hoccer.xo.android.service.CancelableHandlerService;
import com.hoccer.xo.android.service.NotificationId;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.hoccer.xo.android.backup.BackupAndRestoreService.BackupAction;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.BACKUP;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.RESTORE;

public class BackupController implements CreateBackupDialogFragment.CreateBackupDialogListener {

    public static final String TIME_PATTERN = "HH:mm";

    private final Activity mActivity;
    private final BackupPreference mCreateBackupPreference;
    private final BackupPreference mRestoreBackupPreference;

    private BackupAndRestoreService mBackupService;
    private final ServiceConnection mServiceConnection;
    private final BroadcastReceiver mBroadcastReceiver;

    public interface OnCancelListener {
        public void onCancel();
    }

    public BackupController(Activity activity, BackupPreference createBackupPreference, BackupPreference restoreBackupPreference) {
        mActivity = activity;
        mBroadcastReceiver = createBroadcastReceiver();
        mServiceConnection = createServiceConnection();

        OnCancelListener cancelListener = new OnCancelListener() {
            @Override
            public void onCancel() {
                mBackupService.cancel();
            }
        };

        mCreateBackupPreference = createBackupPreference;
        mCreateBackupPreference.setCancelListener(cancelListener);
        mCreateBackupPreference.setOnPreferenceClickListener(new OnPreferenceClickListenerThrottled(500) {
            @Override
            public boolean onPreferenceClickThrottled() {
                showCreateBackupDialog();
                return true;
            }
        });

        mRestoreBackupPreference = restoreBackupPreference;
        mRestoreBackupPreference.setCancelListener(cancelListener);
        mRestoreBackupPreference.setOnPreferenceClickListener(new OnPreferenceClickListenerThrottled(500) {
            @Override
            public boolean onPreferenceClickThrottled() {
                showRestoreBackupDialog();
                return false;
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
                } else {
                    setBackupInProgress(false);
                    setRestoreInProgress(false);
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
                handleIntent(intent);
            }
        };
    }

    public void handleIntent(Intent intent) {

        BackupAction action = BackupAction.fromString(intent.getAction());

        if (action != null) {
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
    }

    private void showCreateBackupDialog() {
        CreateBackupDialogFragment createBackupDialog = new CreateBackupDialogFragment();
        createBackupDialog.setListener(this);
        createBackupDialog.show(mActivity.getFragmentManager(), "CreateBackupDialog");
    }

    private void createBackup(final String password, BackupType type) {
        mActivity.startService(new Intent(mActivity, BackupAndRestoreService.class)
                .putExtra(BackupAndRestoreService.EXTRA_CREATE_BACKUP_TYPE, type.toString())
                .putExtra(BackupAndRestoreService.EXTRA_PASSWORD, password));
    }

    private void showRestoreBackupDialog() {
        final List<Backup> backups = BackupFileUtils.getBackups(XoApplication.getBackupDirectory());
        if (backups.isEmpty()) {
            showNoBackupsDialog();
        } else {
            showListBackupsDialog(backups);
        }
    }

    private void showNoBackupsDialog() {
        String message = mActivity.getString(R.string.restore_no_backups_found_message, XoApplication.getAttachmentDirectory().getName()
                + File.separator + XoApplication.getBackupDirectory().getName());
        XoDialogs.showOkDialog("NoBackupsFoundDialog", mActivity.getString(R.string.restore_no_backups_found_title), message, mActivity);
    }

    private void showListBackupsDialog(final List<Backup> backups) {
        List<String> items = new ArrayList<String>(backups.size());
        for (Backup backup : backups) {
            String timestamp = new SimpleDateFormat(getLocalizedDatePattern() + " " + TIME_PATTERN).format(backup.getCreationDate());
            items.add(timestamp + " " + FileUtils.byteCountToDisplaySize(backup.getSize()));
        }

        XoDialogs.showSingleChoiceDialog("RestoreDialog",
                R.string.restore_choose_backup,
                items.toArray(new String[items.size()]),
                mActivity,
                new XoDialogs.OnSingleSelectionFinishedListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id, final int selectedItem) {

                        Backup backup = backups.get(selectedItem);
                        try {
                            if (BackupFileUtils.isEnoughDiskSpaceAvailable(backup.getFile())) {
                                XoDialogs.showInputPasswordDialog("RestoreBackupPasswordDialog",
                                        R.string.restore_enter_password,
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
                                long size = BackupFileUtils.getUncompressedSize(backup.getFile());
                                String message = mActivity.getString(R.string.restore_failure_insufficient_disk_space_message,
                                        FileUtils.byteCountToDisplaySize(size));
                                XoDialogs.showOkDialog("NotEnoughDiskSpaceAvailableDialog",
                                        mActivity.getString(R.string.restore_failure_title), message, mActivity);
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
        removeNotification();
    }

    private void showBackupSuccessDialog(Backup backup) {
        String date = String.format(mActivity.getString(R.string.date) + ": " + new SimpleDateFormat(getLocalizedDatePattern() + " " + TIME_PATTERN).format(backup.getCreationDate()));
        String user = String.format(mActivity.getString(R.string.user) + ": " + backup.getClientName());
        String path = String.format(mActivity.getString(R.string.file) + ": " + backup.getFile().getAbsolutePath());
        String size = String.format(mActivity.getString(R.string.size) + ": " + FileUtils.byteCountToDisplaySize(backup.getSize()));
        String message = String.format("%s\n%s\n%s\n%s", date, user, path, size);
        XoDialogs.showOkDialog("BackupCreatedDialog", mActivity.getString(R.string.backup_success_title), message, mActivity);
    }

    private void handleBackupCanceled() {
        setBackupInProgress(false);
    }

    private void handleBackupFailed() {
        setBackupInProgress(false);
        showBackupFailedDialog();
        removeNotification();
    }

    private void showBackupFailedDialog() {
        XoDialogs.showOkDialog("BackupFailedDialog", mActivity.getString(R.string.backup_failure_title), mActivity.getString(R.string.backup_failure_message), mActivity);
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
        removeNotification();
    }

    private void showRestoreSuccessDialog(Backup backup) {
        String date = String.format(mActivity.getString(R.string.date) + ": " + new SimpleDateFormat(getLocalizedDatePattern() + " " + TIME_PATTERN).format(backup.getCreationDate()));
        String user = String.format(mActivity.getString(R.string.user) + ": " + backup.getClientName());
        String info = mActivity.getString(R.string.restore_restart_hoccer);
        String message = String.format("%s\n%s\n\n%s", date, user, info);
        XoDialogs.showOkDialog("BackupRestoredDialog", mActivity.getString(R.string.restore_success_title), message, mActivity, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                XoApplication.restartApplication();
            }
        });
    }

    private void handleRestoreCanceled() {
        setRestoreInProgress(false);
    }

    private void handleRestoreFailed() {
        setRestoreInProgress(false);
        showRestoreFailedDialog();
        removeNotification();
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NotificationId.BACKUP_RESTORE);
    }

    private void showRestoreFailedDialog() {
        XoDialogs.showOkDialog("RestoreFailedDialog", mActivity.getString(R.string.restore_failure_title), mActivity.getString(R.string.restore_failure_default_message), mActivity);
    }

    private void setRestoreInProgress(boolean inProgress) {
        mCreateBackupPreference.setEnabled(!inProgress);
        mRestoreBackupPreference.setInProgress(inProgress);
    }

    @Override
    public void onDialogPositiveClick(String password, boolean includeAttachments) {
        if (includeAttachments) {
            createBackup(password, BackupType.COMPLETE);
        } else {
            createBackup(password, BackupType.DATABASE);
        }
        handleBackupInProgress();
    }

    private static String getLocalizedDatePattern() {
        SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
        return dateFormat.toLocalizedPattern();
    }
}
