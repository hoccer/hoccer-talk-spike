package com.hoccer.talk.client;

public abstract class XoDefaultClientConfiguration implements IXoClientConfiguration {
    @Override
    public String getUrlScheme() {
        return "hxo://";
    }

    @Override
    public int getRSAKeysize() {
        return 1024;
    }

    @Override
    public boolean isSupportModeEnabled() {
        return false;
    }

    @Override
    public String getSupportTag() {
        return null;
    }

    @Override
    public boolean getUseBsonProtocol() {
        return true;
    }

    @Override
    public String getBsonProtocolString() {
        return "com.hoccer.talk.v4.bson";
    }

    @Override
    public String getJsonProtocolString() {
        return "com.hoccer.talk.v4";
    }

    @Override
    public int getTransferThreads() {
        return 2;
    }

    @Override
    public int getConnectTimeout() {
        return 15;
    }

    @Override
    public int getIdleTimeout() {
        return 120;
    }

    @Override
    public boolean getKeepAliveEnabled() {
        return false;
    }

    @Override
    public int getKeepAliveInterval() {
        return 120;
    }

    @Override
    public int getConnectionIdleTimeout() {
        return 900000;
    }

    @Override
    public float getReconnectBackoffFixedDelay() {
        return 3;
    }

    @Override
    public float getReconnectBackoffVariableFactor() {
        return 1;
    }

    @Override
    public float getReconnectBackoffVariableMaximum() {
        return 120;
    }

    @Override
    public boolean isSendDeliveryConfirmationEnabled() {
        return true;
    }
}