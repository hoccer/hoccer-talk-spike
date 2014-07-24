package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientDownload;

public interface IXoDownloadListener {

    public void onDownloadSaved(TalkClientDownload download, boolean isCreated);

    public void onDownloadRemoved(TalkClientDownload download);
}
