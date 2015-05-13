package com.hoccer.talk.client;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.crypto.AESCryptor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.concurrent.Future;

import static com.hoccer.talk.client.model.TalkClientDownload.State.*;

public class DownloadAction implements TransferStateListener {

    private static final Logger LOG = Logger.getLogger(DownloadAction.class);

    public static final int MAX_FAILURES = 16;
    private static final Detector MIME_DETECTOR = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

    private final DownloadAgent mDownloadAgent;
    private TalkClientDownload mDownload;
    private HttpGet mHttpGet;
    private Future mFuture;

    private boolean mActive;

    public DownloadAction(DownloadAgent downloadAgent, TalkClientDownload download) {
        mDownloadAgent = downloadAgent;
        mDownload = download;
        download.registerTransferListener(this);
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
        String downloadFilename = computeDownloadFile(mDownload);
        if (downloadFilename == null) {
            LOG.error("[downloadId: '" + mDownload.getClientDownloadId() + "'] could not determine startDownload filename");
            return;
        }

        LOG.debug("performDownloadRequest(downloadId: '" + mDownload.getClientDownloadId() + "', filename: '" + downloadFilename + "')");
        RandomAccessFile randomAccessFile;
        FileDescriptor fileDescriptor;
        try {
            mHttpGet = new HttpGet(mDownload.getDownloadUrl());

            // determine the requested range
            String range;
            if (mDownload.getContentLength() != -1) {
                long last = mDownload.getContentLength() - 1;
                range = "bytes=" + mDownload.getTransferProgress() + "-" + last;
                LOG.debug("[downloadId: '" + mDownload.getClientDownloadId() + "'] GET " + "requesting range '" + range + "'");
                mHttpGet.addHeader("Range", range);
            }

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

            long bytesToGo = getContentLenghtFromResponse(response);

            String contentRangeString = response.getFirstHeader("Content-Range").getValue();
            ByteRange contentRange = ByteRange.parseContentRange(contentRangeString);

            String contentType = response.getFirstHeader("Content-Type").getValue();
            mDownload.setMimeType(contentType);

            long bytesStart = mDownload.getTransferProgress();
            if (!mDownload.isValidContentRange(contentRange, bytesToGo) || mDownload.getContentLength() == -1) {
                closeResponse(response);
                checkTransferFailure(mDownload.getTransferFailures() + 1, "invalid contentRange or content length is -1 - contentLength: '" + mDownload.getContentLength() + "'", mDownload);
                return;
            }

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            File file = new File(downloadFilename);
            LOG.debug("[downloadId: '" + mDownload.getClientDownloadId() + "'] GET " + "destination: '" + file + "'");

            file.createNewFile();
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileDescriptor = randomAccessFile.getFD();
            randomAccessFile.setLength(mDownload.getContentLength());
            LOG.debug("[downloadId: '" + mDownload.getClientDownloadId() + "'] GET " + "will retrieve '" + bytesToGo + "' bytes");

            randomAccessFile.seek(bytesStart);

            copyData(randomAccessFile, fileDescriptor, inputStream);

            if (mDownload.getTransferProgress() == mDownload.getContentLength()) {
                if (mDownload.getDecryptionKey() != null) {
                    mDownload.switchState(DECRYPTING);
                } else {
                    mDownload.setFilePath(downloadFilename);
                    mDownload.switchState(DETECTING);
                }
            } else {
                checkTransferFailure(mDownload.getTransferFailures() + 1, "Download not completed: " + mDownload.getTransferProgress() + " / " + mDownload.getContentLength() + ", retrying...", mDownload);
            }
        } catch (Exception e) {
            LOG.error("Download error", e);
            checkTransferFailure(mDownload.getTransferFailures() + 1, "startDownload exception!", mDownload);
        }
    }

    private long getContentLenghtFromResponse(HttpResponse response) {
        Header contentLengthHeader = response.getFirstHeader("Content-Length");
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
            InputStream fileInputStream = new FileInputStream(destination);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            Metadata metadata = new Metadata();
            if (mDownload.getMimeType() != null && !"application/octet-stream".equals(mDownload.getMimeType())) {
                metadata.add(Metadata.CONTENT_TYPE, mDownload.getMimeType());
            }
            if (mDownload.getDecryptedFile() != null) {
                metadata.add(Metadata.RESOURCE_NAME_KEY, mDownload.getDecryptedFile());
            }

            MediaType detectedMediaType = MIME_DETECTOR.detect(bufferedInputStream, metadata);

            fileInputStream.close();

            if (detectedMediaType != null) {
                String mediaTypeName = detectedMediaType.toString();
                LOG.info("[downloadId: '" + mDownload.getClientDownloadId() + "'] detected mime-type '" + mediaTypeName + "'");
                mDownload.setMimeType(mediaTypeName);
                MimeType detectedMimeType = MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(mediaTypeName);
                if (detectedMimeType != null) {
                    String extension = detectedMimeType.getExtension();
                    if (extension != null) {
                        LOG.info("[downloadId: '" + mDownload.getClientDownloadId() + "'] renaming to extension '" + detectedMimeType.getExtension() + "'");

                        String destinationFileName = createUniqueFileNameInDirectory(mDownload.getFilename(), extension, destinationDirectory);
                        String destinationPath = destinationDirectory + File.separator + destinationFileName;

                        File newName = new File(destinationPath);
                        if (destination.renameTo(newName)) {
                            mDownload.setDecryptedFile(destinationFileName);
                            mDownload.setFilename(destinationFileName);
                            mDownload.setFilePath(computeRelativeDownloadDirectory(mDownload.getType()) + File.separator + destinationFileName);
                        } else {
                            LOG.warn("could not rename file");
                        }
                    }
                }
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

    /**
     * Creates a unique file name by checking whether a file already exists in a given directory.
     * In case a file with the same name already exists the given file name will be expanded by an underscore and
     * a running number (foo_1.bar) to prevent the existing file from being overwritten.
     *
     * @param file      The given file name
     * @param extension The given file extension
     * @param directory The directory to check
     * @return The file name including running number and extension (foo_1.bar)
     */
    private static String createUniqueFileNameInDirectory(String file, String extension, String directory) {
        if (file == null) {
            file = "unknown_file";
        }
        String newFileName = file;
        String path;
        File f;
        int i = 0;
        while (true) {
            path = directory + File.separator + newFileName + extension;
            f = new File(path);
            if (f.exists()) {
                i++;
                newFileName = file + "_" + i;
            } else {
                break;
            }
        }
        return newFileName + extension;
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
        LOG.error(download.getClientDownloadId() + " " + failureDescription);
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

    private void logGetTrace(String message, int clientDownloadId) {
        LOG.trace("[downloadId: '" + clientDownloadId + "'] GET " + message);
    }

    private void logGetWarning(String message, int clientDownloadId) {
        LOG.warn("[downloadId: '" + clientDownloadId + "'] GET " + message);
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
