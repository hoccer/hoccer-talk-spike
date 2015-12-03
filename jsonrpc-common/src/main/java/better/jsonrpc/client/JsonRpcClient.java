package better.jsonrpc.client;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import better.jsonrpc.exceptions.ExceptionResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A JSON-RPC client
 *
 * This class implements a JSON-RPC client that can be attached
 * to a connection and used to perform JSON-RPC calls through it.
 *
 * Log levels:
 *
 *   DEBUG will show requests and responses
 *   TRACE will show progress of calls
 *
 */
public class JsonRpcClient {

    /** Default request timeout (msecs) */
    public static final long DEFAULT_REQUEST_TIMEOUT = 30 * 1000;

    /** Global logger for clients */
	private static final Logger LOG = Logger.getLogger(JsonRpcClient.class);

    /** JSON-RPC version to pretend speaking */
	private static final String JSON_RPC_VERSION = "2.0";


    /** Request timeout for this client (msecs) */
    private long mRequestTimeout = DEFAULT_REQUEST_TIMEOUT;

    /** number of timeouts on this session */
    private long mRequestSuccessCount = 0;

    /** number of failures on this session */
    private long mRequestFailureCount = 0;

    /** number of timeouts on this session */
    private long mRequestTimeoutCount = 0;

    /** number of timeouts on this session since last success*/
    private long mRequestTimeoutCountSinceLastSuccess = 0;

    // msecs since epoch when the last failure occurred
    private Date mLastRequestFailureOccurred = null;

    // msecs since epoch when the last timeout occurred
    private Date mLastRequestTimeoutOccurred = null;

    // msecs since epoch when the last response was received
    private Date mLastResponseOccurred = null;

    // msecs the last response took, -1 if timeout
    private long mLastResponseTime = 0;

    // msecs the last response took, -1 if timeout
    private long mLastFailureTime = 0;

    // sliding average of the last 10 responses
    private double mAverageResponseTime = 0;

    // sliding average of the last 10 failed responses
    private double mAverageFailureTime = 0;

    /** Generator for request IDs */
	private AtomicInteger mIdGenerator;

    /** Exception converter */
	private ExceptionResolver mExceptionResolver = DefaultExceptionResolver.INSTANCE;

    /** Table of outstanding requests */
    private Hashtable<String, JsonRpcClientRequest> mOutstandingRequests =
            new Hashtable<String, JsonRpcClientRequest>();

    private Date mLastRequestDate = null;
    private String mLastMethodName = null;

    private void resetTimersAndCounters() {
        mRequestSuccessCount = 0;
        mRequestFailureCount = 0;
        mRequestTimeoutCount = 0;
        mRequestTimeoutCountSinceLastSuccess = 0;
        mLastRequestFailureOccurred = new Date(0);
        mLastRequestTimeoutOccurred = new Date(0);
        mLastResponseOccurred = new Date(0);
        mLastResponseTime = 0;
        mLastFailureTime = 0;
        mAverageResponseTime = 0;
        mAverageFailureTime = 0;
        mLastMethodName = null;
        mLastRequestDate = new Date(0);
    }

    /** Listener for connection state changes */
    private JsonRpcConnection.Listener mConnectionListener =
            new JsonRpcConnection.Listener() {
                @Override
                public void onOpen(JsonRpcConnection connection) {
                    resetTimersAndCounters();
                    handleConnectionChange(connection);
                }
                @Override
                public void onClose(JsonRpcConnection connection) {
                    handleConnectionChange(connection);
                }
            };

	/**
	 * Creates a client
	 */
	public JsonRpcClient() {
		this.mIdGenerator = new AtomicInteger(0);
	}

    /**
     * Returns the {@link ExceptionResolver} for this client
     */
    public ExceptionResolver getExceptionResolver() {
        return mExceptionResolver;
    }

    /**
     * Set the {@link ExceptionResolver} for this client
     * @param mExceptionResolver the exceptionResolver to set
     */
    public void setExceptionResolver(ExceptionResolver mExceptionResolver) {
        this.mExceptionResolver = mExceptionResolver;
    }

    /**
     * Get request timeout (in msecs)
     */
    public long getRequestTimeout() {
        return mRequestTimeout;
    }

    /**
     * Set request timeout (in msecs)
     * @param mRequestTimeout
     */
    public void setRequestTimeout(long mRequestTimeout) {
        this.mRequestTimeout = mRequestTimeout;
    }
 /*
    public long getRequestSuccessCount() {
        return mRequestSuccessCount;
    }

    public long getRequestTimeoutCount() {
        return mRequestTimeoutCount;
    }

    public long getRequestTimeoutCountSinceLastSuccess() {
        return mRequestTimeoutCountSinceLastSuccess;
    }

    public long getLastRequestFailureOccurred() {
        return mLastRequestFailureOccurred;
    }

    public long getLastRequestTimeoutOccurred() {
        return mLastRequestTimeoutOccurred;
    }

    public long getLastResponseOccurred() {
        return mLastResponseOccurred;
    }



    public double getAverageResponseTime() {
        return mAverageResponseTime;
    }
  */
    public long getLastResponseTime() {
        return mLastResponseTime;
    }
    public String getLastMethodName() {
        return mLastMethodName;
    }

    public Date getLastRequestDate() {
        return mLastRequestDate;
    }

    public Date getLastRequestFailureOccurred() {
        return mLastRequestFailureOccurred;
    }

    public Date getLastRequestTimeoutOccurred() {
        return mLastRequestTimeoutOccurred;
    }

    private synchronized void responseReceived(JsonRpcClientRequest request) {
        mLastResponseOccurred = new Date(request.getTimeFinished());
        mLastResponseTime = request.getTimeFinished() - request.getTimeStarted();
        mAverageResponseTime = 0.9 * mAverageResponseTime + 0.1 * mLastResponseTime;
        mRequestTimeoutCountSinceLastSuccess = 0;
        mRequestSuccessCount++;
        LOG.debug("responseReceived: responseTime:"+mLastResponseTime+", averageResponseTime:"+String.format("%.2f",mAverageResponseTime)+
                " ms , outstanding:"+mOutstandingRequests.size()+", successes:"+mRequestSuccessCount+", failures:"+mRequestFailureCount+", timeouts:" +mRequestTimeoutCount);
        /*
        if (!isResponsive()) {
            LOG.warn("Client purposely not responsive, disconnecting");
            request.getConnection().disconnect();
        }
        */
    }

    private synchronized void responseFailed(JsonRpcClientRequest request) {
        mLastRequestFailureOccurred = new Date(request.getTimeFailed());
        mLastFailureTime = request.getTimeFailed() - request.getTimeStarted();
        if (request.timeoutOccured()) {
            mLastRequestTimeoutOccurred = new Date(request.getTimeFailed());
            mRequestTimeoutCount++;
            mRequestTimeoutCountSinceLastSuccess++;
        }
        mAverageFailureTime = 0.9 * mAverageFailureTime + 0.1 * mLastFailureTime;

        LOG.debug("responseFailed: timeout:"+request.timeoutOccured()+"failureTime:"+mLastFailureTime+", mAverageFailureTime:"+String.format("%.2f",mAverageFailureTime)+
                " ms, outstanding:"+mOutstandingRequests.size()+", timeouts since last success:"+mRequestTimeoutCountSinceLastSuccess+", failures:"+mRequestFailureCount+", timeouts:" +mRequestTimeoutCount);

        if (!isResponsive()) {
            LOG.warn("Client not responsive, disconnecting");
            if (!request.getConnection().disconnect()) {
                // we are not disconnected
                LOG.warn("Did not really disconnect, there was no connection");
            };
        }
    }

    public synchronized boolean isResponsive() {
        long lastSuccessAgo = System.currentTimeMillis() - mLastResponseOccurred.getTime();
        if (mRequestTimeoutCountSinceLastSuccess > 3 &&  lastSuccessAgo > 60 * 1000/* || mRequestSuccessCount > 20*/) {
            LOG.warn("Client not responsive, request timeouts since last success:"+mRequestTimeoutCountSinceLastSuccess+", last success "+lastSuccessAgo/1000+" secs ago");
            return false;
        }
        return true;
    }

    /**
     * Generate a new request id and return it
     * @return
     */
    private String generateId() {
        return Integer.toHexString(mIdGenerator.incrementAndGet());
    }


    /**
     * Handle binding to a connection
     */
    public void bindConnection(JsonRpcConnection connection) {
        connection.addListener(mConnectionListener);
    }

    /**
     * Handle unbinding from a connection
     */
    public void unbindConnection(JsonRpcConnection connection) {
        connection.removeListener(mConnectionListener);
    }

    /**
     * Send a request through the connection
     */
    public void sendRequest(JsonRpcConnection connection, ObjectNode request) throws Exception {
        // log request
        if (LOG.isDebugEnabled()) {
            LOG.debug("RPC-Request -> [" + connection.getConnectionId() + "] " + request.toString());
        }
        JsonNode methodNode = request.get("method");
        mLastMethodName = (methodNode != null && !methodNode.isNull()) ? methodNode.asText() : null;
        mLastRequestDate = new Date();
        // send it
        connection.sendRequest(request);
    }

    /**
     * Send a notification through the connection
     */
    public void sendNotification(JsonRpcConnection connection, ObjectNode notification) throws Exception {
        // log notification
        if (LOG.isDebugEnabled()) {
            LOG.debug("RPC-Notification -> [" + connection.getConnectionId() + "] " + notification.toString());
        }
        JsonNode methodNode = notification.get("method");
        mLastMethodName = (methodNode != null && !methodNode.isNull()) ? methodNode.asText() : null;
        mLastRequestDate = new Date();

        // send it
        connection.sendNotification(notification);
    }

    /**
     * Handle a connection state change (connect/disconnect)
     *
     * This will remove all requests associated with the connection indicated.
     *
     * @param connection
     */
    private void handleConnectionChange(JsonRpcConnection connection) {
        synchronized (mOutstandingRequests) {
            // vector to collect matched requests into
            Vector<JsonRpcClientRequest> matches = new Vector<JsonRpcClientRequest>();
            // for every outstanding request
            Enumeration<JsonRpcClientRequest> reqs = mOutstandingRequests.elements();
            while(reqs.hasMoreElements()) {
                JsonRpcClientRequest req = reqs.nextElement();
                // if the request belongs to the changed connection
                if(req.getConnection() == connection) {
                    // unblock the requestor
                    req.handleDisconnect();
                    // and remember the request
                    matches.add(req);
                }
            }
            // for all relevant requests
            for(JsonRpcClientRequest req: matches) {
                // remove request from table
                mOutstandingRequests.remove(req.getId());
            }
        }
    }


    /**
     * Invoke the method specified via the given connection
     *
     * Requests submitted via this method may block and will
     * be tracked by the client in its outstanding request table.
     *
     * @param methodName
     * @param arguments
     * @param returnType
     * @param connection
     * @return remote return value
     * @throws Throwable
     */
	public Object invokeMethod(String methodName, Object arguments, Type returnType, JsonRpcConnection connection)
		throws Throwable {
        Object result = null;
        // generate request id
        String id = generateId();
        // log about call
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + id + "] calling " + methodName);
        }
        // construct the JSON request node
        ObjectNode requestNode = createRequest(methodName, arguments, id, connection);
        // construct the request state object
        JsonRpcClientRequest request = new JsonRpcClientRequest(id, requestNode, connection);
        // add the request to client state
        synchronized (mOutstandingRequests) {
            mOutstandingRequests.put(id, request);
        }
        // request execution
        try {
            // send request
            sendRequest(connection, requestNode);
            // wait for response or other result
            result = request.waitForResponse(returnType);
            responseReceived(request);
        } catch (Throwable t) {
            responseFailed(request);
            // log about exception
            if (LOG.isTraceEnabled()) {
                LOG.trace("[" + id + "] call to " + methodName + " throws", t);
            }
            // abort the request
            request.handleException(t);
            // rethrow for library user to handle
            throw t;
        } finally {
            // remove request from client state
            synchronized (mOutstandingRequests) {
                mOutstandingRequests.remove(id);
            }
        }
        // log about return
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + id + "] returning from " + methodName);
        }
        // return final result
        return result;
	}

    /**
     * Invoke the method specified via the given connection
     *
     * Note that notifications are never tracked by the client.
     *
     * @param methodName
     * @param arguments
     * @param connection
     */
	public void invokeNotification(String methodName, Object arguments, JsonRpcConnection connection)
        throws Throwable {
        // log about call
        if (LOG.isTraceEnabled()) {
            LOG.trace("[notification] calling " + methodName);
        }
        // create the JSON request object
		ObjectNode requestNode = createRequest(methodName, arguments, null, connection);
        // create client request object
        JsonRpcClientRequest request = new JsonRpcClientRequest(null, requestNode, connection);
        // execute the request
        try {
            // send request
		    sendNotification(connection, requestNode);
        } catch (Throwable t) {
            // log about exception
            if (LOG.isTraceEnabled()) {
                LOG.trace("[notification] call to " + methodName + " throws", t);
            }
            // abort the request
            request.handleException(t);
            // rethrow
            throw t;
        }
        // log about return
        if (LOG.isTraceEnabled()) {
            LOG.trace("[notification] returning from " + methodName);
        }
	}

    /**
     * Handle an incoming JSON response
     * @param response to process
     * @param connection on which the response arrived
     * @return true if the response was accepted
     */
	public boolean handleResponse(ObjectNode response, JsonRpcConnection connection) {
		JsonNode idNode = response.get("id");
        // check the id, we only use strings
		if(idNode != null && idNode.isTextual()) {
			String id = idNode.asText();
            // log response
            if (LOG.isDebugEnabled()) {
                LOG.debug("RPC-Response <- [" + connection.getConnectionId() + "] " + response.toString());
            }
            // retrieve the request from the client table
            JsonRpcClientRequest req = null;
            synchronized (mOutstandingRequests) {
                req = mOutstandingRequests.get(id);
            }
            // if there was an actual request
			if(req != null && req.getConnection() == connection) {
                // handle the response, unblocking the requestor
				req.handleResponse(response);
                // we have handled the request
                return true;
			}
		}
        // we have not handled the request
        return false;
	}

    /**
     * Creates a JSON request node.
     * @param methodName the method name
     * @param arguments the arguments
     * @param id the optional id
     * @return the new request
     */
    private ObjectNode createRequest(
            String methodName, Object arguments, String id, JsonRpcConnection connection) {

        ObjectMapper mapper = connection.getMapper();

        // create the request
        ObjectNode request = mapper.createObjectNode();

        // add id
        if (id!=null) { request.put("id", id); }

        // add protocol and method
        request.put("jsonrpc", JSON_RPC_VERSION);
        request.put("method", methodName);

        // default empty arguments, will be replaced further down
        request.put("params", mapper.valueToTree(new Object[0]));

        // object array args
        if (arguments!=null && arguments.getClass().isArray()) {
            Object[] args = Object[].class.cast(arguments);
            if (args.length>0) {
                request.put("params", mapper.valueToTree(Object[].class.cast(arguments)));
            }

            // collection args
        } else if (arguments!=null && Collection.class.isInstance(arguments)) {
            if (!Collection.class.cast(arguments).isEmpty()) {
                request.put("params", mapper.valueToTree(arguments));
            }

            // map args
        } else if (arguments!=null && Map.class.isInstance(arguments)) {
            if (!Map.class.cast(arguments).isEmpty()) {
                request.put("params", mapper.valueToTree(arguments));
            }

            // other args
        } else if (arguments!=null) {
            request.put("params", mapper.valueToTree(arguments));
        }

        // return the request
        return request;
    }

}
