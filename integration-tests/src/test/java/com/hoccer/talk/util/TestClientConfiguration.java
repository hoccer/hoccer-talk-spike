package com.hoccer.talk.util;

import com.hoccer.talk.client.XoDefaultClientConfiguration;

public class TestClientConfiguration extends XoDefaultClientConfiguration {
    private TestTalkServer mServer;

    public TestClientConfiguration(TestTalkServer server) {
        mServer = server;
    }

    @Override
    public String getServerUri() {
        return "ws://127.0.0.1:" + mServer.getServerConnector().getPort();
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
