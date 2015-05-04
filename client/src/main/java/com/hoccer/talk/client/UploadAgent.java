package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientUpload;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static com.hoccer.talk.client.model.TalkClientUpload.State.PAUSED;
import static com.hoccer.talk.client.model.TalkClientUpload.State.UPLOADING;

public class UploadAgent extends TransferAgent {

    private static final Logger LOG = Logger.getLogger(UploadAgent.class);
    private static final String UPLOAD_POOL_NAME_PATTERN = "upload-pool-%d";

    private final Map<Integer, UploadAction> mUploadActions = new ConcurrentHashMap<Integer, UploadAction>();

    public UploadAgent(XoClient client) {
        super(client, UPLOAD_POOL_NAME_PATTERN);
    }

    public void register(TalkClientUpload upload) {
        final UploadAction uploadAction = getOrCreateUploadAction(upload);
        uploadAction.register();
    }

    public void startUpload(final TalkClientUpload upload) {
        final UploadAction uploadAction = getOrCreateUploadAction(upload);
        Future future = mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                uploadAction.start();
            }
        });
        uploadAction.setFuture(future);
    }

    private UploadAction getOrCreateUploadAction(TalkClientUpload upload) {
        if (!mUploadActions.containsKey(upload.getClientUploadId())) {
            mUploadActions.put(upload.getClientUploadId(), new UploadAction(this, upload));
        }
        return mUploadActions.get(upload.getClientUploadId());
    }

    public void resumeUpload(final TalkClientUpload upload) {
        if (mUploadActions.containsKey(upload.getClientUploadId())) {
            upload.switchState(UPLOADING);
        } else {
            startUpload(upload);
        }
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

    public void startPendingUploads() throws SQLException {
        for (TalkClientUpload upload : mClient.getDatabase().findAllPendingUploads()) {
            upload.switchState(PAUSED);
            startUpload(upload);
        }
    }
}
