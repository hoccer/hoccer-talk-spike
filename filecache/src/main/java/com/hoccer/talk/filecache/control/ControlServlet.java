package com.hoccer.talk.filecache.control;

import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.websocket.jetty.JettyWebSocket;
import better.jsonrpc.websocket.JsonRpcWsConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.rpc.ICacheControl;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

@WebServlet(urlPatterns = "/control")
public class ControlServlet extends WebSocketServlet {

    ObjectMapper mJsonMapper;

    JsonRpcServer mRpcServer;

    @Override
    public void init() throws ServletException {
        super.init();

        mJsonMapper = new ObjectMapper();

        mRpcServer = new JsonRpcServer(ICacheControl.class);
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        if(protocol.equals("com.hoccer.talk.filecache.control.v1")) {
            JettyWebSocket webSocket = new JettyWebSocket();
            JsonRpcWsConnection connection = new JsonRpcWsConnection(webSocket, mJsonMapper);
            ControlConnection handler = new ControlConnection(this, request);
            connection.bindServer(mRpcServer, handler);
            return webSocket;
        }
        return null;
    }

    public CacheBackend getCacheBackend() {
        ServletContext ctx = this.getServletContext();
        return (CacheBackend)ctx.getAttribute("backend");
    }

}