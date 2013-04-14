package com.hoccer.talk.tool;

import com.hoccer.talk.client.ITalkClientDatabase;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;

public class TalkToolClientDatabase implements ITalkClientDatabase {

    @Override
    public TalkClient getClient() {
        return null;
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
