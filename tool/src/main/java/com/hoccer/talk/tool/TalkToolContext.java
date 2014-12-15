package com.hoccer.talk.tool;

import better.cli.CLIContext;
import better.cli.console.Console;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.HttpClientWithKeyStore;
import com.hoccer.talk.tool.client.TalkToolClient;

import java.io.InputStream;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class TalkToolContext extends CLIContext {

    private static KeyStore KEYSTORE = null;

    TalkTool mApplication;
    ObjectMapper mMapper;
    ScheduledExecutorService mExecutor;
    AtomicInteger mClientIdCounter;
    List<TalkToolClient> mClients;
    Hashtable<Integer, TalkToolClient> mClientsById;
    List<TalkToolClient> mSelectedClients;

    public static KeyStore getKeyStore() {
        if (KEYSTORE == null) {
            throw new RuntimeException("SSL security not initialized");
        }
        return KEYSTORE;
    }

    private static void initializeSsl() {
        try {
            // get the keystore
            KeyStore ks = KeyStore.getInstance("BKS");
            // load keys
            InputStream input = TalkToolContext.class.getClassLoader().getResourceAsStream("ssl_bks");
            try {
                ks.load(input, "password".toCharArray());
            } finally {
                input.close();
            }
            // configure HttpClient
            HttpClientWithKeyStore.initializeSsl(ks);

            KEYSTORE = ks;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TalkToolContext(TalkTool app) {
        super(app);
        Console.debug("- setting up TalkToolContext...");
        mApplication = app;
        mMapper = new ObjectMapper();
        mExecutor = Executors.newScheduledThreadPool(8);
        mClientIdCounter = new AtomicInteger(0);
        mClients = new Vector<TalkToolClient>();
        mClientsById = new Hashtable<Integer, TalkToolClient>();
        mSelectedClients = new Vector<TalkToolClient>();
    }

    public void setupSsl() {
        Console.debug("- setting up ssl...");
        initializeSsl();
    }

    public TalkTool getApplication() {
        return mApplication;
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

    public void addClient(TalkToolClient client) throws SQLException {
        mClients.add(client);
        mClientsById.put(client.getId(), client);
        client.initialize();
    }

    public List<TalkToolClient> getClientsBySelectors(List<String> selectors) {
        ArrayList<TalkToolClient> clients = new ArrayList<TalkToolClient>(selectors.size());
        for (int i = 0; i < selectors.size(); i++) {
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
        if (client == null) {
            client = getClientByClientId(selector);
        }
        return client;
    }

    /**
     * @param selectorOrclientId is either a client in talk tool identified by number (e.g. '2') or
     *                           a clientId
     * @return clientId - An existing talk-tool client number is converted to a clientId.
     * If it does not exist we assume the given String is a UUID and treat it as clientId
     */
    public String getClientIdFromParam(String selectorOrclientId) {
        String clientId = selectorOrclientId;

        final TalkToolClient client = getClientBySelector(selectorOrclientId);
        if (client != null) {
            clientId = client.getClientId();
        }

        // this validates that the evaluated clientId is always a UUID.
        clientId = UUID.fromString(clientId).toString();

        return clientId;
    }

    public TalkToolClient getClientById(int id) {
        return mClientsById.get(id);
    }

    public TalkToolClient getClientByClientId(String clientId) {
        for (TalkToolClient client : mClients) {
            String id = client.getClientId();
            if (id != null && id.equals(clientId)) {
                return client;
            }
        }
        return null;
    }

    public int generateId() {
        return mClientIdCounter.incrementAndGet();
    }

}
