package better.jsonrpc.core;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.util.ProxyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON-RPC connections
 * <p/>
 * These objects represent a single, possibly virtual,
 * connection between JSON-RPC speakers.
 * <p/>
 * Depending on the transport type, a connection can
 * be used as an RPC server and/or client. Notifications
 * may or may not be supported in either direction.
 */
public abstract class JsonRpcConnection {

    /**
     * Global logger, may be used by subclasses
     */
    protected static final Logger LOG = Logger.getLogger(JsonRpcConnection.class);

    static {
        // LOG.setLevel(Level.TRACE);
    }

    /**
     * Global counter for connection IDs
     */
    private static final AtomicInteger sConnectionIdCounter = new AtomicInteger();


    /**
     * Automatically assign connections an ID for debugging and other purposes
     */
    protected final int mConnectionId = sConnectionIdCounter.incrementAndGet();

    protected boolean mDisconnectAfterRequest = false;


    /**
     * Object mapper to be used for this connection
     * <p/>
     * Both client and server should always use this mapper for this connection.
     */
    ObjectMapper mMapper;

    /**
     * Server instance attached to this client
     * <p/>
     * This object is responsible for handling requests and notifications.
     * <p/>
     * It will also send responses where appropriate.
     */
    JsonRpcServer mServer;

    /**
     * Client instance attached to this client
     * <p/>
     * This object is responsible for handling responses.
     * <p/>
     * It will send requests and notifications where appropriate.
     */
    JsonRpcClient mClient;

    /**
     * Handler instance for this client
     * <p/>
     * RPC calls will be dispatched to this through the server.
     */
    Object mServerHandler;


    /**
     * Connection State listeners
     */
    Vector<Listener> mListeners = new Vector<Listener>();

    /**
     * Connection Events listeners
     */
    Vector<ConnectionEventListener> mConnectionEventListeners = new Vector<ConnectionEventListener>();


    /**
     * Main constructor
     */
    public JsonRpcConnection(ObjectMapper mapper) {
        mMapper = mapper;
    }

    /**
     * Get the numeric local connection ID
     *
     * @return
     */
    public int getConnectionId() {
        return mConnectionId;
    }

    /**
     * @return the object mapper for this connection
     */
    public ObjectMapper getMapper() {
        return mMapper;
    }

    /**
     * @return true if this connection has a client bound to it
     */
    public boolean isClient() {
        return mClient != null;
    }

    /**
     * @return the client if there is one (throws otherwise!)
     */
    public JsonRpcClient getClient() {
        if (mClient == null) {
            throw new RuntimeException("Connection not configured for client mode");
        }
        return mClient;
    }

    /**
     * Bind the given client to this connection
     */
    public void bindClient(JsonRpcClient client) {
        if (mClient != null) {
            throw new RuntimeException("Connection already has a client");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("[" + mConnectionId + "] binding client");
        }

        mClient = client;

        mClient.bindConnection(this);
    }

    /**
     * Create and return a client proxy
     */
    public <T> T makeProxy(Class<T> clazz) {
        return ProxyUtil.createClientProxy(clazz.getClassLoader(), clazz, this);
    }


    /**
     * @return true if this connection has a server bound to it
     */
    public boolean isServer() {
        return mServer != null;
    }

    /**
     * @return the server if there is one (throws otherwise!)
     */
    public JsonRpcServer getServer() {
        if (mServer == null) {
            throw new RuntimeException("Connection not configured for server mode");
        }
        return mServer;
    }

    /**
     * Bind the given server to this connection
     */
    public void bindServer(JsonRpcServer server, Object handler) {
        if (mServer != null) {
            throw new RuntimeException("Connection already has a server");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("[" + mConnectionId + "] binding server");
        }

        mServer = server;
        mServerHandler = handler;
    }

    public Object getServerHandler() {
        if (mServerHandler == null) {
            throw new RuntimeException("Connection has no server handler (not configured for server mode)");
        }
        return mServerHandler;
    }

    /**
     * Returns true if the connection is currently connected
     */
    abstract public boolean isConnected();

    abstract public boolean disconnect();

        /**
         * Sends a request through the connection
         */
    abstract public void sendRequest(ObjectNode request) throws Exception;

    /**
     * Sends a response through the connection
     */
    abstract public void sendResponse(ObjectNode response) throws Exception;

    /**
     * Sends a notification through the connection
     */
    abstract public void sendNotification(ObjectNode notification) throws Exception;

    /**
     * Dispatch connection open event (for subclasses to call)
     */
    protected void onOpen() {
        for (Listener l : mListeners) {
            l.onOpen(this);
        }
    }

    /**
     * Dispatch connection close event (for subclasses to call)
     */
    protected void onClose() {
        for (Listener l : mListeners) {
            l.onClose(this);
        }
    }

    /**
     * Dispatch an incoming request (for subclasses to call)
     */
    public void handleRequest(ObjectNode request) {
        LOG.trace("handleRequest id="+ request.get("id"));
        if (mServer != null) {
            try {
                for (ConnectionEventListener l : mConnectionEventListeners) {
                    LOG.trace("handleRequest onPreHandleRequest id="+ request.get("id"));
                    l.onPreHandleRequest(this, request);
                }
                LOG.trace("handleRequest serverid="+ request.get("id"));
                mServer.handleRequest(mServerHandler, request, this);
                LOG.trace("handleRequest server done id="+ request.get("id"));
            } catch (Throwable throwable) {
                LOG.error("Exception handling request", throwable);
            } finally {
                for (ConnectionEventListener l : mConnectionEventListeners) {
                    LOG.trace("handleRequest onPostHandleRequest id="+ request.get("id"));
                    l.onPostHandleRequest(this, request);
                }
            }
        }
        LOG.trace("handleRequest done id="+ request.get("id"));
        if (mDisconnectAfterRequest) {
            LOG.warn("handleRequest done id="+ request.get("id")+", terminating connection ["+mConnectionId+"] because internally requested.");
            this.disconnectAfterRequest();
        }
    }

    /**
     * Dispatch an incoming response (for subclasses to call)
     */
    public void handleResponse(ObjectNode response) {
        LOG.trace("handleResponse id="+ response.get("id"));
        if (mClient != null) {
            try {
                for (ConnectionEventListener l : mConnectionEventListeners) {
                    LOG.trace("handleResponse onPreHandleResponse id="+ response.get("id"));
                    l.onPreHandleResponse(this, response);
                }
                LOG.trace("handleResponse client id="+ response.get("id"));
                mClient.handleResponse(response, this);
                LOG.trace("handleResponse client done id="+ response.get("id"));
            } catch (Throwable throwable) {
                LOG.error("Exception handling response", throwable);
            } finally {
                for (ConnectionEventListener l : mConnectionEventListeners) {
                    LOG.trace("handleResponse onPostHandleResponse id="+ response.get("id"));
                    l.onPostHandleResponse(this, response);
                }
            }
        }
        LOG.trace("handleResponse done id="+ response.get("id"));
    }

    /**
     * Dispatch an incoming notification (for subclasses to call)
     */
    public void handleNotification(ObjectNode notification) {
        if (mServer != null) {
            try {
                for (ConnectionEventListener l : mConnectionEventListeners) {
                    l.onPreHandleNotification(this, notification);
                }
                mServer.handleRequest(mServerHandler, notification, this);
            } catch (Throwable throwable) {
                LOG.error("Exception handling notification", throwable);
            } finally {
                for (ConnectionEventListener l : mConnectionEventListeners) {
                    l.onPostHandleNotification(this, notification);
                }
            }
        }
    }

    /**
     * Interface of connection state listeners
     */
    public interface Listener {
        public void onOpen(JsonRpcConnection connection);

        public void onClose(JsonRpcConnection connection);

    }

    /**
     * Interface of connection event listeners
     */
    public interface ConnectionEventListener {

        /**
         * Gets fired imediately *before* a request is handled
         *
         * @param connection
         * @param request
         */
        public void onPreHandleRequest(JsonRpcConnection connection, ObjectNode request);

        /**
         * Gets fired immediately *after* a request was handled
         *
         * @param connection
         * @param request
         */
        public void onPostHandleRequest(JsonRpcConnection connection, ObjectNode request);

        /**
         * Get fired immediately *before* a notification is handled
         *
         * @param connection
         * @param notification
         */
        public void onPreHandleNotification(JsonRpcConnection connection, ObjectNode notification);

        /**
         * Gets fired immediately *after* a notification was handled
         *
         * @param connection
         * @param notification
         */
        public void onPostHandleNotification(JsonRpcConnection connection, ObjectNode notification);

        /**
         * Gets fired immediately *before* a response is handled
         *
         * @param connection
         * @param response
         */
        public void onPreHandleResponse(JsonRpcConnection connection, ObjectNode response);

        /**
         * Gets fired immediately *after* a response was handled
         *
         * @param connection
         * @param response
         */
        public void onPostHandleResponse(JsonRpcConnection connection, ObjectNode response);
    }

    /**
     * Add a connection state listener
     *
     * @param l
     */
    public void addListener(Listener l) {
        mListeners.add(l);
    }

    /**
     * Remove the given connection state listener
     *
     * @param l
     */
    public void removeListener(Listener l) {
        mListeners.remove(l);
    }


    /**
     * Add a connection event listener
     *
     * @param l ConnectionEventListener
     */
    public void addConnectionEventListener(ConnectionEventListener l) {
        mConnectionEventListeners.add(l);
    }

    /**
     * Remove the given connection event listener
     *
     * @param l ConnectionEventListener
     */
    public void removeConnectionEventListener(ConnectionEventListener l) {
        mConnectionEventListeners.remove(l);
    }

    public void disconnectAfterRequest() {
        LOG.warn("disconnectAfterRequest set for connection ["+mConnectionId+"]");
        mDisconnectAfterRequest = true;
    }
}
