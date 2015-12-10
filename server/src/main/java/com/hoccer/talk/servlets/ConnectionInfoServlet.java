package com.hoccer.talk.servlets;

import com.hoccer.talk.model.*;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.push.PushRequest;
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

        w.write("[connection id] created login-time/ready-time status presence remote-addr 'nickname' (pushInfo, lastIncomingRequest, lastOutgoingRequest) clientInfo [clientId]\n");

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

        w.write("Push Requests not yet answered:\n");
        Map<String,PushRequest> notAnswered = new HashMap<String, PushRequest>(server.getPushAgent().getNotAnswered());
        printPushInfo(db, w, notAnswered);
        w.write("\n");

        w.write("Push Requests answered:\n");
        Map<String,PushRequest> answered = new HashMap<String, PushRequest>(server.getPushAgent().getAnswered());
        printPushInfo(db, w, answered);
        w.write("\n");

        // nearby groups
        List<TalkGroupPresence> nearbyGroups = db.findGroupPresencesWithTypeAndState(TalkGroupPresence.GROUP_TYPE_NEARBY, TalkGroupPresence.STATE_EXISTS);
        w.write("Nearby groups ("+nearbyGroups.size()+"):\n");
        for (TalkGroupPresence groupPresence : nearbyGroups) {
            List<TalkGroupMembership> memberships = db.findGroupMembershipsByIdWithStates(groupPresence.getGroupId(),
                    new String[]{TalkGroupMembership.STATE_JOINED});
            long createdAgo = new Date().getTime() - groupPresence.getLastChanged().getTime();
            w.write("Nearby group with groupId:"+groupPresence.getGroupId()+" name: "+
                    groupPresence.getGroupName()+" has "+memberships.size()+" members, keyid: "+groupPresence.getSharedKeyId()+", lastChanged "+createdAgo/1000+" s ago\n");
            double latitudeSum = 0;
            double longitudeSum = 0;
            Vector<Double[]> coords = new Vector<Double[]>();
            for (TalkGroupMembership membership : memberships) {
                TalkPresence presence = db.findPresenceForClient(membership.getClientId());
                TalkEnvironment environment = nearby.get(membership.getClientId());
                if (environment != null) {
                    long receivedAgo = new Date().getTime() - environment.getTimeReceived().getTime();
                    Double[] geoPosition = environment.getGeoLocation();
                    double latitude = 0;
                    double longitude = 0;
                    if (geoPosition != null && geoPosition.length == 2) {
                        latitude = geoPosition[TalkEnvironment.LATITUDE_INDEX];
                        longitude = geoPosition[TalkEnvironment.LONGITUDE_INDEX];
                        latitudeSum += latitude;
                        longitudeSum += longitude;
                        coords.add(geoPosition);
                    }
                    w.write("    clientId " + membership.getClientId() + " keyid "+membership.getSharedKeyId()+" nick '" + presence.getClientName() +
                            "' received " + receivedAgo/1000 + "s ago, long/lat:" + longitude+","+latitude+
                            " acc: "+environment.getAccuracy()+" BSSIDS:"+Arrays.toString(environment.getBssids())+ " \n");
                } else {
                    w.write("    Member clientId " + membership.getClientId() + "\n");
                }
            }
            if (coords.size() > 0) {
                double latitudeCenter = latitudeSum / coords.size();
                double longitudeCenter = longitudeSum / coords.size();
                w.write("  center long/lat:" + longitudeCenter + "," + latitudeCenter + ", distances of each member from center in m:");
                for (int i = 0; i < coords.size(); ++i) {
                    w.write(" " + distFrom(latitudeCenter, longitudeCenter, coords.get(i)[TalkEnvironment.LATITUDE_INDEX], coords.get(i)[TalkEnvironment.LONGITUDE_INDEX]));
                }
            }
            w.write("\n");
            w.write("\n");
        }
        w.write("\n");

        // worldwide groups
        List<TalkGroupPresence> worldwideGroups = db.findGroupPresencesWithTypeAndState(TalkGroupPresence.GROUP_TYPE_WORLDWIDE, TalkGroupPresence.STATE_EXISTS);
        w.write("Worldwide groups ("+worldwideGroups.size()+"):\n");
        for (TalkGroupPresence groupPresence : worldwideGroups) {
            List<TalkGroupMembership> memberships = db.findGroupMembershipsByIdWithStates(groupPresence.getGroupId(),
                    new String[]{TalkGroupMembership.STATE_JOINED});
            long createdAgo = new Date().getTime() - groupPresence.getLastChanged().getTime();
            w.write("Worldwide group with groupId:"+groupPresence.getGroupId()+" name: "+
                    groupPresence.getGroupName()+" has "+memberships.size()+" members, keyid: "+groupPresence.getSharedKeyId()+", lastChanged "+createdAgo/1000+" s ago\n");
            double latitudeSum = 0;
            double longitudeSum = 0;
            List<TalkEnvironment> environments = db.findEnvironmentsForGroup(groupPresence.getGroupId());
            Map<String, TalkEnvironment> environmentsByClientId = new HashMap<String, TalkEnvironment>();
            for (TalkEnvironment e : environments) {
                environmentsByClientId.put(e.getClientId(), e);
            }
            Vector<Double[]> coords = new Vector<Double[]>();
            for (TalkGroupMembership membership : memberships) {
                TalkPresence presence = db.findPresenceForClient(membership.getClientId());
                TalkEnvironment environment = environmentsByClientId.get(membership.getClientId());
                if (environment != null) {
                    long receivedAgo = new Date().getTime() - environment.getTimeReceived().getTime();
                    Double[] geoPosition = environment.getGeoLocation();
                    double latitude = 0;
                    double longitude = 0;
                    if (geoPosition != null && geoPosition.length == 2) {
                        latitude = geoPosition[TalkEnvironment.LATITUDE_INDEX];
                        longitude = geoPosition[TalkEnvironment.LONGITUDE_INDEX];
                        latitudeSum += latitude;
                        longitudeSum += longitude;
                        coords.add(geoPosition);
                    }

                    w.write("    clientId " + membership.getClientId() + " keyid "+membership.getSharedKeyId()+" nick '" + presence.getClientName() +
                            "' received " + receivedAgo/1000 + "s ago, ttl "+environment.getTimeToLive()+" ms "+envStatus(environment)+ "long/lat:" + longitude+","+latitude+
                            " acc: "+environment.getAccuracy()+" BSSIDS:"+Arrays.toString(environment.getBssids())+ " \n");
                    environmentsByClientId.remove(membership.getClientId());
                } else {
                    w.write("    Member clientId " + membership.getClientId() + "\n");
                }
            }
            for (String cid : environmentsByClientId.keySet()) {
                TalkEnvironment environment = environmentsByClientId.get(cid);
                long receivedAgo = new Date().getTime() - environment.getTimeReceived().getTime();
                w.write("   ? Not a member, but environment there: ["+cid+"], received " + receivedAgo/1000
                        + "s ago, ttl "+environment.getTimeToLive()+" ms "+envStatus(environment)+ "long/lat:" + Arrays.toString(environment.getGeoLocation())+
                " acc: "+environment.getAccuracy()+" BSSIDS:"+Arrays.toString(environment.getBssids())+ " \n");
            }
            if (coords.size() > 0) {
                double latitudeCenter = latitudeSum / coords.size();
                double longitudeCenter = longitudeSum / coords.size();
                w.write("  center long/lat:" + longitudeCenter + "," + latitudeCenter + ", distances of each member from center in m:");
                for (int i = 0; i < coords.size(); ++i) {
                    w.write(" " + distFrom(latitudeCenter, longitudeCenter, coords.get(i)[TalkEnvironment.LATITUDE_INDEX], coords.get(i)[TalkEnvironment.LONGITUDE_INDEX]));
                }
            }
            w.write("\n");
            w.write("\n");
        }
        w.write("\n");
        Map<Thread, StackTraceElement[]> sortedThreads = sortByName(threadMap);

        w.write("Threads:\n");
        for (Thread t : sortedThreads.keySet()) {
            w.write(""+t+", State "+threadStateName(t.getState())+"\n");
            for (StackTraceElement st : sortedThreads.get(t)) {
                w.write("    "+st.toString()+"\n");
                break; // remove to see full traces
            }
            w.write("\n");
        }
        w.write("\n");

        w.close();
    }

    public static String envStatus(TalkEnvironment environment) {
        String status = "";
        if (environment.getTimeReleased() != null) {
            status = "released ";
        }
        if (environment.hasExpired()) {
            status += "expired ";
        }
        return status;
    }

    public void printPushInfo(ITalkServerDatabase db, OutputStreamWriter w, Map<String,PushRequest> unsortedPushRequests)  throws ServletException, IOException {
        Map<String,PushRequest>  pushRequests = sortPushByDate(unsortedPushRequests);
        Date now = new Date();
        for (String clientId : pushRequests.keySet()) {
            PushRequest request = pushRequests.get(clientId);
            TalkClientHostInfo hostInfo = db.findClientHostInfoForClient(clientId);
            TalkClient client = db.findClientById(clientId);
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
                    if (client.getTimeLastLogin() != null && client.getTimeLastLogin().after(client.getTimeLastPush())) {
                        pushStatus = pushStatus + ", logged in "+ (client.getTimeLastLogin().getTime()-client.getTimeLastPush().getTime())/1000+" s after push";
                    }
                }
            }
            long pushCreatedAgo = (new Date().getTime() - request.getCreatedTime().getTime()) / 1000;

            w.write("["+clientId+"] created "+pushCreatedAgo+" ago ("+ pushStatus + ") "+hostInfo.info());
            w.write("\n");
        }
    }

    public static Map sortPushByDate(Map unsortedMap) {
        Map sortedMap = new TreeMap(new PushValueComparator(unsortedMap));
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

    static class PushValueComparator implements Comparator {

        Map map;

        public PushValueComparator(Map map) {
            this.map = map;
        }

        public int compare(Object keyA, Object keyB) {
            PushRequest valueA = (PushRequest) map.get(keyA);
            PushRequest valueB = (PushRequest) map.get(keyB);
            return valueB.getCreatedTime().compareTo(valueA.getCreatedTime());
        }
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
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
            //status = status + (connection.isLoggedInFlag() ? " LIF" : "");

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
            status = status + (hasNearbyEnvironment ? " Nearby" : "");
            status = status + (hasWorldwideEnvironment ? " Worldwide" : "");
            status = status + (hasWorldwideEnvironmentWithTTL ? "-TTL" : "");
            status = status + (hasWorldwideEnvironmentExpired ? "-expired" : "");

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
                        lastRequestStatus = "<-"+lastRequest + " (" + (lastFinished.getTime() - lastStarted.getTime()) + "ms " + (now.getTime() - lastStarted.getTime())/1000 + "s ago)";
                    } else {
                        lastRequestStatus = "<-"+lastRequest + "  started " + (now.getTime() - lastStarted.getTime()) + "ms ago, previous request finished " + (now.getTime() - lastStarted.getTime()) + "ms ago";
                    }
                } else {
                    lastRequestStatus = "<-"+lastRequest + "  started " + (now.getTime() - lastStarted.getTime()) + "ms ago, no previous request finished ";
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
                clientStatus = String.format(", ->%s (%sms %ds ago)", lastClientRequest, connection.getLastClientResponseTime(), lastClientRequestDateAgo) + (isClientResponsive ? "" : ",stalled");
            } else {
                clientStatus = "";
            }
            long loginTook = 0;
            long readyTook = 0;
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
                if (connection.isLoggedIn()) {
                    loginTook = client.getTimeLastLogin().getTime() - connection.getCreationTime().getTime();
                }
                if (connection.isReady()) {
                    readyTook = client.getTimeReady().getTime() - client.getTimeLastLogin().getTime();
                }
            }
            long age = (new Date().getTime() - connection.getCreationTime().getTime())/1000;
            w.write(String.format("[%d] %ds %dms/%dms %s %s" , connection.getConnectionId(), age, loginTook, readyTook, status, presenceStatus)
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
