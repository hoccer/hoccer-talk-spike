package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientUpload;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static com.hoccer.talk.client.model.TalkClientUpload.State.*;

public class UploadAgent extends TransferAgent {

    private static final Logger LOG = Logger.getLogger(UploadAgent.class);
    private static final String UPLOAD_POOL_NAME_PATTERN = "upload-pool-%d";

    private final Map<Integer, UploadAction> mUploadActions = new ConcurrentHashMap<Integer, UploadAction>();

    public UploadAgent(XoClient client) {
        super(client, UPLOAD_POOL_NAME_PATTERN);
    }

    public void register(TalkClientUpload upload) {
        final UploadAction uploadAction = new UploadAction(this, upload);
        mUploadActions.put(upload.getClientUploadId(), uploadAction);

        uploadAction.register();
    }

    public void startUpload(final TalkClientUpload upload) {
        final UploadAction uploadAction = mUploadActions.get(upload.getClientUploadId());
        Future future = mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                uploadAction.start();
            }
        });
        uploadAction.setFuture(future);
    }

    public void resumeUpload(TalkClientUpload upload) {
        startUpload(upload);
    }

    public void pauseUpload(TalkClientUpload upload) {
        upload.switchState(PAUSED);
    }

    public void cancelUpload(TalkClientUpload upload) {
        upload.switchState(PAUSED);
        mUploadActions.remove(upload.getClientUploadId());
    }

    public void onUploadStarted(TalkClientUpload upload) {
        for (TransferListener listener : mListeners) {
            listener.onUploadStarted(upload);
        }
    }

    public void onUploadProgress(TalkClientUpload upload) {
        for (TransferListener listener : mListeners) {
            listener.onUploadProgress(upload);
        }
    }

    public void onUploadFinished(TalkClientUpload upload) {
        mUploadActions.remove(upload.getClientUploadId());
//        mHttpClient.getConnectionManager().closeExpiredConnections();

        for (TransferListener listener : mListeners) {
            listener.onUploadFinished(upload);
        }
    }

    public void onUploadFailed(TalkClientUpload upload) {
        mUploadActions.remove(upload.getClientUploadId());

        for (TransferListener listener : mListeners) {
            listener.onUploadFailed(upload);
        }
    }

    public void onUploadStateChanged(TalkClientUpload upload) {
        LOG.info("onUploadStateChanged(id: " + upload.getClientUploadId() + ")");

        if (upload.getState() == TalkClientUpload.State.PAUSED) {
            LOG.debug("Upload paused. " + upload.getClientUploadId() + " Removing from queue.");
        }

        for (TransferListener listener : mListeners) {
            listener.onUploadStateChanged(upload);
        }
    }

    public boolean isUploadActive(TalkClientUpload upload) {
        return mUploadActions.containsKey(upload.getClientUploadId());
    }
}
