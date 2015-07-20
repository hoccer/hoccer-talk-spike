package com.hoccer.talk.tool;

import better.cli.CLIContext;
import better.cli.console.Console;
import com.hoccer.talk.client.HttpClientWithKeyStore;
import com.hoccer.talk.tool.client.TalkToolClient;

import java.io.InputStream;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TalkToolContext extends CLIContext {

    private static KeyStore KEYSTORE;

    private final TalkTool mApplication;
    private final AtomicInteger mClientIdCounter;
    private final List<TalkToolClient> mClients;
    private final Hashtable<Integer, TalkToolClient> mClientsById;
    private List<TalkToolClient> mSelectedClients;

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
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public TalkToolContext(final TalkTool app) {
        super(app);
        Console.debug("- setting up TalkToolContext...");
        mApplication = app;
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

    public void setSelectedClients(final List<TalkToolClient> selectedClients) {
        this.mSelectedClients = new Vector<TalkToolClient>(selectedClients);
    }

    public void addClient(final TalkToolClient client) throws SQLException {
        mClients.add(client);
        mClientsById.put(client.getId(), client);
        client.initialize();
    }

    public List<TalkToolClient> getClientsBySelectors(final List<String> selectors) {
        ArrayList<TalkToolClient> clients = new ArrayList<TalkToolClient>(selectors.size());
        for (int i = 0; i < selectors.size(); i++) {
            String name = selectors.get(i);
            TalkToolClient client = getClientBySelector(name);
            clients.add(i, client);
        }
        return clients;
    }

    public TalkToolClient getClientBySelector(final String selector) {
        TalkToolClient client = null;
        try {
            int id = Integer.parseInt(selector);
            client = getClientById(id);
        } catch (final NumberFormatException e) {
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
    public String getClientIdFromParam(final String selectorOrclientId) {
        String clientId = selectorOrclientId;

        final TalkToolClient client = getClientBySelector(selectorOrclientId);
        if (client != null) {
            clientId = client.getClientId();
        }

        // this validates that the evaluated clientId is always a UUID.
        clientId = UUID.fromString(clientId).toString();

        return clientId;
    }

    TalkToolClient getClientById(final int id) {
        return mClientsById.get(id);
    }

    TalkToolClient getClientByClientId(final String clientId) {
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
