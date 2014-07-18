package com.hoccer.talk.client.model;

import com.google.appengine.api.blobstore.ByteRange;

import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.XoTransferAgent;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.util.IProgressListener;
import com.hoccer.talk.util.ProgressOutputHttpEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

@DatabaseTable(tableName = "clientUpload")
public class TalkClientUpload extends XoTransfer implements IXoTransferObject, IProgressListener {

    private final static Logger LOG = Logger.getLogger(TalkClientUpload.class);

    private HttpPut mUploadRequest = null;

    private XoTransferAgent mTransferAgent;

    private IXoTransferListener mTransferListener = null;

    public enum State {
        NEW {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(REGISTERING);
            }
        },
        REGISTERING {
            @Override
            public Set<State> possibleFollowUps() {
                return EnumSet.of(UPLOADING, FAILED);
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
                return EnumSet.of(UPLOADING, FAILED);
            }
        },
        COMPLETE, FAILED;

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
    private String contentUrl;

    @DatabaseField
    private String fileName;

    /** Plain data file */
    @DatabaseField(width = 2000)
    private String dataFile;

    /** Plain data size */
    @DatabaseField
    private int dataLength;

    @DatabaseField(width = 2000)
    private String encryptedFile;

    @DatabaseField
    private int encryptedLength = -1;

    @DatabaseField
    private String encryptionKey;

    /** Size of upload */
    @DatabaseField
    private int uploadLength;

    /** URL for upload */
    @DatabaseField(width = 2000)
    private String uploadUrl;

    /** URL for download */
    @DatabaseField(width = 2000)
    private String downloadUrl;

    /** Id for file transfer */
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
    private int progress;

    public TalkClientUpload() {
        super(Direction.UPLOAD);
        this.state = State.NEW;
        this.aspectRatio = 1.0;
        this.dataLength = -1;
        this.uploadLength = -1;
        this.encryptedLength = -1;
    }

    public void initializeAsAvatar(String contentUrl, String url, String contentType, int contentLength) {
        LOG.info("[new] initializing as avatar: " + url + " length " + contentLength);
        this.type = Type.AVATAR;
        this.contentUrl = contentUrl;
        this.dataFile = url;
        this.dataLength = contentLength;
        this.contentType = contentType;
        this.mediaType = "image";
    }

    public void initializeAsAttachment(String fileName, String contentUrl, String url, String contentType, String mediaType, double aspectRatio,
            int contentLength, String hmac) {
        LOG.info("[new] initializing as attachment: " + url + " length " + contentLength);
        this.type = Type.ATTACHMENT;
        this.contentUrl = contentUrl;
        this.fileName = fileName;
        this.dataFile = url;
        this.dataLength = contentLength;
        this.contentHmac = hmac;
        this.contentType = contentType;
        this.mediaType = mediaType;
        this.aspectRatio = aspectRatio;
    }

    @Override
    public void start(XoTransferAgent agent) {
        mTransferAgent = agent;
        if (state == State.NEW) {
            switchState(State.REGISTERING);
        }
        switchState(State.UPLOADING);
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
            case NEW:
                switchState(State.REGISTERING);
                break;
            case REGISTERING:
                doRegisteringAction();
                break;
            case UPLOADING:
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

    private void setState(State newState) {
        LOG.info("[upload " + clientUploadId + "] switching to state " + newState);
        state = newState;
        mTransferListener.onStateChanged(state);
    }

    private void doRegisteringAction() {
        LOG.info("performRegistration(), state: ‘" + state + "'");
        XoClient talkClient = mTransferAgent.getClient();
        if (this.state == State.NEW || state == State.REGISTERING) {
            LOG.info("[uploadId: '" + clientUploadId + "'] performing registration");
            try {
                ITalkRpcServer.FileHandles handles;
                if (isAvatar()) {
                    this.uploadLength = dataLength;
                    handles = talkClient.getServerRpc().createFileForStorage(this.uploadLength);
                } else {
                    this.encryptedLength = AESCryptor.calcEncryptedSize(getContentLength(), AESCryptor.NULL_SALT, AESCryptor.NULL_SALT);
                    this.uploadLength = encryptedLength;
                    handles = talkClient.getServerRpc().createFileForTransfer(this.encryptedLength);
                }
                fileId = handles.fileId;
                uploadUrl = handles.uploadUrl;
                downloadUrl = handles.downloadUrl;
                LOG.info("[uploadId: '" + clientUploadId + "'] registered as fileId: '" + handles.fileId + "'");

                switchState(State.UPLOADING);
            } catch (Exception e) {
                setState(State.NEW);
                LOG.error("error registering", e);
            }
        }
    }

    private void doUploadingAction() {
//        try {
//            if (!performCheckRequest()) {
//                switchState(State.PAUSED);
//                return;
//            }
//        } catch (IOException e) {
//            LOG.error(e);
//            switchState(State.PAUSED);
//            return;
//        }

        String filename = this.dataFile;
        if (filename == null || filename.isEmpty()) {
            LOG.error("filename was empty");
            switchState(State.PAUSED);
            return;
        }

        LOG.info("[uploadId: '" + clientUploadId + "'] performing upload request");
        int bytesToGo = uploadLength - this.progress;
        HttpClient client = createHttpClientAndSetHeaders();
        try {
            InputStream clearIs = mTransferAgent.getClient().getHost().openInputStreamForUrl(filename);
            InputStream is;
            if (isAttachment()) {
                byte[] key = Hex.decode(encryptionKey);
                is = AESCryptor.encryptingInputStream(clearIs, key, AESCryptor.NULL_SALT);
            } else {
                is = clearIs;
            }

            is.skip(this.progress);
            mUploadRequest.setEntity(new ProgressOutputHttpEntity(is, bytesToGo, this));
            LOG.trace("PUT-upload '" + uploadUrl + "' commencing");
            logRequestHeaders(mUploadRequest, "PUT-upload response header ");
            mTransferAgent.onUploadStarted(this);
            HttpResponse uploadResponse = client.execute(mUploadRequest);

            this.progress = uploadLength;
            saveToDatabase();
            StatusLine uploadStatus = uploadResponse.getStatusLine();
            int uploadSc = uploadStatus.getStatusCode();
            LOG.trace("PUT-upload '" + uploadUrl + "' with status '" + uploadSc + "': " + uploadStatus.getReasonPhrase());
            if (uploadSc != HttpStatus.SC_OK && uploadSc != 308 /* resume incomplete */) {
                // client error - mark as failed
                if (uploadSc >= 400 && uploadSc <= 499) {
                    switchState(State.PAUSED);
                }
                uploadResponse.getEntity().consumeContent();
                switchState(State.PAUSED); // do we want to restart this task anytime again?
            }
            logRequestHeaders(uploadResponse, "PUT-upload response header ");
            // process range header from upload request
            Header checkRangeHeader = uploadResponse.getFirstHeader("Range");
            uploadResponse.getEntity().consumeContent();
            if (isUploadComplete(checkRangeHeader)) {
                switchState(State.COMPLETE);
            } else {
                LOG.warn("[uploadId: '" + clientUploadId + "'] no range header in upload response");
                switchState(State.PAUSED);
            }
        } catch (IOException e) {
            LOG.error("Connection terminated", e);
            switchState(State.PAUSED);
        } catch (Exception e) {
            LOG.error("Exception while performing upload request: ", e);
            switchState(State.PAUSED);
        }
    }

    private void doPausedAction() {
        if (mUploadRequest != null) {
            mUploadRequest.abort();
            mUploadRequest = null;
            LOG.debug("aborted current Upload request. Upload can still resume.");
        }
        saveToDatabase();
    }

    private void doCompleteAction() {
        deleteTemporaryFile();
        mTransferAgent.onUploadFinished(this);
        saveToDatabase();
    }

    private void doFailedAction() {
        deleteTemporaryFile();
        mTransferAgent.onUploadFailed(this);
        saveToDatabase();
    }

    private boolean performCheckRequest() throws IOException {
        HttpClient client = mTransferAgent.getHttpClient();

        LOG.info("[uploadId: '" + clientUploadId + "'] performing check request");

        int last = uploadLength - 1;
        //int confirmedProgress = 0;
        // perform a check request to ensure correct progress
        HttpPut checkRequest = new HttpPut(uploadUrl);
        String contentRangeValue = "bytes */" + uploadLength;
        LOG.trace("PUT-check range '" + contentRangeValue + "'");
        //checkRequest.addHeader("Content-Range", contentRangeValue);
        //checkRequest.setHeader("Content-Length","0");
        LOG.trace("PUT-check '" + uploadUrl + "' commencing");
        logRequestHeaders(checkRequest, "PUT-check request header ");

        HttpResponse checkResponse = client.execute(checkRequest);
        StatusLine checkStatus = checkResponse.getStatusLine();
        int checkSc = checkStatus.getStatusCode();
        LOG.trace("PUT-check '" + uploadUrl + "' with status '" + checkSc + "': " + checkStatus.getReasonPhrase());
        if (checkSc != HttpStatus.SC_OK && checkSc != 308 /* resume incomplete */) {
            checkResponse.getEntity().consumeContent();
            return false;
        }
        logRequestHeaders(checkResponse, "PUT-check response header ");

        // process range header from check request
        Header checkRangeHeader = checkResponse.getFirstHeader("Range");
        if (checkRangeHeader != null && isUploadComplete(checkRangeHeader)) {
            switchState(State.COMPLETE);
        } else {
            LOG.warn("[uploadId: '" + clientUploadId + "'] no range header in check response");
            this.progress = 0;
        }
        checkResponse.getEntity().consumeContent();
        return true;
    }

    private boolean isUploadComplete(Header checkRangeHeader) {
        if(checkRangeHeader == null) {
            return false;
        }
        int last = uploadLength - 1;
        int confirmedProgress = 0;

        ByteRange uploadedRange = ByteRange.parseContentRange(checkRangeHeader.getValue());
        LOG.info("probe returned uploaded range '" + uploadedRange.toContentRangeString() + "'");

        if (uploadedRange.hasTotal()) {
            if (uploadedRange.getTotal() != uploadLength) {
                LOG.error("server returned wrong upload length");
                switchState(State.FAILED);
                return false;
            }
        }

        if (uploadedRange.hasStart()) {
            if (uploadedRange.getStart() != 0) {
                LOG.error("server returned non-zero start");
                switchState(State.FAILED);
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
            mTransferAgent.getDatabase().saveClientUpload(this);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    private void logRequestHeaders(HttpMessage theMessage, String logPrefix) {
        Header[] hdrs = theMessage.getAllHeaders();
        for (int i = 0; i < hdrs.length; i++) {
            Header h = hdrs[i];
            LOG.trace(logPrefix + h.getName() + ": " + h.getValue());
        }
    }

    private void deleteTemporaryFile() {
        if (encryptedFile != null) {
            String path = mTransferAgent.getClient().getEncryptedUploadDirectory()
                    + File.separator + encryptedFile;
            File file = new File(path);
            file.delete();
        }
    }

    private HttpClient createHttpClientAndSetHeaders() {
        HttpClient client = mTransferAgent.getHttpClient();
        int last = uploadLength - 1;
        int bytesToGo = uploadLength - this.progress;

        LOG.trace("PUT-upload '" + uploadUrl + "' '" + bytesToGo + "' bytes to go ");
        String uploadRange = "bytes " + this.progress + "-" + last + "/" + uploadLength;
        LOG.trace("PUT-upload '" + uploadUrl + "' with range '" + uploadRange + "'");
        mUploadRequest = new HttpPut(uploadUrl);
        if (this.progress > 0) {
            mUploadRequest.addHeader("Content-Range", uploadRange);
        }
        return client;
    }


    /**********************************************************************************************/
    /**********************************************************************************************/
    /***************************** IProgressListener implementation *******************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    @Override
    public void onProgress(int progress) {
        this.progress = progress;
        if(mTransferListener != null) {
            mTransferListener.onProgress(this.progress);
        }
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /********************************* XoTransfer implementation **********************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    @Override
    public Type getTransferType() {
        return type;
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /******************************* IContentObject implementation ********************************/
    /**********************************************************************************************/
    /**********************************************************************************************/
    @Override
    public boolean isContentAvailable() {
        // uploaded content is always available
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
    public ContentDisposition getContentDisposition() {
        return ContentDisposition.UPLOAD;
    }

    @Override
    public int getTransferLength() {
        return encryptedLength;
    }

    @Override
    public int getTransferProgress() {
        return progress;
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
    public String getContentDataUrl() {
        if (dataFile != null) {
            if (dataFile.startsWith("/")) {
                return "file://" + dataFile;
            } else {
                return dataFile;
            }
        }
        return null;
    }

    @Override
    public int getContentLength() {
        return dataLength;
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

    public String getDataFile() {
        return dataFile;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public int getEncryptedLength() {
        return encryptedLength;
    }

    public int getDataLength() {
        return dataLength;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public String getContentHmac() {
        return contentHmac;
    }

    public void setcontentHmac(String hmac) {
        this.contentHmac = hmac;
    }

    public boolean isAvatar() {
        return type == Type.AVATAR;
    }

    public boolean isAttachment() {
        return type == Type.ATTACHMENT;
    }

    /**
     * Only used for migrating existing filecache Uris to new host. Delete this Method once
     * the migration is done!
     */
    @Deprecated
    public void setUploadUrl(String url) {
        this.uploadUrl = url;
    }

    public String getUploadUrl() {
        return this.uploadUrl;
    }

    public void provideEncryptionKey(String key) {
        this.encryptionKey = key;
    }
}