package com.hoccer.talk.server.push;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.util.NamedThreadFactory;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent for push notifications
 */
public class PushAgent {

    private static final Logger LOG = Logger.getLogger(PushAgent.class);

    private final ScheduledExecutorService mExecutor;

    private final TalkServer mServer;
    private final TalkServerConfiguration mConfig;
    private final ITalkServerDatabase mDatabase;

    private Sender mGcmSender;

    public enum APNS_SERVICE_TYPE {
        PRODUCTION, SANDBOX
    }

    private final HashMap<String, HashMap<APNS_SERVICE_TYPE, ApnsService>> mApnsServices = new HashMap<String, HashMap<APNS_SERVICE_TYPE, ApnsService>>();

    private final Hashtable<String, PushRequest> mOutstanding;

    private final AtomicInteger mPushRequests = new AtomicInteger();
    private final AtomicInteger mPushDelayed = new AtomicInteger();
    private final AtomicInteger mPushIncapable = new AtomicInteger();
    private final AtomicInteger mPushBatched = new AtomicInteger();

    public PushAgent(TalkServer server) {
        mExecutor = Executors.newScheduledThreadPool(
                server.getConfiguration().getPushAgentThreadPoolSize(),
                new NamedThreadFactory("push-agent")
        );
        mServer = server;
        mDatabase = mServer.getDatabase();
        mConfig = mServer.getConfiguration();
        mOutstanding = new Hashtable<String, PushRequest>();
        if (mConfig.isGcmEnabled()) {
            initializeGcm();
        }
        if (mConfig.isApnsEnabled()) {
            initializeApns();
        }
        initializeMetrics(mServer.getMetrics());
    }

    private void initializeMetrics(MetricRegistry metrics) {
        metrics.register(MetricRegistry.name(PushAgent.class, "pushRequests"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPushRequests.intValue();
                    }
                });
        metrics.register(MetricRegistry.name(PushAgent.class, "pushIncapable"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPushIncapable.intValue();
                    }
                });
        metrics.register(MetricRegistry.name(PushAgent.class, "pushDelayed"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPushDelayed.intValue();
                    }
                });
        metrics.register(MetricRegistry.name(PushAgent.class, "pushBatched"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPushBatched.intValue();
                    }
                });
    }

    public void submitSystemMessage(final TalkClient client, final String message) {
        LOG.info("submitSystemMessage -> clientId: '" + client.getClientId() + "' message: '" + message + "'");
        final PushMessage pushMessage = new PushMessage(this, client, mDatabase.findClientHostInfoForClient(client.getClientId()), message);
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.info(" -> initializing Message Push for clientId: '" + client.getClientId() +"'!");
                    pushMessage.perform();
                    LOG.info(" -> Message Push done for clientId: '" + client.getClientId() +"'!");
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    public void submitRequest(TalkClient client) {
        LOG.debug("submitRequest for client: " + client.getClientId() + ", lastPushMessage=" + client.getLastPushMessage());
        long now = System.currentTimeMillis();

        mPushRequests.incrementAndGet();

        // bail if no push
        if (!client.isPushCapable()) {
            mPushIncapable.incrementAndGet();
            return;
        }

        // limit push rate
        Date lastPush = client.getTimeLastPush();
        if (lastPush == null) {
            lastPush = new Date();
        }
        long delta = Math.max(0, now - lastPush.getTime());
        long delay = 0;
        int limit = mConfig.getPushRateLimit();
        if (delta < limit) {
            mPushDelayed.incrementAndGet();
            delay = Math.max(0, limit - delta);
        }

        // update timestamp
        client.setTimeLastPush(new Date());
        mDatabase.saveClient(client);

        // only perform push when we aren't doing so already
        final String clientId = client.getClientId();
        synchronized (mOutstanding) {
            if (mOutstanding.containsKey(clientId)) {
                // request has been batched
                mPushBatched.incrementAndGet();
            } else {
                // schedule the request
                final PushRequest request = new PushRequest(this, clientId, mDatabase.findClientHostInfoForClient(client.getClientId()));
                mExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // no longer outstanding
                            synchronized (mOutstanding) {
                                // perform the request
                                request.perform();
                                mOutstanding.remove(clientId);
                            }
                        } catch (Throwable t) {
                            LOG.error("caught and swallowed exception escaping runnable", t);
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS);
                mOutstanding.put(clientId, request);
            }
        }
    }

    public TalkServerConfiguration getConfiguration() {
        return mConfig;
    }

    public ITalkServerDatabase getDatabase() {
        return mDatabase;
    }

    public Sender getGcmSender() {
        return mGcmSender;
    }

    public ApnsService getApnsService(String clientName, APNS_SERVICE_TYPE type) {
        if (mApnsServices.containsKey(clientName)) {
            return mApnsServices.get(clientName).get(type);
        }

        return null;
    }

    private void initializeGcm() {
        LOG.info("GCM support enabled");
        mGcmSender = new Sender(mConfig.getGcmApiKey());
    }

    private void initializeApns() {
        LOG.info("APNS support enabled");

        // set up services
        for (Map.Entry<String, ApnsConfiguration> entry : mConfig.getApnsConfigurations().entrySet()) {
            String clientName = entry.getKey();

            ApnsConfiguration apnsConfiguration = entry.getValue();
            ApnsConfiguration.Certificate productionCertificate = apnsConfiguration.getCertificate(PushAgent.APNS_SERVICE_TYPE.PRODUCTION);
            ApnsConfiguration.Certificate sandboxCertificate = apnsConfiguration.getCertificate(PushAgent.APNS_SERVICE_TYPE.SANDBOX);

            HashMap<APNS_SERVICE_TYPE, ApnsService> services = new HashMap<APNS_SERVICE_TYPE, ApnsService>();

            LOG.info("  * setting up APNS service (clientName: '" + clientName + "', type: '" + APNS_SERVICE_TYPE.PRODUCTION + "')");
            services.put(APNS_SERVICE_TYPE.PRODUCTION, APNS.newService()
                    .withCert(productionCertificate.getPath(), productionCertificate.getPassword())
                    .withProductionDestination()
                    .build());

            LOG.info("  * setting up APNS service (clientName: '" + clientName + "', type: '" + APNS_SERVICE_TYPE.SANDBOX + "')");
            services.put(APNS_SERVICE_TYPE.SANDBOX, APNS.newService()
                    .withCert(sandboxCertificate.getPath(), sandboxCertificate.getPassword())
                    .withSandboxDestination()
                    .build());

            mApnsServices.put(clientName, services);
        }

        // set up invalidation
        int delay = mConfig.getApnsInvalidateDelay();
        int interval = mConfig.getApnsInvalidateInterval();
        if (interval > 0) {
            LOG.info("APNS will check for invalidations every " + interval + " seconds");
            mExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        invalidateApns();
                    } catch (Throwable t) {
                        LOG.error("caught and swallowed exception escaping runnable", t);
                    }
                }
            }, delay, interval, TimeUnit.SECONDS);
        }
    }

    private void invalidateApns() {
        LOG.info("APNS retrieving inactive devices");

        for (Map.Entry<String, HashMap<APNS_SERVICE_TYPE, ApnsService>> clientServices : mApnsServices.entrySet()) {
            String clientName = clientServices.getKey();

            for (Map.Entry<APNS_SERVICE_TYPE, ApnsService> entry : clientServices.getValue().entrySet()) {
                final APNS_SERVICE_TYPE type = entry.getKey();
                final ApnsService service = entry.getValue();

                LOG.info("  * APNS retrieving inactive devices from " + type + " for clientName " + clientName);
                final Map<String, Date> inactive = service.getInactiveDevices();
                if (!inactive.isEmpty()) {
                    LOG.info("  * APNS reports " + inactive.size() + " inactive devices");
                    for (String token : inactive.keySet()) {
                        TalkClient client = mDatabase.findClientByApnsToken(token);
                        if (client == null) {
                            LOG.warn("    * APNS invalidates unknown client (token '" + token + "')");
                        } else {
                            LOG.info("    * APNS client '" + client.getClientId() + "' invalid since '" + inactive.get(token) + "'");
                            client.setApnsToken(null);
                        }
                    }
                }
            }
        }
    }
}
