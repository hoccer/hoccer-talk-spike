package com.hoccer.talk.client;

import com.hoccer.talk.util.IProgressListener;

public interface TransferStateListener extends IProgressListener {

    public void onStateChanged(XoTransfer transfer);

    public void onProgressUpdated(long progress, long contentLength);
}
