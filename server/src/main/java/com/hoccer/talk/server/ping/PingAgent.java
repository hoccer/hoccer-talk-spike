package com.hoccer.talk.server.ping;

import better.jsonrpc.client.JsonRpcClientDisconnect;
import better.jsonrpc.client.JsonRpcClientTimeout;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hoccer.talk.rpc.ITalkRpcClient;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import com.hoccer.talk.util.NamedThreadFactory;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ping measurement agent
 * <p/>
 * This gets kicked on login and reports the round-trip call latency
 * from the server to the client and back.
 * <p/>
 * It is intended purely for monitoring.
 */
public class PingAgent {

    private static final Logger LOG = Logger.getLogger(PingAgent.class);

    private final TalkServer mServer;

    private final ScheduledExecutorService mExecutor;

    private final AtomicInteger mPingRequests = new AtomicInteger();
    private final AtomicInteger mPingAttempts = new AtomicInteger();

    private final AtomicInteger mPingFailures = new AtomicInteger();
    private final AtomicInteger mPingSuccesses = new AtomicInteger();
    private final TalkServerConfiguration mConfig;

    private Timer mPingLatency;


    public PingAgent(TalkServer server) {
        mServer = server;
        mConfig = mServer.getConfiguration();
        mExecutor = Executors.newScheduledThreadPool(
                mConfig.getPingAgentThreadPoolSize(),
                new NamedThreadFactory("ping-agent")
        );
        initializeMetrics(mServer.getMetrics());

        if (mConfig.getPerformPingAtInterval()) {
            schedulePingAllReadyClients();
        } else {
            LOG.warn("Not scheduling regular ping since it is deactivated by configuration.");
        }
    }

    private void schedulePingAllReadyClients() {
        LOG.info("Scheduling pinging of all ready clients to occur in '" + mConfig.getPingInterval() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    pingReadyClients();
                    disconnectStaleClients();
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
                schedulePingAllReadyClients();
            }
        }, mConfig.getPingInterval(), TimeUnit.SECONDS);
    }

    private void initializeMetrics(MetricRegistry metrics) {
        metrics.register(MetricRegistry.name(PingAgent.class, "pingRequests"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPingRequests.intValue();
                    }
                }
        );
        metrics.register(MetricRegistry.name(PingAgent.class, "pingAttempts"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPingAttempts.intValue();
                    }
                }
        );
        metrics.register(MetricRegistry.name(PingAgent.class, "pingFailures"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPingFailures.intValue();
                    }
                }
        );
        metrics.register(MetricRegistry.name(PingAgent.class, "pingSuccesses"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mPingSuccesses.intValue();
                    }
                }
        );
        mPingLatency = metrics.timer(MetricRegistry.name(PingAgent.class, "latency"));
    }

    public void requestPing(final String clientId) {
        mPingRequests.incrementAndGet();
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    performPing(clientId);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void performPing(String clientId) {
        if (clientId == null) {
            return;
        }
        TalkRpcConnection conn = mServer.getClientConnection(clientId);
        if (conn != null) {
            Date intervalDate = new Date(new Date().getTime()-10*1000); // do not ping if already pinged in the last 10 sec.
            if (conn.getLastPingOccured() == null || conn.getLastPingOccured().before(intervalDate)) {
                LOG.info("pinging client: '" + conn.getConnectionId() + "' (clientId: '" + clientId + "')");

                ITalkRpcClient rpc = conn.getClientRpc();
                mPingAttempts.incrementAndGet();
                Timer.Context timer = mPingLatency.time();
                try {
                    rpc.ping();
                    long elapsed = (timer.stop() / 1000000);
                    conn.setLastPingOccured(new Date());
                    conn.setLastPingLatency(elapsed);
                    LOG.info("ping on " + clientId + " took " + elapsed + " msecs, [id:"+conn.getConnectionId()+"]");
                    mPingSuccesses.incrementAndGet();
                } catch (JsonRpcClientDisconnect e) {
                    LOG.info("ping on " + clientId + " disconnect [id:"+conn.getConnectionId()+"]");
                    mPingFailures.incrementAndGet();
                } catch (JsonRpcClientTimeout e) {
                    LOG.info("ping on " + clientId + " timeout [id:"+conn.getConnectionId()+"]");
                    mPingFailures.incrementAndGet();
                } catch (Throwable t) {
                    LOG.error("exception in ping on " + clientId+" [id:"+conn.getConnectionId()+"] ", t);
                    mPingFailures.incrementAndGet();
                }
            } else {
                LOG.info("has been pinged recently, not pinging client: '" + conn.getConnectionId() + "' (clientId: '" + clientId + "')");
            }
        }
    }

    private void pingReadyClients() {
        Vector<TalkRpcConnection> pingConnections = mServer.getPingConnections();
        LOG.info("pingReadyClients checking for ping on " + pingConnections.size() + " pingable connections");
        for (TalkRpcConnection connection : pingConnections) {
            String clientId = connection.getClientId();
            if (clientId != null) {
                Date idleTimeoutDate = new Date(new Date().getTime() - mConfig.getPingIdleTimeoutInterval()*1000);
                if (connection.getLastRequestFinished() != null && connection.getLastRequestFinished().before(idleTimeoutDate)) {
                    LOG.info("disconnecting idle client: '" + connection.getConnectionId() + "' (clientId: '" + clientId + "')");
                    connection.disconnect();
                } else {
                    LOG.trace("pinging ready client: '" + connection.getConnectionId() + "' (clientId: '" + clientId + "')");
                    requestPing(clientId);
                }
            }
        }
    }
    private void disconnectStaleClients() {
        Vector<TalkRpcConnection> staleConnections = mServer.getStaleConnections();
        LOG.info("disconnectStaleClients checking " + staleConnections.size() + " connections");
        for (TalkRpcConnection connection : staleConnections) {
            String clientId = connection.getClientId();
            if (clientId == null && !connection.isLoggedInFlag()) {
                LOG.info("disconnecting stale connection: '" + connection.getConnectionId() + "'");
                connection.disconnect();
            }
        }
    }
}
