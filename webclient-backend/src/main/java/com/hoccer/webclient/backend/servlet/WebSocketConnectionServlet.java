package com.hoccer.webclient.backend.servlet;

import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.webclient.backend.updates.WebSocketConnection;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.http.HttpServletRequest;

public class WebSocketConnectionServlet extends WebSocketServlet {

    private XoClientDatabase mClientDatabase;

    public WebSocketConnectionServlet(XoClientDatabase database) {
        mClientDatabase = database;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new WebSocketConnection(mClientDatabase);
    }
}
