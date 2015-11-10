package com.hoccer.xo.android;


import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.model.TalkPresence;

public class BackgroundConnectionHandler implements BackgroundManager.Listener, IXoStateListener {

    private static BackgroundConnectionHandler sInstance;
    private final XoClient mClient;
    private PowerManager.WakeLock mWakeLock;
    private final PowerManager mPowerManager;

    public static BackgroundConnectionHandler get() {
        if (sInstance == null) {
            sInstance = new BackgroundConnectionHandler();
        }
        return sInstance;
    }

    private BackgroundConnectionHandler() {
        mClient = XoApplication.get().getClient();

        mClient.registerStateListener(this);
        BackgroundManager.get().registerListener(this);

        mPowerManager = (PowerManager) XoApplication.get().getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public void onBecameForeground(Activity activity) {
        releaseWakeLock();

        mClient.setPresenceStatus(TalkPresence.STATUS_ONLINE);
        mClient.cancelDisconnectTimeout();

        if (mClient.isDisconnected()) {
            connectClientIfNetworkAvailable();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void connectClientIfNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) XoApplication.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected()) {
            mClient.connect();
        }
    }

    @Override
    public void onBecameBackground(Activity activity) {
        acquireWakeLockToCompleteDisconnect();
        mClient.setPresenceStatus(TalkPresence.STATUS_BACKGROUND);
        mClient.disconnectAfterTimeout(XoApplication.getConfiguration().getBackgroundDisconnectTimeoutSeconds());
    }

    private void acquireWakeLockToCompleteDisconnect() {
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Background disconnect");
        mWakeLock.acquire();
    }

    @Override
    public void onClientStateChange(XoClient client) {
        if (client.isDisconnected()) {
            releaseWakeLock();
        }
    }
}
