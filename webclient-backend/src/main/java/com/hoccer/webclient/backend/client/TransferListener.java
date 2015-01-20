package com.hoccer.webclient.backend.client;

import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import org.apache.log4j.Logger;

/**
 * Implements a tansfer handler.
 */
public class TransferListener implements IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(TransferListener.class);
    private XoClient mClient;

    public TransferListener(XoClient client) {
        mClient = client;
    }

    public void onDownloadRegistered(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadRegistered: " + download.getDownloadFile());
        mClient.requestDownload(download, true);
    }

    public void onDownloadStarted(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadStarted: " + download.getDownloadFile());
    }

    public void onDownloadProgress(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadProgress: " + download.getDownloadFile());
    }

    public void onDownloadFinished(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadFinished: " + download.getDownloadFile());
    }

    public void onDownloadFailed(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadFailed" + download.getDownloadFile());
    }

    public void onDownloadStateChanged(TalkClientDownload download) {
        LOG.debug("TransferHandler::onDownloadStateChanged: " + download.getDownloadFile());
    }

    public void onUploadStarted(TalkClientUpload upload) {
        LOG.debug("TransferHandler::onUploadStarted: " + upload.getDownloadUrl());
    }

    public void onUploadProgress(TalkClientUpload upload) {
        LOG.debug("TransferHandler::onUploadProgress: " + upload.getDownloadUrl());
    }

    public void onUploadFinished(TalkClientUpload upload) {
        LOG.debug("TransferHandler::onUploadFinished: " + upload.getDownloadUrl());
    }

    public void onUploadFailed(TalkClientUpload upload) {
        LOG.debug("TransferHandler::onUploadFailed: " + upload.getDownloadUrl());
    }

    public void onUploadStateChanged(TalkClientUpload upload) {
        LOG.debug("TransferHandler::onUploadStateChanged: " + upload.getDownloadUrl());
    }
}
