package com.hoccer.talk.client;

import com.hoccer.talk.content.IContentObject;

import java.util.List;

public abstract class XoTransfer<T> implements IContentObject {

    public enum State{};

    protected List<IXoTransferListener> mTransferListeners;

    public enum Direction {
        UPLOAD, DOWNLOAD
    }

    public enum Type {
        AVATAR, ATTACHMENT
    }

    private Direction mDirection;

    protected XoTransfer(Direction direction) {
        mDirection = direction;
    }

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

    public void registerTransferListener(IXoTransferListener listener) {
        if (!mTransferListeners.contains(listener)) {
            mTransferListeners.add(listener);
        }
    }

    public void unregisterTransferListener(IXoTransferListener listener) {
        if (mTransferListeners.contains(listener)) {
            mTransferListeners.remove(listener);
        }
    }

}
