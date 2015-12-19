package com.hoccer.talk.servlets;

import better.jsonrpc.server.JsonRpcServer;
import com.hoccer.talk.model.*;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.push.PushAgent;
import com.hoccer.talk.server.push.PushRequest;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import org.jongo.MongoCollection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;


/**
 * Created by pavel on 18.12.15.
 */


@WebServlet(urlPatterns = {"/rpcinfo"})
public class RpcInfoServlet extends HttpServlet {

    private ITalkServerDatabase db;
    private TalkServer server;

    private MongoCollection getCollection(String name) {
        return (MongoCollection)db.getRawCollection(name);
    }

    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    Hashtable validUsers = new Hashtable();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // ie this user has no password
        //validUsers.put("james:","authorized");

        validUsers.put("hoccer:hcrSrv$23Info","authorized");
    }

    // This method checks the user information sent in the Authorization
    // header against the database of users maintained in the users Hashtable.
    protected boolean allowUser(String auth) throws IOException {

        if (auth == null) {
            return false;  // no auth
        }
        if (!auth.toUpperCase().startsWith("BASIC ")) {
            return false;  // we only do BASIC
        }
        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);
        // Decode it, using any base 64 decoder
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        String userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));

        // Check our user list to see if that user and password are "allowed"
        if ("authorized".equals(validUsers.get(userpassDecoded))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        // Do we allow that user?
        if (!allowUser(auth)) {
            // Not allowed, so report he's unauthorized
            resp.setHeader("WWW-Authenticate", "BASIC realm=\"Hoccer Server RPC Info\"");
            resp.sendError(resp.SC_UNAUTHORIZED);
            return;
        }

        server = (TalkServer)getServletContext().getAttribute("server");

        db = server.getDatabase();

        resp.setContentType("text/plain; charset=UTF-8");

        OutputStream s = resp.getOutputStream();
        OutputStreamWriter w = new OutputStreamWriter(s, "UTF-8");

        if (server == null || !server.isReady()) {
            w.write("Server is starting, retry in a few seconds...\n\n");
            w.close();
            return;
        }

        Date now = new Date();

        w.write("RPC Info at "+now+"\n\n");

        JsonRpcServer rpcServer = server.getRpcServer();

        Map<String, JsonRpcServer.CallInfo> sortedCallInfoMap = rpcServer.getSortedCallInfoMapClone();
        //Map<String, JsonRpcServer.CallInfo> unsortedCallInfoMap = rpcServer.getCallInfoMapClone();

        w.write("Sorted by total time spent ("+sortedCallInfoMap.size()+"):\n");
        JsonRpcServer.CallInfo totalCallInfo = new JsonRpcServer.CallInfo("TOTAL");
        for (String callName : sortedCallInfoMap.keySet()) {
            JsonRpcServer.CallInfo callInfo = sortedCallInfoMap.get(callName);
            if (callInfo != null) {
                totalCallInfo.accumulate(callInfo);
                w.write(callInfo.info() + "\n");
            } else {
                w.write("#ERROR: no callinfo for "+callName+"\n");
            }
        }
        w.write(totalCallInfo.totalInfo()+"\n");
        w.write("\n");

        w.write("RPC full info at "+now+"\n\n");
        for (String callName : sortedCallInfoMap.keySet()) {
            JsonRpcServer.CallInfo callInfo = sortedCallInfoMap.get(callName);
            if (callInfo != null) {
                w.write(callInfo.fullInfo() + "\n");
            }  else {
                w.write("#ERROR: (full Info) no CallInfo for "+callName+"\n");
            }
            w.write("\n");
        }

        w.write("\n");

        w.close();
    }
}
