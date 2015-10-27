package com.hoccer.talk.client;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.crypto.AESCryptor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import java.io.*;
import java.net.SocketTimeoutException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.concurrent.Future;

import static com.hoccer.talk.client.model.TalkClientDownload.State.*;

public class DownloadAction implements TransferStateListener {

    private static final Logger LOG = Logger.getLogger(DownloadAction.class);

    public static final int MAX_FAILURES = 16;

    private final DownloadAgent mDownloadAgent;
    private TalkClientDownload mDownload;
    private HttpGet mHttpGet;
    private Future mFuture;

    private boolean mActive;

    public DownloadAction(DownloadAgent downloadAgent, TalkClientDownload download) {
        mDownloadAgent = downloadAgent;
        mDownload = download;
        download.registerTransferStateListener(this);
    }

    public void setFuture(Future downloadFuture) {
        mFuture = downloadFuture;
    }

    public void start() {
        mDownload.switchState(DOWNLOADING);
    }

    @Override
    public void onStateChanged(XoTransfer transfer) {
        mDownload = (TalkClientDownload) transfer;
        saveToDatabase(mDownload);
        switch (mDownload.getState()) {
            case DOWNLOADING:
                mActive = true;
                doDownloadingAction();
                break;
            case PAUSED:
            case PAUSED_BY_UPLOAD:
                mActive = false;
                doPausedAction();
                break;
            case RETRYING:
                mActive = true;
                doRetryingAction();
                break;
            case DECRYPTING:
                mActive = true;
                doDecryptingAction();
                break;
            case DETECTING:
                mActive = true;
                doDetectingAction();
                break;
            case COMPLETE:
                mActive = false;
                doCompleteAction();
                break;
            case FAILED:
                mActive = false;
                doFailedAction();
                break;
            case ON_HOLD:
                mActive = false;
                doOnHoldAction();
                break;
        }
    }

    public void saveToDatabase(TalkClientDownload download) {
        LOG.debug("save TalkClientDownload (" + download.getClientDownloadId() + ") to database");
        try {
            mDownloadAgent.getXoClient().getDatabase().saveClientDownload(download);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    public void doDownloadingAction() {
        if (checkTransferComplete(mDownload)){
            LOG.debug("Download already complete");
            return;
        }

        if (!mDownloadAgent.getXoClient().isReady()) {
            LOG.debug("Client not connected. Download not started.");
            return;
        }
        String downloadFilename = computeDownloadFile(mDownload);
        if (downloadFilename == null) {
            LOG.error("[downloadId: '" + mDownload.getClientDownloadId() + "'] could not determine startDownload filename");
            return;
        }

        LOG.debug("performDownloadRequest(downloadId: '" + mDownload.getClientDownloadId() + "', filename: '" + downloadFilename + "')");

        try {
            mHttpGet = new HttpGet(mDownload.getDownloadUrl());
            addContentRangeHeader(mHttpGet, mDownload);

            mDownloadAgent.onDownloadStarted(mDownload);
            HttpResponse response = mDownloadAgent.getHttpClient().execute(mHttpGet);
            StatusLine status = response.getStatusLine();
            int sc = status.getStatusCode();
            if (sc != HttpStatus.SC_OK && sc != HttpStatus.SC_PARTIAL_CONTENT) {
                closeResponse(response);
                LOG.debug("Http status code (" + sc + ") is not OK (" + HttpStatus.SC_OK + ") or partial content (" +
                        HttpStatus.SC_PARTIAL_CONTENT + ")");
                checkTransferFailure(mDownload.getTransferFailures() + 1, "http status is not OK (" + HttpStatus.SC_OK + ") or partial content (" +
                        HttpStatus.SC_PARTIAL_CONTENT + ")", mDownload);
                return;
            }

            long bytesToGo = getContentLengthFromResponse(response);

            String contentRangeString = response.getFirstHeader("Content-Range").getValue();
            ByteRange contentRange = ByteRange.parseContentRange(contentRangeString);

            long bytesStart = mDownload.getTransferProgress();
            if (!mDownload.isValidContentRange(contentRange, bytesToGo) || mDownload.getContentLength() == -1) {
                closeResponse(response);
                checkTransferFailure(mDownload.getTransferFailures() + 1, "invalid contentRange or content length is -1 - contentLength: '" + mDownload.getContentLength() + "'", mDownload);
                return;
            }

            InputStream inputStream = response.getEntity().getContent();
            File file = new File(downloadFilename);
            LOG.debug("[downloadId: '" + mDownload.getClientDownloadId() + "'] GET " + "destination: '" + file + "'");

            file.createNewFile();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.setLength(mDownload.getContentLength());
            LOG.debug("[downloadId: '" + mDownload.getClientDownloadId() + "'] GET " + "will retrieve '" + bytesToGo + "' bytes");

            randomAccessFile.seek(bytesStart);

            copyData(randomAccessFile, randomAccessFile.getFD(), inputStream);

            if (!checkTransferComplete(mDownload) && (mDownload.getState() != PAUSED) && (mDownload.getState() != PAUSED_BY_UPLOAD) ) {
                checkTransferFailure(mDownload.getTransferFailures() + 1, "Download not completed: " + mDownload.getTransferProgress() + " / " + mDownload.getContentLength() + ", retrying...", mDownload);
            }
        } catch(SocketTimeoutException socketTimeoutException) {
            LOG.error("ReadTimeout. "+mDownload.getTransferFailures()+" Pausing Download.", socketTimeoutException);
            mDownload.switchState(PAUSED_BY_UPLOAD);
        } catch (InterruptedIOException ioe) {
            mDownloadAgent.resetClient();
            LOG.error("InterruptedIOException", ioe);
        } catch (Exception e) {
            LOG.error("Download error", e);
            checkTransferFailure(mDownload.getTransferFailures() + 1, "startDownload exception!", mDownload);
        }
    }

    private void addContentRangeHeader(HttpRequestBase request, TalkClientDownload download) {
        if (download.getContentLength() != -1) {
            long last = download.getContentLength() - 1;
            String range = "bytes=" + download.getTransferProgress() + "-" + last;
            LOG.debug("[downloadId: '" + download.getClientDownloadId() + "'] GET " + "requesting range '" + range + "'");
            request.addHeader("Range", range);
        }
    }

    private boolean checkTransferComplete(TalkClientDownload download) {
        if (download.getTransferProgress() == download.getContentLength()) {
            if (download.getDecryptionKey() != null) {
                download.switchState(DECRYPTING);
            } else {
                download.setFilePath(computeDownloadFile(download));
                download.switchState(DETECTING);
            }
            return true;
        }
        return false;
    }

    private long getContentLengthFromResponse(HttpResponse response) {
        Header contentLengthHeader = response.getFirstHeader("Content-Length");
        // A little dangerous?
        long contentLengthValue = mDownload.getContentLength();
        if (contentLengthHeader != null) {
            String contentLengthString = contentLengthHeader.getValue();
            contentLengthValue = Integer.valueOf(contentLengthString);
        }
        return contentLengthValue;
    }

    public void doDecryptingAction() {
        String sourceFile = computeDownloadFile(mDownload);
        String destinationFile = computeDecryptionFile(mDownload);

        LOG.debug("performDecryption(downloadId: '" + mDownload.getClientDownloadId() + "', sourceFile: '" + sourceFile + "', " + "destinationFile: '" + destinationFile + "')");

        File source = new File(sourceFile);
        File destination = new File(destinationFile);
        if (destination.exists()) {
            destination.delete();
        }

        try {
            byte[] key = Hex.decodeHex(mDownload.getDecryptionKey().toCharArray());
            int bytesToDecrypt = (int) source.length();
            byte[] buffer = new byte[1 << 16];
            InputStream inputStream = new FileInputStream(source);
            OutputStream fileOutputStream = new FileOutputStream(destination);
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            OutputStream digestOutputStream = new DigestOutputStream(fileOutputStream, digest);
            OutputStream decryptingOutputStream = AESCryptor.decryptingOutputStream(digestOutputStream, key, AESCryptor.NULL_SALT);

            int bytesToGo = bytesToDecrypt;
            while (bytesToGo > 0) {
                int bytesToCopy = Math.min(buffer.length, bytesToGo);
                int bytesRead = inputStream.read(buffer, 0, bytesToCopy);
                decryptingOutputStream.write(buffer, 0, bytesRead);
                bytesToGo -= bytesRead;
            }

            decryptingOutputStream.flush();
            decryptingOutputStream.close();
            digestOutputStream.flush();
            digestOutputStream.close();
            inputStream.close();

            String computedHMac = new String(Base64.encodeBase64(digest.digest()));
            if (mDownload.getContentHmac() != null) {
                if (mDownload.getContentHmac().equals(computedHMac)) {
                    LOG.info("startDownload hmac ok");
                } else {
                    LOG.error("startDownload hmac mismatch, computed hmac: '" + computedHMac + "', should be: '" + mDownload.getContentHmac() + "'");
                }
            }

            mDownload.setFilePath(destinationFile);
            mDownload.switchState(DETECTING);
        } catch (IOException ioe){
            LOG.error("decryption error", ioe);
            mDownload.switchState(PAUSED);
            checkTransferFailure(mDownload.getTransferFailures() + 1, "IOFailure during decryption", mDownload);
        } catch (Exception e) {
            LOG.error("decryption error", e);
            checkTransferFailure(mDownload.getTransferFailures() + 1, "failure during decryption", mDownload);
        }
    }

    private String computeDecryptionFile(TalkClientDownload download) {
        String file = null;
        String directory = computeDecryptionDirectory(download);
        if (directory != null) {
            file = directory + File.separator + download.getDecryptedFile();
        }
        return file;
    }

    private String computeDecryptionDirectory(TalkClientDownload download) {
        String directory = null;
        switch (download.getType()) {
            case ATTACHMENT:
                directory = mDownloadAgent.getXoClient().getAttachmentDirectory();
                break;
        }
        return directory;
    }

    public void doDetectingAction() {
        String tempDestinationFilePath;
        String destinationDirectory;
        if (mDownload.getDecryptedFile() != null) {
            tempDestinationFilePath = computeDecryptionFile(mDownload);
            destinationDirectory = computeDecryptionDirectory(mDownload);
        } else {
            tempDestinationFilePath = computeDownloadFile(mDownload);
            destinationDirectory = computeDownloadDirectory(mDownload.getType());
        }

        LOG.debug("performDetection(downloadId: '" + mDownload.getClientDownloadId() + "', destinationFile: '" + tempDestinationFilePath + "')");
        File destination = new File(tempDestinationFilePath);

        try {
            String destinationFileName = createUniqueFileNameInDirectory(mDownload.getFilename(), destinationDirectory);
            String destinationPath = destinationDirectory + File.separator + destinationFileName;
            File newName = new File(destinationPath);
            if (destination.renameTo(newName)) {
                mDownload.setDecryptedFile(destinationFileName);
                mDownload.setFilename(destinationFileName);
                mDownload.setFilePath(computeRelativeDownloadDirectory(mDownload.getType()) + File.separator + destinationFileName);
            } else {
                LOG.warn("could not rename file");
            }
            mDownload.switchState(COMPLETE);
        } catch (Exception e) {
            LOG.error("detection error", e);
            checkTransferFailure(mDownload.getTransferFailures() + 1, "detection failed", mDownload);
        }
    }

    private String computeDownloadDirectory(XoTransfer.Type type) {
        String directory = null;
        switch (type) {
            case AVATAR:
                directory = mDownloadAgent.getXoClient().getAvatarDirectory();
                break;
            case ATTACHMENT:
                directory = mDownloadAgent.getXoClient().getEncryptedDownloadDirectory();
                break;
        }
        return directory;
    }

    private String computeRelativeDownloadDirectory(XoTransfer.Type type) {
        switch (type) {
            case ATTACHMENT:
                return mDownloadAgent.getXoClient().getRelativeAttachmentDirectory();
            case AVATAR:
                return mDownloadAgent.getXoClient().getRelativeAvatarDirectory();
        }
        return null;
    }

    public void doOnHoldAction() {
        if (mHttpGet != null) {
            mHttpGet.abort();
        }
        mDownloadAgent.onDownloadStateChanged(mDownload);
    }

    public void doPausedAction() {
        if (mHttpGet != null) {
            mHttpGet.abort();
        }
        if (mFuture != null) {
            mFuture.cancel(true);
        }
        mDownloadAgent.onDownloadStateChanged(mDownload);
    }

    public void doRetryingAction() {
        if (mHttpGet != null) {
            mHttpGet.abort();
        }
        if (mFuture != null) {
            mFuture.cancel(true);
        }
        mDownloadAgent.scheduleDownloadTask(mDownload);
        mDownloadAgent.onDownloadStateChanged(mDownload);
    }

    public void doCompleteAction() {
        mFuture.cancel(false);
        mDownloadAgent.onDownloadFinished(mDownload);
    }

    public void doFailedAction() {
        mFuture.cancel(true);
        mDownloadAgent.onDownloadFailed(mDownload);
    }

    private static String createUniqueFileNameInDirectory(String fileName, String directory) {
        String fileTitle = fileName;
        String extension = "";

        if (fileName.contains(".")) {
            fileTitle = fileName.substring(0, fileName.lastIndexOf("."));
            extension = fileName.substring(fileName.lastIndexOf("."));
        }

        String newFileTitle = fileTitle;

        int i = 0;
        while (true) {
            if (new File(directory + File.separator + newFileTitle + extension).exists()) {
                i++;
                newFileTitle = fileTitle + "_" + i;
            } else {
                break;
            }
        }
        return newFileTitle + extension;
    }

    private void copyData(RandomAccessFile randomAccessFile, FileDescriptor fileDescriptor, InputStream inputStream) throws IOException {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                randomAccessFile.write(buffer, 0, bytesRead);
                mDownload.setTransferProgress((int) (mDownload.getTransferProgress() + bytesRead));
            }
        } finally {
            fileDescriptor.sync();
            IOUtils.closeQuietly(randomAccessFile);
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void checkTransferFailure(int failures, String failureDescription, TalkClientDownload download) {
        LOG.error(download.getClientDownloadId() + " " + failureDescription+" Count:"+failures);
        download.setTransferFailures(failures);
        if (failures <= MAX_FAILURES) {
            download.switchState(RETRYING);
        } else {
            download.switchState(FAILED);
        }
    }

    private void closeResponse(HttpResponse response) throws IOException {
        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            response.getEntity().consumeContent();
        }
    }

    private String computeDownloadFile(TalkClientDownload download) {
        String file = null;
        String directory = getDownloadDirectory(download);
        if (directory != null) {
            file = directory + File.separator + download.getDownloadFile();
        }
        return file;
    }

    private String getDownloadDirectory(TalkClientDownload download) {
        String directory = null;
        switch (download.getType()) {
            case AVATAR:
                directory = mDownloadAgent.getXoClient().getAvatarDirectory();
                break;
            case ATTACHMENT:
                directory = mDownloadAgent.getXoClient().getEncryptedDownloadDirectory();
                break;
        }
        return directory;
    }

    @Override
    public void onProgress(int progress) {

    }

    @Override
    public void onProgressUpdated(long progress, long contentLength) {

    }

    public TalkClientDownload getDownload() {
        return mDownload;
    }

    public boolean isActive() {
        return mActive;
    }
}
