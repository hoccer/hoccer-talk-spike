package com.hoccer.talk.client.model;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.XoTransferAgent;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.crypto.CryptoUtils;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.util.IProgressListener;
import com.hoccer.talk.util.ProgressOutputHttpEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@DatabaseTable(tableName = "clientUpload")
public class TalkClientUpload extends XoTransfer implements IXoTransferObject, IProgressListener {

    private final static Logger LOG = Logger.getLogger(TalkClientUpload.class);

    private HttpPut mUploadRequest;

    private XoTransferAgent mTransferAgent;

    private final List<IXoTransferListener> mTransferListeners = new ArrayList<IXoTransferListener>();

    public enum State implements IXoTransferState {
        NEW {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(REGISTERING);
            }
        },
        REGISTERING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(PAUSED, NEW);
            }
        },
        UPLOADING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(COMPLETE, FAILED, PAUSED);
            }
        },
        PAUSED {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(UPLOADING);
            }
        },
        COMPLETE,
        FAILED;

        public Set<State> possibleFollowUps() {
            return EnumSet.noneOf(State.class);
        }
    }

    @DatabaseField(generatedId = true)
    private int clientUploadId;

    @DatabaseField
    private Type type;

    @DatabaseField
    private State state;

    @DatabaseField
    @Deprecated
    private String contentUrl;

    @DatabaseField
    private String fileName;

    @DatabaseField(width = 2000)
    private String dataFile;

    @DatabaseField(width = 2000)
    private String cachedDataFile;

    @DatabaseField
    @Deprecated
    private long dataLength;

    @DatabaseField(width = 2000)
    private String encryptedFile;

    @DatabaseField
    private long encryptedLength = -1;

    @DatabaseField
    @Nullable
    private String encryptionKey;

    /**
     * Size of upload
     */
    @DatabaseField
    private long uploadLength;

    /**
     * URL for upload
     */
    @DatabaseField(width = 2000)
    private String uploadUrl;

    /**
     * URL for download
     */
    @DatabaseField(width = 2000)
    private String downloadUrl;

    /**
     * Id for file transfer
     */
    @DatabaseField
    private String fileId;

    @DatabaseField(width = 128)
    private String contentType;

    @DatabaseField(width = 128)
    private String mediaType;

    @DatabaseField(width = 128)
    private String contentHmac;

    @DatabaseField
    private double aspectRatio;

    @DatabaseField
    private long progress;

    public TalkClientUpload() {
        super(Direction.UPLOAD);
        this.state = State.NEW;
        this.aspectRatio = 1.0;
        this.uploadLength = -1;
        this.encryptedLength = -1;
    }

    public void initializeAsAvatar(SelectedContent content) {
        content.createContentFile();
        LOG.info("[new] initializing as avatar: '" + content.getFilePath() + "'");
        this.type = Type.AVATAR;
        this.dataFile = content.getFilePath();
        this.contentType = content.getMimeType();
        this.mediaType = content.getMediaType();
        this.aspectRatio = content.getAspectRatio();
    }

    public void initializeAsAttachment(SelectedContent content) {
        content.createContentFile();
        LOG.info("[new] initializing as attachment: '" + content.getFilePath() + "'");
        this.type = Type.ATTACHMENT;
        this.dataFile = content.getFilePath();
        this.fileName = dataFile.substring(dataFile.lastIndexOf(File.separator) + 1);
        this.contentHmac = computeHmac(dataFile);
        this.contentType = content.getMimeType();
        this.mediaType = content.getMediaType();
        this.aspectRatio = content.getAspectRatio();
    }

    @Override
    public void start(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.UPLOADING);
    }

    public void register(XoTransferAgent agent) {
        mTransferAgent = agent;
        switchState(State.REGISTERING);
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

    @Override
    public void hold(XoTransferAgent agent) {
    }

    private void switchState(State newState) {
        if (!state.possibleFollowUps().contains(newState)) {
            LOG.warn("State " + newState + " is no possible followup to " + state);
            return;
        }
        setState(newState);
        switch (state) {
            case NEW:
                switchState(State.REGISTERING);
                break;
            case REGISTERING:
                doRegisteringAction();
                break;
            case PAUSED:
                doPausedAction();
                break;
            case UPLOADING:
                doResumeCheckAction();
                doUploadingAction();
                break;
            case COMPLETE:
                doCompleteAction();
                break;
            case FAILED:
                doFailedAction();
                break;
        }
    }

    private void setState(State newState) {
        LOG.info("[upload " + clientUploadId + "] switching to state " + newState);
        state = newState;

        saveToDatabase();

        LOG.debug("notify all listeners about state change");
        for (IXoTransferListener listener : mTransferListeners) {
            listener.onStateChanged(state);
        }
    }

    private void doRegisteringAction() {
        LOG.info("performRegistration(), state: ‘" + state + "'");
        if (fileId != null) {
            LOG.debug("we already have a fileId. no need to register.");
            switchState(State.PAUSED);
            return;
        }

        XoClient talkClient = mTransferAgent.getClient();
        LOG.info("[uploadId: '" + clientUploadId + "'] performing registration");
        try {
            ITalkRpcServer.FileHandles handles;
            if (isAvatar()) {
                uploadLength = getContentLength();
                handles = talkClient.getServerRpc().createFileForStorage((int) uploadLength);
            } else {
                encryptedLength = AESCryptor.calcEncryptedSize((int) getContentLength(), AESCryptor.NULL_SALT, AESCryptor.NULL_SALT);
                uploadLength = encryptedLength;
                handles = talkClient.getServerRpc().createFileForTransfer((int) encryptedLength);
            }
            fileId = handles.fileId;
            uploadUrl = handles.uploadUrl;
            downloadUrl = handles.downloadUrl;
            LOG.info("[uploadId: '" + clientUploadId + "'] registered as fileId: '" + handles.fileId + "'");
            switchState(State.PAUSED);
        } catch (Exception e) {
            LOG.error("error registering", e);
            switchState(State.NEW);
        }
    }

    private void doResumeCheckAction() {
        HttpClient client = mTransferAgent.getHttpClient();

        LOG.info("[uploadId: '" + clientUploadId + "'] performing check request");

        HttpPut checkRequest = new HttpPut(uploadUrl);
        String contentRangeValue = "bytes */" + uploadLength;
        LOG.trace("PUT-check range '" + contentRangeValue + "'");
        LOG.trace("PUT-check '" + uploadUrl + "' commencing");
        logRequestHeaders(checkRequest, "PUT-check request header ");

        try {
            HttpResponse checkResponse = client.execute(checkRequest);
            StatusLine statusLine = checkResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            LOG.trace("PUT-check '" + uploadUrl + "' with status '" + statusCode + "': " + statusLine.getReasonPhrase());
            if (statusCode != HttpStatus.SC_OK && statusCode != 308 /* resume incomplete */) {
                if (statusCode >= 400 && statusCode <= 499) {
                    LOG.warn("[uploadId: '" + clientUploadId + "'] Check request received HTTP error: " + statusCode);
                }
                checkResponse.getEntity().consumeContent();

            }
            logRequestHeaders(checkResponse, "PUT-check response header ");

            // process range header from check request
            Header checkRangeHeader = checkResponse.getFirstHeader("Range");
            if (checkRangeHeader != null) {
                if (!checkCompletion(mTransferAgent, checkRangeHeader)) {
                    // TODO: pause ? same as below ?
                }
            } else {
                LOG.warn("[uploadId: '" + clientUploadId + "'] no range header in check response");
                progress = 0;
            }
            checkResponse.getEntity().consumeContent();

        } catch (IOException e) {
            LOG.error("IOException while retrieving uploaded range from server ", e);
        }
    }

    private boolean checkCompletion(XoTransferAgent agent, Header checkRangeHeader) {
        long last = uploadLength - 1;
        int confirmedProgress = 0;

        ByteRange uploadedRange = ByteRange.parseContentRange(checkRangeHeader.getValue());

        LOG.info("probe returned uploaded range '" + uploadedRange.toContentRangeString() + "'");

        if (uploadedRange.hasTotal()) {
            if (uploadedRange.getTotal() != uploadLength) {
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

        LOG.info("progress believed " + progress + " confirmed " + confirmedProgress);
        progress = confirmedProgress;
        agent.onUploadProgress(this);

        if (uploadedRange.hasStart() && uploadedRange.hasEnd()) {
            if (uploadedRange.getStart() == 0 && uploadedRange.getEnd() == last) {
                LOG.info("upload complete");
                return true;
            }
        }

        return false;
    }

    private void doUploadingAction() {
        LOG.info("[uploadId: '" + clientUploadId + "'] performing upload request");
        long bytesToGo = uploadLength - progress;
        LOG.debug("'[uploadId: '" + clientUploadId + "'] bytes to go " + bytesToGo);

        if (bytesToGo == 0) {
            LOG.debug("'[uploadId: '" + clientUploadId + "'] bytes to go is 0.");
            switchState(State.COMPLETE);
        }
        LOG.debug("'[uploadId: '" + clientUploadId + "'] current progress: " + progress + " | current upload length: " + uploadLength);

        HttpClient client = mTransferAgent.getHttpClient();
        try {
            InputStream clearIs = new FileInputStream(cachedDataFile != null ? cachedDataFile : dataFile);
            InputStream encryptingInputStream;
            if (isAttachment()) {
                byte[] key = Hex.decode(encryptionKey);
                encryptingInputStream = AESCryptor.encryptingInputStream(clearIs, key, AESCryptor.NULL_SALT);
            } else {
                encryptingInputStream = clearIs;
            }

            int skipped = (int) encryptingInputStream.skip(progress);
            LOG.debug("'[uploadId: '" + clientUploadId + "'] skipped " + skipped + " bytes");
            mUploadRequest = createHttpUploadRequest();
            mUploadRequest.setEntity(new ProgressOutputHttpEntity(encryptingInputStream, bytesToGo, this, progress));

            LOG.trace("PUT-upload '" + uploadUrl + "' commencing");
            logRequestHeaders(mUploadRequest, "PUT-upload response header ");
            mTransferAgent.onUploadStarted(this);
            HttpResponse uploadResponse = client.execute(mUploadRequest);

            saveToDatabase();
            StatusLine uploadStatus = uploadResponse.getStatusLine();
            int uploadStatusCode = uploadStatus.getStatusCode();
            LOG.trace("PUT-upload '" + uploadUrl + "' with status '" + uploadStatusCode + "': " + uploadStatus.getReasonPhrase());
            if (uploadStatusCode != HttpStatus.SC_OK && uploadStatusCode != 308 /* resume incomplete */) {
                // client error - mark as failed
                if (uploadStatusCode >= 400 && uploadStatusCode <= 499) {
                    LOG.warn("[uploadId: '" + clientUploadId + "'] Upload request received HTTP error: " + uploadStatusCode);
                    switchState(State.PAUSED);
                    return;
                }
                uploadResponse.getEntity().consumeContent();
                LOG.error("[uploadId: '" + clientUploadId + "'] Received error from server. Status code: " + uploadStatusCode);
                switchState(State.PAUSED); // do we want to restart this task anytime again?
                return;
            }
            logRequestHeaders(uploadResponse, "PUT-upload response header ");
            // process range header from upload request
            Header checkRangeHeader = uploadResponse.getFirstHeader("Range");
            uploadResponse.getEntity().consumeContent();
            if (isUploadComplete(checkRangeHeader)) {
                dataFile = computeRelativeUploadFilePath(dataFile);
                switchState(State.COMPLETE);
            } else {
                LOG.warn("[uploadId: '" + clientUploadId + "'] no range header in upload response");
                switchState(State.PAUSED);
            }
        } catch (IOException e) {
            LOG.error("IOException while performing upload request: ", e);
            switchState(State.PAUSED);
        } catch (Exception e) {
            LOG.error("Exception while performing upload request: ", e);
            switchState(State.PAUSED);
        }
    }

    private String computeRelativeUploadFilePath(String filePath) {
        String externalStorageDirectory = mTransferAgent.getClient().getExternalStorageDirectory();
        if (filePath.startsWith(externalStorageDirectory)) {
            return filePath.substring(externalStorageDirectory.length() + 1);
        } else {
            return filePath;
        }
    }

    private void doPausedAction() {
        if (mUploadRequest != null) {
            mUploadRequest.abort();
            mUploadRequest = null;
            LOG.debug("aborted current Upload request. Upload can still resume.");
        }
        mTransferAgent.onUploadStateChanged(this);
    }

    private void doCompleteAction() {
        deleteTemporaryFile();
        mTransferAgent.onUploadFinished(this);
    }

    private void doFailedAction() {
        deleteTemporaryFile();
        mTransferAgent.onUploadFailed(this);
    }

    private boolean isUploadComplete(Header checkRangeHeader) {
        if (checkRangeHeader == null) {
            return false;
        }
        long last = uploadLength - 1;
        int confirmedProgress = 0;

        ByteRange uploadedRange = ByteRange.parseContentRange(checkRangeHeader.getValue());
        LOG.info("probe returned uploaded range '" + uploadedRange.toContentRangeString() + "'");

        if (uploadedRange.hasTotal()) {
            if (uploadedRange.getTotal() != uploadLength) {
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

        LOG.info("progress believed " + progress + " confirmed " + confirmedProgress);
        if (uploadedRange.hasStart() && uploadedRange.hasEnd()) {
            if (uploadedRange.getStart() == 0 && uploadedRange.getEnd() == last) {
                LOG.info("upload complete");
                return true;
            }
        }

        return false;
    }

    private void saveToDatabase() {
        try {
            LOG.debug("save TalkClientUpload (" + clientUploadId + ") to database");
            mTransferAgent.getDatabase().saveClientUpload(this);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    private static void logRequestHeaders(HttpMessage httpMessage, String logPrefix) {
        Header[] allHeaders = httpMessage.getAllHeaders();
        for (Header header : allHeaders) {
            LOG.trace(logPrefix + header.getName() + ": " + header.getValue());
        }
    }

    private void deleteTemporaryFile() {
        if (encryptedFile != null) {
            String path = mTransferAgent.getClient().getEncryptedUploadDirectory() + File.separator + encryptedFile;
            File file = new File(path);
            file.delete();
        }
    }

    private HttpPut createHttpUploadRequest() {
        long last = uploadLength - 1;
        long bytesToGo = uploadLength - progress;

        LOG.trace("PUT-upload '" + uploadUrl + "' '" + bytesToGo + "' bytes to go ");
        String uploadRange = "bytes " + progress + "-" + last + "/" + uploadLength;
        LOG.debug("PUT-upload '" + uploadUrl + "' with range '" + uploadRange + "'");

        HttpPut uploadRequest = new HttpPut(uploadUrl);
        if (progress > 0) {
            uploadRequest.addHeader("Content-Range", uploadRange);
        }
        return uploadRequest;
    }

    @Override
    public void onProgress(int progress) {
        LOG.trace("upload (" + clientUploadId + ") progress: " + progress + " of " + uploadLength);

        if (progress > uploadLength) {
            LOG.error("upload (" + clientUploadId + ") progress greater than upload size: " + progress + " > " + uploadLength);
        }

        this.progress = progress;
        for (IXoTransferListener listener : mTransferListeners) {
            listener.onProgressUpdated(progress, uploadLength);
        }
    }

    @Override
    public int getTransferId() {
        return -1 * clientUploadId;
    }

    @Override
    public int getUploadOrDownloadId() {
        return clientUploadId;
    }

    @Override
    public Type getTransferType() {
        return type;
    }

    @Override
    public boolean isContentAvailable() {
        return true;
    }

    @Override
    public ContentState getContentState() {
        switch (state) {
            case NEW:
                return ContentState.UPLOAD_NEW;
            case COMPLETE:
                return ContentState.UPLOAD_COMPLETE;
            case FAILED:
                return ContentState.UPLOAD_FAILED;
            case REGISTERING:
                return ContentState.UPLOAD_REGISTERING;
            case UPLOADING:
                return ContentState.UPLOAD_UPLOADING;
            case PAUSED:
                return ContentState.UPLOAD_PAUSED;
            default:
                throw new IllegalArgumentException("Unknown upload state '" + state + "'");
        }
    }

    @Override
    public long getTransferLength() {
        return encryptedLength;
    }

    @Override
    public long getTransferProgress() {
        return progress;
    }

    @Override
    public double getContentAspectRatio() {
        return aspectRatio;
    }

    @Override
    public String getFilePath() {
        return dataFile;
    }

    public String getCachedFilePath() {
        return cachedDataFile;
    }

    public void setCachedFilePath(String cachedFilePath) {
        cachedDataFile = cachedFilePath;
    }

    @Override
    public long getContentLength() {
        return new File(cachedDataFile != null ? cachedDataFile : dataFile).length();
    }


    public int getClientUploadId() {
        return clientUploadId;
    }

    public State getState() {
        return state;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public String getMimeType() {
        return contentType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public long getProgress() {
        return progress;
    }

    @Override
    public String getContentHmac() {
        return contentHmac;
    }

    public boolean isAvatar() {
        return type == Type.AVATAR;
    }

    public boolean isAttachment() {
        return type == Type.ATTACHMENT;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void provideEncryptionKey(String key) {
        encryptionKey = key;
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

    public boolean isEncryptionKeySet() {
        return encryptionKey != null;
    }

    private static String computeHmac(String filePath) {
        try {
            return CryptoUtils.computeHmac(filePath);
        } catch (Exception e) {
            LOG.error("Error computing HMAC", e);
        }

        return null;
    }
}
