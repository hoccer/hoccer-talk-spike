package com.hoccer.talk.client;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.util.ProgressOutputHttpEntity;
import org.apache.commons.io.FileUtils;
import org.apache.http.*;
import org.apache.http.client.methods.HttpPut;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.concurrent.Future;

import static com.hoccer.talk.client.model.TalkClientUpload.State.*;

public class UploadAction implements TransferStateListener {

    private static final Logger LOG = Logger.getLogger(UploadAction.class);

    private final UploadAgent mUploadAgent;
    private TalkClientUpload mUpload;
    private HttpPut mHttpPut;
    private Future mFuture;

    public UploadAction(UploadAgent uploadAgent, TalkClientUpload upload) {
        mUploadAgent = uploadAgent;
        mUpload = upload;
        upload.registerTransferStateListener(this);
    }

    public void setFuture(Future uploadFuture) {
        mFuture = uploadFuture;
    }

    public void register() {
        mUpload.switchState(REGISTERING);
    }

    @Override
    public void onStateChanged(XoTransfer transfer) {
        mUpload = (TalkClientUpload) transfer;
        saveToDatabase(mUpload);
        switch (mUpload.getState()) {
            case NEW:
                mUpload.switchState(REGISTERING);
                break;
            case REGISTERING:
                doRegisteringAction();
                break;
            case UPLOADING:
                doResumeCheckAction();
                doUploadingAction();
                break;
            case PAUSED:
                doPausedAction();
                break;
            case COMPLETE:
                doCompleteAction();
                break;
            case FAILED:
                doFailedAction();
                break;
        }
    }

    public void saveToDatabase(TalkClientUpload upload) {
        try {
            LOG.debug("save TalkClientUpload (" + upload.getClientUploadId() + ") to database");
            mUploadAgent.getXoClient().getDatabase().saveClientUpload(upload);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    public void doRegisteringAction() {
        LOG.info("performRegistration(), state: ‘" + mUpload.getState() + "'");
        if (mUpload.getFileId() != null) {
            LOG.debug("we already have a fileId. no need to register.");
            mUpload.switchState(PAUSED);
            return;
        }

        LOG.info("[uploadId: '" + mUpload.getClientUploadId() + "'] performing registration");
        try {
            ITalkRpcServer.FileHandles handles;
            if (mUpload.isAvatar()) {
                mUpload.setUploadLength(mUpload.getContentLength());
                handles = mUploadAgent.getXoClient().getServerRpc().createFileForStorage((int) mUpload.getUploadLength());
            } else {
                int encryptedLength = AESCryptor.calcEncryptedSize((int) mUpload.getContentLength(), AESCryptor.NULL_SALT, AESCryptor.NULL_SALT);
                mUpload.setTransferLength(encryptedLength);
                mUpload.setUploadLength(encryptedLength);
                handles = mUploadAgent.getXoClient().getServerRpc().createFileForTransfer(encryptedLength);
            }
            mUpload.setFileId(handles.fileId);
            mUpload.setUploadUrl(handles.uploadUrl);
            mUpload.setDownloadUrl(handles.downloadUrl);
            LOG.info("[uploadId: '" + mUpload.getClientUploadId() + "'] registered as fileId: '" + handles.fileId + "'");
            mUpload.switchState(PAUSED);
        } catch (Exception e) {
            LOG.error("error registering", e);
            mUpload.switchState(REGISTERING);
        }
    }

    public void doResumeCheckAction() {
        LOG.info("[uploadId: '" + mUpload.getClientUploadId() + "'] performing check request");

        HttpPut checkRequest = new HttpPut(mUpload.getUploadUrl());
        String contentRangeValue = "bytes */" + mUpload.getUploadLength();
        LOG.trace("PUT-check range '" + contentRangeValue + "'");
        LOG.trace("PUT-check '" + mUpload.getUploadUrl() + "' commencing");
        logRequestHeaders(checkRequest, "PUT-check request header ");

        try {
            HttpResponse checkResponse = mUploadAgent.getHttpClient().execute(checkRequest);
            StatusLine statusLine = checkResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            LOG.trace("PUT-check '" + mUpload.getUploadUrl() + "' with status '" + statusCode + "': " + statusLine.getReasonPhrase());
            if (statusCode != HttpStatus.SC_OK && statusCode != 308 /* resume incomplete */) {
                if (statusCode >= 400 && statusCode <= 499) {
                    LOG.warn("[uploadId: '" + mUpload.getClientUploadId() + "'] Check request received HTTP error: " + statusCode);
                }
                checkResponse.getEntity().consumeContent();

            }
            logRequestHeaders(checkResponse, "PUT-check response header ");

            // process range header from check request
            Header checkRangeHeader = checkResponse.getFirstHeader("Range");
            if (checkRangeHeader != null) {
                if (!checkCompletion(checkRangeHeader, mUpload)) {
                    // TODO: pause ? same as below ?
                }
            } else {
                LOG.warn("[uploadId: '" + mUpload.getClientUploadId() + "'] no range header in check response");
                mUpload.setProgress(0);
            }
            checkResponse.getEntity().consumeContent();

        } catch (IOException e) {
            LOG.error("IOException while retrieving uploaded range from server ", e);
        }
    }

    private static void logRequestHeaders(HttpMessage httpMessage, String logPrefix) {
        Header[] allHeaders = httpMessage.getAllHeaders();
        for (Header header : allHeaders) {
            LOG.trace(logPrefix + header.getName() + ": " + header.getValue());
        }
    }

    public boolean checkCompletion(Header checkRangeHeader, TalkClientUpload upload) {
        long last = upload.getUploadLength() - 1;
        int confirmedProgress = 0;

        ByteRange uploadedRange = ByteRange.parseContentRange(checkRangeHeader.getValue());

        LOG.info("probe returned uploaded range '" + uploadedRange.toContentRangeString() + "'");

        if (uploadedRange.hasTotal()) {
            if (uploadedRange.getTotal() != upload.getUploadLength()) {
                LOG.error("server returned wrong upload length");
                return false;
            }
        }

        if (uploadedRange.hasStart()) {
            if (uploadedRange.getStart() != 0) {
                LOG.error("server returned non-zero start");
                return false;
            }
        }

        if (uploadedRange.hasEnd()) {
            confirmedProgress = (int) uploadedRange.getEnd() + 1;
        }

        LOG.info("progress believed " + upload.getProgress() + " confirmed " + confirmedProgress);
        upload.setProgress(confirmedProgress);
        mUploadAgent.onUploadProgress(upload);

        if (uploadedRange.hasStart() && uploadedRange.hasEnd()) {
            if (uploadedRange.getStart() == 0 && uploadedRange.getEnd() == last) {
                LOG.info("upload complete");
                return true;
            }
        }

        return false;
    }

    public void doUploadingAction() {
        LOG.info("[uploadId: '" + mUpload.getClientUploadId() + "'] performing upload request");
        long bytesToGo = mUpload.getUploadLength() - mUpload.getProgress();
        LOG.debug("'[uploadId: '" + mUpload.getClientUploadId() + "'] bytes to go " + bytesToGo);

        if (bytesToGo == 0) {
            LOG.debug("'[uploadId: '" + mUpload.getClientUploadId() + "'] bytes to go is 0.");
            mUpload.switchState(COMPLETE);
        }
        LOG.debug("'[uploadId: '" + mUpload.getClientUploadId() + "'] current progress: " + mUpload.getProgress() + " | current upload length: " + mUpload.getUploadLength());

        try {
            InputStream clearIs = new FileInputStream(mUpload.getTempCompressedFilePath() != null ? mUpload.getTempCompressedFilePath() : mUpload.getFilePath());
            InputStream encryptingInputStream;
            if (mUpload.isAttachment()) {
                byte[] key = Hex.decode(mUpload.getEncryptionKey());
                encryptingInputStream = AESCryptor.encryptingInputStream(clearIs, key, AESCryptor.NULL_SALT);
            } else {
                encryptingInputStream = clearIs;
            }

            int skipped = (int) encryptingInputStream.skip(mUpload.getProgress());
            LOG.debug("'[uploadId: '" + mUpload.getClientUploadId() + "'] skipped " + skipped + " bytes");

            mHttpPut = createHttpUploadRequest(mUpload);
            mHttpPut.setEntity(new ProgressOutputHttpEntity(encryptingInputStream, bytesToGo, mUpload, mUpload.getProgress()));

            LOG.trace("PUT-upload '" + mUpload.getUploadUrl() + "' commencing");
            logRequestHeaders(mHttpPut, "PUT-upload response header ");
            mUploadAgent.onUploadStarted(mUpload);
            HttpResponse uploadResponse = mUploadAgent.getHttpClient().execute(mHttpPut);

            saveToDatabase(mUpload);
            StatusLine uploadStatus = uploadResponse.getStatusLine();
            int uploadStatusCode = uploadStatus.getStatusCode();
            LOG.trace("PUT-upload '" + mUpload.getUploadUrl() + "' with status '" + uploadStatusCode + "': " + uploadStatus.getReasonPhrase());
            if (uploadStatusCode != HttpStatus.SC_OK && uploadStatusCode != 308 /* resume incomplete */) {
                // client error - mark as failed
                if (uploadStatusCode >= 400 && uploadStatusCode <= 499) {
                    LOG.warn("[uploadId: '" + mUpload.getClientUploadId() + "'] Upload request received HTTP error: " + uploadStatusCode);
                    mUpload.switchState(PAUSED);
                    return;
                }
                uploadResponse.getEntity().consumeContent();
                LOG.error("[uploadId: '" + mUpload.getClientUploadId() + "'] Received error from server. Status code: " + uploadStatusCode);
                mUpload.switchState(PAUSED);  // do we want to restart this task anytime again?
                return;
            }
            logRequestHeaders(uploadResponse, "PUT-upload response header ");
            // process range header from upload request
            Header checkRangeHeader = uploadResponse.getFirstHeader("Range");
            uploadResponse.getEntity().consumeContent();
            if (isUploadComplete(checkRangeHeader, mUpload)) {
                mUpload.setFilePath(computeRelativeUploadFilePath(mUpload.getFilePath()));
                mUpload.switchState(COMPLETE);
            } else {
                LOG.warn("[uploadId: '" + mUpload.getClientUploadId() + "'] no range header in upload response");
                mUpload.switchState(PAUSED);
            }
        } catch (Exception e) {
            LOG.error("Exception while performing upload request: ", e);
        }
    }

    private HttpPut createHttpUploadRequest(TalkClientUpload upload) {
        long last = upload.getUploadLength() - 1;
        long bytesToGo = upload.getUploadLength() - upload.getProgress();

        LOG.trace("PUT-upload '" + upload.getUploadUrl() + "' '" + bytesToGo + "' bytes to go ");
        String uploadRange = "bytes " + upload.getProgress() + "-" + last + "/" + upload.getUploadLength();
        LOG.debug("PUT-upload '" + upload.getUploadUrl() + "' with range '" + uploadRange + "'");

        HttpPut uploadRequest = new HttpPut(upload.getUploadUrl());
        if (upload.getProgress() > 0) {
            uploadRequest.addHeader("Content-Range", uploadRange);
        }
        return uploadRequest;
    }

    private boolean isUploadComplete(Header checkRangeHeader, TalkClientUpload upload) {
        if (checkRangeHeader == null) {
            return false;
        }
        long last = upload.getUploadLength() - 1;
        int confirmedProgress = 0;

        ByteRange uploadedRange = ByteRange.parseContentRange(checkRangeHeader.getValue());
        LOG.info("probe returned uploaded range '" + uploadedRange.toContentRangeString() + "'");

        if (uploadedRange.hasTotal()) {
            if (uploadedRange.getTotal() != upload.getUploadLength()) {
                LOG.error("server returned wrong upload length");
                return false;
            }
        }

        if (uploadedRange.hasStart()) {
            if (uploadedRange.getStart() != 0) {
                LOG.error("server returned non-zero start");
                return false;
            }
        }

        if (uploadedRange.hasEnd()) {
            confirmedProgress = (int) uploadedRange.getEnd() + 1;
        }

        LOG.info("progress believed " + upload.getProgress() + " confirmed " + confirmedProgress);
        if (uploadedRange.hasStart() && uploadedRange.hasEnd()) {
            if (uploadedRange.getStart() == 0 && uploadedRange.getEnd() == last) {
                LOG.info("upload complete");
                return true;
            }
        }

        return false;
    }

    public String computeRelativeUploadFilePath(String filePath) {
        String externalStorageDirectory = mUploadAgent.getXoClient().getExternalStorageDirectory();
        if (filePath.startsWith(externalStorageDirectory)) {
            return filePath.substring(externalStorageDirectory.length() + 1);
        } else {
            return filePath;
        }
    }

    public void doPausedAction() {

        if (mHttpPut != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mHttpPut.abort();
                }
            }).start();

            //mHttpPut = null;
            LOG.debug("aborted current Upload request. Upload can still resume.");
        }
        mUploadAgent.onUploadStateChanged(mUpload);
    }

    public void doCompleteAction() {
        deleteTemporaryFile(mUpload.getEncryptedFile());
        deleteCachedFile(mUpload.getTempCompressedFilePath());
        if (mFuture != null) {
            mFuture.cancel(false);
        }
        mUploadAgent.onUploadFinished(mUpload);
    }

    public void doFailedAction() {
        deleteTemporaryFile(mUpload.getEncryptedFile());
        deleteCachedFile(mUpload.getTempCompressedFilePath());
        if (mFuture != null) {
            mFuture.cancel(false);
        }
        mUploadAgent.onUploadFailed(mUpload);
    }

    private void deleteTemporaryFile(String encryptedFile) {
        if (encryptedFile != null) {
            String path = mUploadAgent.getXoClient().getEncryptedUploadDirectory() + File.separator + encryptedFile;
            FileUtils.deleteQuietly(new File(path));
        }
    }

    private void deleteCachedFile(String path) {
        if (path != null) {
            FileUtils.deleteQuietly(new File(path));
        }
    }

    @Override
    public void onProgressUpdated(long progress, long contentLength) {

    }

    @Override
    public void onProgress(int progress) {

    }
}
