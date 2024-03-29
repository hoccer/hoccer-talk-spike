package com.hoccer.talk.server.push;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;

public class ApnsPushProvider extends PushProvider {
    private static final Logger LOG = Logger.getLogger(ApnsPushProvider.class);

    private static final Predicate<TalkClient> SUPPORTED_CLIENT_PREDICATE = new Predicate<TalkClient>() {
        @Override
        public boolean evaluate(TalkClient client) {
            return client.isApnsCapable();
        }
    };

    private TalkServer mServer;
    private String mDefaultClientName;
    private HashMap<Target, ApnsService> mApnsServices;

    public ApnsPushProvider(HashMap<Target, ApnsService> apnsServices, TalkServer server, String defaultClientName) {
        mApnsServices = apnsServices;
        mServer = server;
        mDefaultClientName = defaultClientName;
    }

    @Override
    protected Predicate<TalkClient> getSupportedClientPredicate() {
        return SUPPORTED_CLIENT_PREDICATE;
    }

    @Override
    protected void doPushMessage(Collection<TalkClient> clients, String message) {
        MultiValueMap<Target, String> map = new MultiValueMap<Target, String>();

        for (TalkClient client : clients) {
            if (isConnected(client)) {
                mServer.getUpdateAgent().requestUserAlert(client.getClientId(), message);
            } else {
                client.setPushAlertMessage(message);
                mServer.getDatabase().saveClient(client);

                Target target = getTarget(client);
                map.put(target, client.getApnsToken());
            }
        }

        for (Target target : map.keySet()) {
            ApnsService apnsService = mApnsServices.get(target);

            if (apnsService != null) {
                doPushMessage(message, apnsService, map.getCollection(target));
            } else {
                LOG.error("No APNS service configured for clientName " + target.clientName + ", type " + target.type);
            }
        }
    }

    private boolean isConnected(TalkClient client) {
        TalkRpcConnection connection = mServer.getClientConnection(client.getClientId());
        return connection != null && connection.isConnected();
    }

    public static PushAgent.APNS_SERVICE_TYPE apnsServiceType(TalkClient client, TalkClientHostInfo hostInfo) {
        PushAgent.APNS_SERVICE_TYPE type = PushAgent.APNS_SERVICE_TYPE.PRODUCTION;
        String clientName = "unknown";
        if (hostInfo != null) {
            clientName = hostInfo.getClientName();

            if ("production".equals(hostInfo.getEnvironment())) {
                type = PushAgent.APNS_SERVICE_TYPE.PRODUCTION;
            } else if ("development".equals(hostInfo.getEnvironment())) {
                type = PushAgent.APNS_SERVICE_TYPE.SANDBOX;
            } else if ("debug".equals(hostInfo.getClientBuildVariant())) {
                type = PushAgent.APNS_SERVICE_TYPE.SANDBOX;
            }
            LOG.debug("APNS target based environment for client = "+client.getClientId()+" is "+ hostInfo.getEnvironment() + ", build variant is "+hostInfo.getClientBuildVariant());
        }
        LOG.debug("APNS target for client "+client.getClientId()+" name "+ clientName + " is "+type);
        return type;
    }

    private Target getTarget(TalkClient client) {
        TalkClientHostInfo hostInfo = mServer.getDatabase().findClientHostInfoForClient(client.getClientId());

        String clientName = mDefaultClientName;
        PushAgent.APNS_SERVICE_TYPE type = apnsServiceType(client, hostInfo);

        if (hostInfo != null) {
            clientName = hostInfo.getClientName();
        }
        return new Target(clientName, type);
    }

    private void doPushMessage(String message, ApnsService apnsService, Collection<String> apnsTokens) {
        String payload = APNS.newPayload()
                .alertBody(message)
                .sound("default")
                .build();

        apnsService.push(apnsTokens, payload);
    }


    static class Target {
        public String clientName;
        public PushAgent.APNS_SERVICE_TYPE type;

        public Target(@NotNull String clientName, @NotNull PushAgent.APNS_SERVICE_TYPE type) {
            this.clientName = clientName;
            this.type = type;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            Target target = (Target) other;

            return new EqualsBuilder()
                    .append(clientName, target.clientName)
                    .append(type, target.type)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(clientName)
                    .append(type)
                    .toHashCode();
        }
    }
}
