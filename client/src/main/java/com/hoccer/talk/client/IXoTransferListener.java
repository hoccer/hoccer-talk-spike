package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.util.IProgressListener;

public interface IXoTransferListener extends IProgressListener {

    public void onStateChanged(TalkClientUpload.State state);
}
