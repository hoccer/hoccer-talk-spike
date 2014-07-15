package com.hoccer.xo.android;

import better.jsonrpc.websocket.JsonRpcWsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;

import org.eclipse.jetty.websocket.WebSocketClient;

import java.net.URI;

/**
 * Created by jacob on 10.02.14.
 */
public class XoAndroidClient extends XoClient {

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoAndroidClient(IXoClientHost host) {
        super(host);
    }

    @Override
    protected void createJsonRpcClient(URI uri, WebSocketClient wsClient, ObjectMapper rpcMapper) {
        String protocol = mClientHost.getUseBsonProtocol()
                ? mClientHost.getBsonProtocolString()
                : mClientHost.getJsonProtocolString();
        mConnection = new JsonRpcWsClient(uri, protocol, wsClient, rpcMapper, mClientHost.getIncomingBackgroundExecutor());
        mConnection.setMaxIdleTime(mClientHost.getConnectionIdleTimeout());
        mConnection.setSendKeepAlives(mClientHost.getKeepAliveEnabled());
        if(mClientHost.getUseBsonProtocol()) {
            mConnection.setSendBinaryMessages(true);
        }
    }

}
