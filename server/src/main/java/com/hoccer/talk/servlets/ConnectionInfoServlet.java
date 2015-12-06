package com.hoccer.talk.servlets;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.model.TalkEnvironment;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
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

        String query = req.getQueryString();
        if (query != null && query.length()>0) {
            Map<String, String> params = splitQuery(query);
            String idString = params.get("id");
            if (idString != null) {
                int id = Integer.parseInt(idString);
                w.write("Info for connection "+id+" at "+now+"\n\n");
                TalkRpcConnection connection = server.findConnectionById(id);
                if (connection == null) {
                    w.write("Connection with "+id+" not found.\n");
                    w.close();
                    return;
                }
                Map<String, TalkPresence> all = new HashMap<String, TalkPresence>();
                Map<String, TalkEnvironment> worldwide = new HashMap<String, TalkEnvironment>();
                Map<String, TalkEnvironment> nearby = new HashMap<String, TalkEnvironment>();
                if (connection.isLoggedInFlag()) {
                    String clientId = connection.getClientId();
                    TalkPresence presence = db.findPresenceForClient(clientId);
                    if (presence != null) {
                        all.put(presence.getClientId(), presence);
                        TalkEnvironment wwe = db.findEnvironmentByClientId(TalkEnvironment.TYPE_WORLDWIDE, clientId);
                        if (wwe != null) {
                            worldwide.put(clientId, wwe);
                        }
                        TalkEnvironment nbe = db.findEnvironmentByClientId(TalkEnvironment.TYPE_NEARBY, clientId);
                        if (nbe != null) {
                            nearby.put(clientId, nbe);
                        }
                    }
                }

                printConnection(w, connection, now, all, worldwide, nearby);

                w.write("\n");

                w.write("Http Headers for user agent: "+connection.getUserAgent()+":\n\n");

                Map<String,String> headers = connection.getInitialRequestHeaders();
                for (String key : headers.keySet()) {
                    w.write("" + key + " : " + headers.get(key));
                    w.write("\n");
                }
                w.write("\n");
                w.close();
                return;
            }
        }

        w.write("Connection Info "+now+"\n\n");

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

        Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
        w.write("Threads: " + threadMap.size()+ "\n");
        w.write("\n");

        w.write("Presences not offline : " + activePresences.size()+ "\n");
        w.write("Presences online      : " + online.size() + "\n");
        w.write("Presences background  : " + background.size() + "\n");
        w.write("Presences typing      : " + typing.size()+ "\n");
        w.write("\n");

        Vector<TalkRpcConnection> connections =  server.getConnectionsClone();
        Hashtable<String, TalkRpcConnection> connectionsById = server.getConnectionsByIdClone();

        long unconnectedUnreleasedEnvironmentsWorldwide = 0;
        long unconnectedUnreleasedEnvironmentsWorldwideNoTTL = 0;
        Map<String, TalkEnvironment> worldwide = new HashMap<String, TalkEnvironment>();
        List<TalkEnvironment> worldwideList = db.findEnvironmentsByType(TalkEnvironment.TYPE_WORLDWIDE);
        for (TalkEnvironment environment : worldwideList) {
            worldwide.put(environment.getClientId(), environment);
            if (connectionsById.get(environment.getClientId()) == null) {
                if (environment.getTimeReleased() == null) {
                    unconnectedUnreleasedEnvironmentsWorldwide++;
                    if (!environment.willLiveAfterRelease()) {
                        unconnectedUnreleasedEnvironmentsWorldwideNoTTL++;
                    }
                }
            }
        }

        long unconnectedEnvironmentsNearby = 0;
        Map<String, TalkEnvironment> nearby = new HashMap<String, TalkEnvironment>();
        List<TalkEnvironment> nearbyList = db.findEnvironmentsByType(TalkEnvironment.TYPE_NEARBY);
        for (TalkEnvironment environment : nearbyList) {
            nearby.put(environment.getClientId(), environment);
            if (connectionsById.get(environment.getClientId()) == null) {
                unconnectedEnvironmentsNearby++;
            }
        }
        w.write("Environments:\n");
        w.write("Worldwide total                         : " + worldwide.size()+ "\n");
        w.write("Worldwide unconnected unreleased        : " + unconnectedUnreleasedEnvironmentsWorldwide+ "\n");
        w.write("Worldwide unconnected unreleased no ttl : " + unconnectedUnreleasedEnvironmentsWorldwideNoTTL+ "\n");
        w.write("Nearby total                            : " + nearby.size() + "\n");
        w.write("Nearby unconnected                      : " + unconnectedEnvironmentsNearby + "\n");
        w.write("\n");


        long worldwideConnections = 0;
        long worldwideConnectionsTTL = 0;
        long worldwideConnectionsExpired = 0;
        long nearbyConnections = 0;

        for (TalkRpcConnection connection : connections) {
            String clientId = connection.getClientId();
            if (clientId != null) {
                TalkEnvironment worldwideEnvironment = worldwide.get(clientId);
                if (worldwideEnvironment != null) {
                    worldwideConnections += 1;
                    worldwideConnectionsTTL += worldwideEnvironment.willLiveAfterRelease() ? 1 : 0;
                    worldwideConnectionsExpired += worldwideEnvironment.hasExpired() ? 1 : 0;
                }
                nearbyConnections += nearby.containsKey(clientId) ? 1 : 0;
            }
        }

        w.write("Open connections list size : " + connections.size()+ "\n");
        w.write("Open Connections counter   : " + server.getConnectionsOpen()+ "\n");
        w.write("Logged in map size         : " + server.numberOfClientConnections()+ "\n");
        w.write("Logged in counter          : " + server.getConnectionsLoggedIn()+ "\n");
        w.write("Ready counter              : " + server.getConnectionsReady()+ "\n");
        w.write("Total served counter       : " + server.getConnectionsTotal()+ "\n");
        w.write("\n");
        w.write("Connections with Environments:\n");
        w.write("Worldwide                  : " + worldwideConnections+ "\n");
        w.write("Worldwide with TTL         : " + worldwideConnectionsTTL+ "\n");
        w.write("Worldwide expired          : " + worldwideConnectionsExpired+ "\n");
        w.write("Nearby                     : " + nearbyConnections + "\n");
        w.write("\n");

        for (TalkRpcConnection connection : connections) {
            printConnection(w, connection, now, all, worldwide, nearby);
        }
        w.write("\n");


        w.write("Connections in table but not in list:\n");

        for (String clientId : connectionsById.keySet()) {
            TalkRpcConnection connection = connectionsById.get(clientId);
            if (connection == null) {
                w.write("#FATAL: No connection found for client Id " + clientId+"\n"); // should never happen
            } else {
                if (!connections.contains(connection)) {
                    printConnection(w, connection, now, all, worldwide, nearby);
                }
            }
        }
        w.write("\n");

        Map<Thread, StackTraceElement[]> sortedThreads = sortByName(threadMap);

        w.write("Threads:\n");
        for (Thread t : sortedThreads.keySet()) {
            w.write(""+t+", State "+threadStateName(t.getState())+"\n");
            for (StackTraceElement st : sortedThreads.get(t)) {
                w.write("    "+st.toString()+"\n");
            }
            w.write("\n");
        }
        w.write("\n");

        w.close();
    }

    public static String threadStateName(Thread.State state) {
        switch (state) {
            case NEW:
                return "NEW";
            case RUNNABLE:
                return "RUNNABLE";
            case BLOCKED:
                return "BLOCKED";
            case WAITING:
                return "WAITING";
            case TIMED_WAITING:
                return "TIMED_WAITING";
            case TERMINATED:
                return "TERMINATED";
            default:
                return "ILLEGAL_STATE";
        }
    }
    public static Map sortByName(Map unsortedMap) {
        Map sortedMap = new TreeMap(new StringValueComparator(unsortedMap));
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

    static class StringValueComparator implements Comparator {
        Map map;
        public StringValueComparator(Map map) {
            this.map = map;
        }

        public int compare(Object keyA, Object keyB) {
            return keyA.toString().compareTo(keyB.toString());
        }
    }

    void printConnection(OutputStreamWriter w, TalkRpcConnection connection, Date now,
                         Map<String, TalkPresence> all, Map<String, TalkEnvironment> worldwide,
                         Map<String, TalkEnvironment> nearby) throws ServletException, IOException
    {
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

            boolean hasWorldwideEnvironment = false;
            boolean hasWorldwideEnvironmentWithTTL = false;
            boolean hasWorldwideEnvironmentExpired = false;
            boolean hasNearbyEnvironment = false;

            String clientId = connection.getClientId();
            TalkPresence presence = null;
            TalkClientHostInfo hostInfo = null;
            if (clientId != null) {
                presence = all.get(clientId);
                hostInfo = db.findClientHostInfoForClient(clientId);
                TalkEnvironment worldwideEnvironment = worldwide.get(clientId);
                if (worldwideEnvironment != null) {
                    hasWorldwideEnvironment = true;
                    hasWorldwideEnvironmentWithTTL = worldwideEnvironment.willLiveAfterRelease();
                    hasWorldwideEnvironmentExpired = worldwideEnvironment.hasExpired();
                }
                hasNearbyEnvironment = nearby.containsKey(clientId);
            }
            status = status + (hasNearbyEnvironment ? " N" : "");
            status = status + (hasWorldwideEnvironment ? " W" : "");
            status = status + (hasWorldwideEnvironmentWithTTL ? " T" : "");
            status = status + (hasWorldwideEnvironmentExpired ? " E" : "");

            String nickName = "-";
            String presenceStatus = "unknown";
            if (presence != null) {
                presenceStatus = presence.getConnectionStatus();
                nickName = presence.getClientName();
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
                        lastRequestStatus = "<-"+lastRequest + " took " + (lastFinished.getTime() - lastStarted.getTime()) + " ms " + (now.getTime() - lastStarted.getTime())/1000 + " s ago";
                    } else {
                        lastRequestStatus = "<-"+lastRequest + "  started " + (now.getTime() - lastStarted.getTime()) + " ms ago, previous request finished " + (now.getTime() - lastStarted.getTime()) + " ms ago";
                    }
                } else {
                    lastRequestStatus = "<-"+lastRequest + "  started " + (now.getTime() - lastStarted.getTime()) + " ms ago, no previous request finished ";
                }
            } else {
                lastRequestStatus = "no requests yet";
            }
            String lastClientRequest = connection.getLastClientRequestName();
            Date lastClientRequestDate = connection.getLastClientRequestDate();
            long lastClientRequestDateAgo = 0;
            if (lastClientRequestDate != null) {
                lastClientRequestDateAgo = (now.getTime() - lastClientRequestDate.getTime())/1000;
            } else {
                lastClientRequestDateAgo = 0;
            }

            boolean isClientResponsive = connection.isClientResponsive();
            String clientStatus;
            if (lastClientRequest != null) {
                clientStatus = String.format(", ->%s took %s ms %d s ago", lastClientRequest, connection.getLastClientResponseTime(), lastClientRequestDateAgo) + (isClientResponsive ? "" : ",stalled");
            } else {
                clientStatus = "";
            }
            TalkClient client = connection.getClient();
            String pushStatus = "No push";
            if (client != null) {
                if (client.isApnsCapable()) {
                    pushStatus = "APNS";
                } else if (client.isGcmCapable()) {
                    pushStatus = "GCM";
                }

                if (client.getTimeLastPush() != null) {
                    long pushAgo = (new Date().getTime() - client.getTimeLastPush().getTime()) / 1000;
                    pushStatus = pushStatus + " " + pushAgo + " s ago";
                }
            }

            long age = (new Date().getTime() - connection.getCreationTime().getTime())/1000;
            w.write(String.format("[%d] %d s %s %s" , connection.getConnectionId(), age, status, presenceStatus)
                    +" "+connection.getRemoteAddress()+" '"+nickName+"'("+pushStatus+","+ lastRequestStatus + clientStatus+"), "+ clientInfo+ " ["+clientId+"]"+"\n");
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
