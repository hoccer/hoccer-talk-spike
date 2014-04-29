package com.hoccer.talk.server.rpc;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.websocket.JsonRpcWsConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.server.TalkServer;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * WebSocket handler
 * <p/>
 * This is responsible for accepting websocket connections,
 * creating and configuring a connection object for them.
 */
public class TalkRpcConnectionHandler extends WebSocketHandler {

    private static final Logger LOG = Logger.getLogger(TalkRpcConnectionHandler.class);
    private static final int MAX_IDLE_TIME = 1800 * 1000; // in ms

    private static final String TALK_TEXT_PROTOCOL_NAME = "com.hoccer.talk.v1";
    private static final String TALK_BINARY_PROTOCOL_NAME = "com.hoccer.talk.v1.bson";

    /**
     * Talk server instance
     */
    private final TalkServer mTalkServer;

    /**
     * JSON-RPC server object
     */
    private final JsonRpcServer mJsonRpcServer;

    /**
     * Construct a handler for the given server
     *
     * @param server to add connections to
     */
    public TalkRpcConnectionHandler(TalkServer server) {
        mJsonRpcServer = server.getRpcServer();
        mTalkServer = server;
    }

    /**
     * Create a websocket for the given HTTP request and WS protocol
     *
     * @param request which initiated websocket upgrade
     * @param protocol the user defined websocket protocol (can be null!)
     * @return
     */
    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        if (protocol == null) {
            LOG.info("protocol is null - aborting connection attempt");
            return null;
        }

        if (protocol.equals(TALK_TEXT_PROTOCOL_NAME)) {
            return createTalkV1Connection(request, mTalkServer.getJsonMapper(), false);
        } else if (protocol.equals(TALK_BINARY_PROTOCOL_NAME)) {
            return createTalkV1Connection(request, mTalkServer.getBsonMapper(), true);
        }

        LOG.info("new connection with unknown protocol '" + protocol + "'");
        return null;
    }

    private WebSocket createTalkV1Connection(HttpServletRequest request, ObjectMapper mapper, boolean binary) {
        // create JSON-RPC connection (this implements the websocket interface)
        JsonRpcWsConnection connection = new JsonRpcWsConnection(mapper);
        // create talk high-level connection object
        TalkRpcConnection rpcConnection = new TalkRpcConnection(mTalkServer, connection, request);
        // configure the connection
        connection.setSendBinaryMessages(binary);
        connection.setMaxIdleTime(MAX_IDLE_TIME);
        connection.setAnswerKeepAlives(true);
        connection.bindClient(new JsonRpcClient());
        connection.bindServer(mJsonRpcServer, new TalkRpcHandler(mTalkServer, rpcConnection));
        // return the raw connection (will be called by server for incoming messages)
        return connection;
    }

}
