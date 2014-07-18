package com.hoccer.talk.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class XoTransferAgent implements IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(XoTransferAgent.class);

    private static final long TASK_TIMEOUT = 30;


    XoClient mClient;
    XoClientDatabase mDatabase;
    IXoClientDatabaseBackend mDatabaseBackend; // for fixups

    private final ScheduledExecutorService mUploadExecutor;
    private final ScheduledExecutorService mDownloadExecutor;

    List<IXoTransferListenerOld> mListeners;

    HttpClient mHttpClient;

    Map<Integer, TalkClientDownload> mDownloadsById;
    Map<Integer, TalkClientUpload> mUploadsById;

    public XoTransferAgent(XoClient client) {
        mClient = client;
        mDatabase = mClient.getDatabase();
        mDatabaseBackend = mClient.getHost().getDatabaseBackend();

        ThreadFactoryBuilder tfbUpload = new ThreadFactoryBuilder();
        tfbUpload.setNameFormat("upload-%d");
        tfbUpload.setUncaughtExceptionHandler(client.getHost().getUncaughtExceptionHandler());
        mUploadExecutor = Executors.newScheduledThreadPool(client.getHost().getTransferThreads(), tfbUpload.build());

        ThreadFactoryBuilder tfbDownload = new ThreadFactoryBuilder();
        tfbDownload.setNameFormat("download-%d");
        tfbDownload.setUncaughtExceptionHandler(client.getHost().getUncaughtExceptionHandler());
        mDownloadExecutor = Executors.newScheduledThreadPool(client.getHost().getTransferThreads(), tfbDownload.build());


        mListeners = new ArrayList<IXoTransferListenerOld>();
        mDownloadsById = new HashMap<Integer, TalkClientDownload>();
        mUploadsById = new HashMap<Integer, TalkClientUpload>();
        initializeHttpClient();
    }

    private void initializeHttpClient() {
        mHttpClient = new HttpClientWithKeyStore();
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

    public boolean isDownloadActive(TalkClientDownload download) {
        synchronized (mDownloadsById) {
            return mDownloadsById.containsKey(download.getClientDownloadId());
        }
    }

    public void registerDownload(final TalkClientDownload download) {
        try {
            mDatabase.saveClientDownload(download);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
        if(download.getState() == TalkClientDownload.State.INITIALIZING) {
            LOG.info("registerDownload(" + download.getClientDownloadId() + ")");
            download.switchState(this, TalkClientDownload.State.NEW);
            onDownloadRegistered(download);
        }
    }

    public void requestDownload(final TalkClientDownload download) {
        LOG.info("requestDownload()");

        registerDownload(download);

        synchronized (mDownloadsById) {
            final int downloadId = download.getClientDownloadId();
            if(!mDownloadsById.containsKey(downloadId)) {
                TalkClientDownload.State state = download.getState();
                if(state == TalkClientDownload.State.COMPLETE) {
                    LOG.debug("no need to download " + downloadId);
                    return;
                }

                LOG.info("requesting download " + downloadId);

                mDownloadsById.put(downloadId, download);

                mDownloadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        LOG.info("performing download " + downloadId + " in state " + download.getState());
                        try {
                            download.resumeDownload(XoTransferAgent.this);
//                            download.performDownloadAttempt(XoTransferAgent.this);
                        } catch (Exception e) {
                            LOG.error("error performing download", e);
                        }
                    }
                });
            } else {
                LOG.info("download " + download.getClientDownloadId() + " already active");
            }
        }
    }

    public void pauseDownload(TalkClientDownload download) {
        LOG.info("pauseDownload(" + download.getClientDownloadId() + ")");
        download.pauseDownload(this);
        cancelDownload(download);
    }

    public void resumeDownload(TalkClientDownload download) {
        LOG.info("resumeUpload(" + download.getClientDownloadId() + ")");
        requestDownload(download);
    }

    public void cancelDownload(TalkClientDownload download) {
        LOG.info("cancelDownload(" + download.getClientDownloadId() + ")");
        synchronized (mDownloadsById) {
            download.pauseDownload(this);
            mDownloadsById.remove(download.getClientDownloadId());
        }
    }

    public boolean isUploadActive(TalkClientUpload upload) {
        synchronized (mUploadsById) {
            return mUploadsById.containsKey(upload.getClientUploadId());
        }
    }

    public void requestUpload(final TalkClientUpload upload) {
        LOG.info("requestUpload(), dataurl: " + upload.getContentDataUrl() +
                                " | contenturl: " + upload.getContentUrl() +
                                " | datafile: " + upload.getDataFile() +
                                " | contenttype: " + upload.getContentType() +
                                " | clientUploadId: " + upload.getClientUploadId());

        try {
            mDatabase.saveClientUpload(upload);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }

        synchronized (mUploadsById) {
            final int uploadId = upload.getClientUploadId();
            if(!mUploadsById.containsKey(uploadId)) {
                TalkClientUpload.State state = upload.getState();
                if(state == TalkClientUpload.State.COMPLETE) {
                    LOG.debug("no need to upload with id: '" + uploadId + "'");
                }

                LOG.info("requesting upload with id '" + uploadId + "'");

                mUploadsById.put(uploadId, upload);

                mUploadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        LOG.info("performing upload with id '" + uploadId + "' in state '" + upload.getState() + "'");

                        try {
                            upload.start(XoTransferAgent.this);
//                            upload.performUploadAttempt(XoTransferAgent.this);
                        } catch (Exception e) {
                            LOG.error("error performing upload", e);
                        }
                    }
                });


            } else {
                LOG.info("upload " + upload.getClientUploadId() + " already active");
            }
        }
    }

    public void pauseUpload(TalkClientUpload upload) {
        LOG.info("pauseUpload(" + upload.getClientUploadId() + ")");
        upload.pause(this);
        cancelUpload(upload);
    }

    public void resumeUpload(TalkClientUpload upload) {
        LOG.info("resumeUpload(" + upload.getClientUploadId() + ")");
        requestUpload(upload);
    }

    public void cancelUpload(TalkClientUpload upload) {
        LOG.info("cancelUpload(" + upload.getClientUploadId() + ")");
        synchronized (mUploadsById) {
            upload.cancel(this);
            mUploadsById.remove(upload.getClientUploadId());
        }
    }

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
        mDownloadsById.remove(download.getClientDownloadId());
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
        mUploadsById.remove(upload.getClientUploadId());
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
