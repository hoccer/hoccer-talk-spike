package com.hoccer.talk.tool;

import better.cli.CLIContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.tool.client.TalkToolClient;

import java.util.ArrayList;
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

    public TalkToolContext(TalkTool app) {
        super(app);
        mMapper = new ObjectMapper();
        mExecutor = Executors.newScheduledThreadPool(8);
        mClientIdCounter = new AtomicInteger(0);
        mClients = new Vector<TalkToolClient>();
    }

    public ScheduledExecutorService getExecutor() {
        return mExecutor;
    }

    public ObjectMapper getMapper() {
        return mMapper;
    }

    public List<TalkToolClient> getClients() {
        return new ArrayList<TalkToolClient>(mClients);
    }

    public void addClient(TalkToolClient client) {
        mClients.add(client);
        client.initialize();
    }

    public int generateId() {
        return mClientIdCounter.incrementAndGet();
    }

}
