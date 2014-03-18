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

    private static final Logger log = Logger.getLogger(TalkRpcConnectionHandler.class);

    /**
     * Talk server instance
     */
    private final TalkServer mTalkServer;

    /**
     * JSON-RPC server object
     */
    private final JsonRpcServer mRpcServer;

    /**
     * Construct a handler for the given server
     *
     * @param server to add connections to
     */
    public TalkRpcConnectionHandler(TalkServer server) {
        mRpcServer = server.getRpcServer();
        mTalkServer = server;
    }

    /**
     * Create a websocket for the given HTTP request and WS protocol
     *
     * @param request the incoming HTTP upgrade request
     * @param protocol the websocket sub protocol
     * @return null
     */
    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        // The protocol originates from the Header 'Sec-WebSocket-Protocol' and is user defined
        // It is not mandatory (see https://tools.ietf.org/html/rfc6455#section-11.3.4)
        if (protocol == null) {
            log.info("protocol is null - aborting connection attempt");
            return null;
        }
        if (protocol.equals("com.hoccer.talk.v1")) {
            return createTalkV1Connection(request, mTalkServer.getJsonMapper(), false);
        }
        if (protocol.equals("com.hoccer.talk.v1.bson")) {
            return createTalkV1Connection(request, mTalkServer.getBsonMapper(), true);
        }
        log.info("new connection with unknown protocol " + protocol);
        return null;
    }

    private WebSocket createTalkV1Connection(HttpServletRequest request, ObjectMapper mapper, boolean binary) {
        // create JSON-RPC connection (this implements the websocket interface)
        JsonRpcWsConnection connection = new JsonRpcWsConnection(mapper);
        // create talk high-level connection object
        TalkRpcConnection rpcConnection = new TalkRpcConnection(mTalkServer, connection, request);
        // configure the connection
        connection.setSendBinaryMessages(binary);
        connection.setMaxIdleTime(1800 * 1000);
        connection.setAnswerKeepAlives(true);
        connection.bindClient(new JsonRpcClient());
        connection.bindServer(mRpcServer, new TalkRpcHandler(mTalkServer, rpcConnection));
        // return the raw connection (will be called by server for incoming messages)
        return connection;
    }

}
