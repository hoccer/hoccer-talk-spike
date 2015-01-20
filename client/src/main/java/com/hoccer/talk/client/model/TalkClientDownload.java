package com.hoccer.talk.client.model;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.XoTransferAgent;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.model.TalkAttachment;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
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
import java.util.*;

@DatabaseTable(tableName = "clientDownload")
public class TalkClientDownload extends XoTransfer implements IXoTransferObject {

    private final static Logger LOG = Logger.getLogger(TalkClientDownload.class);

    private static final Detector MIME_DETECTOR = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

    private static final int MAX_FAILURES = 16;

    private XoTransferAgent mTransferAgent;

    private List<IXoTransferListener> mTransferListeners = new ArrayList<IXoTransferListener>();

    public enum State implements IXoTransferState {
        INITIALIZING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(NEW);
            }
        },
        NEW {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(DOWNLOADING, ON_HOLD);
            }
        },
        DOWNLOADING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(PAUSED, RETRYING, DECRYPTING, DETECTING, FAILED);
            }
        },
        PAUSED {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(DOWNLOADING);
            }
        },
        RETRYING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(DOWNLOADING, PAUSED);
            }
        },
        DECRYPTING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(DETECTING, FAILED);
            }
        },
        DETECTING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(COMPLETE, FAILED);
            }
        },
        COMPLETE,
        FAILED,
        ON_HOLD {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(DOWNLOADING);
            }
        };

        public Set<State> possibleFollowUps() {
            return EnumSet.noneOf(State.class);
        }
    }

    @DatabaseField(generatedId = true)
    private int clientDownloadId;

    @DatabaseField
    private Type type;

    @DatabaseField
    private State state;

    @DatabaseField
    private String fileName;

    @DatabaseField
    private String contentUrl;

    @DatabaseField
    private int contentLength;

    @DatabaseField(width = 128)
    private String contentType;

    @DatabaseField(width = 64)
    private String mediaType;

    @DatabaseField
    private String dataFile;

    /**
     * URL to download
     */
    @DatabaseField(width = 2000)
    private String downloadUrl;

    @DatabaseField
    private String fileId;

    /**
     * Name of the file the download itself will go to
     * <p/>
     * This is relative to the result of computeDownloadDirectory().
     */
    @DatabaseField(width = 2000)
    private String downloadFile;

    @DatabaseField
    private int downloadProgress;

    @DatabaseField(width = 128)
    private String decryptionKey;

    @DatabaseField(width = 2000)
    private String decryptedFile;

    @DatabaseField
    private double aspectRatio;

    @DatabaseField
    private int transferFailures;

    @DatabaseField(width = 128)
    private String contentHmac;

	@DatabaseField
    private ApprovalState approvalState;

    private HttpGet mDownloadRequest = null;

    /** Only for display purposes, the real content length will be retrieved from server since after encryption this value will differ */
    @DatabaseField
    private int transmittedContentLength = -1;

    public TalkClientDownload() {
        super(Direction.DOWNLOAD);
        this.state = State.INITIALIZING;
        this.approvalState = ApprovalState.PENDING;
        this.aspectRatio = 1.0;
        this.downloadProgress = 0;
        this.contentLength = -1;
        mTransferListeners = new ArrayList<IXoTransferListener>();
    }

    public void initializeAsAvatar(XoTransferAgent agent, String url, String id, Date timestamp) {
        LOG.info("[new] initializeAsAvatar(url: '" + url + "')");
        mTransferAgent = agent;
        this.type = Type.AVATAR;
        this.downloadUrl = url;
        this.downloadFile = id + "-" + timestamp.getTime();
        this.fileName = this.downloadFile;
        switchState(State.NEW, "new avatar");
    }

    public void initializeAsAttachment(XoTransferAgent agent, TalkAttachment attachment, String id, byte[] key) {
        LOG.info("[new] initializeAsAttachment(url: '" + attachment.getUrl() + "')");
        mTransferAgent = agent;
        this.type = Type.ATTACHMENT;
        this.contentType = attachment.getMimeType();
        this.mediaType = attachment.getMediaType();
        this.aspectRatio = attachment.getAspectRatio();
        this.transmittedContentLength = attachment.getContentSizeAsInt();
        this.downloadUrl = attachment.getUrl();
        this.downloadFile = id;
        this.decryptedFile = UUID.randomUUID().toString();
        String fileName = attachment.getFileName();
        if (fileName != null) {
            this.fileName = fileName;
        }
        this.decryptionKey = new String(Hex.encodeHex(key));
        this.contentHmac = attachment.getHmac();
        this.fileId = attachment.getFileId();
        switchState(State.NEW, "new attachment");
    }

    @Override
    public void start(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.DOWNLOADING, "starting");
    }

    @Override
    public void pause(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.PAUSED, "pausing");
    }

    @Override
    public void cancel(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.PAUSED, "cancelling");
    }

    @Override
    public void hold(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.ON_HOLD, "put on hold");
    }

    private void switchState(State newState, String reason) {
        if (!state.possibleFollowUps().contains(newState)) {
            LOG.warn("State " + newState + " is no possible followup to " + state);
            return;
        }
        LOG.info("switching to state '" + newState + "' - supplied reason: '" + reason + "'");
        setState(newState);

        switch (state) {
            case INITIALIZING:
                switchState(State.NEW, "initializing done");
                break;
            case NEW:
                // DO NOTHING
                break;
            case DOWNLOADING:
                doDownloadingAction();
                break;
            case PAUSED:
                doPausedAction();
                break;
            case RETRYING:
                doRetryingAction();
                break;
            case DECRYPTING:
                doDecryptingAction();
                break;
            case DETECTING:
                doDetectingAction();
                break;
            case COMPLETE:
                doCompleteAction();
                break;
            case FAILED:
                doFailedAction();
                break;
            case ON_HOLD:
                doOnHoldAction();
                break;
        }
    }

	public TalkClientDownload.ApprovalState getApprovalState() {
        if (approvalState == null) {
            return ApprovalState.PENDING;
        } else {
            return approvalState;
        }
    }

    public void setApprovalState(TalkClientDownload.ApprovalState approvalState) {
        this.approvalState = approvalState;
    }


    private void setState(State newState) {
        LOG.info("[download " + clientDownloadId + "] switching to new state '" + newState + "'");
        state = newState;

        saveToDatabase();

        for (int i = 0; i < mTransferListeners.size(); i++) {
            IXoTransferListener listener = mTransferListeners.get(i);
            // TODO: try/catch here? What happens if a listener call produces exception?
            listener.onStateChanged(state);
        }
    }

    private void doOnHoldAction() {
        if (mDownloadRequest != null) {
            mDownloadRequest.abort();
            mDownloadRequest = null;
            LOG.debug("aborted current Download request. Download can still resume.");
        }
        mTransferAgent.deactivateDownload(this);
        mTransferAgent.onDownloadStateChanged(this);
    }

    private void doDownloadingAction() {
        String downloadFilename = computeDownloadFile();
        if (downloadFilename == null) {
            LOG.error("[downloadId: '" + clientDownloadId + "'] could not determine download filename");
            return;
        }

        LOG.debug("performDownloadRequest(downloadId: '" + clientDownloadId + "', filename: '" + downloadFilename + "')");
        HttpClient client = mTransferAgent.getHttpClient();
        RandomAccessFile randomAccessFile = null;
        FileDescriptor fileDescriptor = null;
        try {
            logGetDebug("downloading '" + downloadUrl + "'");
            // create the GET request
            //synchronized (mDownloadRequest) {
            if (mDownloadRequest != null) {
                LOG.warn("Found running mDownloadRequest. Aborting.");
                mDownloadRequest.abort();
            }
            mDownloadRequest = new HttpGet(downloadUrl);

            // determine the requested range
            String range = null;
            if (contentLength != -1) {
                long last = contentLength - 1;
                range = "bytes=" + downloadProgress + "-" + last;
                logGetDebug("requesting range '" + range + "'");
                mDownloadRequest.addHeader("Range", range);
            }
            //}
            mTransferAgent.onDownloadStarted(this);

            // start performing the request
            HttpResponse response = client.execute(mDownloadRequest);
            // process status line
            StatusLine status = response.getStatusLine();
            int sc = status.getStatusCode();
            logGetDebug("got status '" + sc + "': " + status.getReasonPhrase());
            if (sc != HttpStatus.SC_OK && sc != HttpStatus.SC_PARTIAL_CONTENT) {
                LOG.debug("http status is not OK (" + HttpStatus.SC_OK + ") or partial content (" +
                        HttpStatus.SC_PARTIAL_CONTENT + ")");
                checkTransferFailure(transferFailures + 1, "http status is not OK (" + HttpStatus.SC_OK + ") or partial content (" +
                        HttpStatus.SC_PARTIAL_CONTENT + ")");
                return;
            }

            int bytesToGo = getContentLengthFromResponse(response);
            ByteRange contentRange = getContentRange(response);
            setContentTypeByResponse(response);

            int bytesStart = downloadProgress;
            if (!isValidContentRange(contentRange, bytesToGo) || contentLength == -1) {
                LOG.debug("invalid contentRange or content length is -1 - contentLength: '" + contentLength + "'");
                checkTransferFailure(transferFailures + 1, "invalid contentRange or content length is -1 - contentLength: '" + contentLength + "'");
                return;
            }

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            File file = new File(downloadFilename);
            logGetDebug("destination: '" + file + "'");
            file.createNewFile();
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileDescriptor = randomAccessFile.getFD();
            randomAccessFile.setLength(contentLength);
            logGetDebug("will retrieve '" + bytesToGo + "' bytes");
            randomAccessFile.seek(bytesStart);

            if (!copyData(bytesToGo, randomAccessFile, fileDescriptor, inputStream)) {
                checkTransferFailure(transferFailures + 1, "copyData returned null.");
            }

            LOG.debug("doDownloadingAction - ensuring file handles are closed...");
            if (downloadProgress == contentLength) {
                if (decryptionKey != null) {
                    switchState(State.DECRYPTING, "downloading of encrypted file finished");
                } else {
                    dataFile = downloadFilename;
                    switchState(State.DETECTING, "downloading of unencrypted file finished");
                }
            }

        } catch (IOException e) {
            LOG.error("IOException in copyData while reading ", e);
            checkTransferFailure(transferFailures + 1, "download exception!");
        } catch (Exception e) {
            LOG.error("download exception", e);
            checkTransferFailure(transferFailures + 1, "download exception!");
        }
    }

    private void checkTransferFailure(int failures, String failureDescription) {
        transferFailures = failures;
        if (transferFailures <= MAX_FAILURES) {
            switchState(State.RETRYING, "pausing because transfer failures still allow resuming (" + transferFailures + "/" + MAX_FAILURES + " transferFailures), cause: '" + failureDescription + "'");
        } else {
            switchState(State.FAILED, "failing because transfer failures reached max count (" + transferFailures + "/" + MAX_FAILURES + " transferFailures), cause: '" + failureDescription + "'");
        }
    }

    private boolean copyData(int bytesToGo, RandomAccessFile randomAccessFile, FileDescriptor fileDescriptor, InputStream inputStream) throws IOException {
        boolean success = false;
        try {
            success = copyDataImpl(bytesToGo, randomAccessFile, fileDescriptor, inputStream);
        } finally {
            try {
                fileDescriptor.sync();
                randomAccessFile.close();
                inputStream.close();
            } catch (Exception e) {
                LOG.error("IOException in copyDataImpl while closing streams ", e);
            }
        }
        return success;
    }

    private boolean copyDataImpl(int bytesToGo, RandomAccessFile randomAccessFile, FileDescriptor fileDescriptor, InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1 << 12]; // length == 4096 == 2^12
        while (bytesToGo > 0) {
            logGetTrace("bytesToGo: '" + bytesToGo + "'");
            logGetTrace("downloadProgress: '" + downloadProgress + "'");
            // determine how much to copy
            int bytesToRead = Math.min(buffer.length, bytesToGo);
            // perform the copy
            int bytesRead = inputStream.read(buffer, 0, bytesToRead);
            logGetTrace("reading: '" + bytesToRead + "' bytes, returned: '" + bytesRead + "' bytes");
            if (bytesRead == -1) {
                logGetWarning("eof with '" + bytesToGo + "' bytes to go");
                return false;
            }
            randomAccessFile.write(buffer, 0, bytesRead);
            downloadProgress += bytesRead;
            bytesToGo -= bytesRead;
            for (int i = 0; i < mTransferListeners.size(); i++) {
                IXoTransferListener listener = mTransferListeners.get(i);
                // TODO: try/catch here? What happens if a listener call produces exception?
                listener.onProgressUpdated(downloadProgress, getTransferLength());
            }
        }
        return true;
    }

    private boolean isValidContentRange(ByteRange contentRange, int bytesToGo) {
        if (contentRange != null) {
            if (contentRange.getStart() != downloadProgress) {
                logGetError("server returned wrong offset");
                return false;
            }
            if (contentRange.hasEnd()) {
                int rangeSize = (int) (contentRange.getEnd() - contentRange.getStart() + 1);
                if (rangeSize != bytesToGo) {
                    logGetError("server returned range not corresponding to content length");
                    return false;
                }
            }
            if (contentRange.hasTotal()) {
                if (contentLength == -1) {
                    long total = contentRange.getTotal();
                    logGetDebug("inferred content length '" + total + "' from range");
                    contentLength = (int) total;
                }
            }
        }
        return true;
    }

    private void setContentTypeByResponse(HttpResponse response) {
        Header contentTypeHeader = response.getFirstHeader("Content-Type");
        if (contentTypeHeader != null) {
            String contentTypeValue = contentTypeHeader.getValue();
            if (contentType == null) {
                logGetDebug("got content type '" + contentTypeValue + "'");
                contentType = contentTypeValue;
            }
        }
    }

    private ByteRange getContentRange(HttpResponse response) {
        Header contentRangeHeader = response.getFirstHeader("Content-Range");
        if (contentRangeHeader != null) {
            String contentRangeString = contentRangeHeader.getValue();
            logGetDebug("got range '" + contentRangeString + "'");
            return ByteRange.parseContentRange(contentRangeString);
        }
        return null;
    }

    private int getContentLengthFromResponse(HttpResponse response) {
        Header contentLengthHeader = response.getFirstHeader("Content-Length");
        int contentLengthValue = this.contentLength;
        if (contentLengthHeader != null) {
            String contentLengthString = contentLengthHeader.getValue();
            contentLengthValue = Integer.valueOf(contentLengthString);
            logGetDebug("got content length '" + contentLengthValue + "'");
        }
        return contentLengthValue;
    }

    private void doPausedAction() {
        if (mDownloadRequest != null) {
            mDownloadRequest.abort();
            mDownloadRequest = null;
            LOG.debug("aborted current Download request. Download can still resume.");
        }
        mTransferAgent.deactivateDownload(this);
        mTransferAgent.onDownloadStateChanged(this);
    }

    private void doRetryingAction() {
        if (mDownloadRequest != null) {
            mDownloadRequest.abort();
            mDownloadRequest = null;
            LOG.debug("aborted current Download request. Download can still resume.");
        }
        mTransferAgent.deactivateDownload(this);
        mTransferAgent.onDownloadStateChanged(this);
        mTransferAgent.scheduleDownloadAttempt(this);
    }

    private void doDecryptingAction() {
        String sourceFile = computeDownloadFile();
        String destinationFile = computeDecryptionFile();

        LOG.debug("performDecryption(downloadId: '" + clientDownloadId + "', sourceFile: '" + sourceFile + "', " + "destinationFile: '" + destinationFile + "')");

        File source = new File(sourceFile);
        File destination = new File(destinationFile);
        if (destination.exists()) {
            destination.delete();
        }

        try {
            byte[] key = Hex.decodeHex(decryptionKey.toCharArray());
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
            if (this.contentHmac != null) {
                if (this.contentHmac.equals(computedHMac)) {
                    LOG.info("download hmac ok");
                } else {
                    LOG.error("download hmac mismatch, computed hmac: '" + computedHMac + "', should be: '" + this.contentHmac + "'");
                }
            }

            dataFile = destinationFile;
            switchState(State.DETECTING, "decryption finished successfully");
        } catch (Exception e) {
            LOG.error("decryption error", e);
            checkTransferFailure(transferFailures + 1, "failure during decryption");
        }
    }

    private void doDetectingAction() {
        String tempDestinationFilePath;
        String destinationDirectory;
        if (this.decryptedFile != null) {
            tempDestinationFilePath = computeDecryptionFile();
            destinationDirectory = computeDecryptionDirectory();
        } else {
            tempDestinationFilePath = computeDownloadFile();
            destinationDirectory = computeDownloadDirectory();
        }

        LOG.debug("performDetection(downloadId: '" + clientDownloadId + "', destinationFile: '" + tempDestinationFilePath + "')");
        File destination = new File(tempDestinationFilePath);

        try {
            InputStream fileInputStream = new FileInputStream(destination);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            Metadata metadata = new Metadata();
            if (contentType != null && !"application/octet-stream".equals(contentType)) {
                metadata.add(Metadata.CONTENT_TYPE, contentType);
            }
            if (decryptedFile != null) {
                metadata.add(Metadata.RESOURCE_NAME_KEY, decryptedFile);
            }

            MediaType detectedMediaType = MIME_DETECTOR.detect(bufferedInputStream, metadata);

            fileInputStream.close();

            if (detectedMediaType != null) {
                String detectedMediaTypeName = detectedMediaType.toString();
                LOG.info("[downloadId: '" + clientDownloadId + "'] detected mime-type '" + detectedMediaTypeName + "'");
                this.contentType = detectedMediaTypeName;
                MimeType detectedMimeType = MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(detectedMediaTypeName);
                if (detectedMimeType != null) {
                    String extension = detectedMimeType.getExtension();
                    if (extension != null) {
                        LOG.info("[downloadId: '" + clientDownloadId + "'] renaming to extension '" + detectedMimeType.getExtension() + "'");

                        String destinationFileName = createUniqueFileNameInDirectory(this.fileName, extension, destinationDirectory);
                        String destinationPath = destinationDirectory + File.separator + destinationFileName;

                        File newName = new File(destinationPath);
                        if (destination.renameTo(newName)) {
                            this.decryptedFile = destinationFileName;
                            this.fileName = destinationFileName;
                            this.dataFile = computeRelativeDownloadDirectory() + File.separator + destinationFileName;
                        } else {
                            LOG.warn("could not rename file");
                        }
                    }
                }
            }
            switchState(State.COMPLETE, "detection successful");
        } catch (Exception e) {
            LOG.error("detection error", e);
            checkTransferFailure(transferFailures + 1, "detection failed");
        }
    }

    private void doCompleteAction() {
        mTransferAgent.onDownloadFinished(this);
    }

    private void doFailedAction() {
        mTransferAgent.onDownloadFailed(this);
    }

    private String computeDecryptionDirectory() {
        String directory = null;
        switch (this.type) {
            case ATTACHMENT:
                directory = mTransferAgent.getClient().getAttachmentDirectory();
                break;
        }
        return directory;
    }

    private String computeDownloadDirectory() {
        String directory = null;
        switch (this.type) {
            case AVATAR:
                directory = mTransferAgent.getClient().getAvatarDirectory();
                break;
            case ATTACHMENT:
                directory = mTransferAgent.getClient().getEncryptedDownloadDirectory();
                break;
        }
        return directory;
    }

    private String computeRelativeDownloadDirectory() {
        switch (this.type) {
            case ATTACHMENT:
                return mTransferAgent.getClient().getRelativeAttachmentDirectory();
            case AVATAR:
                return mTransferAgent.getClient().getRelativeAvatarDirectory();
        }
        return null;
    }

    private String computeDecryptionFile() {
        String file = null;
        String directory = computeDecryptionDirectory();
        if (directory != null) {
            file = directory + File.separator + this.decryptedFile;
        }
        return file;
    }

    private String computeDownloadFile() {
        String file = null;
        String directory = computeDownloadDirectory();
        if (directory != null) {
            file = directory + File.separator + this.downloadFile;
        }
        return file;
    }

    private void logGetDebug(String message) {
        LOG.debug("[downloadId: '" + clientDownloadId + "'] GET " + message);
    }

    private void logGetTrace(String message) {
        LOG.trace("[downloadId: '" + clientDownloadId + "'] GET " + message);
    }

    private void logGetWarning(String message) {
        LOG.warn("[downloadId: '" + clientDownloadId + "'] GET " + message);
    }

    private void logGetError(String message) {
        LOG.error("[downloadId: '" + clientDownloadId + "'] GET " + message);
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

    private void saveToDatabase() {
        LOG.debug("save TalkClientDownload (" + getClientDownloadId() + ") to database");
        try {
            mTransferAgent.getDatabase().saveClientDownload(this);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    @Override
    public int getTransferId() {
        return getClientDownloadId();
    }

    @Override
    public int getUploadOrDownloadId() {
        return getClientDownloadId();
    }

    @Override
    public Type getTransferType() {
        return type;
    }

    @Override
    public boolean isContentAvailable() {
        return state == State.COMPLETE;
    }

    @Override
    public ContentState getContentState() {
        switch (state) {
            case INITIALIZING:
            case NEW:
                return ContentState.DOWNLOAD_NEW;
            case DOWNLOADING:
            case RETRYING:
                return ContentState.DOWNLOAD_DOWNLOADING;
            case PAUSED:
                return ContentState.DOWNLOAD_PAUSED;
            case DECRYPTING:
                return ContentState.DOWNLOAD_DECRYPTING;
            case DETECTING:
                return ContentState.DOWNLOAD_DETECTING;
            case FAILED:
                return ContentState.DOWNLOAD_FAILED;
            case COMPLETE:
                return ContentState.DOWNLOAD_COMPLETE;
            case ON_HOLD:
                return ContentState.DOWNLOAD_ON_HOLD;
            default:
                throw new RuntimeException("Unknown download state '" + state + "'");
        }
    }

	public enum ApprovalState {
        APPROVED, DECLINED, PENDING
    }

    @Override
    public ContentDisposition getContentDisposition() {
        return ContentDisposition.DOWNLOAD;
    }

    @Override
    public int getTransferLength() {
        return contentLength;
    }

    @Override
    public int getTransferProgress() {
        return downloadProgress;
    }

    @Override
    public String getContentMediaType() {
        return mediaType;
    }

    @Override
    public double getContentAspectRatio() {
        return aspectRatio;
    }

    @Override
    public String getContentUrl() {
        return contentUrl;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getFilePath() {
        return dataFile;
    }

    public int getClientDownloadId() {
        return clientDownloadId;
    }

    public Type getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public String getContentHmac() {
        return contentHmac;
    }

    public void setContentHmac(String hmac) {
        this.contentHmac = hmac;
    }

    public int getTransmittedContentLength() {
        return transmittedContentLength;
    }

    public int getTransferFailures() {
        return transferFailures;
    }

    public boolean isAvatar() {
        return type == Type.AVATAR;
    }

    public boolean isAttachment() {
        return type == Type.ATTACHMENT;
    }

    public void provideContentUrl(XoTransferAgent agent, String url) {
        if (url.startsWith("file://")) {
            return;
        }
        if (url.startsWith("content://media/external/file")) {
            return;
        }
        this.contentUrl = url;
        saveToDatabase();
        agent.onDownloadStateChanged(this);
    }

    @Override
    public void registerTransferListener(IXoTransferListener listener) {
        if (!mTransferListeners.contains(listener)) {
            mTransferListeners.add(listener);
        }
    }

    @Override
    public void unregisterTransferListener(IXoTransferListener listener) {
        if (mTransferListeners.contains(listener)) {
            mTransferListeners.remove(listener);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TalkClientDownload && (clientDownloadId == ((TalkClientDownload)obj).getClientDownloadId())) {
            return true;
        } else {
            return false;
        }
    }

}
