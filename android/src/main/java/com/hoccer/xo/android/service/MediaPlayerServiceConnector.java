package com.hoccer.xo.android.service;

import android.content.*;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import com.hoccer.xo.android.util.IntentHelper;

/**
 * Wraps the binding and callback mechanism.
 */
public class MediaPlayerServiceConnector {

    private final Context mContext;
    private String mIntent;
    private Listener mListener;

    private boolean mIsConnected;

    private MediaPlayerService mMediaPlayerService;
    private ServiceConnection mMediaPlayerServiceConnection;
    private BroadcastReceiver mBroadcastReceiver;

    public interface Listener {
        void onConnected(MediaPlayerService service);
        void onDisconnected();
        void onAction(String action, MediaPlayerService service);
    }

    public MediaPlayerServiceConnector(Context mContext) {
        this.mContext = mContext;
    }

    public void connect() {
        disconnect();

        Intent serviceIntent = new Intent(mContext, MediaPlayerService.class);
        bindMediaPlayerService(serviceIntent);
    }

    public void connect(String intent, Listener listener) {
        disconnect();

        mIntent = intent;
        mListener = listener;

        Intent serviceIntent = new Intent(mContext, MediaPlayerService.class);
        bindMediaPlayerService(serviceIntent);

        createMediaPlayerBroadcastReceiver();
    }

    public void disconnect() {
        if(mIsConnected)
        {
            mContext.unbindService(mMediaPlayerServiceConnection);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
            mMediaPlayerService = null;
            mIsConnected = false;
        }
    }

    private void createMediaPlayerBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mListener.onAction(intent.getAction(), mMediaPlayerService);
            }
        };
        IntentFilter filter = new IntentFilter(mIntent);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver, filter);
    }

    private void bindMediaPlayerService(Intent intent) {

        mMediaPlayerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) service;
                mMediaPlayerService = binder.getService();
                mIsConnected = true;

                if(mListener != null) {
                    mListener.onConnected(mMediaPlayerService);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if(mListener != null) {
                    mListener.onDisconnected();
                }
            }
        };

        mContext.bindService(intent, mMediaPlayerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public MediaPlayerService getService() {
        return mMediaPlayerService;
    }

    public boolean isConnected() {
        return mIsConnected;
    }
}
