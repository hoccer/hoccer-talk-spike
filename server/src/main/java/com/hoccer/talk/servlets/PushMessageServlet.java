package com.hoccer.talk.servlets;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PushMessageServlet extends HttpServlet {

    private TalkServer mServer;

    @Override
    public void init() throws ServletException {
        mServer = (TalkServer) getServletContext().getAttribute("server");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ITalkServerDatabase database = mServer.getDatabase();

        for (TalkClient client : database.findAllClients()) {
            mServer.getPushAgent().submitSystemMessage(client, "good day, sir!");
        }

        response.setStatus(200);
        response.getWriter().println("good");
    }
}
