package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientUpload;

public interface IXoUploadListener {

    public void onUploadCreated(TalkClientUpload upload);
    public void onUploadUpdated(TalkClientUpload upload);
    public void onUploadDeleted(TalkClientUpload upload);
}
