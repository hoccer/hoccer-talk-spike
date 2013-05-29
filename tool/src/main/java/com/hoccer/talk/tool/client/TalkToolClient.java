package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.HoccerTalkClient;
import com.hoccer.talk.client.TalkClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;

import java.sql.SQLException;

public class TalkToolClient {

    TalkToolContext mContext;

    int mId;

    HoccerTalkClient mClient;

    TalkToolClientDatabase mDatabaseBackend;

    public TalkToolClient(TalkToolContext context) {
        mContext = context;
        mId = context.generateId();
    }

    public void initialize() {
        mDatabaseBackend = new TalkToolClientDatabase(this);
        mClient = new HoccerTalkClient(mContext.getExecutor(), mDatabaseBackend);
    }

    public void start() {
        mClient.activate();
    }

    public void stop() {
        mClient.deactivate();
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

    public TalkToolClientDatabase getDatabaseBackend() {
        return mDatabaseBackend;
    }

    public TalkClientDatabase getDatabase() {
        return mClient.getDatabase();
    }

    public String getClientId() {
        try {
            TalkClientContact self = mClient.getDatabase().findSelfContact(true);
            return self.getClientId();
        } catch (SQLException e) {
        }
        return null;
    }

}
