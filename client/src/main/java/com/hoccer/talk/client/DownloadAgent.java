package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientDownload;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.hoccer.talk.client.model.TalkClientDownload.State.*;

public class DownloadAgent extends TransferAgent {

    private static final Logger LOG = Logger.getLogger(DownloadAgent.class);
    private static final String DOWNLOAD_POOL_NAME_PATTERN = "startDownload-pool-%d";

    private final Map<Integer, DownloadAction> mDownloadActions = new ConcurrentHashMap<Integer, DownloadAction>();

    public DownloadAgent(XoClient client) {
        super(client, DOWNLOAD_POOL_NAME_PATTERN);
    }

    public void startDownload(TalkClientDownload download) {
        if (manualDownloadActivated() || exceedsTransferLimit(download)) {
            holdDownload(download);
        } else {
            startDownloadTask(download);
        }
    }

    private boolean manualDownloadActivated() {
        return mClient.getDownloadLimit() == MANUAL;
    }

    private boolean exceedsTransferLimit(TalkClientDownload download) {
        return mClient.getDownloadLimit() != UNLIMITED && download.getTransmittedContentLength() >= mClient.getDownloadLimit();
    }

    private void holdDownload(TalkClientDownload download) {
        download.switchState(ON_HOLD);
    }

    public void startDownloadTask(final TalkClientDownload download) {
        final DownloadAction downloadAction = getOrCreateDownloadAction(download);
        Future downloadFuture = mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                downloadAction.start();
            }
        });
        downloadAction.setFuture(downloadFuture);
    }

    private DownloadAction getOrCreateDownloadAction(TalkClientDownload download) {
        if (!mDownloadActions.containsKey(download.getClientDownloadId())) {
            mDownloadActions.put(download.getClientDownloadId(), new DownloadAction(this, download));
        }
        return mDownloadActions.get(download.getClientDownloadId());
    }

    public void scheduleDownloadTask(TalkClientDownload download) {
        DownloadAction downloadAction = mDownloadActions.get(download.getClientDownloadId());
        Future future = scheduleDownloadAttempt(downloadAction, nextDelay(download));
        downloadAction.setFuture(future);
    }

    private Future scheduleDownloadAttempt(final DownloadAction downloadAction, long delay) {
        return mExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                downloadAction.start();
            }
        }, delay, TimeUnit.SECONDS);
    }

    private long nextDelay(TalkClientDownload download) {
        long delay = 2 * (download.getTransferFailures() * download.getTransferFailures() + 1);
        LOG.debug("Scheduling Download " + download.getClientDownloadId() + " for retry with delay " + delay);
        return delay;
    }

    public void pauseDownload(TalkClientDownload download) {
        mDownloadActions.get(download.getClientDownloadId()).cancel();
    }

    public void retryDownload(TalkClientDownload download) {
        download.switchState(RETRYING);
    }

    public void onDownloadStarted(TalkClientDownload download) {
        LOG.info("onDownloadStarted(" + download.getClientDownloadId() + ")");
        for (TransferListener listener : mListeners) {
            listener.onDownloadStarted(download);
        }
    }

    public void onDownloadFinished(TalkClientDownload download) {
        mDownloadActions.remove(download.getClientDownloadId());
        for (TransferListener listener : mListeners) {
            listener.onDownloadFinished(download);
        }
    }

    public void onDownloadFailed(TalkClientDownload download) {
        LOG.info("onDownloadFailed(" + download.getClientDownloadId() + ")");
//        mHttpClient.getConnectionManager().closeExpiredConnections();
        mDownloadActions.remove(download.getClientDownloadId());
        for (TransferListener listener : mListeners) {
            listener.onDownloadFailed(download);
        }
    }

    public void onDownloadStateChanged(TalkClientDownload download) {
        LOG.info("onDownloadStateChanged(" + download.getClientDownloadId() + ")");
        for (TransferListener listener : mListeners) {
            listener.onDownloadStateChanged(download);
        }
    }
    
    @Override
    public void onClientStateChange(XoClient client) {
        if (client.isReady()) {
            try {
                startPendingDownloads();
            } catch (SQLException e) {
                e.printStackTrace();
                LOG.error("SQL error", e);
            }
        }
    }

    private void startPendingDownloads() throws SQLException {
        for (TalkClientDownload download : mClient.getDatabase().findAllPendingDownloads()) {
            download.switchState(PAUSED);
            startDownload(download);
        }
    }


//    public void onDownloadReceived(TalkClientDownload download) {
//        if (download.isAttachment()) {
//            startDownload(download);
//        } else {
//            startDownloadTask(download);
//        }
//    }
//
//    public void onDownloadRegistered(TalkClientDownload download) {
//        for (TransferListener listener : mListeners) {
//            listener.onDownloadRegistered(download);
//        }
//    }
//
//    public void onDownloadProgress(TalkClientDownload download) {
//        for (TransferListener listener : mListeners) {
//            listener.onDownloadProgress(download);
//        }
//    }
}