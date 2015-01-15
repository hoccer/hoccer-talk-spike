package com.hoccer.talk.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class XoTransferAgent implements IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(XoTransferAgent.class);

    private final XoClient mClient;
    private final XoClientDatabase mDatabase;

    private final ScheduledExecutorService mUploadExecutor;
    private final ScheduledExecutorService mDownloadExecutor;

    private final List<IXoTransferListenerOld> mListeners;

    private HttpClient mHttpClient;

    private final Map<Integer, TalkClientUpload> mUploadsById;
    private final Map<Integer, TalkClientDownload> mDownloadsById;
    private final Map<Integer, ScheduledFuture> mDownloadRetryQueue;

    public XoTransferAgent(XoClient client) {
        mClient = client;
        mDatabase = mClient.getDatabase();

        mUploadExecutor = createScheduledThreadPool("upload-%d");
        mDownloadExecutor = createScheduledThreadPool("download-%d");

        mListeners = new ArrayList<IXoTransferListenerOld>();
        mDownloadsById = new ConcurrentHashMap<Integer, TalkClientDownload>();
        mUploadsById = new ConcurrentHashMap<Integer, TalkClientUpload>();
        mDownloadRetryQueue = new ConcurrentHashMap<Integer, ScheduledFuture>();
        initializeHttpClient();
    }

    private ScheduledExecutorService createScheduledThreadPool(final String name) {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setNameFormat(name);
        threadFactoryBuilder.setUncaughtExceptionHandler(mClient.getHost().getUncaughtExceptionHandler());
        return Executors.newScheduledThreadPool(mClient.getConfiguration().getTransferThreads(), threadFactoryBuilder.build());
    }

    private void initializeHttpClient() {
        // FYI: http://stackoverflow.com/questions/12451687/http-requests-with-httpclient-too-slow
        // && http://stackoverflow.com/questions/3046424/http-post-requests-using-httpclient-take-2-seconds-why
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        mHttpClient = new HttpClientWithKeyStore(httpParams);
    }

    public XoClient getClient() {
        return mClient;
    }

    public XoClientDatabase getDatabase() {
        return mDatabase;
    }

    public HttpClient getHttpClient() {
        return mHttpClient;
    }

    public void registerListener(IXoTransferListenerOld listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void unregisterListener(IXoTransferListenerOld listener) {
        mListeners.remove(listener);
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /****************************************** Download*******************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    public boolean isDownloadActive(TalkClientDownload download) {
        synchronized (mDownloadsById) {
            return mDownloadsById.containsKey(download.getClientDownloadId());
        }
    }

    public void startOrRestartDownload(final TalkClientDownload download, boolean forcedDownload) {
        LOG.info("startOrRestartDownload()");

        if (!forcedDownload) {
            int transferLimit = mClient.getDownloadLimit();
            if (transferLimit == -2) {
                LOG.debug("download put on hold because manual downloads are activated");
                download.hold(this);
                return;
            }
            if (transferLimit != -1 && download.getTransmittedContentLength() >= transferLimit) {
                LOG.debug("download put on hold because the download exceeds the transferLimit");
                download.hold(this);
                return;
            }
        }

        synchronized (mDownloadsById) {
            final int downloadId = download.getClientDownloadId();
            if (!mDownloadsById.containsKey(downloadId)) {
                LOG.info("requesting download " + downloadId);
                mDownloadsById.put(downloadId, download);
                unscheduleDownloadAttempt(downloadId);
                mDownloadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        LOG.info("performing download " + downloadId + " in state " + download.getState());
                        try {
                            download.start(XoTransferAgent.this);
                        } catch (Exception e) {
                            LOG.error("error performing download", e);
                        }
                    }
                });
            } else {
                LOG.info("download " + download.getClientDownloadId() + " already scheduled in state '" + download.getState() + "'");
            }
        }
    }

    public void pauseDownload(TalkClientDownload download) {
        LOG.info("pauseDownload(" + download.getClientDownloadId() + ")");
        download.pause(this);
        mDownloadsById.remove(download.getClientDownloadId());
        mDownloadRetryQueue.remove(download.getClientDownloadId());
    }

    public void resumeDownload(TalkClientDownload download) {
        LOG.info("resumeUpload(" + download.getClientDownloadId() + ")");
        startOrRestartDownload(download, true);
    }

    public void cancelDownload(TalkClientDownload download) {
        LOG.info("cancelDownload(" + download.getClientDownloadId() + ")");
        download.pause(this);
        mDownloadsById.remove(download.getClientDownloadId());
        mDownloadRetryQueue.remove(download.getClientDownloadId());
    }

    public void deactivateDownload(TalkClientDownload download) {
        LOG.info("deactivateDownload(" + download.getClientDownloadId() + ")");
        mDownloadsById.remove(download.getClientDownloadId());
        mDownloadRetryQueue.remove(download.getClientDownloadId());
    }

    public void scheduleDownloadAttempt(final TalkClientDownload download) {
        if (mDownloadRetryQueue.containsKey(download.getClientDownloadId())) {
            LOG.debug("download with id (" + download.getClientDownloadId() + ") is already scheduled for retry");
            return;
        }
        int transferFailures = download.getTransferFailures();
        long delay = 2 * (transferFailures * transferFailures + 1);
        LOG.debug("Scheduling Download " + download.getClientDownloadId() + " for retry with delay " + delay);
        ScheduledFuture<?> future = mDownloadExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                startOrRestartDownload(download, true);
            }
        }, delay, TimeUnit.SECONDS);
        mDownloadRetryQueue.put(download.getClientDownloadId(), future);
    }

    private void unscheduleDownloadAttempt(int downloadId) {
        ScheduledFuture future = mDownloadRetryQueue.remove(downloadId);
        if (future != null) {
            LOG.debug("Unscheduling download " + downloadId);
            future.cancel(true);
        }
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /****************************************** Upload ********************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    public boolean isUploadActive(TalkClientUpload upload) {
        synchronized (mUploadsById) {
            return mUploadsById.containsKey(upload.getClientUploadId());
        }
    }

    public void startOrRestartUpload(final TalkClientUpload upload) {
        LOG.info("startOrRestartUpload(), dataurl: " + upload.getContentDataUrl() +
                " | contenturl: " + upload.getContentUrl() +
                " | datafile: " + upload.getDataFile() +
                " | contenttype: " + upload.getContentType() +
                " | clientUploadId: " + upload.getClientUploadId());

        if (upload.getState() == TalkClientUpload.State.COMPLETE) {
            mUploadsById.remove(upload.getClientUploadId());
            return;
        }

        if (upload.getFileId() == null) {
            upload.register(this);
        }

        synchronized (mUploadsById) {
            final int uploadId = upload.getClientUploadId();
            if (!mUploadsById.containsKey(uploadId)) {
                LOG.info("requesting upload with id '" + uploadId + "'");
                mUploadsById.put(uploadId, upload);
                mUploadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        LOG.info("performing upload with id '" + uploadId + "' in state '" + upload.getState() + "'");
                        try {
                            upload.start(XoTransferAgent.this);
                        } catch (Exception e) {
                            LOG.error("error performing upload", e);
                        }
                    }
                });
            } else {
                LOG.info("upload " + upload.getClientUploadId() + " already scheduled in state '" + upload.getState() + "'");
            }
        }
    }

    public void pauseUpload(TalkClientUpload upload) {
        LOG.info("pauseUpload(" + upload.getClientUploadId() + ")");
        upload.pause(this);
        mUploadsById.remove(upload.getClientUploadId());
    }

    public void resumeUpload(TalkClientUpload upload) {
        LOG.info("resumeUpload(" + upload.getClientUploadId() + ")");
        startOrRestartUpload(upload);
    }

    public void cancelUpload(TalkClientUpload upload) {
        LOG.info("cancelUpload(" + upload.getClientUploadId() + ")");
        upload.cancel(this);
        mUploadsById.remove(upload.getClientUploadId());
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /**************************** XoTransferListenerOld implementation ****************************/
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */
    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
        LOG.info("onDownloadRegistered(" + download.getClientDownloadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onDownloadRegistered(download);
        }
    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {
        LOG.info("onDownloadStarted(" + download.getClientDownloadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onDownloadStarted(download);
        }
    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {
        LOG.trace("onDownloadProgress(" + download.getClientDownloadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onDownloadProgress(download);
        }
    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        LOG.info("onDownloadFinished(" + download.getClientDownloadId() + ")");
        mDownloadsById.remove(download.getClientDownloadId());
        mDownloadRetryQueue.remove(download.getClientDownloadId());
        LOG.info("removed Download with id (" + download.getClientDownloadId() + ") from HashMap");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onDownloadFinished(download);
        }
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {
        LOG.info("onDownloadFailed(" + download.getClientDownloadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onDownloadFailed(download);
        }
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        LOG.info("onDownloadStateChanged(" + download.getClientDownloadId() + ")");

        if (download.getState() == TalkClientDownload.State.PAUSED
                || download.getState() == TalkClientDownload.State.ON_HOLD
                || download.getState() == TalkClientDownload.State.RETRYING) {
            LOG.debug("Download paused. " + download.getClientDownloadId() + " Removing from queue.");
            deactivateDownload(download);
        }

        for (IXoTransferListenerOld listener : mListeners) {
            listener.onDownloadStateChanged(download);
        }
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        LOG.info("onUploadStarted(id: " + upload.getClientUploadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onUploadStarted(upload);
        }
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
        LOG.trace("onUploadProgress(" + upload.getClientUploadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onUploadProgress(upload);
        }
    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {
        LOG.info("onUploadFinished(" + upload.getClientUploadId() + ")");
        LOG.info("removed Upload with id (" + upload.getClientUploadId() + ") from HashMap");
        mUploadsById.remove(upload.getClientUploadId());
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onUploadFinished(upload);
        }
    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {
        LOG.info("onUploadFailed(" + upload.getClientUploadId() + ")");
        for (IXoTransferListenerOld listener : mListeners) {
            listener.onUploadFailed(upload);
        }
    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        LOG.info("onUploadStateChanged(id: " + upload.getClientUploadId() + ")");

        if (upload.getState() == TalkClientUpload.State.PAUSED) {
            LOG.debug("Upload paused. " + upload.getClientUploadId() + " Removing from queue.");
            deactivateUpload(upload);
        }

        for (IXoTransferListenerOld listener : mListeners) {
            listener.onUploadStateChanged(upload);
        }
    }

    public void deactivateUpload(TalkClientUpload upload) {
        LOG.info("deactivateUpload(" + upload.getClientUploadId() + ")");
        mUploadsById.remove(upload.getClientUploadId());
    }
}
