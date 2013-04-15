package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.ITalkClientDatabase;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;

import java.util.UUID;

public class TalkToolClientDatabase implements ITalkClientDatabase {

    TalkClient mClient;

    @Override
    public TalkClient getClient() {
        if(mClient == null) {
            mClient = new TalkClient(UUID.randomUUID().toString());
        }
        return mClient;
    }

    @Override
    public TalkMessage getMessageByTag(String messageTag) throws Exception {
        return null;
    }

    @Override
    public TalkDelivery[] getDeliveriesByTag(String messageTag) throws Exception {
        return new TalkDelivery[0];
    }

}
