package com.hoccer.webclient.backend.factories;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import org.glassfish.hk2.api.Factory;

public class XoClientDatabaseFactory implements Factory<XoClientDatabase> {

    private XoClient client;

    public XoClientDatabaseFactory(XoClient client) {
        this.client = client;
    }

    @Override
    public XoClientDatabase provide() {
        return client.getDatabase();
    }

    @Override
    public void dispose(XoClientDatabase xoClientDatabase) {

    }
}
