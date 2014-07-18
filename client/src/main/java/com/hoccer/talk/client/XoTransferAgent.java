package com.hoccer.talk.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class XoTransferAgent implements IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(XoTransferAgent.class);

    private static final long TASK_TIMEOUT = 30;

    private XoClient mClient;
    private XoClientDatabase mDatabase;

    private final ScheduledExecutorService mUploadExecutor;
    private final ScheduledExecutorService mDownloadExecutor;

    List<IXoTransferListenerOld> mListeners;

    HttpClient mHttpClient;

    Map<Integer, TalkClientDownload> mDownloadsById;
    Map<Integer, TalkClientUpload> mUploadsById;

    public XoTransferAgent(XoClient client) {
        mClient = client;
        mDatabase = mClient.getDatabase();

        mUploadExecutor = createScheduledThreadPool("upload-%d");
        mDownloadExecutor = createScheduledThreadPool("download-%d");

        mListeners = new ArrayList<IXoTransferListenerOld>();
        mDownloadsById = new ConcurrentHashMap<Integer, TalkClientDownload>();
        mUploadsById = new ConcurrentHashMap<Integer, TalkClientUpload>();
        initializeHttpClient();
    }

    private ScheduledExecutorService createScheduledThreadPool(String name) {
        ThreadFactoryBuilder tfbUpload = new ThreadFactoryBuilder();
        tfbUpload.setNameFormat(name);
        tfbUpload.setUncaughtExceptionHandler(mClient.getHost().getUncaughtExceptionHandler());
        return Executors.newScheduledThreadPool(mClient.getHost().getTransferThreads(), tfbUpload.build());
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

    /**********************************************************************************************/
    /**********************************************************************************************/
    /****************************************** Upload ********************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    public boolean isDownloadActive(TalkClientDownload download) {
        synchronized (mDownloadsById) {
            return mDownloadsById.containsKey(download.getClientDownloadId());
        }
    }

    public void startOrRestartDownload(final TalkClientDownload download) {
        LOG.info("startOrRestartDownload()");
        Collections.synchronizedMap(mDownloadsById);
        synchronized (mDownloadsById) {
            final int downloadId = download.getClientDownloadId();
            if(!mDownloadsById.containsKey(downloadId)) {
                LOG.info("requesting download " + downloadId);
                mDownloadsById.put(downloadId, download);
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
                LOG.info("download " + download.getClientDownloadId() + " already active");
            }
        }
    }

    public void pauseDownload(TalkClientDownload download) {
        LOG.info("pauseDownload(" + download.getClientDownloadId() + ")");
        download.pause(this);
        mDownloadsById.remove(download.getClientDownloadId());
    }

    public void resumeDownload(TalkClientDownload download) {
        LOG.info("resumeUpload(" + download.getClientDownloadId() + ")");
        startOrRestartDownload(download);
    }

    public void cancelDownload(TalkClientDownload download) {
        LOG.info("cancelDownload(" + download.getClientDownloadId() + ")");
        download.pause(this);
        mDownloadsById.remove(download.getClientDownloadId());
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

        Collections.synchronizedMap(mUploadsById);
        synchronized (mUploadsById) {
            final int uploadId = upload.getClientUploadId();
            if(!mUploadsById.containsKey(uploadId)) {
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
                LOG.info("upload " + upload.getClientUploadId() + " already active");
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
