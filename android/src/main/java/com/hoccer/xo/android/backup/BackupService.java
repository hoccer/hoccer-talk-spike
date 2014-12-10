package com.hoccer.xo.android.backup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
    private NotificationManager mNotificationManager;

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
        HandlerThread thread = new HandlerThread(BACKUP_HANDLER_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mLooper);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final Bundle extras = intent.getExtras();
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.setData(extras);
            mServiceHandler.sendMessage(msg);
        }

        return START_REDELIVER_INTENT; // If process gets killed by os, the last intent will be redelivered.
    }

    public void createBackup(final String password, String type) {

        try {
            startInForeground();

            Backup backup = null;
            if (type.equals(BackupType.COMPLETE.toString())) {
                backup = BackupFactory.createCompleteBackup(password);
            } else if (type.equals(BackupType.DATABASE.toString())) {
                backup = BackupFactory.createDatabaseBackup(password);
            }

            stopForeground(true);

            triggerSuccessNotification();

            broadcastResult(backup);
        } catch (InterruptedException e) {
            broadcastCanceled();
            e.printStackTrace();
        } catch (Exception e) {
            broadcastFailed();
            triggerFailedNotification();
            e.printStackTrace();
        } finally {
            stopForeground(true); // TODO Check for exceptions?
            stopSelf();
        }
    }

    private void startInForeground() {
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Creating backup ..")
                .setOngoing(true)
                .setContentIntent(createPendingIntent())
                .setPriority(Notification.PRIORITY_MAX).build();

        startForeground(BACKUP_NOTIFICATION_ID, notification);
    }

    private void broadcastResult(Backup backup) {
        Intent intent = new Intent(IntentHelper.ACTION_BACKUP_SUCCEEDED);
        intent.putExtra("result", backup);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastCanceled() {
        Intent intent = new Intent(IntentHelper.ACTION_BACKUP_CANCELED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastFailed() {
        Intent intent = new Intent(IntentHelper.ACTION_BACKUP_FAILED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public void cancel() {
        mLooper.getThread().interrupt();
    }

    private void triggerSuccessNotification() {
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Successfully created backup.")
                .setContentIntent(createPendingIntent())
                .setPriority(Notification.PRIORITY_MAX).build();

        mNotificationManager.notify(BACKUP_NOTIFICATION_ID, notification);
    }

    private void triggerFailedNotification() {
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Failed creating backup.")
                .setContentIntent(createPendingIntent())
                .setPriority(Notification.PRIORITY_MAX).build();

        mNotificationManager.notify(BACKUP_NOTIFICATION_ID, notification);
    }

    private PendingIntent createPendingIntent() {
        Intent resultIntent = new Intent(this, XoPreferenceActivity.class);
        resultIntent.putExtra("show_backup_preferences", true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
