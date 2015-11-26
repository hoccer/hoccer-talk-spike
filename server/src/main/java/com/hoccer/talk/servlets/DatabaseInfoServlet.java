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

        w.write("Database Stats:\n\n");

        MongoCollection client = getCollection("client");

        w.write("Clients Total     : " + client.count()+ "\n");
        w.write("Clients APNS      : " + client.count("{apnsToken : {$exists : true}}")+ "\n");
        w.write("Clients GCM       : " + client.count("{gcmRegistration : {$exists : true}}")+ "\n");
        w.write("Clients w/o push  : " + client.count("{apnsToken : {$exists : false}, gcmRegistration : {$exists : false}}")+ "\n");

        MongoCollection clientHostInfo = getCollection("clientHostInfo");

        w.write("\n");

        List<String> systemNames = clientHostInfo.distinct("systemName").as(String.class);
        for (String systemName : systemNames) {
            w.write("Client system "+systemName+" : " + clientHostInfo.count("{ systemName: # }", systemName)+ "\n");
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

        w.close();
    }

    void printActive(OutputStreamWriter w, MongoCollection clientHostInfo, List<String> systemNames, int sinceDaysAgo) throws ServletException, IOException{
        long day = 24 * 60 * 60 *1000;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);
        w.write("\n");
        w.write("Active since "+sinceDaysAgo+" days (since "+since+"):\n");
        w.write("Total : " + clientHostInfo.count("{serverTime: {$gt: # } }", since)+ "\n");
        for (String systemName : systemNames) {
            w.write("Client system "+systemName+" : " + clientHostInfo.count("{ systemName: #, serverTime: {$gt: # } }", systemName, since)+ "\n");
        }
    }
    void printRegistered(OutputStreamWriter w, MongoCollection client, int sinceDaysAgo) throws ServletException, IOException{
        long day = 24 * 60 * 60 *1000;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);
        w.write("\n");
        w.write("Registered last "+sinceDaysAgo+" days (since "+since+"):\n");
        //w.write("Total : " + client.count("{timeRegistered: {$gt: # }, timeDeleted : {$exists : false } }", since)+ "\n");
        w.write("Total : " + client.count("{timeRegistered: {$gt: # } }", since)+ "\n");
    }
    void printDeleted(OutputStreamWriter w, MongoCollection client, int sinceDaysAgo) throws ServletException, IOException{
        long day = 24 * 60 * 60 *1000;
        Date since = new Date(new Date().getTime() - day * sinceDaysAgo);
        w.write("\n");
        w.write("Deleted last "+sinceDaysAgo+" days (since "+since+"):\n");
        w.write("Total : " + client.count("{ timeDeleted: {$gt: # } }", since)+ "\n");
    }


}
