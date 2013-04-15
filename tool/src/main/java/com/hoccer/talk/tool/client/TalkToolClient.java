package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.HoccerTalkClient;
import com.hoccer.talk.tool.TalkToolContext;

public class TalkToolClient {

    TalkToolContext mContext;

    int mId;

    HoccerTalkClient mClient;

    TalkToolClientDatabase mDatabase;

    public TalkToolClient(TalkToolContext context) {
        mContext = context;
        mId = context.generateId();
    }

    public void initialize() {
        mDatabase = new TalkToolClientDatabase();
        mClient = new HoccerTalkClient(mContext.getExecutor(), mDatabase);
    }

    public void start() {
        mClient.activate();
    }

    public void stop() {
        mClient.deactivateNow();
    }

    public void wake() {
        mClient.wake();
    }

    public int getId() {
        return mId;
    }

    public HoccerTalkClient getClient() {
        return mClient;
    }

    public TalkToolClientDatabase getDatabase() {
        return mDatabase;
    }

}
