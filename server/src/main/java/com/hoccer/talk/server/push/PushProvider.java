package com.hoccer.talk.server.push;

import com.hoccer.talk.model.TalkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.log4j.Logger;

import java.util.Collection;

public abstract class PushProvider {

    private static final Logger LOG = Logger.getLogger(PushProvider.class);

    public void pushMessage(Collection<TalkClient> clients, String message) {
        Collection<TalkClient> supportedClients = CollectionUtils.select(clients, getSupportedClientPredicate());
        LOG.info(supportedClients.size() + " clients supported by " + getClass().getName());
        doPushMessage(supportedClients, message);
    }

    protected abstract Predicate<TalkClient> getSupportedClientPredicate();
    protected abstract void doPushMessage(Collection<TalkClient> clients, String message);
}
