package com.hoccer.talk.server;

import better.jsonrpc.server.JsonRpcServer;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.server.cleaning.CleaningAgent;
import com.hoccer.talk.server.database.DatabaseHealthCheck;
import com.hoccer.talk.server.delivery.DeliveryAgent;
import com.hoccer.talk.server.filecache.FilecacheClient;
import com.hoccer.talk.server.ping.PingAgent;
import com.hoccer.talk.server.push.PushAgent;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import com.hoccer.talk.server.update.UpdateAgent;
import com.hoccer.talk.util.CountedSet;
import de.undercouch.bson4jackson.BsonFactory;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * Main object of the Talk server
 * <p/>
 * This holds global state such as the list of active connections,
 * references to common database mapping helpers and so on.
 */
public class TalkServer {

    protected final Logger LOG = Logger.getLogger(getClass());

    /**
     * server-global JSON mapper
     */
    ObjectMapper mJsonMapper;

    /**
     * server-global BSON mapper
     */
    ObjectMapper mBsonMapper;

    /**
     * Metrics registry
     */
    MetricRegistry mMetricsRegistry;
    HealthCheckRegistry mHealthRegistry;
    JmxReporter mJmxReporter;

    /**
     * JSON-RPC server instance
     */
    JsonRpcServer mRpcServer;

    /**
     * Server configuration
     */
    TalkServerConfiguration mConfiguration;

    /**
     * Database accessor
     */
    ITalkServerDatabase mDatabase;

    /**
     * Stats collector
     */
    ITalkServerStatistics mStatistics;

    /**
     * Delivery agent
     */
    DeliveryAgent mDeliveryAgent;

    /**
     * Push service agent
     */
    PushAgent mPushAgent;

    /**
     * Presence update agent
     */
    UpdateAgent mUpdateAgent;

    /**
     * Ping measurement agent
     */
    PingAgent mPingAgent;

    /**
     * Cleaning agent
     */
    CleaningAgent mCleaningAgent;

    /**
     * Client for the filecache control interface
     */
    FilecacheClient mFilecacheClient;

    /**
     * All connections (every connected websocket)
     */
    Vector<TalkRpcConnection> mConnections =
            new Vector<TalkRpcConnection>();
    //Set<TalkRpcConnection> mConnections =
    //        new HashSet<TalkRpcConnection>();

    /**
     * All logged-in connections by client ID
     */
    Hashtable<String, TalkRpcConnection> mConnectionsByClientId =
            new Hashtable<String, TalkRpcConnection>();

    AtomicInteger mConnectionsTotal = new AtomicInteger();
    AtomicInteger mConnectionsOpen = new AtomicInteger();
    AtomicInteger mConnectionsReady = new AtomicInteger();
    AtomicInteger mConnectionsLoggedIn = new AtomicInteger();

    Map<String,String> mIdLocks;

    public class NonReentrantLock{

        private boolean mIsLocked;
        private int mWaiting;
        private CountedSet<String> mWaiterTypes;
        public String mName;

        NonReentrantLock() {
            mIsLocked = false;
            mWaiting = 0;
            mName = "<unnamed>";
            mWaiterTypes = new CountedSet<String>();
        }

        NonReentrantLock(String name) {
            mIsLocked = false;
            mWaiting = 0;
            mName = name;
            mWaiterTypes = new CountedSet<String>();
        }

        public String getName() {
            return mName;
        }

        public boolean isLocked() {
            return mIsLocked;
        }

        public int getWaiting() {
            return mWaiting;
        }

        public synchronized boolean tryLock()  {
            //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" tryLock");
            if (!mIsLocked) {
                mIsLocked = true;
                //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" tryLock : acquired lock");
                return true;
            }
            //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" tryLock : not locking, is already locked");
            return false;
        }

        public synchronized void lock() throws InterruptedException{
            while (mIsLocked) {
                //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" wait");
                ++mWaiting;
                this.wait();
                --mWaiting;
                //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" wakeup");
            }
            mIsLocked = true;
            //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" acquired lock");
        }

        public synchronized int getWaiting(String waiterType) {
            if (mWaiting > 0) {
                return mWaiterTypes.getCount(waiterType);
            } else {
                return 0;
            }
        }

        public synchronized void lock(String waiterType) throws InterruptedException{
            while (mIsLocked) {
                //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" lockWithWaiterType '"+waiterType+"' wait");
                ++mWaiting;
                mWaiterTypes.add(waiterType);
                this.wait();
                mWaiterTypes.remove(waiterType);
                --mWaiting;
                //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" lockWithWaiterType '"+waiterType+"' wakeup");
            }
            mIsLocked = true;
            //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" lockWithWaiterType '"+waiterType+"' acquired lock");
        }

        public synchronized void unlock(){
            mIsLocked = false;
            //System.out.println("NonReentrantLock +"+mName+" Thread " + Thread.currentThread()+" unlock");
            this.notify();
        }
    }

    Map<String,NonReentrantLock> mNonReentrantIdLocks;

    /**
     * Create and initialize a Hoccer Talk server
     */
    public TalkServer(TalkServerConfiguration configuration, ITalkServerDatabase database) {
        mIdLocks = new HashMap<String, String>();
        mNonReentrantIdLocks = new HashMap<String, NonReentrantLock>();

        mConfiguration = configuration;
        mDatabase = database;

        mJsonMapper = createObjectMapper(new JsonFactory());
        mBsonMapper = createObjectMapper(new BsonFactory());

        mMetricsRegistry = new MetricRegistry();
        initializeMetrics();
        mHealthRegistry = new HealthCheckRegistry();
        initializeHealthChecks();
        mStatistics = new TalkMetricStats(mMetricsRegistry);

        mRpcServer = new JsonRpcServer(ITalkRpcServer.class);
        mDeliveryAgent = new DeliveryAgent(this);
        mPushAgent = new PushAgent(this);
        mUpdateAgent = new UpdateAgent(this);
        mPingAgent = new PingAgent(this);
        mCleaningAgent = new CleaningAgent(this);
        mFilecacheClient = new FilecacheClient(this.getConfiguration());

        // For instrumenting metrics via JMX
        /*
        mJmxReporter = JmxReporter.forRegistry(mMetricsRegistry).build();
        mJmxReporter.start();
        */
    }

    public NonReentrantLock idLockNonReentrant(String id) {
        synchronized (mNonReentrantIdLocks) {
            NonReentrantLock lock = mNonReentrantIdLocks.get(id);
            if (lock == null) {
                lock = new NonReentrantLock(id);
                mNonReentrantIdLocks.put(id, lock);
            }
            return lock;
        }
    }

    public String idLock(String id) {
        synchronized (mIdLocks) {
            String lock = mIdLocks.get(id);
            if (lock == null) {
                lock = new String(id);
                mIdLocks.put(id, lock);
            }
            return lock;
        }
    }

    // lock for two Ids at the same time; note that the individual id will not be locked that way
    public Object dualIdLock(String prefix, String id1, String id2) {
        if (id1.compareTo(id2) > 0)   {
            return idLock(prefix + id1 + id2);
        } else {
            return idLock(prefix + id2 + id1);
        }
    }

    private void cleanIdLocks() {
        LOG.debug("cleanIdLocks removing locks");

        synchronized (mIdLocks) {
            long count = mIdLocks.size();
            Iterator it = mIdLocks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                synchronized (pair.getValue()) {
                    it.remove();
                }
            }
            LOG.debug("cleanIdLocks removed "+count+" locks");
        }
    }

    private void cleanNonReentrantLocks() {
        LOG.debug("cleanNonReentrantLocks removing locks");

        synchronized (mNonReentrantIdLocks) {
            long count = mNonReentrantIdLocks.size();
            long removed = 0;
            Iterator it = mNonReentrantIdLocks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                NonReentrantLock lock =  (NonReentrantLock)pair.getValue();
                if (lock.tryLock()) {
                    it.remove();
                    lock.unlock();
                    ++removed;
                }
            }
            LOG.debug("cleanNonReentrantLocks removed "+removed+" of "+count+" locks");
        }
    }

    public void cleanAllLocks () {
        cleanIdLocks();
        cleanNonReentrantLocks();
    }

    /**
     * @return the JSON mapper used by this server
     */
    public ObjectMapper getJsonMapper() {
        return mJsonMapper;
    }

    /**
     * @return the BSON mapper used by this server
     */
    public ObjectMapper getBsonMapper() {
        return mBsonMapper;
    }

    /**
     * @return the metrics registry for the server
     */
    public MetricRegistry getMetrics() {
        return mMetricsRegistry;
    }

    /**
     * @return the JSON-RPC server
     */
    public JsonRpcServer getRpcServer() {
        return mRpcServer;
    }

    /**
     * @return the configuration of this server
     */
    public TalkServerConfiguration getConfiguration() {
        return mConfiguration;
    }

    /**
     * @return the database accessor of this server
     */
    public ITalkServerDatabase getDatabase() {
        return mDatabase;
    }

    /**
     * @return the stats collector for this server
     */
    public ITalkServerStatistics getStatistics() {
        return mStatistics;
    }

    /**
     * @return the push agent of this server
     */
    public PushAgent getPushAgent() {
        return mPushAgent;
    }

    /**
     * @return the delivery agent of this server
     */
    public DeliveryAgent getDeliveryAgent() {
        return mDeliveryAgent;
    }

    /**
     * @return the update agent of this server
     */
    public UpdateAgent getUpdateAgent() {
        return mUpdateAgent;
    }

    /**
     * @return the ping agent of this server
     */
    public PingAgent getPingAgent() {
        return mPingAgent;
    }

    /**
     * @return the cleaning agent
     */
    public CleaningAgent getCleaningAgent() {
        return mCleaningAgent;
    }

    /**
     * @return the filecache control client
     */
    public FilecacheClient getFilecacheClient() {
        return mFilecacheClient;
    }

    /**
     * Check if the given client is connected
     *
     * @param clientId of the client to check for
     * @return true if the client is connected
     */
    public boolean isClientConnected(String clientId) {
        return getClientConnection(clientId) != null;
    }

    /**
     * Check if the given client is connected
     *
     * @param clientId of the client to check for
     * @return true if the client is connected
     */
    public boolean isClientReady(String clientId) {
        TalkRpcConnection client = getClientConnection(clientId);
        return client != null && client.isReady();
    }

    /**
     * Retrieve the connection of the given client
     *
     * @param clientId of the client to check for
     * @return connection of the client or null
     */
    @Nullable
    public TalkRpcConnection getClientConnection(String clientId) {
        synchronized (mConnectionsByClientId) {
            return mConnectionsByClientId.get(clientId);
        }
    }
    private void addClientConnection(String clientId, TalkRpcConnection connection) {
        synchronized (mConnectionsByClientId) {
            mConnectionsByClientId.put(clientId, connection);
        }
    }
    private void removeClientConnection(String clientId) {
        synchronized (mConnectionsByClientId) {
            if (hasClientConnection(clientId)) {
                mConnectionsByClientId.remove(clientId);
             } else {
                LOG.error("Could not remove connection for clientId "+clientId+", not found");
            }
        }
    }
    public boolean hasClientConnection(String clientId) {
        synchronized (mConnectionsByClientId) {
            return mConnectionsByClientId.containsKey(clientId);
        }
    }
    public int numberOfClientConnections() {
        synchronized (mConnectionsByClientId) {
            return mConnectionsByClientId.size();
        }
    }

    /**
     * Notify the server of a successful login
     *
     * @param client     that was logged in
     * @param connection the client is on
     */
    public void identifyClient(TalkClient client, TalkRpcConnection connection) {
        synchronized (connection) {
            String clientId = client.getClientId();
            TalkRpcConnection oldConnection = getClientConnection(clientId);
            if (oldConnection != null) {
                LOG.warn("identifyClient: old connection with id " + oldConnection.getConnectionId() + " exists for client " + clientId + ", disconnecting old connection, new connection id" + connection.getConnectionId());
                oldConnection.disconnect();
            }
            connection.getServerHandler().destroyEnvironment(TalkEnvironment.TYPE_NEARBY);  // after logon, destroy possibly left over environments
            connection.getServerHandler().releaseEnvironment(TalkEnvironment.TYPE_WORLDWIDE);  // after logon, release possibly left over environments
            addClientConnection(clientId, connection);
            mConnectionsLoggedIn.incrementAndGet();
            LOG.info("[connectionId: '" + connection.getConnectionId() + "'] logged in" +
                    ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                    ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
        }
    }

    /**
     * Notify the server of a ready call
     *
     * @param client     that called ready
     * @param connection the client is on
     */
    public void readyClient(TalkClient client, TalkRpcConnection connection) {
        synchronized (connection) {
            mUpdateAgent.requestPresenceUpdate(client.getClientId(), null);
            mConnectionsReady.incrementAndGet();
            LOG.info("[connectionId: '" + connection.getConnectionId() + "'] ready" +
                    ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                    ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
        }
    }

    /**
     * Register a new connection with the server
     *
     * @param connection to be registered
     */
    public void connectionOpened(TalkRpcConnection connection) {
        synchronized (connection) {
            mConnectionsTotal.incrementAndGet();
            mConnectionsOpen.incrementAndGet();
            mConnections.add(connection);
            LOG.info("[connectionId: '" + connection.getConnectionId() + "'] opened" +
                    ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                    ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
        }
    }

    public static final String[] CONNECTION_STATUS_UPDATE_FIELDS_ARRAY = new String[] { TalkPresence.FIELD_CLIENT_ID, TalkPresence.FIELD_CONNECTION_STATUS };
    public static final Set<String> CONNECTION_STATUS_UPDATE_FIELDS = new HashSet<String>(Arrays.asList(CONNECTION_STATUS_UPDATE_FIELDS_ARRAY));

    /**
     * Unregister a connection from the server
     *
     * @param connection to be removed
     */
    public void connectionClosed(TalkRpcConnection connection) {
        synchronized (connection) {

            if (mConnections.contains(connection)) {

                LOG.debug("[connectionId: '" + connection.getConnectionId() + "'] connection closed (start)" +
                        ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                        ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
                mConnectionsOpen.decrementAndGet();
                if (connection.wasReady()) {
                    mConnectionsReady.decrementAndGet();
                    LOG.debug("[connectionId: '" + connection.getConnectionId() + "'] closed (was ready connection)" +
                            ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                            ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
                } else {
                    if (LOG.isDebugEnabled()) {
                        String reason;
                        TalkClient client = connection.getClient();
                        if (client == null) {
                            reason = "null client";
                        } else {
                            if (client.getTimeReady() == null) {
                                reason = "null timeReady";
                            } else {
                                if (client.getTimeLastLogin() == null) {
                                    reason = "null timeLastLogin";
                                } else {
                                    reason = "timeReady=" + client.getTimeReady() + " not greater than timeLastLogin=" + client.getTimeLastLogin();
                                }
                            }
                        }
                        LOG.debug("[connectionId: '" + connection.getConnectionId() + "'] closed (was not ready)" +
                                ", reason: " + reason);
                    }
                }
                // remove connection from list
                mConnections.remove(connection);
                // remove connection from table
                LOG.debug("[connectionId: '" + connection.getConnectionId() + "'] closed (removed from map)" +
                        ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() + ", loggedIn: " + mConnectionsLoggedIn.get() + ", ready: " + mConnectionsReady.get());
            } else {
                LOG.warn("[connectionId: '" + connection.getConnectionId() + "'] connection closed not in set" +
                        ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                        ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
            }

            String clientId = connection.getClientId();
            if (clientId != null) {
                boolean hasClientConnection = hasClientConnection(clientId);
                if (hasClientConnection) {
                    // remove connection from table
                    removeClientConnection(clientId);
                    mConnectionsLoggedIn.decrementAndGet();
                    LOG.debug("[connectionId: '" + connection.getConnectionId() + "'] closed (was logged in, removed from mapByCID)" +
                            ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                            ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
                    // update presence for connection status change
                    mUpdateAgent.requestPresenceUpdate(clientId, CONNECTION_STATUS_UPDATE_FIELDS);
                    connection.getServerHandler().destroyEnvironment(TalkEnvironment.TYPE_NEARBY);
                    connection.getServerHandler().releaseEnvironment(TalkEnvironment.TYPE_WORLDWIDE);
                    mDeliveryAgent.requestDelivery(clientId, false);
                } else {
                    LOG.warn("[connectionId: '" + connection.getConnectionId() + "'] not in mapByCID, hasClientConnection:" + hasClientConnection +
                            ", open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                            ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
                }
            } else {
                LOG.debug("[connectionId: '" + connection.getConnectionId() + "'] closed (was not logged in)");
            }
            connection.doLogout();
            LOG.info("[connectionId: '" + connection.getConnectionId() + "'] finally closed" +
                    ", still open: " + mConnectionsOpen.get() + ", inMap: " + mConnections.size() +
                    ", loggedIn: " + mConnectionsLoggedIn.get() + ", inMapByCID: " + numberOfClientConnections() + ", ready: " + mConnectionsReady.get());
        }
    }

    /**
     * Creates the object mapper for this server
     */
    private ObjectMapper createObjectMapper(JsonFactory factory) {
        ObjectMapper result = new ObjectMapper(factory);
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return result;
    }

    /**
     * Set up server metrics
     */
    private void initializeMetrics() {
        mMetricsRegistry.register(MetricRegistry.name(TalkServer.class, "connectionsOpen"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mConnectionsOpen.intValue();
                    }
                }
        );
        mMetricsRegistry.register(MetricRegistry.name(TalkServer.class, "connectionsTotal"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mConnectionsTotal.intValue();
                    }
                }
        );
        mMetricsRegistry.register(MetricRegistry.name(TalkServer.class, "connectionsLoggedIn"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mConnectionsLoggedIn.intValue();
                    }
                }
        );
        mMetricsRegistry.register(MetricRegistry.name(TalkServer.class, "connectionsReady"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return mConnectionsReady.intValue();
                    }
                }
        );

        // For instrumenting JMX via Metrics
        /*
        mMetricsRegistry.register(MetricRegistry.name("jvm", "gc"), new GarbageCollectorMetricSet());
        mMetricsRegistry.register(MetricRegistry.name("jvm", "memory"), new MemoryUsageGaugeSet());
        mMetricsRegistry.register(MetricRegistry.name("jvm", "thread-states"), new ThreadStatesGaugeSet());
        mMetricsRegistry.register(MetricRegistry.name("jvm", "fd", "usage"), new FileDescriptorRatioGauge());
        */
    }

    private void initializeHealthChecks() {
        mHealthRegistry.register("database", new DatabaseHealthCheck(mDatabase));
    }

    public HealthCheckRegistry getHealthCheckRegistry() {
        return mHealthRegistry;
    }

    public Vector<TalkRpcConnection> getReadyConnections() {
        Vector<TalkRpcConnection> readyClientConnections = new Vector<TalkRpcConnection>();
        Iterator<TalkRpcConnection> iterator = mConnections.iterator();
        while (iterator.hasNext()) {
            TalkRpcConnection connection = iterator.next();
            if (connection.getClient() != null && connection.getClient().isReady()) {
                readyClientConnections.add(connection);
            }
        }
        return readyClientConnections;
    }
}
