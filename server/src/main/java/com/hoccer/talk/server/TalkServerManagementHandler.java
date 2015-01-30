package com.hoccer.talk.server;

import com.hoccer.talk.servlets.PushMessageServlet;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class TalkServerManagementHandler extends HandlerCollection {

    public TalkServerManagementHandler(TalkServer talkServer) {
        addHandler(createPushMessageHandler(talkServer));
    }

    private ServletContextHandler createPushMessageHandler(TalkServer talkServer) {
        ServletContextHandler pushMessageHandler = new ServletContextHandler();

        pushMessageHandler.setContextPath("/push");
        pushMessageHandler.setAttribute("server", talkServer);
        pushMessageHandler.addServlet(PushMessageServlet.class, "/*");

        return pushMessageHandler;
    }
}
