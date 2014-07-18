package com.hoccer.talk.client.model;

import com.google.appengine.api.blobstore.ByteRange;

import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.XoClientDatabase;
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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;

@DatabaseTable(tableName = "clientDownload")
public class TalkClientDownload extends XoTransfer implements IXoTransferObject {

    /** Maximum amount of retry attempts when downloading an attachment */
    public static final int MAX_DOWNLOAD_RETRY = 16;

    /**
     * Minimum amount of progress to justify a db update
     *
     * This amount of data can be lost in case of crashes.
     * In all other cases progress will be saved.
     */
    private final static int PROGRESS_SAVE_MINIMUM = 1 << 16;

    private final static Logger LOG = Logger.getLogger(TalkClientDownload.class);

    private static final Detector MIME_DETECTOR = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

    private XoTransferAgent mTransferAgent;

    private IXoTransferListener mTransferListener = null;

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
                return EnumSet.of(DOWNLOADING);
            }
        },
        DOWNLOADING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(PAUSED, DECRYPTING, DETECTING, FAILED);
            }
        },
        PAUSED {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(DOWNLOADING);
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
        COMPLETE, FAILED;

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

    /** URL to download */
    @DatabaseField(width = 2000)
    private String downloadUrl;

    @DatabaseField
    private String fileId;

    /**
     * Name of the file the download itself will go to
     *
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

    private transient long progressRateLimit;

    private Timer mTimer;

    private HttpGet mDownloadRequest = null;

    public TalkClientDownload() {
        super(Direction.DOWNLOAD);
        this.state = State.INITIALIZING;
        this.aspectRatio = 1.0;
        this.downloadProgress = 0;
        this.contentLength = -1;
    }

    /**
     * Initialize this download as an avatar download
     *
     * @param url       to download
     * @param id        for avatar, identifying what the avatar belongs to
     * @param timestamp for avatar, takes care of collisions over id
     */
    public void initializeAsAvatar(String url, String id, Date timestamp) {
        LOG.info("[new] initializeAsAvatar(url: '" + url + "')");
        this.type = Type.AVATAR;
//        url = checkFilecacheUrl(url); // TODO: ToBeDeleted
        this.downloadUrl = url;
        this.downloadFile = id + "-" + timestamp.getTime();
    }

    public void initializeAsAttachment(TalkAttachment attachment, String id, byte[] key) {
        LOG.info("[new] initializeAsAttachment(url: '" + attachment.getUrl() + "')");
        this.type = Type.ATTACHMENT;
        this.contentType = attachment.getMimeType();
        this.mediaType = attachment.getMediaType();
        this.aspectRatio = attachment.getAspectRatio();
//        String filecacheUrl = checkFilecacheUrl(attachment.getUrl()); // TODO: ToBeDeleted
//        attachment.setUrl(filecacheUrl);
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
    }

    @Override
    public void start(XoTransferAgent agent) {
        mTransferAgent = agent;
        if (state == State.INITIALIZING) {
            switchState(State.NEW);
        }
        switchState(State.DOWNLOADING);
    }

    @Override
    public void pause(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.PAUSED);
    }

    @Override
    public void cancel(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.PAUSED);
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /************************************** PRIVATE METHODS ***************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    private void switchState(State newState) {
        if (!state.possibleFollowUps().contains(newState)) {
            LOG.warn("State " + newState.toString() + " is no possible followup to " + state.toString());
            return;
        }
        setState(newState);
        switch (state) {
            case INITIALIZING:
                switchState(State.NEW);
                break;
            case NEW:
                switchState(State.DOWNLOADING);
                break;
            case DOWNLOADING:
                doDownloadingAction();
                break;
            case PAUSED:
                doPausedAction();
                break;
            case DECRYPTING:
                doDecryptingAction();
                case DETECTING:
                doDetectingAction();
            case COMPLETE:
                doCompleteAction();
                break;
            case FAILED:
                doFailedAction();
                break;
        }
    }

    private void setState(State newState) {
        LOG.info("[download " + clientDownloadId + "] switching to state " + newState);
        state = newState;
        mTransferListener.onStateChanged(state);
    }

    private void doDownloadingAction() {
        String downloadFilename = computeDownloadFile(mTransferAgent);
        if (downloadFilename == null) {
            LOG.error("[downloadId: '" + clientDownloadId + "'] could not determine download filename");
            return;
        }

        mTimer = new Timer();
        DownloadTask downloadTask = new DownloadTask(this, mTransferAgent, downloadFilename);
        mTimer.scheduleAtFixedRate(downloadTask, 0, 5 * 1000);
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            LOG.error("Error while performing download attempt: ", e);
        }
    }

    private void doPausedAction() {

    }

    private void doDecryptingAction() {
        String sourceFile = computeDownloadFile(mTransferAgent);
        String destinationFile = computeDecryptionFile(mTransferAgent);

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
            InputStream is = new FileInputStream(source);
            OutputStream ofs = new FileOutputStream(destination);
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            OutputStream os = new DigestOutputStream(ofs, digest);
            OutputStream dos = AESCryptor.decryptingOutputStream(os, key, AESCryptor.NULL_SALT);

            int bytesToGo = bytesToDecrypt;
            while (bytesToGo > 0) {
                int bytesToCopy = Math.min(buffer.length, bytesToGo);
                int bytesRead = is.read(buffer, 0, bytesToCopy);
                dos.write(buffer, 0, bytesRead);
                bytesToGo -= bytesRead;
//                if (!mTransferAgent.isDownloadActive(this)) {
//                    return true;
//                }
            }

            dos.flush();
            dos.close();
            os.flush();
            os.close();
            is.close();

            String computedHMac = new String(Base64.encodeBase64(digest.digest()));
            if (this.contentHmac != null) {
                if (this.contentHmac.equals(computedHMac)) {
                    LOG.info("download hmac ok");
                } else {
                    LOG.error("download hmac mismatch, computed hmac: '" + computedHMac + "', should be: '" + this.contentHmac + "'");
                }
            }

            dataFile = destinationFile;
            switchState(State.DETECTING);
        } catch (Exception e) {
            LOG.error("decryption error", e);
            switchState(State.FAILED);
        }
    }

    private void doDetectingAction() {

        String destinationFilePath;
        if (this.decryptedFile != null) {
            destinationFilePath = computeDecryptionFile(mTransferAgent);
        } else {
            destinationFilePath = computeDownloadFile(mTransferAgent);
        }

        LOG.debug("performDetection(downloadId: '" + clientDownloadId + "', destinationFile: '" + destinationFilePath + "')");
        File destination = new File(destinationFilePath);

        try {
            InputStream tis = new FileInputStream(destination);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(tis);

            Metadata metadata = new Metadata();
            if (contentType != null && !contentType.equals("application/octet-stream")) {
                metadata.add(Metadata.CONTENT_TYPE, contentType);
            }
            if (decryptedFile != null) {
                metadata.add(Metadata.RESOURCE_NAME_KEY, decryptedFile);
            }

            MediaType detectedMediaType = MIME_DETECTOR.detect(bufferedInputStream, metadata);

            tis.close();

            if (detectedMediaType != null) {
                String detectedMediaTypeName = detectedMediaType.toString();
                LOG.info("[downloadId: '" + clientDownloadId + "'] detected mime-type '" + detectedMediaTypeName + "'");
                this.contentType = detectedMediaTypeName;
                MimeType detectedMimeType = MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(detectedMediaTypeName);
                if (detectedMimeType != null) {
                    String extension = detectedMimeType.getExtension();
                    if (extension != null) {
                        LOG.info("[downloadId: '" + clientDownloadId + "'] renaming to extension '" + detectedMimeType.getExtension() + "'");

                        String destinationDirectory = computeDecryptionDirectory(mTransferAgent);
                        String destinationFileName = createUniqueFileNameInDirectory(this.fileName, extension, destinationDirectory);
                        String destinationPath = destinationDirectory + File.separator + destinationFileName;

                        File newName = new File(destinationPath);
                        if (destination.renameTo(newName)) {
                            this.decryptedFile = destinationFileName;
                            this.dataFile = destinationPath;
                        } else {
                            LOG.warn("could not rename file");
                        }
                    }
                }
            }
            synchronized (this) {
                switchState(State.COMPLETE);
            }
        } catch (Exception e) {
            LOG.error("detection error", e);
            switchState(State.FAILED);
        }
    }

    private void doCompleteAction() {
        saveToDatabase();
        mTransferAgent.onDownloadFinished(this);
    }

    private void doFailedAction() {
        saveToDatabase();
        mTransferAgent.onDownloadFailed(this);
    }



    /**********************************************************************************************/
    /***************************************** UGLIES *********************************************/
    /**********************************************************************************************/
    /**********************************************************************************************/

    private String computeDecryptionDirectory(XoTransferAgent agent) {
        String directory = null;
        switch (this.type) {
            case ATTACHMENT:
                directory = agent.getClient().getAttachmentDirectory();
                break;
        }
        return directory;
    }

    private String computeDownloadDirectory(XoTransferAgent agent) {
        String directory = null;
        switch (this.type) {
            case AVATAR:
                directory = agent.getClient().getAvatarDirectory();
                break;
            case ATTACHMENT:
                directory = agent.getClient().getEncryptedDownloadDirectory();
                break;
        }
        return directory;
    }

    private String computeDecryptionFile(XoTransferAgent agent) {
        String file = null;
        String directory = computeDecryptionDirectory(agent);
        if (directory != null) {
            file = directory + File.separator + this.decryptedFile;
        }
        return file;
    }

    private String computeDownloadFile(XoTransferAgent agent) {
        String file = null;
        String directory = computeDownloadDirectory(agent);
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

    private boolean performDownloadRequest(XoTransferAgent agent, String filename) {
        LOG.debug("performDownloadRequest(downloadId: '" + clientDownloadId + "', filename: '" + filename + "')");
        agent.onDownloadStarted(this);
        HttpClient client = agent.getHttpClient();
        XoClientDatabase database = agent.getDatabase();
        RandomAccessFile raf = null;
        FileDescriptor fd = null;
        try {
            logGetDebug("downloading '" + downloadUrl + "'");
            if(mDownloadRequest != null) {
                LOG.info("Download already in progress");
                return false;
            }
            // create the GET request
            mDownloadRequest = new HttpGet(downloadUrl);
            // determine the requested range
            String range = null;
            if (contentLength != -1) {
                long last = contentLength - 1;
                range = "bytes=" + downloadProgress + "-" + last;
                logGetDebug("requesting range '" + range + "'");
                mDownloadRequest.addHeader("Range", range);
            }
            // start performing the request
            HttpResponse response = client.execute(mDownloadRequest);
            // process status line
            StatusLine status = response.getStatusLine();
            int sc = status.getStatusCode();
            logGetDebug("got status '" + sc + "': " + status.getReasonPhrase());
            if (sc != HttpStatus.SC_OK && sc != HttpStatus.SC_PARTIAL_CONTENT) {
                // client error - mark as failed
                if (sc >= 400 && sc <= 499) {
                    switchState(State.FAILED);
                }
                return false;
            }
            // parse content length from response
            Header contentLengthHeader = response.getFirstHeader("Content-Length");
            int contentLengthValue = this.contentLength;
            if (contentLengthHeader != null) {
                String contentLengthString = contentLengthHeader.getValue();
                contentLengthValue = Integer.valueOf(contentLengthString);
                logGetDebug("got content length '" + contentLengthValue + "'");
            }
            // parse content range from response
            ByteRange contentRange = null;
            Header contentRangeHeader = response.getFirstHeader("Content-Range");
            if (contentRangeHeader != null) {
                String contentRangeString = contentRangeHeader.getValue();
                logGetDebug("got range '" + contentRangeString + "'");
                contentRange = ByteRange.parseContentRange(contentRangeString);
            }
            // remember content type if we don't have one yet
            Header contentTypeHeader = response.getFirstHeader("Content-Type");
            if (contentTypeHeader != null) {
                String contentTypeValue = contentTypeHeader.getValue();
                if (contentType == null) {
                    logGetDebug("got content type '" + contentTypeValue + "'");
                    contentType = contentTypeValue;
                }
            }
            // get ourselves a buffer
            byte[] buffer = new byte[1 << 12];
            // determine what to copy
            int bytesStart = downloadProgress;
            int bytesToGo = contentLengthValue;
            if (contentRange != null) {
                if (contentRange.getStart() != downloadProgress) {
                    logGetError("server returned wrong offset");
                    switchState(State.FAILED);
                    return false;
                }
                if (contentRange.hasEnd()) {
                    int rangeSize = (int) (contentRange.getEnd() - contentRange.getStart() + 1);
                    if (rangeSize != bytesToGo) {
                        logGetError("server returned range not corresponding to content length");
                        switchState(State.FAILED);
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
            if (contentLength == -1) {
                logGetError("could not determine content length");
                switchState(State.FAILED);
                return false;
            }
            // handle content
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            // create and open destination file
            File f = new File(filename);
            logGetDebug("destination: '" + f.toString() + "'");
            f.createNewFile();
            raf = new RandomAccessFile(f, "rw");
            fd = raf.getFD();
            // resize the file
            raf.setLength(contentLength);
            // log about what we are to do
            logGetDebug("will retrieve '" + bytesToGo + "' bytes");
            // seek to start of region
            raf.seek(bytesStart);
            // copy data
            int savedProgress = downloadProgress;
            while (bytesToGo > 0) {
                logGetTrace("bytesToGo: '" + bytesToGo + "'");
                logGetTrace("downloadProgress: '" + downloadProgress + "'");
                // determine how much to copy
                int bytesToRead = Math.min(buffer.length, bytesToGo);
                // perform the copy
                int bytesRead = is.read(buffer, 0, bytesToRead);
                logGetTrace("reading: '" + bytesToRead + "' bytes, returned: '" + bytesRead + "' bytes");
                if (bytesRead == -1) {
                    logGetWarning("eof with '" + bytesToGo + "' bytes to go");
                    return false;
                }
                raf.write(buffer, 0, bytesRead);
                //raf.getFD().sync();
                // sync the file
                fd.sync();
                // update state
                downloadProgress += bytesRead;
                bytesToGo -= bytesRead;
                // call listeners
                notifyProgress(agent);
                if (!agent.isDownloadActive(this)) {
                    return true;
                }
            }

            // close streams
            fd = null;
            raf.close();
            is.close();
        } catch (Exception e) {
            LOG.error("download exception", e);

            switchState(State.FAILED);

            return false;
        } finally {
            if (fd != null) {
                try {
                    fd.sync();
                } catch (SyncFailedException e) {
                    LOG.warn("sync failed while handling download exception", e);
                }
            }
            try {
                database.saveClientDownload(this);
            } catch (SQLException e) {
                LOG.warn("save failed while handling download exception", e);
            }

            // update state
            if (downloadProgress == contentLength) {
                if (decryptionKey != null) {
                    switchState(State.DECRYPTING);
                } else {
                    dataFile = filename;
                    switchState(State.DETECTING);
                }
            }
        }

        return true;
    }

    /**
     * Creates a unique file name by checking whether a file already exists in a given directory.
     * In case a file with the same name already exists the given file name will be expanded by an underscore and
     * a running number (foo_1.bar) to prevent the existing file from being overwritten.
     *
     * @param file The given file name
     * @param extension The given file extension
     * @param directory The directory to check
     * @return The file name including running number and extension (foo_1.bar)
     */
    private String createUniqueFileNameInDirectory(String file, String extension, String directory) {
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

    private void notifyProgress(XoTransferAgent agent) {
        agent.onDownloadProgress(this);
    }

    private void saveToDatabase() {
        try {
            mTransferAgent.getDatabase().saveClientDownload(this);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    private class DownloadTask extends TimerTask {

        private final XoTransferAgent mAgent;

        private final String mFilename;

        private final TalkClientDownload mDownload;

        public DownloadTask(TalkClientDownload download, XoTransferAgent agent, String filename) {
            mAgent = agent;
            mFilename = filename;
            mDownload = download;
        }

        @Override
        public void run() {
            LOG.info("[downloadId: '" + clientDownloadId + "'] download attempt " + transferFailures + "/"
                    + MAX_DOWNLOAD_RETRY);
            boolean success = performDownloadRequest(mAgent, mFilename);
            if (success) {
                LOG.info("[downloadId: '" + clientDownloadId + "'] download succeeded");
                synchronized (mDownload) {
                    mDownload.notify();
                }
                this.cancel();
            }
            LOG.info("[downloadId: '" + clientDownloadId + "'] download failed");
            setTransferFailures(transferFailures + 1);
        }
    }

    ///////// getters and setters

    /* XoTransfer implementation */
    @Override
    public Type getTransferType() {
        return type;
    }

    /* IContentObject implementation */
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
            case COMPLETE:
                return ContentState.DOWNLOAD_COMPLETE;
            case FAILED:
                return ContentState.DOWNLOAD_FAILED;
            case DOWNLOADING:
                return ContentState.DOWNLOAD_DOWNLOADING;
            case DECRYPTING:
                return ContentState.DOWNLOAD_DECRYPTING;
            case DETECTING:
                return ContentState.DOWNLOAD_DETECTING;
            case PAUSED:
                return ContentState.DOWNLOAD_PAUSED;
            default:
                throw new RuntimeException("Unknown download state '" + state + "'");
        }
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
    public String getContentDataUrl() {
        // TODO fix up this field on db upgrade
        if (dataFile != null) {
            if (dataFile.startsWith("file://")) {
                return dataFile;
            } else {
                return "file://" + dataFile;
            }
        }
        return null;
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

    /**
     * Only used for migrating existing filecache Uris to new host. Delete this Method once
     * the migration is done!
     *
     * @param downloadUrl
     */
    @Deprecated
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadFile() {
        return downloadFile;
    }

    public long getDownloadProgress() {
        return downloadProgress;
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

    public String getDataFile() {
        // TODO fix up this field on db upgrade
        if (dataFile != null) {
            if (dataFile.startsWith("file://")) {
                return dataFile.substring(7);
            } else {
                return dataFile;
            }
        }
        return null;
    }

    public int getTransferFailures() {
        return transferFailures;
    }

    public void setTransferFailures(int transferFailures) {
        this.transferFailures = transferFailures;
        if(transferFailures > 16) {
            // max retries reached. stop download and reset retries
            LOG.debug("cancel Downloads. No more retries.");
            mTimer.cancel();
            this.transferFailures = 0;
        }
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

    // TODO: DELETE THIS PIECE OF ****
    private String checkFilecacheUrl(String url) {
        String migratedUrl = url.substring(url.indexOf("/", 8));
        migratedUrl = "https://filecache.talk.hoccer.de:8444" + migratedUrl;
        return migratedUrl;
    }

}
