package com.hoccer.talk.client.model;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.XoTransferAgent;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.util.IProgressListener;
import com.hoccer.talk.util.ProgressOutputHttpEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.CipherInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.UUID;

@DatabaseTable(tableName = "clientUpload")
public class TalkClientUpload extends XoTransfer implements IContentObject {

    private final static Logger LOG = Logger.getLogger(TalkClientUpload.class);

    public enum State {
        NEW, ENCRYPTING, REGISTERING, UPLOADING, PAUSED, COMPLETE, FAILED
    }

    @DatabaseField(generatedId = true)
    private int clientUploadId;

    @DatabaseField
    private Type type;

    @DatabaseField
    private State state;


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


    @DatabaseField(width = 2000)
    private String downloadUrl;


    @DatabaseField(width = 128)
    private String contentType;

    @DatabaseField(width = 128)
    private String mediaType;

    @DatabaseField
    private double aspectRatio;

    @DatabaseField
    private int progress;


    public TalkClientUpload() {
        super(Direction.UPLOAD);
        this.state = State.ENCRYPTING;
        this.aspectRatio = 1.0;
        this.dataLength = -1;
        this.uploadLength = -1;
        this.encryptedLength = -1;
    }

    /* IContentObject */
    @Override
    public boolean isContentAvailable() {
        // uploaded content is always available
        return true;
    }
    @Override
    public ContentState getContentState() {
        switch(this.state) {
            case NEW:
                return ContentState.UPLOAD_NEW;
            case ENCRYPTING:
            case REGISTERING:
            case UPLOADING:
                return ContentState.UPLOAD_IN_PROGRESS;
            case PAUSED:
                return ContentState.UPLOAD_PAUSED;
            case COMPLETE:
                return ContentState.UPLOAD_COMPLETE;
            case FAILED:
                return ContentState.UPLOAD_FAILED;
            default:
                throw new RuntimeException("Unknown upload state");
        }
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
        return dataFile;
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

    public String getDataFile() {
        return dataFile;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

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

    public void provideEncryptionKey(String key) {
        this.encryptionKey = key;
    }

    public void initializeAsAvatar(String url, String contentType, int contentLength) {
        LOG.info("[new] initializing as avatar: " + url);
        this.type = Type.AVATAR;

        this.dataFile = url;
        this.dataLength = contentLength;

        this.contentType = contentType;
        this.mediaType = "image";
    }

    public void initializeAsAttachment(String url, String contentType, String mediaType, double aspectRatio, int contentLength) {
        LOG.info("[new] initializing as attachment: " + url);

        this.type = Type.ATTACHMENT;

        this.dataFile = url;
        this.dataLength = contentLength;

        this.contentType = contentType;
        this.mediaType = mediaType;
        this.aspectRatio = aspectRatio;

        this.encryptedFile = UUID.randomUUID().toString();
    }

    private String computeUploadFile(XoTransferAgent agent) {
        String file = null;
        switch(this.type) {
            case AVATAR:
                file = this.dataFile;
                break;
            case ATTACHMENT:
                file = agent.getClient().getEncryptedUploadDirectory() + File.separator + this.encryptedFile;
                break;
        }
        return file;
    }

    public void performUploadAttempt(XoTransferAgent agent) {
        XoClientDatabase database = agent.getDatabase();

        String uploadFile = computeUploadFile(agent);
        if(uploadFile == null) {
            LOG.error("could not compute upload location for " + clientUploadId);
            return;
        }

        if(state == State.COMPLETE) {
            LOG.warn("Tried to perform completed upload");
            return;
        }

        if(state == State.NEW) {
            if(this.encryptionKey != null) {
                switchState(agent, State.ENCRYPTING);
            } else {
                switchState(agent, State.REGISTERING);
            }
        }

        if(state == State.ENCRYPTING) {
            if(!performEncryption(agent)) {
                markFailed(agent);
            }
        }


        if(state == State.REGISTERING) {
            if(!performRegistration(agent)) {
                markFailed(agent);
            }
        }

        if(state == State.UPLOADING) {
            try {
                performCheckRequest(agent);
                performUploadRequest(agent, uploadFile);
            } catch (IOException e) {
                LOG.error("problem during upload", e);
            }
            saveProgress(agent);
        }

        LOG.info("upload attempt finished in state" + this.state);
    }

    public boolean performRegistration(XoTransferAgent agent) {
        XoClient talkClient = agent.getClient();
        if((this.state == State.ENCRYPTING && this.encryptionKey == null) || state == State.REGISTERING) {
            LOG.info("[" + clientUploadId + "] performing registration");

            if(encryptionKey != null) {
                this.uploadLength = encryptedLength;
            } else {
                this.uploadLength = dataLength;
            }
            try {
                ITalkRpcServer.FileHandles handles;
                if(type == Type.AVATAR) {
                    handles = talkClient.getServerRpc().createFileForStorage(this.uploadLength);
                } else {
                    handles = talkClient.getServerRpc().createFileForTransfer(this.uploadLength);
                }
                uploadUrl = handles.uploadUrl;
                downloadUrl = handles.downloadUrl;
                switchState(agent, State.UPLOADING);
            } catch (Exception e) {
                LOG.error("error registering", e);
                return false;
            }
        }
        return true;
    }

    public boolean performEncryption(XoTransferAgent agent) {
        LOG.info("[" + clientUploadId + "] performing encryption");

        String destinationFile = computeUploadFile(agent);
        if(destinationFile == null) {
            LOG.error("could not determine encryption destination");
            return false;
        }

        File destination = new File(destinationFile);
        if(destination.exists()) {
            destination.delete();
        }

        byte[] key = Hex.decode(encryptionKey);

        try {
            URL url = new URL(this.dataFile);
            URLConnection urlConnection = url.openConnection();

            OutputStream os = new FileOutputStream(destination);
            InputStream is = urlConnection.getInputStream();
            CipherInputStream eis = AESCryptor.encryptingInputStream(is, key, AESCryptor.NULL_SALT);

            byte[] buffer = new byte[1 << 16];
            int bytesRead;
            do {
                bytesRead = eis.read(buffer, 0, buffer.length);
                if(bytesRead > 0) {
                    os.write(buffer, 0, bytesRead);
                }
            } while (bytesRead != -1);

            eis.close();
            is.close();
            os.flush();
            os.close();

            this.encryptedLength = (int)destination.length();

            switchState(agent, State.REGISTERING);
        } catch (Exception e) {
            LOG.error("encryption error", e);
            return false;
        }

        return true;
    }

    private boolean performCheckRequest(XoTransferAgent agent) throws IOException {
        HttpClient client = agent.getHttpClient();

        LOG.info("[" + clientUploadId + "] performing check request");

        int last = uploadLength - 1;
        int confirmedProgress = 0;
        // perform a check request to ensure correct progress
        HttpPut checkRequest = new HttpPut(uploadUrl);
        checkRequest.addHeader("Content-Range", "bytes */" + uploadLength);
        LOG.trace("PUT-check " + uploadUrl + " commencing");
        HttpResponse checkResponse = client.execute(checkRequest);
        StatusLine checkStatus = checkResponse.getStatusLine();
        int checkSc = checkStatus.getStatusCode();
        LOG.trace("PUT-check " + uploadUrl + " status " + checkSc + ": " + checkStatus.getReasonPhrase());
        if(checkSc != HttpStatus.SC_OK && checkSc != 308 /* resume incomplete */) {
            // client error - mark as failed
            if(checkSc >= 400 && checkSc <= 499) {
                markFailed(agent);
            }
            return false;
        }
        // dump headers
        Header[] hdrs = checkResponse.getAllHeaders();
        for(int i = 0; i < hdrs.length; i++) {
            Header h = hdrs[i];
            LOG.trace("PUT-check " + uploadUrl + " header " + h.getName() + ": " + h.getValue());
        }
        // process range header from check request
        Header checkRangeHeader = checkResponse.getFirstHeader("Range");
        if(checkRangeHeader != null) {
            if(checkCompletion(agent, checkRangeHeader)) {
                return true;
            }
        } else {
            LOG.warn("[" + clientUploadId + "] no range header in check response");
        }
        return false;
    }

    private boolean performUploadRequest(final XoTransferAgent agent, String filename) throws IOException {
        HttpClient client = agent.getHttpClient();

        LOG.info("[" + clientUploadId + "] performing upload request");

        int last = uploadLength - 1;

        int bytesToGo = uploadLength - this.progress;
        LOG.trace("PUT-upload " + uploadUrl + " " + bytesToGo + " bytes to go ");

        String uploadRange = "bytes " + this.progress + "-" + last + "/" + uploadLength;
        LOG.trace("PUT-upload " + uploadUrl + " range " + uploadRange);

        File sourceFile = new File(filename);
        InputStream is = new FileInputStream(sourceFile);
        is.skip(this.progress);
        final int startProgress = this.progress;
        IProgressListener progressListener = new IProgressListener() {
            @Override
            public void onProgress(int progress) {
                TalkClientUpload.this.progress = startProgress + progress;
                agent.onUploadProgress(TalkClientUpload.this);
                saveProgress(agent);
            }
        };
        HttpEntity entity = new ProgressOutputHttpEntity(is, bytesToGo, progressListener);
        HttpPut uploadRequest = new HttpPut(uploadUrl);
        uploadRequest.setEntity(entity);
        uploadRequest.addHeader("Content-Range", uploadRange);
        LOG.trace("PUT-upload " + uploadUrl + " commencing");
        HttpResponse uploadResponse = client.execute(uploadRequest);
        this.progress = uploadLength;
        StatusLine uploadStatus = uploadResponse.getStatusLine();
        int uploadSc = uploadStatus.getStatusCode();
        LOG.trace("PUT-upload " + uploadUrl + " status " + uploadSc + ": " + uploadStatus.getReasonPhrase());
        if(uploadSc != HttpStatus.SC_OK && uploadSc != 308 /* resume incomplete */) {
            // client error - mark as failed
            if(uploadSc >= 400 && uploadSc <= 499) {
                markFailed(agent);
            }
            return false;
        }

        // dump headers
        Header[] uploadHdrs = uploadResponse.getAllHeaders();
        for(int i = 0; i < uploadHdrs.length; i++) {
            Header h = uploadHdrs[i];
            LOG.trace("PUT-upload " + uploadUrl + " header " + h.getName() + ": " + h.getValue());
        }
        // process range header from upload request
        Header checkRangeHeader = uploadResponse.getFirstHeader("Range");
        if(checkRangeHeader != null) {
            if(checkCompletion(agent, checkRangeHeader)) {
                return true;
            }
        } else {
            LOG.warn("[" + clientUploadId + "] no range header in upload response");
        }

        return false;
    }

    private boolean checkCompletion(XoTransferAgent agent, Header checkRangeHeader) {
        int last = uploadLength - 1;
        int confirmedProgress = 0;

        ByteRange uploadedRange = ByteRange.parseContentRange(checkRangeHeader.getValue());

        LOG.info("probe returned uploaded range " + uploadedRange.toContentRangeString());

        if(uploadedRange.hasTotal()) {
            if(uploadedRange.getTotal() != uploadLength) {
                LOG.error("server returned wrong upload length");
                markFailed(agent);
                return false;
            }
        }

        if(uploadedRange.hasStart()) {
            if(uploadedRange.getStart() != 0) {
                LOG.error("server returned non-zero start");
                markFailed(agent);
                return false;
            }
        }

        if(uploadedRange.hasEnd()) {
            confirmedProgress = (int)uploadedRange.getEnd() + 1;
        }

        LOG.info("progress believed " + progress + " confirmed " + confirmedProgress);
        this.progress = confirmedProgress;
        agent.onUploadProgress(this);

        if(uploadedRange.hasStart() && uploadedRange.hasEnd()) {
            if(uploadedRange.getStart() == 0 && uploadedRange.getEnd() == last) {
                LOG.info("upload complete");
                switchState(agent, State.COMPLETE);
                return true;
            }
        }

        return false;
    }

    private void markFailed(XoTransferAgent agent) {
        switchState(agent, State.FAILED);
    }

    private void switchState(XoTransferAgent agent, State newState) {
        LOG.info("[" + clientUploadId + "] switching to state " + newState);

        state = newState;

        if(state == State.COMPLETE || state == State.FAILED) {
            if(encryptedFile != null) {
                String path = agent.getClient().getEncryptedUploadDirectory()
                        + File.separator + encryptedFile;
                File file = new File(path);
                file.delete();
            }
        }

        saveProgress(agent);

        agent.onUploadStateChanged(this);
    }

    private void saveProgress(XoTransferAgent agent) {
        try {
            agent.getDatabase().saveClientUpload(this);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

}
