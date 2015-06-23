package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientDownload;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.hoccer.talk.client.model.TalkClientDownload.State.ON_HOLD;
import static com.hoccer.talk.client.model.TalkClientDownload.State.PAUSED;

public class DownloadAgent extends TransferAgent {

    private static final Logger LOG = Logger.getLogger(DownloadAgent.class);
    private static final String DOWNLOAD_POOL_NAME_PATTERN = "startDownload-pool-%d";

    private final Map<Integer, DownloadAction> mDownloadActions = new ConcurrentHashMap<Integer, DownloadAction>();

    public DownloadAgent(XoClient client) {
        super(client, DOWNLOAD_POOL_NAME_PATTERN);
    }

    public void startDownload(TalkClientDownload download) {
        if (isManualDownload(download)) {
            holdDownload(download);
        } else {
            DownloadAction downloadAction = getOrCreateDownloadAction(download);
            if (downloadAction.getDownload().getState() != PAUSED && downloadAction.getDownload().getState() != ON_HOLD && !downloadAction.isActive()) {
                startDownloadTask(download);
            }
        }
    }

    public void forceStartDownload(TalkClientDownload download) {
        if (isManualDownload(download)) {
            holdDownload(download);
        } else {
            startDownloadTask(download);
        }
    }

    private boolean isManualDownload(TalkClientDownload download) {
        return manualDownloadActivated()
                || exceedsTransferLimit(download)
                || isManualWorldwideDownload(download);
    }

    private boolean isManualWorldwideDownload(TalkClientDownload download) {
        return isManualWorldwideDownloadEnabled() && isWorldwideDownload(download);
    }

    private boolean isManualWorldwideDownloadEnabled() {
        return !mClient.getConfiguration().isWorldwideAutoDownloadEnabled();
    }

    private boolean isWorldwideDownload(TalkClientDownload download) {
        try {
            return mClient.getDatabase().isWorldwideDownload(download.getClientDownloadId());
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return false;
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
        download.switchState(PAUSED);
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

    public void startPendingDownloads() throws SQLException {
        for (TalkClientDownload download : mClient.getDatabase().findAllPendingDownloads()) {
            getOrCreateDownloadAction(download);
            if (download.getState() != PAUSED && download.getState() != ON_HOLD) {
                download.switchState(PAUSED);
                forceStartDownload(download);
            }
        }
    }
}
