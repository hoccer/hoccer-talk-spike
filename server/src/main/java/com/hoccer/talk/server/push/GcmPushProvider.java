package com.hoccer.talk.server.push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class GcmPushProvider extends PushProvider {
    private static final Logger LOG = Logger.getLogger(GcmPushProvider.class);

    private static final Predicate<TalkClient> SUPPORTED_CLIENT_PREDICATE = new Predicate<TalkClient>() {
        @Override
        public boolean evaluate(TalkClient client) {
            return client.isGcmCapable();
        }
    };

    private final Sender mSender;

    public GcmPushProvider(Sender sender) {
        mSender = sender;
    }

    @Override
    protected Predicate<TalkClient> getSupportedClientPredicate() {
        return SUPPORTED_CLIENT_PREDICATE;
    }

    @Override
    protected void doPushMessage(Collection<TalkClient> clients, String message) {
        MultiValueMap<String, String> map = new MultiValueMap();

        for (TalkClient client : clients) {
            map.put(client.getGcmPackage(), client.getGcmRegistration());
        }

        for (String gcmPackage : map.keySet()) {
            Collection<String> registrations = map.getCollection(gcmPackage);
            Collection<String> uniqueRegistrations = getUniqueRegistrations(gcmPackage, registrations);

            LOG.info("sending message to " + uniqueRegistrations.size() + " unique GCM registrations");

            doPushMessage(gcmPackage, uniqueRegistrations, message);
        }
    }

    private Collection<String> getUniqueRegistrations(String gcmPackage, Collection<String> registrations) {
        Message gcmMessage = new Message.Builder()
                .restrictedPackageName(gcmPackage)
                .dryRun(true)
                .build();

        try {
            ArrayList<String> registrationsList = new ArrayList<String>(registrations);
            MulticastResult multicastResult = mSender.send(gcmMessage, registrationsList, 10);
            List<Result> results = multicastResult.getResults();
            HashSet<String> uniqueRegistrations = new HashSet<String>();

            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).getCanonicalRegistrationId() != null) {
                    uniqueRegistrations.add(results.get(i).getCanonicalRegistrationId());
                } else {
                    uniqueRegistrations.add(registrationsList.get(i));
                }
            }

            return uniqueRegistrations;
        } catch (IOException e) {
            LOG.error("GCM multicast error", e);
        }

        return CollectionUtils.emptyCollection();
    }

    private void doPushMessage(String gcmPackage, Collection<String> uniqueGcmRegistrations, String message) {
        Message gcmMessage = new Message.Builder()
                .restrictedPackageName(gcmPackage)
                .addData("message", message)
                .build();

        try {
            MulticastResult res = mSender.send(gcmMessage, new ArrayList<String>(uniqueGcmRegistrations), 10);

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
