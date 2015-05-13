package com.hoccer.talk.client;

import com.hoccer.talk.content.ContentState;


public abstract class XoTransfer {

    public enum State {}

    public enum Direction {
        UPLOAD, DOWNLOAD
    }

    public enum Type {
        AVATAR, ATTACHMENT
    }

    private final Direction mDirection;

    protected XoTransfer(Direction direction) {
        mDirection = direction;
    }

    public abstract boolean isContentAvailable();

    public abstract ContentState getContentState();

    public abstract long getTransferLength();

    public abstract long getTransferProgress();

    public abstract String getMediaType();

    public abstract double getContentAspectRatio();

    public abstract String getMimeType();

    public abstract String getFilename();

    public abstract String getFilePath();

    public abstract long getContentLength();

    public abstract String getContentHmac();

    public abstract int getTransferId();

    public abstract int getUploadOrDownloadId();

    public Direction getDirection() {
        return mDirection;
    }

    public boolean isDownload() {
        return mDirection == Direction.DOWNLOAD;
    }

    public boolean isUpload() {
        return mDirection == Direction.UPLOAD;
    }

    public abstract Type getTransferType();

    public boolean isAvatar() {
        return getTransferType() == Type.AVATAR;
    }

    public boolean isAttachment() {
        return getTransferType() == Type.ATTACHMENT;
    }

    public abstract void registerTransferListener(TransferStateListener listener);

    public abstract void unregisterTransferListener(TransferStateListener listener);
}
