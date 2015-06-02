package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.XoDefaultClientConfiguration;

public class TalkToolClientConfiguration extends XoDefaultClientConfiguration{
    private TalkToolClient mClient;

    public TalkToolClientConfiguration(TalkToolClient client) {
        mClient = client;
    }

    @Override
    public String getServerUri() {
        return mClient.getContext().getApplication().getServer();
    }

    @Override
    public boolean isSupportModeEnabled() {
        return mClient.getSupportMode();
    }

    @Override
    public String getSupportTag() {
        return mClient.getSupportTag();
    }

    @Override
    public long getTimeToLiveInWorldwide() {
        return 0;
    }

    @Override
    public String getNotificationPreferenceForWorldwide() {
        return null;
    }

}
