package com.hoccer.talk.tool;

import com.hoccer.talk.client.HoccerTalkClient;
import com.hoccer.talk.client.ITalkClientDatabase;

public class TalkToolClient {

    TalkToolContext mContext;

    String mName;

    HoccerTalkClient mClient;

    TalkToolClientDatabase mDatabase;

    public TalkToolClient(TalkToolContext context) {
        mContext = context;
    }

    public void initialize() {
        mDatabase = new TalkToolClientDatabase();
        mClient = new HoccerTalkClient(mContext.getExecutor(), mDatabase);
    }

    public String getName() {
        return mName;
    }

    public HoccerTalkClient getClient() {
        return mClient;
    }

    public TalkToolClientDatabase getDatabase() {
        return mDatabase;
    }

}
