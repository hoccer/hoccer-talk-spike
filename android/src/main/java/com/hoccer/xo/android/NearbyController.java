package com.hoccer.xo.android;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.nearby.EnvironmentUpdater;
import com.hoccer.xo.android.service.NotificationId;
import org.apache.log4j.Logger;

public class NearbyController implements BackgroundManager.Listener {

    private static final Logger LOG = Logger.getLogger(NearbyController.class);

    private static NearbyController sInstance;

    private boolean mNearbyEnabled;
    private Runnable mNearbyTimeout;
    private final Handler mNearbyTimeoutHandler = new Handler();


    private final NotificationManager mNotificationManager;
    private final EnvironmentUpdater mEnvironmentUpdater;

    public static NearbyController get() {
        if (sInstance == null) {
            sInstance = new NearbyController();
        }
        return sInstance;
    }

    private NearbyController() {
        mEnvironmentUpdater = new EnvironmentUpdater(XoApplication.get(), XoApplication.get().getXoClient());
        mNotificationManager = (NotificationManager) XoApplication.get().getSystemService(Context.NOTIFICATION_SERVICE);

        BackgroundManager.get().registerListener(this);
        XoApplication.get().getXoClient().registerStateListener(new IXoStateListener() {
            @Override
            public void onClientStateChange(XoClient client, int state) {
                if (mNearbyEnabled) {
                    if (state == XoClient.STATE_READY) {
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
    public void onBecameForeground() {
        if (mNearbyTimeout != null) {
            mNearbyTimeoutHandler.removeCallbacks(mNearbyTimeout);
            mNearbyTimeout = null;
        }

        if (mNearbyEnabled) {
            startEnvironmentUpdates();
        }
    }

    @Override
    public void onBecameBackground() {
        if (mNearbyEnabled && !XoApplication.get().getStayActiveInBackground()) {
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
        if (XoApplication.get().getXoClient().getState() == XoClient.STATE_READY) {
            LOG.info("start environment updates");
            mEnvironmentUpdater.start();
            showNotification();
        }
    }

    private void pauseEnvironmentUpdates() {
        LOG.info("pause environment updates");
        mEnvironmentUpdater.pause();
        removeNotification();
    }

    private void stopEnvironmentUpdates() {
        LOG.info("stop environment updates");
        mEnvironmentUpdater.stop();
        removeNotification();
    }

    private static Notification buildNotification() {
        return new NotificationCompat.Builder(XoApplication.get())
                .setSmallIcon(R.drawable.ic_notification_nearby)
                .setContentTitle(XoApplication.get().getString(R.string.nearby_notification_title))
                .setContentText(XoApplication.get().getString(R.string.nearby_notification_text))
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
        return mEnvironmentUpdater.locationServicesEnabled();
    }
}
