package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientUpload;

public interface IXoUploadListener {

    public void onUploadCreated(TalkClientUpload download);
    public void onUploadUpdated(TalkClientUpload download);
    public void onUploadDeleted(TalkClientUpload upload);
}
