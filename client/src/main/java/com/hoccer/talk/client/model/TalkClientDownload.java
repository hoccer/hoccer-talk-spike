package com.hoccer.talk.client.model;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.client.TransferStateListener;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.model.TalkAttachment;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import java.util.*;

@DatabaseTable(tableName = "clientDownload")
public class TalkClientDownload extends XoTransfer {

    private final static Logger LOG = Logger.getLogger(TalkClientDownload.class);

    private List<TransferStateListener> mTransferListeners = new ArrayList<TransferStateListener>();

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
    @Deprecated
    private String contentUrl;

    @DatabaseField
    private long contentLength;

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

    /**
     * Only for display purposes, the real content length will be retrieved from server since after encryption this value will differ
     */
    @DatabaseField
    private long transmittedContentLength = -1;

    public TalkClientDownload() {
        super(Direction.DOWNLOAD);
        this.state = State.INITIALIZING;
        this.approvalState = ApprovalState.PENDING;
        this.aspectRatio = 1.0;
        this.downloadProgress = 0;
        this.contentLength = -1;
        mTransferListeners = new ArrayList<TransferStateListener>();
    }

    public void initializeAsAvatar(String url, String id, Date timestamp) {
        LOG.info("[new] initializeAsAvatar(url: '" + url + "')");
        this.type = Type.AVATAR;
        this.downloadUrl = url;
        this.downloadFile = id + "-" + timestamp.getTime();
        this.fileName = this.downloadFile;
        switchState(State.NEW);
    }

    public void initializeAsAttachment(TalkAttachment attachment, String id, byte[] key) {
        LOG.info("[new] initializeAsAttachment(url: '" + attachment.getUrl() + "')");
        this.type = Type.ATTACHMENT;
        this.contentType = attachment.getMimeType();
        this.mediaType = attachment.getMediaType();
        this.aspectRatio = attachment.getAspectRatio();
        this.transmittedContentLength = Long.parseLong(attachment.getContentSize());
        this.downloadUrl = attachment.getUrl();
        this.downloadFile = id;
        this.decryptedFile = UUID.randomUUID().toString();
        this.fileName = attachment.getFileName();
        this.decryptionKey = new String(Hex.encodeHex(key));
        this.contentHmac = attachment.getHmac();
        this.fileId = attachment.getFileId();
        switchState(State.NEW);
    }

    public void switchState(State newState) {
        if (!state.possibleFollowUps().contains(newState)) {
            LOG.warn("State " + newState + " is no possible followup to " + state);
            return;
        }
        setState(newState);
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
        this.state = newState;

        for (TransferStateListener listener : mTransferListeners) {
            listener.onStateChanged(this);
        }
    }

    public boolean isValidContentRange(ByteRange contentRange, long bytesToGo) {
        if (contentRange != null) {
            if (contentRange.getStart() != downloadProgress) {
                LOG.error("[downloadId: '" + clientDownloadId + "'] GET " + "server returned wrong offset: contentRange.getStart() [" + contentRange.getStart() + "] != downloadProgress [" + downloadProgress + "]");
                return false;
            }
            if (contentRange.hasEnd()) {
                long rangeSize = (int) (contentRange.getEnd() - contentRange.getStart() + 1);
                if (rangeSize != bytesToGo) {
                    LOG.error("[downloadId: '" + clientDownloadId + "'] GET " + "server returned range not corresponding to content length");
                    return false;
                }
            }
            if (contentRange.hasTotal()) {
                if (contentLength == -1) {
                    long total = contentRange.getTotal();
                    LOG.debug("[downloadId: '" + clientDownloadId + "'] GET " + "inferred content length '" + total + "' from range");
                    contentLength = total;
                }
            }
        }
        return true;
    }

    public String getDownloadFile() {
        return downloadFile;
    }

    @Override
    public int getTransferId() {
        return clientDownloadId;
    }

    @Override
    public int getUploadOrDownloadId() {
        return clientDownloadId;
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
    public long getTransferLength() {
        return contentLength;
    }

    public void setContentLength(Integer contentLength) {
        this.contentLength = contentLength;
    }

    public void setTransferProgress(int transferProgress) {
        downloadProgress = transferProgress;
        for (TransferStateListener listener : mTransferListeners) {
            listener.onProgressUpdated(downloadProgress, contentLength);
        }
    }

    @Override
    public long getTransferProgress() {
        return downloadProgress;
    }

    @Override
    public double getContentAspectRatio() {
        return aspectRatio;
    }

    public void setFilename(String filename) {
        this.fileName = filename;
    }

    @Override
    public String getFilename() {
        return fileName;
    }

    public void setFilePath(String filePath) {
        dataFile = filePath;
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

    public long getContentLength() {
        return contentLength;
    }

    public String getMimeType() {
        return contentType;
    }

    public void setMimeType(String contentType) {
        this.contentType = contentType;
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

    public long getTransmittedContentLength() {
        return transmittedContentLength;
    }

    public void setTransferFailures(int transferFailures) {
        this.transferFailures = transferFailures;
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

    public String getDecryptionKey() {
        return decryptionKey;
    }

    public void setDecryptedFile(String decryptedFile) {
        this.decryptedFile = decryptedFile;
    }

    public String getDecryptedFile() {
        return decryptedFile;
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TalkClientDownload && (clientDownloadId == ((TalkClientDownload) obj).clientDownloadId);
    }
}
