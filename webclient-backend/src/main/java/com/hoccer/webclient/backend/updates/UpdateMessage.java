package com.hoccer.webclient.backend.updates;

public class UpdateMessage {
    private String mPath;
    private Object mData;

    public UpdateMessage(String path, Object data) {
        this.mPath = path;
        this.mData = data;
    }

    public String getPath() {
        return mPath;
    }

    public Object getData() {
        return mData;
    }
}
