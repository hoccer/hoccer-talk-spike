package com.hoccer.talk.client;

import com.hoccer.talk.client.model.IXoTransferState;
import com.hoccer.talk.util.IProgressListener;

public interface IXoTransferListener extends IProgressListener {

    public void onStateChanged(IXoTransferState state);

    public void onProgressUpdated(int progress, int contentLength);
}
