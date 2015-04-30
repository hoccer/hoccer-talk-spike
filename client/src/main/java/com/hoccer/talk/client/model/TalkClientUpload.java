package com.hoccer.talk.client.model;

import com.hoccer.talk.client.TransferStateListener;
import com.hoccer.talk.client.UploadAction;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.crypto.CryptoUtils;
import com.hoccer.talk.util.IProgressListener;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@DatabaseTable(tableName = "clientUpload")
public class TalkClientUpload extends XoTransfer implements IProgressListener {

    private final static Logger LOG = Logger.getLogger(TalkClientUpload.class);

    private final List<TransferStateListener> mTransferListeners = new ArrayList<TransferStateListener>();

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
                return EnumSet.of(PAUSED, NEW, UPLOADING);
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
    private String tempCompressedDataFile;

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
     * URL for startDownload
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

    public void switchState(State newState) {
        if (!state.possibleFollowUps().contains(newState)) {
            LOG.warn("State " + newState + " is no possible followup to " + state);
            return;
        }
        setState(newState);
    }

    private void setState(State state) {
        this.state = state;

        if (mTransferListeners.get(0) instanceof UploadAction) {
            TransferStateListener uploadAction = mTransferListeners.remove(0);
            mTransferListeners.add(uploadAction);
        }

        for (final TransferStateListener listener : mTransferListeners) {
            listener.onStateChanged(TalkClientUpload.this);
        }
    }

    @Override
    public void onProgress(int progress) {
        LOG.trace("upload (" + clientUploadId + ") progress: " + progress + " of " + uploadLength);

        if (progress > uploadLength) {
            LOG.error("upload (" + clientUploadId + ") progress greater than upload size: " + progress + " > " + uploadLength);
        }

        this.progress = progress;
        for (TransferStateListener listener : mTransferListeners) {
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

    public void setTransferLength(int transferLength) {
        this.encryptedLength = transferLength;
    }

    public long getUploadLength() {
        return uploadLength;
    }

    public void setUploadLength(long uploadLength) {
        this.uploadLength = uploadLength;
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

    public void setFilePath(String filePath) {
        this.dataFile = filePath;
    }

    public String getTempCompressedFilePath() {
        return tempCompressedDataFile;
    }

    public void setTempCompressedFilePath(String filePath) {
        tempCompressedDataFile = filePath;
    }

    @Override
    public long getContentLength() {
        return new File(tempCompressedDataFile != null ? tempCompressedDataFile : dataFile).length();
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
    public String getFilename() {
        return fileName;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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

    public void setProgress(long progress) {
        this.progress = progress;
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

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setEncryptionKey(String key) {
        encryptionKey = key;
    }

    @Nullable
    public String getEncryptionKey() {
        return encryptionKey;
    }

    public String getEncryptedFile() {
        return encryptedFile;
    }

    public void setEncryptedFile(String encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    @Override
    public void registerTransferListener(TransferStateListener listener) {
        if (!mTransferListeners.contains(listener)) {
            mTransferListeners.add(listener);
        }
    }

    @Override
    public void unregisterTransferListener(TransferStateListener listener) {
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
