package com.hoccer.xo.android.base;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base class for list adapters
 * <p/>
 * This base class implements our own lifecycle for adapters
 * so that they can attach client listeners and manage resources.
 * <p/>
 * Adapters get the following lifecycle calls, similar to activities:
 * onCreate, onResume, onPause, onDestroy
 * <p/>
 * Adapter reload is integrated into the lifecycle by implementing
 * an onRequestReload method. The method requestReload may be used
 * to request a reload whenever the adapter becomes active.
 * <p/>
 * Reloads are rate-limited to a minimum interval to prevent
 * hogging the CPU with superfluous view updates.
 */
public abstract class MessagesAdapter extends BaseAdapter {

    private static final Logger LOG = Logger.getLogger(MessagesAdapter.class);

    private static final long RATE_LIMIT_MSECS = 1000;

    protected final XoActivity mActivity;

    protected final XoClientDatabase mDatabase;

    protected final Resources mResources;

    protected final LayoutInflater mInflater;

    private final ScheduledExecutorService mExecutor;

    private ScheduledFuture<?> mNotifyFuture;

    private AdapterReloadListener mAdapterReloadListener;

    private boolean mActive;
    private boolean mNeedsReload;
    private long mNotifyTimestamp;

    protected MessagesAdapter(XoActivity activity) {
        mActivity = activity;
        mDatabase = mActivity.getXoDatabase();
        mInflater = mActivity.getLayoutInflater();
        mResources = mActivity.getResources();
        mExecutor = XoApplication.get().getExecutor();
    }

    public void runOnUiThread(Runnable runnable) {
        mActivity.runOnUiThread(runnable);
    }

    public XoClient getXoClient() {
        return XoApplication.get().getXoClient();
    }

    public File getAvatarDirectory() {
        return new File(mActivity.getFilesDir(), "avatars");
    }

    public AdapterReloadListener getAdapterReloadListener() {
        return mAdapterReloadListener;
    }

    public void setAdapterReloadListener(AdapterReloadListener adapterReloadListener) {
        mAdapterReloadListener = adapterReloadListener;
    }

    public boolean isActive() {
        return mActive;
    }

    public void onResume() {
        LOG.debug("onResume()");
        mActive = true;
        if (mNeedsReload) {
            mNeedsReload = false;
            performReload();
        }
    }

    public void onPause() {
        LOG.debug("onPause()");
        mActive = false;
    }

    public void requestReload() {
        LOG.debug("requestReload()");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mActive) {
                    performReload();
                } else {
                    mNeedsReload = true;
                }
            }
        });
    }

    private void performReload() {
        LOG.debug("performReload()");
        if (mAdapterReloadListener != null) {
            mAdapterReloadListener.onAdapterReloadStarted(this);
        }
        onReloadRequest();
    }

    protected void reloadFinished() {
        LOG.debug("reloadFinished()");
        if (mAdapterReloadListener != null) {
            mAdapterReloadListener.onAdapterReloadFinished(this);
        }
    }

    public void onCreate() {
    }

    public void onDestroy() {
    }

    public void onReloadRequest() {
    }

    @Override
    public void notifyDataSetChanged() {
        LOG.trace("notifyDataSetChanged()");
        long now = System.currentTimeMillis();
        long delta = now - mNotifyTimestamp;
        if (mNotifyFuture != null) {
            mNotifyFuture.cancel(false);
            mNotifyFuture = null;
        }
        if (delta < RATE_LIMIT_MSECS) {
            long delay = RATE_LIMIT_MSECS - delta;

            if (mExecutor != null) {
                mNotifyFuture = mExecutor.schedule(
                        new Runnable() {
                            @Override
                            public void run() {
                                mNotifyTimestamp = System.currentTimeMillis();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MessagesAdapter.super.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                        , delay, TimeUnit.MILLISECONDS);
            }
        } else {
            mNotifyTimestamp = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessagesAdapter.super.notifyDataSetChanged();
                }
            });
        }
    }

    public interface AdapterReloadListener {

        public void onAdapterReloadStarted(MessagesAdapter adapter);

        public void onAdapterReloadFinished(MessagesAdapter adapter);
    }
}
