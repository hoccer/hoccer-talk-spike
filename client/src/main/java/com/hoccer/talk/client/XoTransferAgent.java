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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class XoTransferAgent implements IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(XoTransferAgent.class);

    private final XoClient mClient;
    private final XoClientDatabase mDatabase;

    private final ScheduledExecutorService mUploadExecutor;
    private final ScheduledExecutorService mDownloadExecutor;

    private final List<IXoTransferListenerOld> mListeners;

    private HttpClient mHttpClient;

    private final Map<Integer, ScheduledFuture> mDownloadRetryQueue;

    public XoTransferAgent(XoClient client) {
        mClient = client;
        mDatabase = mClient.getDatabase();

        mUploadExecutor = createScheduledThreadPool("upload-%d");
        mDownloadExecutor = createScheduledThreadPool("download-%d");

        mListeners = new ArrayList<IXoTransferListenerOld>();
        mDownloadRetryQueue = new ConcurrentHashMap<Integer, ScheduledFuture>();
        initializeHttpClient();
    }

    private ScheduledExecutorService createScheduledThreadPool(String name) {
        ThreadFactoryBuilder tfbUpload = new ThreadFactoryBuilder();
        tfbUpload.setNameFormat(name);
        tfbUpload.setUncaughtExceptionHandler(mClient.getHost().getUncaughtExceptionHandler());
        return Executors.newScheduledThreadPool(mClient.getHost().getTransferThreads(), tfbUpload.build());
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
        return download.getState() == TalkClientDownload.State.DOWNLOADING;
    }

    public void startOrRestartDownload(final TalkClientDownload download) {
        LOG.info("startOrRestartDownload()");
        final int downloadId = download.getClientDownloadId();
        LOG.info("requesting download " + downloadId);
        unscheduledDownloadAttempt(downloadId);
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
    }

    public void pauseDownload(TalkClientDownload download) {
        LOG.info("pauseDownload(" + download.getClientDownloadId() + ")");
        download.pause(this);
    }

    public void resumeDownload(TalkClientDownload download) {
        LOG.info("resumeUpload(" + download.getClientDownloadId() + ")");
        startOrRestartDownload(download);
    }

    public void cancelDownload(TalkClientDownload download) {
        LOG.info("cancelDownload(" + download.getClientDownloadId() + ")");
        download.pause(this);
    }

    public void scheduleDownloadAttempt(final TalkClientDownload download) {
        if(mDownloadRetryQueue.containsKey(download.getClientDownloadId())) {
            LOG.debug("download with id (" + download.getClientDownloadId() + ") is already scheduled for retry");
            return;
        }
        LOG.debug(String.format("scheduling Download(%1$d) for retry.", download.getClientDownloadId()));
        int transferFailures = download.getTransferFailures();
        long delay = 2 * (transferFailures * transferFailures + 1);
        ScheduledFuture<?> future = mDownloadExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                startOrRestartDownload(download);
            }
        }, delay, TimeUnit.SECONDS);
        mDownloadRetryQueue.put(download.getClientDownloadId(), future);
    }

    private void unscheduledDownloadAttempt(int downloadId) {
        ScheduledFuture future = mDownloadRetryQueue.remove(downloadId);
        if(future != null) {
            future.cancel(true);
        }
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /****************************************** Upload ********************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    public boolean isUploadActive(TalkClientUpload upload) {
        return upload.getState() == TalkClientUpload.State.UPLOADING;
    }

    public void startOrRestartUpload(final TalkClientUpload upload) {
        LOG.info("startOrRestartUpload(), dataurl: " + upload.getContentDataUrl() +
                                " | contenturl: " + upload.getContentUrl() +
                                " | datafile: " + upload.getDataFile() +
                                " | contenttype: " + upload.getContentType() +
                                " | clientUploadId: " + upload.getClientUploadId());

        if(upload.getFileId() == null) {
            upload.register(this);
        }
        final int uploadId = upload.getClientUploadId();
        LOG.info("requesting upload with id '" + uploadId + "'");
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
    }

    public void pauseUpload(TalkClientUpload upload) {
        LOG.info("pauseUpload(" + upload.getClientUploadId() + ")");
        upload.pause(this);
    }

    public void resumeUpload(TalkClientUpload upload) {
        LOG.info("resumeUpload(" + upload.getClientUploadId() + ")");
        startOrRestartUpload(upload);
    }

    public void cancelUpload(TalkClientUpload upload) {
        LOG.info("cancelUpload(" + upload.getClientUploadId() + ")");
        upload.cancel(this);
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /**************************** XoTransferListenerOld implementation ****************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
        LOG.info("onDownloadRegistered(" + download.getClientDownloadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onDownloadRegistered(download);
        }
    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {
        LOG.info("onDownloadStarted(" + download.getClientDownloadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onDownloadStarted(download);
        }
    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {
        LOG.trace("onDownloadProgress(" + download.getClientDownloadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onDownloadProgress(download);
        }
    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        LOG.info("onDownloadFinished(" + download.getClientDownloadId() + ")");
        LOG.info("removed Download with id (" + download.getClientDownloadId() + ") from HashMap");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onDownloadFinished(download);
        }
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {
        LOG.info("onDownloadFailed(" + download.getClientDownloadId() + ")");
        for(IXoTransferListenerOld listener: mListeners) {
            listener.onDownloadFailed(download);
        }
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        LOG.info("onDownloadStateChanged(" + download.getClientDownloadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onDownloadStateChanged(download);
        }
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        LOG.info("onUploadStarted(id: " + upload.getClientUploadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onUploadStarted(upload);
        }
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
        LOG.trace("onUploadProgress(" + upload.getClientUploadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onUploadProgress(upload);
        }
    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {
        LOG.info("onUploadFinished(" + upload.getClientUploadId() + ")");
        LOG.info("removed Upload with id (" + upload.getClientUploadId() + ") from HashMap");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onUploadFinished(upload);
        }
    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {
        LOG.info("onUploadFailed(" + upload.getClientUploadId() + ")");
        for(IXoTransferListenerOld listener: mListeners) {
            listener.onUploadFailed(upload);
        }
    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        LOG.info("onUploadStateChanged(id: " + upload.getClientUploadId() + ")");
        for (int i=0; i < mListeners.size(); i++) {
            IXoTransferListenerOld listener = mListeners.get(i);
            listener.onUploadStateChanged(upload);
        }
    }

}
