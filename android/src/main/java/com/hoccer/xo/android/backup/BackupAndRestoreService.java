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
import com.hoccer.xo.android.activity.ChatsActivity;
import com.hoccer.xo.android.activity.XoPreferenceActivity;
import com.hoccer.xo.android.service.CancelableHandlerService;
import com.hoccer.xo.android.service.NotificationId;
import org.apache.log4j.Logger;

import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.*;
import static com.hoccer.xo.android.backup.BackupAndRestoreService.OperationInProgress.RESTORE;
import static com.hoccer.xo.android.util.IntentHelper.*;

public class BackupAndRestoreService extends CancelableHandlerService {

    public static final String EXTRA_SELECT_BACKUP_PREFERENCES = "select_backup_preferences";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_CREATE_BACKUP_TYPE = "createBackup";
    public static final String EXTRA_BACKUP = "backup";
    public static final String EXTRA_RESTORE_BACKUP = "restoreBackup";

    public enum OperationInProgress {
        RESTORE, BACKUP
    }

    private static final Logger LOG = Logger.getLogger(BackupAndRestoreService.class.getName());

    private LocalBroadcastManager mLocalBroadcastManager;
    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder mNotificationBuilder;

    private OperationInProgress operationInProgress;

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
                .setContentIntent(createPendingIntent());
    }

    private PendingIntent createPendingIntent() {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);
        resultIntent.putExtra(EXTRA_SELECT_BACKUP_PREFERENCES, true);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntent(resultIntent).addParentStack(ChatsActivity.class);
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
            restoreInForeground(backup, password);
            triggerNotification(R.string.restore_backup_success);
            broadcast(ACTION_RESTORE_SUCCEEDED, backup);
        } catch (InterruptedException e) {
            broadcast(ACTION_RESTORE_CANCELED);
        } catch (Exception e) {
            broadcast(ACTION_RESTORE_FAILED);
            triggerNotification(R.string.restore_backup_failed);
            LOG.error("Restoring " + backup.getFile().getPath() + " failed", e);
        } finally {
            setOperationInProgress(null);
            stopSelf();
        }
    }

    private void restoreInForeground(Backup backup, String password) throws Exception {
        startInForeground(buildOngoingNotification(getString(R.string.restore_backup_in_progress)));
        backup.restore(password);
        stopForeground(true);
    }

    private void createBackup(BackupType type, String password) {
        try {
            setOperationInProgress(BACKUP);
            Backup result = backupInForeground(type, password);
            triggerNotification(R.string.create_backup_success);
            broadcast(ACTION_BACKUP_SUCCEEDED, result);
        } catch (InterruptedException e) {
            broadcast(ACTION_BACKUP_CANCELED);
        } catch (Exception e) {
            broadcast(ACTION_BACKUP_FAILED);
            triggerNotification(R.string.create_backup_failed);
            LOG.error("Creating " + type + " backup failed", e);
        } finally {
            setOperationInProgress(null);
            stopSelf();
        }
    }

    private Backup backupInForeground(BackupType type, String password) throws Exception {
        startInForeground(buildOngoingNotification(getString(R.string.create_backup_in_progress)));
        Backup backup = BackupFactory.createBackup(type, password);
        stopForeground(true);
        return backup;
    }

    private void triggerNotification(int stringId) {
        Notification notification = buildNotification(getString(stringId));
        mNotificationManager.notify(NotificationId.BACKUP_RESTORE, notification);
    }

    private Notification buildNotification(String title) {
        return mNotificationBuilder
                .setContentTitle(title)
                .setOngoing(false)
                .build();
    }

    private void startInForeground(Notification notification) {
        startForeground(NotificationId.BACKUP_RESTORE, notification);
    }

    private Notification buildOngoingNotification(String title) {
        return mNotificationBuilder
                .setContentTitle(title)
                .setOngoing(true)
                .build();
    }

    private void broadcast(String action, Backup backup) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_BACKUP, backup);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcast(String action) {
        Intent intent = new Intent(action);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void setOperationInProgress(OperationInProgress operationInProgress) {
        this.operationInProgress = operationInProgress;
    }

    public OperationInProgress getOperationInProgress() {
        return operationInProgress;
    }
}
