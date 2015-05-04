package com.hoccer.webclient.backend.client;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.webclient.backend.Configuration;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Implements a tansfer handler.
 */
public class TransferListener implements com.hoccer.talk.client.TransferListener {

    private static final Logger LOG = Logger.getLogger(TransferListener.class);

    private XoClient mClient;
    private Configuration mConfiguration;

    public TransferListener(XoClient client, Configuration configuration) {
        mClient = client;
        mConfiguration = configuration;
    }

    public void onDownloadRegistered(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadRegistered: " + download.getFilename());
        mClient.forceDownload(download);
    }

    public void onDownloadStarted(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadStarted: " + download.getFilename());
    }

    public void onDownloadProgress(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadProgress: " + download.getFilename());
    }

    public void onDownloadFinished(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadFinished: " + download.getFilename());

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
        LOG.debug("TransferHandler::onDownloadFailed" + download.getFilename());
    }

    public void onDownloadStateChanged(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadStateChanged: " + download.getFilename());
    }

    public void onUploadStarted(TalkClientUpload upload) {}

    public void onUploadProgress(TalkClientUpload upload) {}

    public void onUploadFinished(TalkClientUpload upload) {}

    public void onUploadFailed(TalkClientUpload upload) {}

    public void onUploadStateChanged(TalkClientUpload upload) {}
}
