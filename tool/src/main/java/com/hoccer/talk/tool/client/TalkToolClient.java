package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.sql.SQLException;

public class TalkToolClient {

    TalkToolContext mContext;

    int mId;

    XoClient mClient;

    TalkToolClientHost mHost;

    TalkToolClientDatabase mDatabaseBackend;

    private Boolean mSupportModeEnabled = false;
    private String mSupportTag = "log";

    public TalkToolClient(TalkToolContext context) {
        mContext = context;
        mId = context.generateId();
    }

    public void initialize() throws SQLException {
        mDatabaseBackend = new TalkToolClientDatabase(this);
        mDatabaseBackend.initializeDb();
        mHost = new TalkToolClientHost(this);
        mClient = new XoClient(mHost);
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

    public TalkToolContext getContext() {
        return mContext;
    }

    public XoClient getClient() {
        return mClient;
    }

    public TalkToolClientDatabase getDatabaseBackend() {
        return mDatabaseBackend;
    }

    public XoClientDatabase getDatabase() {
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

    public Boolean getSupportMode() {
        return mSupportModeEnabled;
    }

    public String getSupportTag() {
        return mSupportTag;
    }

    public void updateSupportMode(Boolean modeEnabled, String tag) {
        if (modeEnabled != null) {
            mSupportModeEnabled = modeEnabled;
        }
        if (tag != null) {
            mSupportTag = tag;
        }
        if (mClient.isAwake()) {
            mClient.hello();
        }
    }
}
