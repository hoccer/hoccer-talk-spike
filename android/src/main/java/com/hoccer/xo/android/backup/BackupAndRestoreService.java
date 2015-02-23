package com.hoccer.xo.android.backup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.activity.*;
import com.hoccer.xo.android.service.CancelableHandlerService;
import com.hoccer.xo.android.service.NotificationId;
import org.apache.log4j.Logger;

import java.nio.channels.ClosedByInterruptException;

import static com.hoccer.xo.android.backup.BackupAndRestoreService.BackupAction.*;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.BACKUP;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.RESTORE;

public class BackupAndRestoreService extends CancelableHandlerService {

    public static final String EXTRA_SELECT_BACKUP_PREFERENCES = "select_backup_preferences";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_CREATE_BACKUP_TYPE = "createBackup";
    public static final String EXTRA_BACKUP = "backup";
    public static final String EXTRA_RESTORE_BACKUP = "restoreBackup";

    public enum BackupAction {

        ACTION_BACKUP_IN_PROGRESS("com.hoccer.xo.android.action.ACTION_BACKUP_IN_PROGRESS"),
        ACTION_BACKUP_SUCCEEDED("com.hoccer.xo.android.action.BACKUP_SUCCEEDED"),
        ACTION_BACKUP_CANCELED("com.hoccer.xo.android.action.BACKUP_CANCELED"),
        ACTION_BACKUP_FAILED("com.hoccer.xo.android.action.BACKUP_FAILED"),

        ACTION_RESTORE_IN_PROGRESS("com.hoccer.xo.android.action.ACTION_RESTORE_IN_PROGRESS"),
        ACTION_RESTORE_SUCCEEDED("com.hoccer.xo.android.action.RESTORE_SUCCEEDED"),
        ACTION_RESTORE_CANCELED("com.hoccer.xo.android.action.RESTORE_CANCELED"),
        ACTION_RESTORE_FAILED("com.hoccer.xo.android.action.RESTORE_FAILED");

        private final String mString;

        BackupAction(String string) {
            mString = string;
        }

        @Override
        public String toString() {
            return mString;
        }

        public static BackupAction fromString(String string) {
            for (BackupAction action : BackupAction.values()) {
                if (action.toString().equals(string)) {
                    return action;
                }
            }

            return null;
        }
    }

    public enum OperationInProgress {
        RESTORE, BACKUP
    }

    private static final Logger LOG = Logger.getLogger(BackupAndRestoreService.class.getName());

    private LocalBroadcastManager mLocalBroadcastManager;
    private NotificationManager mNotificationManager;

    private OperationInProgress operationInProgress;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT; // If process gets killed by os, the last intent will be redelivered.
    }

    private PendingIntent createPendingIntent(String action, Backup backup) {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);
        resultIntent.putExtra(EXTRA_SELECT_BACKUP_PREFERENCES, true);
        resultIntent.putExtra(EXTRA_BACKUP, backup);
        resultIntent.setAction(action);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(resultIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void handleMessage(Message msg) {
        Bundle data = msg.getData();
        if (data != null) {
            String password = data.getString(EXTRA_PASSWORD);
            if (password != null) {
                if (data.containsKey(EXTRA_RESTORE_BACKUP)) {
                    Backup backup = data.getParcelable(EXTRA_RESTORE_BACKUP);
                    if (backup != null) {
                        restoreBackup(backup, password);
                    }
                } else if (data.containsKey(EXTRA_CREATE_BACKUP_TYPE)) {
                    String type = data.getString(EXTRA_CREATE_BACKUP_TYPE);
                    if (type != null) {
                        createBackup(BackupType.valueOf(type), password);
                    }
                }
            }
        }
    }

    private void restoreBackup(Backup backup, String password) {
        try {
            setOperationInProgress(RESTORE);
            broadcast(ACTION_RESTORE_IN_PROGRESS);

            startInForeground(buildOngoingNotification(getString(R.string.restore_in_progress_title), getString(R.string.restore_in_progress_subtitle)));
            backup.restore(password);
            stopForeground(true);

            triggerNotification(R.string.restore_success_title, R.string.restore_success_message, ACTION_RESTORE_SUCCEEDED, backup);
            broadcast(ACTION_RESTORE_SUCCEEDED, backup);
        } catch (InterruptedException e) {
            stopForeground(true);
            broadcast(ACTION_RESTORE_CANCELED);
        } catch (ClosedByInterruptException e) {
            stopForeground(true);
            broadcast(ACTION_RESTORE_CANCELED);
        } catch (Exception e) {
            stopForeground(true);
            broadcast(ACTION_RESTORE_FAILED);
            triggerNotification(R.string.restore_failure_title, R.string.restore_failure_default_message, ACTION_RESTORE_FAILED, null);
            LOG.error("Restoring " + backup.getFile().getPath() + " failed", e);
        } finally {
            setOperationInProgress(null);
            stopSelf();
        }
    }

    private void createBackup(BackupType type, String password) {
        try {
            setOperationInProgress(BACKUP);
            broadcast(ACTION_BACKUP_IN_PROGRESS);

            startInForeground(buildOngoingNotification(getString(R.string.backup_in_progress_title), getString(R.string.backup_in_progress_subtitle)));
            Backup backup = BackupFactory.createBackup(type, password);
            stopForeground(true);

            triggerNotification(R.string.backup_success_title, R.string.backup_success_message, ACTION_BACKUP_SUCCEEDED, backup);
            broadcast(ACTION_BACKUP_SUCCEEDED, backup);
        } catch (InterruptedException e) {
            stopForeground(true);
            broadcast(ACTION_BACKUP_CANCELED);
        } catch (Exception e) {
            stopForeground(true);
            broadcast(ACTION_BACKUP_FAILED);
            triggerNotification(R.string.backup_failure_title, R.string.backup_failure_message, ACTION_BACKUP_FAILED, null);
            LOG.error("Creating " + type + " backup failed", e);
        } finally {
            setOperationInProgress(null);
            stopSelf();
        }
    }

    private void triggerNotification(int titleId, int subtitleId, BackupAction action, Backup backup) {
        Notification notification = buildNotification(getString(titleId), getString(subtitleId), action, backup);
        mNotificationManager.notify(NotificationId.BACKUP_RESTORE, notification);
    }

    private Notification buildNotification(String title, String subtitle, BackupAction action, Backup backup) {
        PendingIntent pendingIntent = createPendingIntent(action.toString(), backup);
        return createNotificationBuilder()
                .setContentTitle(title)
                .setContentText(subtitle)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startInForeground(Notification notification) {
        startForeground(NotificationId.BACKUP_RESTORE, notification);
    }

    private Notification buildOngoingNotification(String title, String subtitle) {
        return createNotificationBuilder()
                .setContentTitle(title)
                .setContentText(subtitle)
                .setProgress(0, 0, true)
                .setOngoing(true)
                .build();
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_backup)
                .setContentIntent(createPendingIntent(null, null));
    }

    private void broadcast(BackupAction action, Backup backup) {
        Intent intent = new Intent(action.toString());
        intent.putExtra(EXTRA_BACKUP, backup);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcast(BackupAction action) {
        Intent intent = new Intent(action.toString());
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void setOperationInProgress(OperationInProgress operationInProgress) {
        this.operationInProgress = operationInProgress;
    }

    public OperationInProgress getOperationInProgress() {
        return operationInProgress;
    }
}
