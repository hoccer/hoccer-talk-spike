package com.hoccer.talk.server.push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class GcmPushService extends PushService {
    private static final Logger LOG = Logger.getLogger(GcmPushService.class);

    private static final Predicate<TalkClient> SUPPORTED_CLIENT_PREDICATE = new Predicate<TalkClient>() {
        @Override
        public boolean evaluate(TalkClient client) {
            return client.isGcmCapable();
        }
    };

    private final Sender mSender;

    public GcmPushService(Sender sender) {
        mSender = sender;
    }

    @Override
    protected Predicate<TalkClient> getSupportedClientPredicate() {
        return SUPPORTED_CLIENT_PREDICATE;
    }

    @Override
    protected void doPushMessage(Collection<TalkClient> clients, String message) {
        MultiValueMap<String, String> map = new MultiValueMap<String, String>();

        for (TalkClient client : clients) {
            map.put(client.getGcmPackage(), client.getGcmRegistration());
        }

        for (String gcmPackage : map.keySet()) {
            doPushMessage(message, gcmPackage, map.getCollection(gcmPackage));
        }
    }

    private void doPushMessage(String message, String gcmPackage, Collection<String> gcmRegistrations) {
        Message gcmMessage = new Message.Builder()
                .restrictedPackageName(gcmPackage)
                .addData("message", message)
                .build();

        try {
            MulticastResult res = mSender.send(gcmMessage, new ArrayList<String>(gcmRegistrations), 10);

            if (res.getFailure() > 0) {
                LOG.error("GCM multicast returned " + res.getFailure() + " errors");
            }

            if (res.getCanonicalIds() > 0) {
                LOG.info("GCM multicast returned " + res.getCanonicalIds() + " canonical ids");
            }
        } catch (IOException e) {
            LOG.error("GCM multicast error", e);
        }
    }
}
