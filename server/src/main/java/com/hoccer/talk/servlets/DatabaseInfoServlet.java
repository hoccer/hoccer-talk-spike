package com.hoccer.talk.servlets;

import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import org.jongo.MongoCollection;

import javax.servlet.ServletContext;
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
 * Created by pavel on 13.11.15.
 */

@WebServlet(urlPatterns = {"/dbinfo"})
public class DatabaseInfoServlet extends HttpServlet {

    private ITalkServerDatabase db;

    private MongoCollection getCollection(String name) {
        return (MongoCollection)db.getRawCollection(name);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        TalkServer server = (TalkServer)getServletContext().getAttribute("server");

        db = server.getDatabase();

        resp.setContentType("text/plain; charset=UTF-8");

        OutputStream s = resp.getOutputStream();
        OutputStreamWriter w = new OutputStreamWriter(s, "UTF-8");

        if (server == null || !server.isReady()) {
            w.write("Server is starting, retry in a few seconds...\n\n");
            w.close();
            return;
        }

        w.write("Database Stats: "+new Date()+"\n\n");

        MongoCollection presences = getCollection("presence");
        w.write("Presences Total         : " + presences.count()+ "\n");
        w.write("Presences not offline   : " + presences.count("{connectionStatus : {$ne: 'offline'}}")+ "\n");
        w.write("Presences online        : " + presences.count("{connectionStatus : 'online'}")+ "\n");
        w.write("Presences background    : " + presences.count("{connectionStatus : 'background'}")+ "\n");
        w.write("Presences typing        : " + presences.count("{connectionStatus : 'typing'}")+ "\n");
        w.write("\n");

        MongoCollection client = getCollection("client");
        w.write("Clients Total     : " + client.count()+ "\n");
        w.write("Clients APNS      : " + client.count("{apnsToken : {$exists : true}}")+ "\n");
        w.write("Clients GCM       : " + client.count("{gcmRegistration : {$exists : true}}")+ "\n");
        w.write("Clients w/o push  : " + client.count("{apnsToken : {$exists : false}, gcmRegistration : {$exists : false}}")+ "\n");
        w.write("\n");

        MongoCollection groups = getCollection("group");
        w.write("Groups Total         : " + groups.count()+ "\n");
        w.write("Groups state exists  : " + groups.count("{state : 'exists' }")+ "\n");
        w.write("Groups state none    : " + groups.count("{state : 'none' }")+ "\n");
        w.write("\n");
        w.write("Groups type user     : " + groups.count("{groupType : 'user' }")+ "\n");
        w.write("Groups type nearby   : " + groups.count("{groupType : 'nearby' }")+ "\n");
        w.write("Groups type worldwide: " + groups.count("{groupType : 'worldwide' }")+ "\n");
        w.write("\n");

        MongoCollection environments = getCollection("environment");
        w.write("Environments Total    : " + environments.count()+ "\n");
        w.write("Environments nearby   : " + environments.count("{type : 'nearby' }")+ "\n");
        w.write("Environments worldwide: " + environments.count("{type : 'worldwide' }")+ "\n");
        w.write("\n");

        MongoCollection groupMembers = getCollection("groupMember");
        w.write("Group Members Total       : " + groupMembers.count()+ "\n");
        w.write("Members state none        : " + groupMembers.count("{state : 'none' }")+ "\n");
        w.write("Members state invited     : " + groupMembers.count("{state : 'invited' }")+ "\n");
        w.write("Members state joined      : " + groupMembers.count("{state : 'joined' }")+ "\n");
        w.write("Members state groupRemoved: " + groupMembers.count("{state : 'groupRemoved' }")+ "\n");
        w.write("Members state suspended   : " + groupMembers.count("{state : 'suspended' }")+ "\n");
        w.write("\n");
        w.write("Members role none           : " + groupMembers.count("{role : 'none' }")+ "\n");
        w.write("Members role admin          : " + groupMembers.count("{role : 'admin' }")+ "\n");
        w.write("Members role member         : " + groupMembers.count("{role : 'member' }")+ "\n");
        w.write("Members role nearbyMember   : " + groupMembers.count("{role : 'nearbyMember' }")+ "\n");
        w.write("Members role worldwideMember: " + groupMembers.count("{role : 'worldwideMember' }")+ "\n");
        w.write("\n");
        w.write("Members notifications disabled: " + groupMembers.count("{notificationPreference : 'disabled' }")+ "\n");
        w.write("\n");

        MongoCollection relationships = getCollection("relationship");
        w.write("Relationships Total          : " + relationships.count()+ "\n");
        w.write("Relationships state none     : " + relationships.count("{state : 'none' }")+ "\n");
        w.write("Relationships state invited  : " + relationships.count("{state : 'invited' }")+ "\n");
        w.write("Relationships state invitedMe: " + relationships.count("{state : 'invitedMe' }")+ "\n");
        w.write("Relationships state friend   : " + relationships.count("{state : 'friend' }")+ "\n");
        w.write("Relationships state blocked  : " + relationships.count("{state : 'blocked' }")+ "\n");
        w.write("\n");
        w.write("Relationships notifications disabled: " + relationships.count("{notificationPreference : 'disabled' }")+ "\n");
        w.write("\n");

        MongoCollection messages = getCollection("message");
        w.write("Messages Total          : " + messages.count()+ "\n");
        w.write("Messages w. attachment  : " + messages.count("{attachment : {$exists : true}}")+ "\n");
        w.write("Messages w. attachment upload started  : " + messages.count("{attachmentUploadStarted : {$exists : true}}")+ "\n");
        w.write("Messages w. attachment upload finished : " + messages.count("{attachmentUploadFinished : {$exists : true}}")+ "\n");
        w.write("Messages w. upload started & finished  : " + messages.count("{attachmentUploadStarted : {$exists : true}, attachmentUploadFinished : {$exists : true}}")+ "\n");
        w.write("Messages w. upload not started but finished: " + messages.count("{attachmentUploadStarted : {$exists : false}, attachmentUploadFinished : {$exists : true}}")+ "\n");
        w.write("Messages w. upload started & not finished  : " + messages.count("{attachmentUploadStarted : {$exists : true}, attachmentUploadFinished : {$exists : false}}")+ "\n");
        w.write("\n");

        MongoCollection deliveries = getCollection("delivery");
        w.write("Deliveries Total         : " + deliveries.count()+ "\n");
        List<String> deliveryStates = deliveries.distinct("state").as(String.class);
        for (String deliveryState : deliveryStates) {
            w.write("Deliveries in state "+String.format("%-28s" , deliveryState)+" : " + deliveries.count("{ state: # }", deliveryState)+ "\n");
        }
        w.write("\n");
        List<String> attachmentDeliveryStates = deliveries.distinct("attachmentState").as(String.class);
        for (String attachmentDeliveryState : attachmentDeliveryStates) {
            w.write("Deliveries in attachment state "+String.format("%-28s" , attachmentDeliveryState)+" : " + deliveries.count("{ attachmentState: # }", attachmentDeliveryState)+ "\n");
        }
        w.write("\n");

        MongoCollection tokens = getCollection("token");
        w.write("Tokens Total: " + tokens.count()+ "\n");
        w.write("\n");

        MongoCollection keys = getCollection("key");
        w.write("Keys Total: " + keys.count()+ "\n");
        w.write("\n");

        MongoCollection clientHostInfo = getCollection("clientHostInfo");
        List<String> systemNames = clientHostInfo.distinct("systemName").as(String.class);
        for (String systemName : systemNames) {
            w.write("Client system "+String.format("%-10s" , systemName)+" : " + clientHostInfo.count("{ systemName: # }", systemName)+ "\n");
        }
        w.write("\n");

        printActive(w,clientHostInfo,systemNames,1);
        printActive(w,clientHostInfo,systemNames,7);
        printActive(w,clientHostInfo,systemNames,30);
        printActive(w,clientHostInfo,systemNames,90);
        printActive(w,clientHostInfo,systemNames,365);
        w.write("\n");

        printRegistered(w,client,1);
        printRegistered(w,client,7);
        printRegistered(w,client,30);
        printRegistered(w,client,90);
        printRegistered(w,client,365);
        w.write("\n");

        printDeleted(w,client,1);
        printDeleted(w,client,7);
        printDeleted(w,client,30);
        printDeleted(w,client,90);
        printDeleted(w,client,365);
        w.write("\n");

        long day = 24 * 60 * 60 * 1000;
        long sinceDaysAgo = 30;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);

        //-------------------------------
        List<String> clientNames = clientHostInfo.distinct("clientName").query("{ serverTime: {$gt: # } }", since).as(String.class);
        {
            Map<String, Long> clientsByName = new HashMap<String, Long>();
            for (String clientName : clientNames) {
                long count = clientHostInfo.count("{ clientName: #, serverTime: {$gt: # } }", clientName, since);
                clientsByName.put(clientName, count);
            }
            Map<String, Long> clientsByNameSorted = sortByValue(clientsByName);
            w.write("Clients seen in the last " + sinceDaysAgo + " days since " + since + "\n");
            for (String namedClient : clientsByNameSorted.keySet()) {
                w.write(String.format("%-33s", namedClient) + " : " + clientsByNameSorted.get(namedClient) + "\n");
            }
            w.write("\n");
        }

        //-------------------------------
        {
            Map<String, Long> clients = new HashMap<String, Long>();
            for (String clientName : clientNames) {
                List<String> versions = clientHostInfo.distinct("clientVersion").query("{ clientName: #, serverTime: {$gt: # } }", clientName, since).as(String.class);
                for (String version : versions) {
                    long count = clientHostInfo.count("{ clientName: #, serverTime: {$gt: # }, clientVersion: # }", clientName, since, version);
                    clients.put(clientName + "-" + version, count);
                }
            }

            w.write("Clients and versions seen in the last " + sinceDaysAgo + " days since " + since + "\n");
            Map<String, Long> sortedClients = sortByValue(clients);
            for (String fullClient : sortedClients.keySet()) {
                w.write(String.format("%-38s", fullClient) + " : " + sortedClients.get(fullClient) + "\n");
            }
            w.write("\n");
        }
        //-------------------------------
        {
            Map<String, Long> systemsByVersion = new HashMap<String, Long>();
            for (String systemName : systemNames) {
                List<String> versions = clientHostInfo.distinct("systemVersion").query("{ systemName: #, serverTime: {$gt: # } }", systemName, since).as(String.class);
                for (String systemVersion : versions) {
                    long count = clientHostInfo.count("{ systemName: #, serverTime: {$gt: # }, systemVersion: # }", systemName, since, systemVersion);
                    systemsByVersion.put(systemName + " " + systemVersion, count);
                }
            }

            w.write("Systems and versions seen in the last " + sinceDaysAgo + " days since " + since + "\n");
            Map<String, Long> sorted = sortByValue(systemsByVersion);
            for (String key : sorted.keySet()) {
                w.write(String.format("%-20s", key) + " : " + sorted.get(key) + "\n");
            }
            w.write("\n");
        }
        //-------------------------------
        {
            Map<String, Long> systemsByLanguage = new HashMap<String, Long>();
            for (String systemName : systemNames) {
                List<String> languages = clientHostInfo.distinct("systemLanguage").query("{ systemName: #, serverTime: {$gt: # } }", systemName, since).as(String.class);
                for (String systemLanguage : languages) {
                    long count = clientHostInfo.count("{ systemName: #, serverTime: {$gt: # }, systemLanguage: # }", systemName, since, systemLanguage);
                    systemsByLanguage.put(systemName + "/" + systemLanguage, count);
                }
            }

            w.write("Systems and languages seen in the last " + sinceDaysAgo + " days since " + since + "\n");
            Map<String, Long> sorted = sortByValue(systemsByLanguage);
            for (String key : sorted.keySet()) {
                w.write(String.format("%-20s", key) + " : " + sorted.get(key) + "\n");
            }
            w.write("\n");
        }
        //-------------------------------

        w.close();
    }

    void printActive(OutputStreamWriter w, MongoCollection clientHostInfo, List<String> systemNames, int sinceDaysAgo) throws ServletException, IOException{
        long day = 24 * 60 * 60 * 1000;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);
        w.write("\n");
        w.write("Active since "+sinceDaysAgo+" days (since "+since+"):\n");
        w.write("Total : " + clientHostInfo.count("{serverTime: {$gt: # } }", since)+ "\n");
        for (String systemName : systemNames) {
            // w.write("Client system "+systemName+" : " + clientHostInfo.count("{ systemName: #, serverTime: {$gt: # } }", systemName, since)+ "\n");
            w.write("Client system "+String.format("%-10s" , systemName)+" : " + clientHostInfo.count("{ systemName: #, serverTime: {$gt: # } }", systemName, since)+ "\n");
        }
    }
    void printRegistered(OutputStreamWriter w, MongoCollection client, int sinceDaysAgo) throws ServletException, IOException{
        long day = 24 * 60 * 60 * 1000;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);
        w.write("\n");
        w.write("Registered last "+sinceDaysAgo+" days (since "+since+"):\n");
        w.write("Total : " + client.count("{timeRegistered: {$gt: # } }", since)+ "\n");
    }
    void printDeleted(OutputStreamWriter w, MongoCollection client, int sinceDaysAgo) throws ServletException, IOException{
        long day = 24 * 60 * 60 * 1000;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);
        w.write("\n");
        w.write("Deleted last "+sinceDaysAgo+" days (since "+since+"):\n");
        w.write("Total : " + client.count("{ timeDeleted: {$gt: # } }", since)+ "\n");
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
