package com.hoccer.talk.servlets;

import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import org.jongo.MongoCollection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Created by pavel on 27.11.15.
 */


@WebServlet(urlPatterns = {"/connections"})
public class ConnectionInfoServlet extends HttpServlet {

    private ITalkServerDatabase db;
    private TalkServer server;

    private MongoCollection getCollection(String name) {
        return (MongoCollection)db.getRawCollection(name);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

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
        w.write("Connection Info "+now+"\n\n");

        //MongoCollection presences = getCollection("presence");

        List<TalkPresence> activePresences = db.findPresencesWithStates(TalkPresence.ACTIVE_STATES);

        Map<String, TalkPresence> online = new HashMap<String, TalkPresence>();
        Map<String, TalkPresence> background = new HashMap<String, TalkPresence>();
        Map<String, TalkPresence> typing = new HashMap<String, TalkPresence>();
        Map<String, TalkPresence> all = new HashMap<String, TalkPresence>();

        for (TalkPresence presence : activePresences) {
            if (presence.isBackground()) {
                background.put(presence.getClientId(), presence);
            } else if (presence.isTyping()) {
                typing.put(presence.getClientId(), presence);
            } else if (presence.isOnline()) {
                online.put(presence.getClientId(), presence);
            }
            all.put(presence.getClientId(), presence);
        }

        w.write("Presences not offline : " + activePresences.size()+ "\n");
        w.write("Presences online      : " + online.size() + "\n");
        w.write("Presences background  : " + background.size() + "\n");
        w.write("Presences typing      : " + typing.size()+ "\n");
        w.write("\n");

        Vector<TalkRpcConnection> connections =  server.getConnectionsClone();
        Hashtable<String, TalkRpcConnection> connectionsById = server.getConnectionsByIdClone();

        w.write("Open connections list size : " + connections.size()+ "\n");
        w.write("Open Connections counter   : " + server.getConnectionsOpen()+ "\n");
        w.write("Logged in map size         : " + server.numberOfClientConnections()+ "\n");
        w.write("Logged in counter          : " + server.getConnectionsLoggedIn()+ "\n");
        w.write("Ready counter              : " + server.getConnectionsReady()+ "\n");
        w.write("Total counter              : " + server.getConnectionsTotal()+ "\n");
        w.write("\n");

        for (TalkRpcConnection connection : connections) {
            printConnection(w, connection, now, all);
        }
        w.write("\n");


        w.write("Connections in table but not in list:\n");

        for (String clientId : connectionsById.keySet()) {
            TalkRpcConnection connection = connectionsById.get(clientId);
            if (connection == null) {
                w.write("#FATAL: No connection found for client Id " + clientId+"\n"); // should never happen
            } else {
                if (!connections.contains(connection)) {
                    printConnection(w, connection, now, all);
                }
            }
        }
        w.write("\n");

        w.close();
    }
    void printConnection(OutputStreamWriter w, TalkRpcConnection connection, Date now, Map<String, TalkPresence> all) throws ServletException, IOException {
        synchronized (connection) {
            String status;
            if (connection.isReady()) {
                status = "ready";
            } else if (connection.isLoggedIn()) {
                status = "logged-in";
            } else if (connection.isRegistering()) {
                status = "registering";
            } else if (connection.wasReady()) {
                status = "was-ready";
            } else if (connection.wasLoggedIn()) {
                status = "was-logged-in";
            } else if (connection.isConnected()) {
                status = "connected";
            } else {
                status = "not-connected";
            }
            status = status + (connection.isShuttingDown() ? " closing" : "");
            status = status + (connection.isLoggedInFlag() ? " LIF" : "");

            String clientId = connection.getClientId();
            TalkPresence presence = null;
            TalkClientHostInfo hostInfo = null;
            if (clientId != null) {
                presence = all.get(clientId);
                hostInfo = db.findClientHostInfoForClient(clientId);
            }

            String presenceStatus = "unknown";
            if (presence != null) {
                presenceStatus = presence.getConnectionStatus();
            }

            String clientInfo = "no info";
            if (hostInfo != null) {
                clientInfo = hostInfo.info();
            }
            String lastRequest = connection.getLastRequestName();
            Date lastStarted = connection.getLastRequestStarted();
            Date lastFinished = connection.getLastRequestFinished();
            String lastRequestStatus = "-";
            if (lastStarted != null) {
                if (lastFinished != null) {
                    if (lastFinished.after(lastStarted)) {
                        lastRequestStatus = lastRequest + " took " + (lastFinished.getTime() - lastStarted.getTime()) + " ms " + (now.getTime() - lastStarted.getTime()) + " ms ago";
                    } else {
                        lastRequestStatus = lastRequest + "  started " + (now.getTime() - lastStarted.getTime()) + " ms ago, previous request finished " + (now.getTime() - lastStarted.getTime()) + " ms ago";
                    }
                } else {
                    lastRequestStatus = lastRequest + "  started " + (now.getTime() - lastStarted.getTime()) + " ms ago, no previous request finished ";
                }
            } else {
                lastRequestStatus = "no requests yet";
            }
            long age = (new Date().getTime() - connection.getCreationTime().getTime())/1000;
            w.write(String.format("[%6d]%6d s %15s %-10s ping %6d ms" , connection.getConnectionId(), age, status, presenceStatus, connection.getLastPingLatency())
                    +", ("+ lastRequestStatus +"), "+ clientInfo+" ["+clientId+"]"+"\n");
        }
    }
    public static Map sortByValue(Map unsortedMap) {
        Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

    static class ValueComparator implements Comparator {

        Map map;

        public ValueComparator(Map map) {
            this.map = map;
        }

        public int compare(Object keyA, Object keyB) {
            Comparable valueA = (Comparable) map.get(keyA);
            Comparable valueB = (Comparable) map.get(keyB);
            return valueB.compareTo(valueA);
        }
    }

}
