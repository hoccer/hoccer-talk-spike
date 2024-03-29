package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientDownload;

public interface IXoDownloadListener {

    public void onDownloadCreated(TalkClientDownload download);
    public void onDownloadUpdated(TalkClientDownload download);
    public void onDownloadDeleted(TalkClientDownload download);
}
