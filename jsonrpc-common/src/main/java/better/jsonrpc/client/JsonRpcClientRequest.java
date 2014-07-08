package better.jsonrpc.client;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.log4j.Logger;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JSON-RPC client requests
 *
 * These are used to keep track of outstanding requests in the client.
 *
 * They therefore also contain a considerable part of call logic.
 *
 */
public class JsonRpcClientRequest {

    private static final Logger LOG = Logger.getLogger(JsonRpcClientRequest.class);

    /** Connection used for the request */
    JsonRpcConnection mConnection;
    /** Client tracking this request */
    JsonRpcClient mClient;

    /** Lock for request state */
    Lock mLock;
    /** Condition on mLock */
    Condition mCondition;
    /** Request id */
    String mId;
    /** Flag to indicate abort by disconnect */
    boolean mDisconnected;
    /** Exception that aborted this request */
    Throwable mException;
    /** JSON request */
    ObjectNode mRequest;
    /** JSON response */
    ObjectNode mResponse;

    /** Constructs a client request */
    public JsonRpcClientRequest(String id, ObjectNode request, JsonRpcConnection connection) {
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
        mId = id;
        mRequest = request;
        mResponse = null;
        mConnection = connection;
        mClient = connection.getClient();
    }

    /** Returns the request id of this request */
    public String getId() {
        return mId;
    }

    /** Returns the connection used */
    public JsonRpcConnection getConnection() {
        return mConnection;
    }

    /** Returns the client this request is for */
    public JsonRpcClient getClient() {
        return mClient;
    }

    /** Returns true if this request is done */
    private boolean isDone() {
        return mDisconnected || mResponse != null || mException != null;
    }

    /** Should be called when underlying connection fails */
    public void handleDisconnect() {
        mLock.lock();
        try {
            if (!isDone()) {
                mDisconnected = true;
                mCondition.signalAll();
            }
        } finally {
            mLock.unlock();
        }
    }

    /** Should be called on IO errors, timeouts and other such local abort causes */
    public void handleException(Throwable exception) {
        mLock.lock();
        try {
            if (!isDone()) {
                mException = exception;
                mCondition.signalAll();
            }
        } finally {
            mLock.unlock();
        }
    }

    /** Should be called when a matching response has been received */
    public void handleResponse(ObjectNode response) {
        LOG.trace("handleResponse id="+response.get("id")+" before lock "+mLock+" thread "+Thread.currentThread());
        mLock.lock();
        LOG.trace("handleResponse id="+response.get("id")+" acquired lock "+mLock+" thread "+Thread.currentThread());
        try {
            if (!isDone()) {
                mResponse = response;
                LOG.trace("handleResponse id="+response.get("id")+" signaling  condition "+mCondition+" thread "+Thread.currentThread());
                mCondition.signalAll();
            } else {
                LOG.trace("handleResponse id="+response.get("id")+" not signalling, mDisconnected="+mDisconnected+"mResponse != null="+(mResponse != null)+"mException="+mException+", thread "+Thread.currentThread());

            }
        } finally {
            LOG.trace("handleResponse id="+response.get("id")+" unlocking lock "+mLock+" thread "+Thread.currentThread());
            mLock.unlock();
        }
    }

    /**
     * Wait for this request to finish and take appropriate action
     *
     * This will throw exceptions as resolved by the error resolver.
     *
     * If none occur, the return value of the RPC call is returned.
     *
     * This call will block, limited by the request timeout.
     */
    public Object waitForResponse(Type returnType) throws Throwable {
        LOG.trace("waitForResponse type="+returnType+" locking lock "+mLock+" thread "+Thread.currentThread());
        mLock.lock();
        try {
            // wait until done or timeout is reached
            long timeout = System.currentTimeMillis() + mClient.getRequestTimeout();
            while (!isDone()) {
                // recompute time left
                long timeLeft = timeout - System.currentTimeMillis();
                // throw on timeout
                if (timeLeft <= 0) {
                    mException = new JsonRpcClientTimeout();
                    mCondition.signalAll();
                    throw mException;
                }
                // wait for state changes
                try {
                    LOG.trace("waitForResponse type="+returnType+" wait for condition "+mCondition+" thread "+Thread.currentThread());
                    mCondition.await(timeLeft, TimeUnit.MILLISECONDS);
                    LOG.trace("waitForResponse type="+returnType+" awake from condition "+mCondition+" thread "+Thread.currentThread());
                } catch (InterruptedException e) {
                    LOG.trace("waitForResponse type="+returnType+" interrupted exception: "+e+" thread "+Thread.currentThread());
                }
            }

            // throw if we got disconnected
            if (mDisconnected) {
                LOG.trace("waitForResponse type="+returnType+" disconnected, thread "+Thread.currentThread());
                throw new JsonRpcClientDisconnect();
            }

            // detect rpc failures
            if (mException != null) {
                LOG.trace("waitForResponse type="+returnType+" exception:"+mException+", thread "+Thread.currentThread());
                throw new RuntimeException("JSON-RPC failure", mException);
            }

            // detect errors
            if (mResponse.has("error")
                    && mResponse.get("error") != null
                    && !mResponse.get("error").isNull()) {
                LOG.trace("waitForResponse type="+returnType+" error:"+mResponse.get("error")+", thread "+Thread.currentThread());
                // resolve and throw the exception
                if (mClient.getExceptionResolver() == null) {
                    throw DefaultExceptionResolver.INSTANCE.resolveException(mResponse);
                } else {
                    throw mClient.getExceptionResolver().resolveException(mResponse);
                }
            }

            // convert it to a return object
            if (mResponse.has("result")
                    && !mResponse.get("result").isNull()
                    && mResponse.get("result") != null) {
                LOG.trace("waitForResponse type="+returnType+" result:"+mResponse.get("result")+", thread "+Thread.currentThread());
                if (returnType == null) {
                    // XXX warn
                    return null;
                }
                // get the object mapper for conversion
                ObjectMapper mapper = mConnection.getMapper();
                // create a parser for the result
                JsonParser returnJsonParser = mapper.treeAsTokens(mResponse.get("result"));
                // determine type to convert to
                JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);
                // parse, convert and return
                return mapper.readValue(returnJsonParser, returnJavaType);
            }
        } finally {
            LOG.trace("waitForResponse type="+returnType+" unlocking lock "+mLock+" thread "+Thread.currentThread());
            mLock.unlock();
        }
        return null;
    }

}