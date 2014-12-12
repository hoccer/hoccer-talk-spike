package com.hoccer.xo.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;

public abstract class CancelableHandlerService extends Service {

    protected IBinder mBinder = new ServiceBinder();

    private Looper mLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            CancelableHandlerService.this.handleMessage(msg);
        }
    }

    protected abstract void handleMessage(Message msg);

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(getClass().getSimpleName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mLooper);
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
        return super.onStartCommand(intent, flags, startId);
    }

    public class ServiceBinder extends Binder {
        public CancelableHandlerService getService() {
            return CancelableHandlerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void cancel() {
        mLooper.getThread().interrupt();
    }
}
