package com.hoccer.xo.android;

import better.jsonrpc.websocket.JsonRpcWsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.IXoClientConfiguration;
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
    public XoAndroidClient(IXoClientHost host, IXoClientConfiguration configuration) {
        super(host, configuration);
    }

    @Override
    protected void createJsonRpcClient(URI uri, WebSocketClient wsClient, ObjectMapper rpcMapper) {
        IXoClientConfiguration configuration = getConfiguration();

        String protocol = configuration.getUseBsonProtocol()
                ? configuration.getBsonProtocolString()
                : configuration.getJsonProtocolString();

        mConnection = new JsonRpcWsClient(uri, protocol, wsClient, rpcMapper, getHost().getIncomingBackgroundExecutor());
        mConnection.setMaxIdleTime(configuration.getConnectionIdleTimeout());
        mConnection.setSendKeepAlives(configuration.getKeepAliveEnabled());

        if(configuration.getUseBsonProtocol()) {
            mConnection.setSendBinaryMessages(true);
        }
    }

}
