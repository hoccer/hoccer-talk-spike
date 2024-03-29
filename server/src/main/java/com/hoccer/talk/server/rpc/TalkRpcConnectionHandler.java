package com.hoccer.talk.server.rpc;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.websocket.jetty.JettyWebSocket;
import better.jsonrpc.websocket.JsonRpcWsConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.server.TalkServer;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket handler
 * <p/>
 * This is responsible for accepting websocket connections,
 * creating and configuring a connection object for them.
 */
public class TalkRpcConnectionHandler extends WebSocketHandler {

    private static final Logger LOG = Logger.getLogger(TalkRpcConnectionHandler.class);
    private static final int MAX_IDLE_TIME = 1800 * 1000; // in ms

    // Version 1
    private static final String TALK_TEXT_PROTOCOL_NAME_V1 = "com.hoccer.talk.v1";
    private static final String TALK_BINARY_PROTOCOL_NAME_V1 = "com.hoccer.talk.v1.bson";

    // Version 2
    public static final String TALK_TEXT_PROTOCOL_NAME_V2 = "com.hoccer.talk.v2";
    public static final String TALK_BINARY_PROTOCOL_NAME_V2 = "com.hoccer.talk.v2.bson";

    // Version 3
    public static final String TALK_TEXT_PROTOCOL_NAME_V3 = "com.hoccer.talk.v3";
    public static final String TALK_BINARY_PROTOCOL_NAME_V3 = "com.hoccer.talk.v3.bson";

    // Version 4
    public static final String TALK_TEXT_PROTOCOL_NAME_V4 = "com.hoccer.talk.v4";
    public static final String TALK_BINARY_PROTOCOL_NAME_V4 = "com.hoccer.talk.v4.bson";

    // Version 5
    public static final String TALK_TEXT_PROTOCOL_NAME_V5 = "com.hoccer.talk.v5";
    public static final String TALK_BINARY_PROTOCOL_NAME_V5 = "com.hoccer.talk.v5.bson";

    public static List<String> getLegacyProtocolVersions() {
        List<String> list = new ArrayList<String>();
        list.add(TALK_BINARY_PROTOCOL_NAME_V1);
        list.add(TALK_TEXT_PROTOCOL_NAME_V1);
        list.add(TALK_BINARY_PROTOCOL_NAME_V2);
        list.add(TALK_TEXT_PROTOCOL_NAME_V2);
        list.add(TALK_BINARY_PROTOCOL_NAME_V3);
        list.add(TALK_TEXT_PROTOCOL_NAME_V3);
        return list;
    }

    public static List<String> getCurrentProtocolVersions() {
        List<String> list = new ArrayList<String>();
        list.add(TALK_BINARY_PROTOCOL_NAME_V4);
        list.add(TALK_TEXT_PROTOCOL_NAME_V4);
        list.add(TALK_BINARY_PROTOCOL_NAME_V5);
        list.add(TALK_TEXT_PROTOCOL_NAME_V5);
        return list;
    }

    public static boolean isBinaryProtocol(String protocol) {
        return protocol != null && protocol.endsWith(".bson");
    }

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
     * @param request  which initiated websocket upgrade
     * @param protocol the user defined websocket protocol (can be null!)
     * @return websocket
     */
    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        boolean binary = isBinaryProtocol(protocol);

        if (getCurrentProtocolVersions().contains(protocol)) {
            return createTalkConnection(request, binary, false);
        } else if (getLegacyProtocolVersions().contains(protocol)) {
            return createTalkConnection(request, binary, true);
        }

        LOG.error("new connection with unknown protocol '" + protocol + "'");
        return null;
    }

    private WebSocket createTalkConnection(HttpServletRequest request, boolean binary, boolean legacy) {
        JettyWebSocket webSocket = new JettyWebSocket();
        webSocket.setMaxIdleTime(MAX_IDLE_TIME);

        ObjectMapper mapper = binary ? mTalkServer.getBsonMapper() : mTalkServer.getJsonMapper();
        JsonRpcWsConnection connection = new JsonRpcWsConnection(webSocket, mapper);

        TalkRpcConnection rpcConnection = new TalkRpcConnection(mTalkServer, connection, request);
        rpcConnection.setLegacyMode(legacy);

        connection.setSendBinaryMessages(binary);
        connection.setAnswerKeepAlives(true);
        connection.bindClient(new JsonRpcClient());
        connection.bindServer(mJsonRpcServer, new TalkRpcHandler(mTalkServer, rpcConnection));

        return webSocket;
    }
}
