package com.hoccer.talk.client;

public interface IXoClientConfiguration {
    public String getServerUri();
    public String getUrlScheme();

    public int getRSAKeysize();

    public boolean isSupportModeEnabled();
    public String getSupportTag();

    public boolean getUseBsonProtocol();
    public String getBsonProtocolString();
    public String getJsonProtocolString();
    public int getTransferThreads();
    public int getConnectTimeout();
    public int getIdleTimeout();

    public boolean getKeepAliveEnabled();
    public int getKeepAliveInterval();

    public float getReconnectBackoffFixedDelay();
    public float getReconnectBackoffVariableFactor();
    public float getReconnectBackoffVariableMaximum();

    public boolean isSendDeliveryConfirmationEnabled();
}
