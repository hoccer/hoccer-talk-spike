package com.hoccer.xo.android.backup;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.activity.XoPreferenceActivity;
import com.hoccer.xo.android.util.IntentHelper;

public class BackupService extends Service {

    public static final String BACKUP_HANDLER_THREAD_NAME = "BackupHandlerThread";

    private static final int BACKUP_NOTIFICATION_ID = 2;

    private final IBinder mBinder = new BackupServiceBinder();

    private LocalBroadcastManager mLocalBroadcastManager;
    private Looper mLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            String type = data.getString("type");
            String password = data.getString("password");
            createBackup(password, type);
        }
    }

    public class BackupServiceBinder extends Binder {
        public BackupService getService() {
            return BackupService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(BACKUP_HANDLER_THREAD_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();

        mLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mLooper);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Bundle extras = intent.getExtras();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY; //TODO choose right return value
    }

    public void createBackup(final String password, String type) {
        triggerBackupCreationInProgressNotification();

        try {
            Backup backup;
            if (type.equals(BackupType.COMPLETE.toString())) {
                backup = BackupFactory.createCompleteBackup(password);
            } else if (type.equals(BackupType.DATABASE.toString())) {
                backup = BackupFactory.createDatabaseBackup(password);
            }
            broadcastBackupSucceeded();
            triggerBackupCreationSucceededNotification();
        } catch (InterruptedException e) {
            broadcastBackupCanceled();
            stopForeground(true);
            e.printStackTrace();
        } catch (Exception e) {
            broadcastBackupFailed();
            triggerBackupCreationFailedNotification();
            e.printStackTrace();
        } finally {
            stopSelf();
        }
    }

    private void broadcastBackupSucceeded() {
        Intent intent = new Intent(IntentHelper.ACTION_BACKUP_SUCCEEDED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastBackupCanceled() {
        Intent intent = new Intent(IntentHelper.ACTION_BACKUP_CANCELED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastBackupFailed() {
        Intent intent = new Intent(IntentHelper.ACTION_BACKUP_FAILED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public void cancel() {
        mLooper.getThread().interrupt();
    }

    private void triggerBackupCreationInProgressNotification() {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Creating backup ..")
                .setOngoing(true)
                .setContentIntent(resultPendingIntent)
                .setPriority(Notification.PRIORITY_MAX);

        startForeground(BACKUP_NOTIFICATION_ID, builder.build());
    }

    private void triggerBackupCreationSucceededNotification() {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Successfully created backup.")
                .setContentIntent(resultPendingIntent)
                .setPriority(Notification.PRIORITY_MAX);

        startForeground(BACKUP_NOTIFICATION_ID, builder.build());
    }

    private void triggerBackupCreationFailedNotification() {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Failed creating backup.")
                .setContentIntent(resultPendingIntent)
                .setPriority(Notification.PRIORITY_MAX);

        startForeground(BACKUP_NOTIFICATION_ID, builder.build());
    }
}
