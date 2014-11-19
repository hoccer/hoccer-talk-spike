package com.hoccer.talk.server.push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
            try {
                Collection<String> registrations = map.getCollection(gcmPackage);
                Collection<String> uniqueRegistrations = getUniqueRegistrations(gcmPackage, registrations);

                LOG.info("sending message to " + uniqueRegistrations.size() + " unique GCM registrations");

                doPushMessage(gcmPackage, uniqueRegistrations, message);
            } catch (IOException e) {
                LOG.error("Failed to send push message for package " + gcmPackage, e);
            }
        }
    }

    private Collection<String> getUniqueRegistrations(String gcmPackage, Collection<String> registrations) throws IOException {
        Message gcmMessage = new Message.Builder()
                .restrictedPackageName(gcmPackage)
                .dryRun(true)
                .build();

        ArrayList<String> registrationsList = new ArrayList<String>(registrations);
        List<Result> results = send(registrationsList, gcmMessage);
        HashSet<String> uniqueRegistrations = new HashSet<String>();

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getCanonicalRegistrationId() != null) {
                uniqueRegistrations.add(results.get(i).getCanonicalRegistrationId());
            } else {
                uniqueRegistrations.add(registrationsList.get(i));
            }
        }

        return uniqueRegistrations;
    }

    private void doPushMessage(String gcmPackage, Collection<String> uniqueGcmRegistrations, String message) throws IOException {
        Message gcmMessage = new Message.Builder()
                .restrictedPackageName(gcmPackage)
                .addData("message", message)
                .build();

        List<Result> results = send(uniqueGcmRegistrations, gcmMessage);

        int failures = 0;
        int canonicalIds = 0;

        for (Result result : results) {
            if (result.getMessageId() != null) {
                if (result.getCanonicalRegistrationId() != null) {
                    canonicalIds += 1;
                }
            } else {
                failures += 1;
            }
        }

        LOG.error("GCM multicast returned " + failures + " failures");
        LOG.info("GCM multicast returned " + canonicalIds + " canonical ids");
    }

    private List<Result> send(Collection<String> registrations, Message gcmMessage) throws IOException {
        List<List<String>> badgedRegistrations = ListUtils.partition(new ArrayList<String>(registrations), 1000);
        ArrayList<Result> results = new ArrayList<Result>();

        LOG.debug("Sending push messages in " + badgedRegistrations.size() + " badge(s)");

        for (List<String> badge : badgedRegistrations) {
            MulticastResult badgeResult = mSender.send(gcmMessage, badge, 10);
            results.addAll(badgeResult.getResults());

            LOG.debug("Sent badge (" +
                    badgeResult.getSuccess() + " success, " +
                    badgeResult.getFailure() + " failure, " +
                    badgeResult.getCanonicalIds() + " canonical id(s))");
        }

        return results;
    }
}
