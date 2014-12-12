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
import com.hoccer.xo.android.activity.XoPreferenceActivity;
import com.hoccer.xo.android.service.CancelableHandlerService;
import org.apache.log4j.Logger;

import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.BACKUP;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.RESTORE;
import static com.hoccer.xo.android.util.IntentHelper.*;

public class BackupAndRestoreService extends CancelableHandlerService {

    public enum OperationInProgress {
        RESTORE, BACKUP
    }

    private static final int BACKUP_RESTORE_NOTIFICATION_ID = 2;
    private static final Logger LOG = Logger.getLogger(BackupAndRestoreService.class.getName());

    private LocalBroadcastManager mLocalBroadcastManager;
    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder mNotificationBuilder;

    private OperationInProgress operationInProcess;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = createNotificationBuilder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT; // If process gets killed by os, the last intent will be redelivered.
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(createPendingIntent())
                .setPriority(Notification.PRIORITY_MAX);
    }

    private PendingIntent createPendingIntent() {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);
        resultIntent.putExtra("select_backup_preferences", true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void handleMessage(Message msg) {
        Bundle data = msg.getData();
        if (data != null) {
            String password = data.getString("password");
            if (password != null) {
                if (data.containsKey("backup")) {
                    Backup backup = data.getParcelable("backup");
                    if (backup != null) {
                        restoreBackup(backup, password);
                    }
                } else if (data.containsKey("type")) {
                    String type = data.getString("type");
                    if (type != null) {
                        createBackup(type, password);
                    }
                }
            }
        }
    }

    private void restoreBackup(Backup backup, String password) {
        try {
            setOperationInProcess(RESTORE);
            startRestoreInForeground(backup, password);
            stopForeground(true);
            triggerRestoreSuccessNotification();
            broadcast(ACTION_RESTORE_SUCCEEDED, backup);
        } catch (InterruptedException e) {
            broadcast(ACTION_RESTORE_CANCELED);
        } catch (Exception e) {
            triggerRestoreFailedNotification();
            broadcast(ACTION_RESTORE_FAILED);
            LOG.error("Restoring " + backup.getFile().getPath() + " failed", e);
        } finally {
            setOperationInProcess(null);
            stopSelf();
        }
    }

    private void startRestoreInForeground(Backup backup, String password) throws Exception {
        startInForeground(buildOngoingNotification("Restoring backup ..."));
        backup.restore(password);
    }

    private void triggerRestoreSuccessNotification() {
        Notification notification = buildNotification("Successfully restored backup.");
        notify(notification);
    }

    private void triggerRestoreFailedNotification() {
        Notification notification = buildNotification("Failed restoring backup.");
        notify(notification);
    }

    private void createBackup(String type, String password) {
        try {
            setOperationInProcess(BACKUP);
            Backup result = startBackupInForeground(type, password);
            stopForeground(true);
            triggerBackupSuccessNotification();
            broadcast(ACTION_BACKUP_SUCCEEDED, result);
        } catch (InterruptedException e) {
            broadcast(ACTION_BACKUP_CANCELED);
        } catch (Exception e) {
            broadcast(ACTION_BACKUP_FAILED);
            triggerBackupFailedNotification();
            LOG.error("Creating " + type + " backup failed", e);
        } finally {
            setOperationInProcess(null);
            stopSelf();
        }
    }

    private Backup startBackupInForeground(String type, String password) throws Exception {
        startInForeground(buildOngoingNotification("Creating backup ..."));
        Backup backup = null;
        if (type.equals(BackupType.COMPLETE.toString())) {
            backup = BackupFactory.createCompleteBackup(password);
        } else if (type.equals(BackupType.DATABASE.toString())) {
            backup = BackupFactory.createDatabaseBackup(password);
        }
        return backup;
    }

    private void triggerBackupSuccessNotification() {
        notify(buildNotification("Successfully created backup."));
    }

    private void triggerBackupFailedNotification() {
        notify(buildNotification("Failed creating backup."));
    }

    private void notify(Notification notification) {
        mNotificationManager.notify(BACKUP_RESTORE_NOTIFICATION_ID, notification);
    }

    private Notification buildNotification(String title) {
        return mNotificationBuilder
                .setContentTitle(title)
                .setOngoing(false)
                .build();
    }

    private void startInForeground(Notification notification) {
        startForeground(BACKUP_RESTORE_NOTIFICATION_ID, notification);
    }

    private Notification buildOngoingNotification(String title) {
        return mNotificationBuilder
                .setContentTitle(title)
                .setOngoing(true)
                .build();
    }

    private void broadcast(String action, Backup backup) {
        Intent intent = new Intent(action);
        intent.putExtra("result", backup);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcast(String action) {
        Intent intent = new Intent(action);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void setOperationInProcess(OperationInProgress operationInProcess) {
        this.operationInProcess = operationInProcess;
    }

    public OperationInProgress getOperationInProcess() {
        return operationInProcess;
    }
}
