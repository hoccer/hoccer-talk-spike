package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientDownload;

public interface IXoDownloadListener {

    public void onDownloadAdded(TalkClientDownload download);

    public void onDownloadRemoved(TalkClientDownload download);
}
