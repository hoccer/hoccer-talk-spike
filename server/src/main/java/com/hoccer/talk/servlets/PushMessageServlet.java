package com.hoccer.talk.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class PushMessageServlet extends HttpServlet {

    private TalkServer mServer;
    private ObjectMapper mObjectMapper;
    private ITalkServerDatabase mDatabase;

    @Override
    public void init() throws ServletException {
        mServer = (TalkServer) getServletContext().getAttribute("server");
        mDatabase = mServer.getDatabase();
        mObjectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            CollectionType type = mObjectMapper.getTypeFactory().constructCollectionType(List.class, PushMessageJson.class);
            List<PushMessageJson> messages = mObjectMapper.readValue(request.getReader(), type);

            for (PushMessageJson message : messages) {
                int count = submitPushMessage(mDatabase, message);
                response.getWriter().println(count + " clients (" + message.language + ", " + message.clientName + ")");
            }

            response.setStatus(200);
        } catch (Exception e) {
            response.getWriter().println(e.getMessage());
            response.setStatus(400);
        }
    }

    private int submitPushMessage(ITalkServerDatabase database, PushMessageJson message) {
        int count = 0;

        for (TalkClient client : database.findAllClients()) {
            TalkClientHostInfo info = database.findClientHostInfoForClient(client.getClientId());

            if (shouldClientReceiveMessage(info, message)) {
                mServer.getPushAgent().submitSystemMessage(client, message.message);
                count += 1;
            }
        }

        return count;
    }

    private boolean shouldClientReceiveMessage(TalkClientHostInfo hostInfo, PushMessageJson message) {
        return hostInfo.getClientLanguage().equals(message.language) &&
                hostInfo.getClientName().equals(message.clientName);
    }

    public static class PushMessageJson {
        public String language;
        public String clientName;
        public String message;
    }
}
