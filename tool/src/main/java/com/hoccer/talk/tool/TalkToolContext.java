package com.hoccer.talk.tool;

import better.cli.CLIContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.TalkClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.client.TalkToolClient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class TalkToolContext extends CLIContext {

    ObjectMapper mMapper;

    ScheduledExecutorService mExecutor;

    AtomicInteger mClientIdCounter;

    List<TalkToolClient> mClients;

    Hashtable<Integer, TalkToolClient> mClientsById;

    List<TalkToolClient> mSelectedClients;

    public TalkToolContext(TalkTool app) {
        super(app);
        mMapper = new ObjectMapper();
        mExecutor = Executors.newScheduledThreadPool(8);
        mClientIdCounter = new AtomicInteger(0);
        mClients = new Vector<TalkToolClient>();
        mClientsById = new Hashtable<Integer, TalkToolClient>();
        mSelectedClients = new Vector<TalkToolClient>();
    }

    public ScheduledExecutorService getExecutor() {
        return mExecutor;
    }

    public ObjectMapper getMapper() {
        return mMapper;
    }

    public List<TalkToolClient> getClients() {
        return new Vector<TalkToolClient>(mClients);
    }

    public List<TalkToolClient> getSelectedClients() {
        return new Vector<TalkToolClient>(mSelectedClients);
    }

    public void setSelectedClients(List<TalkToolClient> selectedClients) {
        this.mSelectedClients = new Vector<TalkToolClient>(selectedClients);
    }

    public void addClient(TalkToolClient client) {
        mClients.add(client);
        mClientsById.put(client.getId(), client);
        client.initialize();
    }

    public List<TalkToolClient> getClientsBySelectors(List<String> selectors) {
        ArrayList<TalkToolClient> clients = new ArrayList<TalkToolClient>(selectors.size());
        for(int i = 0; i < selectors.size(); i++) {
            String name = selectors.get(i);
            TalkToolClient client = getClientBySelector(name);
            clients.add(i, client);
        }
        return clients;
    }

    public TalkToolClient getClientBySelector(String selector) {
        TalkToolClient client = null;
        try {
            int id = Integer.parseInt(selector);
            client = getClientById(id);
        } catch (NumberFormatException e) {
        }
        if(client == null) {
            client = getClientByClientId(selector);
        }
        return client;
    }

    public TalkToolClient getClientById(int id) {
        return mClientsById.get(id);
    }

    public TalkToolClient getClientByClientId(String clientId) {
        for(TalkToolClient client: mClients) {
            String id = client.getClientId();
            if(id != null && id.equals(clientId)) {
                return client;
            }
        }
        return null;
    }

    public int generateId() {
        return mClientIdCounter.incrementAndGet();
    }

}
