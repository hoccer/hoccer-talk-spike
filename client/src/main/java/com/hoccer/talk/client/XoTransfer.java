package com.hoccer.talk.client;

import com.hoccer.talk.content.IContentObject;

public abstract class XoTransfer implements IContentObject {

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

    public abstract void registerTransferListener(IXoTransferListener listener);

    public abstract void unregisterTransferListener(IXoTransferListener listener);
}
