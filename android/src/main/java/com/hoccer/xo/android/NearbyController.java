package com.hoccer.xo.android;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.activity.ChatsActivity;
import com.hoccer.xo.android.nearby.NearbyEnvironmentUpdater;
import com.hoccer.xo.android.service.NotificationId;
import org.apache.log4j.Logger;

public class NearbyController implements BackgroundManager.Listener {

    private static final Logger LOG = Logger.getLogger(NearbyController.class);

    private static NearbyController sInstance;

    private boolean mNearbyEnabled;
    private Runnable mNearbyTimeout;
    private final Handler mNearbyTimeoutHandler = new Handler();

    private final NotificationManager mNotificationManager;
    private final NearbyEnvironmentUpdater mNearbyEnvironmentUpdater;

    public static NearbyController get() {
        if (sInstance == null) {
            sInstance = new NearbyController();
        }
        return sInstance;
    }

    private NearbyController() {
        mNearbyEnvironmentUpdater = new NearbyEnvironmentUpdater(XoApplication.get(), XoApplication.get().getXoClient());
        mNotificationManager = (NotificationManager) XoApplication.get().getSystemService(Context.NOTIFICATION_SERVICE);

        BackgroundManager.get().registerListener(this);
        XoApplication.get().getXoClient().registerStateListener(new IXoStateListener() {
            @Override
            public void onClientStateChange(XoClient client) {
                if (mNearbyEnabled) {
                    if (client.isReady()) {
                        startEnvironmentUpdates();
                    } else {
                        pauseEnvironmentUpdates();
                    }
                }
            }
        });
    }

    public void enableNearbyMode() {
        startEnvironmentUpdates();
        mNearbyEnabled = true;
    }

    public void disableNearbyMode() {
        stopEnvironmentUpdates();
        mNearbyEnabled = false;
    }

    @Override
    public void onBecameForeground(Activity activity) {
        if (mNearbyTimeout != null) {
            mNearbyTimeoutHandler.removeCallbacks(mNearbyTimeout);
            mNearbyTimeout = null;
        }

        if (mNearbyEnabled) {
            startEnvironmentUpdates();
        }
    }

    @Override
    public void onBecameBackground(Activity activity) {
        if (mNearbyEnabled) {
            final int timeout = XoApplication.getConfiguration().getBackgroundNearbyTimeoutSeconds();
            mNearbyTimeout = new Runnable() {
                @Override
                public void run() {
                    stopEnvironmentUpdates();
                    mNearbyTimeout = null;
                    LOG.info("Nearby mode timed out after " + timeout + " seconds.");
                }
            };
            mNearbyTimeoutHandler.postDelayed(mNearbyTimeout, timeout * 1000);
        }
    }

    private void startEnvironmentUpdates() {
        if (XoApplication.get().getXoClient().getState() == XoClient.State.READY) {
            LOG.info("start environment updates");
            mNearbyEnvironmentUpdater.start();
            showNotification();
        }
    }

    private void pauseEnvironmentUpdates() {
        LOG.info("pause environment updates");
        mNearbyEnvironmentUpdater.pause();
        removeNotification();
    }

    private void stopEnvironmentUpdates() {
        LOG.info("stop environment updates");
        mNearbyEnvironmentUpdater.stop();
        removeNotification();
    }

    private static Notification buildNotification() {
        Intent intent = new Intent(XoApplication.get(), ChatsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(XoApplication.get(), NotificationId.NEARBY_STATE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(XoApplication.get())
                .setSmallIcon(R.drawable.ic_notification_nearby)
                .setLargeIcon(BitmapFactory.decodeResource(XoApplication.get().getResources(), R.drawable.ic_launcher))
                .setContentTitle(XoApplication.get().getString(R.string.nearby_notification_title))
                .setContentText(XoApplication.get().getString(R.string.nearby_notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();
    }

    private void showNotification() {
        mNotificationManager.notify(NotificationId.NEARBY_STATE, buildNotification());
    }

    public void removeNotification() {
        mNotificationManager.cancel(NotificationId.NEARBY_STATE);
    }

    public boolean locationServicesEnabled() {
        return mNearbyEnvironmentUpdater.locationServicesEnabled();
    }

    public boolean isNearbyEnabled() {
        return mNearbyEnabled;
    }
}
