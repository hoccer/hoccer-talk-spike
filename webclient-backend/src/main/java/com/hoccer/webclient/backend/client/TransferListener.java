package com.hoccer.webclient.backend.client;

import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.webclient.backend.Configuration;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Implements a tansfer handler.
 */
public class TransferListener implements IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(TransferListener.class);

    private XoClient mClient;
    private Configuration mConfiguration;

    public TransferListener(XoClient client, Configuration configuration) {
        mClient = client;
        mConfiguration = configuration;
    }

    public void onDownloadRegistered(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadRegistered: " + download.getFileName());
        mClient.requestDownload(download, true);
    }

    public void onDownloadStarted(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadStarted: " + download.getFileName());
    }

    public void onDownloadProgress(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadProgress: " + download.getFileName());
    }

    public void onDownloadFinished(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadFinished: " + download.getFileName());

        if (mConfiguration.shouldApproveAllDownloads()) {
            download.setApprovalState(TalkClientDownload.ApprovalState.APPROVED);

            try {
                mClient.getDatabase().saveClientDownload(download);
            } catch (SQLException e) {
                LOG.error("Error saving approved download", e);
            }
        }
    }

    public void onDownloadFailed(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadFailed" + download.getFileName());
    }

    public void onDownloadStateChanged(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadStateChanged: " + download.getFileName());
    }

    public void onUploadStarted(TalkClientUpload upload) {}

    public void onUploadProgress(TalkClientUpload upload) {}

    public void onUploadFinished(TalkClientUpload upload) {}

    public void onUploadFailed(TalkClientUpload upload) {}

    public void onUploadStateChanged(TalkClientUpload upload) {}
}
