package com.hoccer.talk.client;

import com.hoccer.talk.client.model.TalkClientMessage;

public interface IXoMessageListener {

    void onMessageCreated(TalkClientMessage message);
    void onMessageUpdated(TalkClientMessage message);
    void onMessageDeleted(TalkClientMessage message);

}
