package better.jsonrpc.client;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.log4j.Level;
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

    static {
        // LOG.setLevel(Level.TRACE);
    }

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

    long mTimeStarted;
    long mTimeFinished;
    long mTimeFailed;
    boolean mTimeoutOccured;

    /** Constructs a client request */
    public JsonRpcClientRequest(String id, ObjectNode request, JsonRpcConnection connection) {
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
        mId = id;
        mRequest = request;
        mResponse = null;
        mConnection = connection;
        mClient = connection.getClient();
        mTimeStarted = 0;
        mTimeFinished = 0;
        mTimeFailed = 0;
        mTimeoutOccured = false;
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

    public long getTimeStarted() {
        return mTimeStarted;
    }

    public long getTimeFinished() {
        return mTimeFinished;
    }

    public long getTimeFailed() {
        return mTimeFailed;
    }

    public boolean timeoutOccured() {
        return mTimeoutOccured;
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
        LOG.trace("waitForResponse type="+returnType+" request id='"+mRequest.get("id")+"', locking lock "+mLock+" thread "+Thread.currentThread());
        mLock.lock();
        try {
            // wait until done or timeout is reached
            mTimeStarted = System.currentTimeMillis();
            long timeout =  mTimeStarted + mClient.getRequestTimeout();
            while (!isDone()) {
                // recompute time left
                long timeLeft = timeout - System.currentTimeMillis();
                // throw on timeout
                if (timeLeft <= 0) {
                    mTimeoutOccured = true;
                    mException = new JsonRpcClientTimeout();
                    mCondition.signalAll();
                    throw mException;
                }
                // wait for state changes
                try {
                    LOG.trace("waitForResponse type="+returnType+" request id="+mRequest.get("id")+", wait for condition "+mCondition+" thread "+Thread.currentThread()+", timeLeft="+timeLeft);
                    mCondition.await(timeLeft, TimeUnit.MILLISECONDS);
                    LOG.trace("waitForResponse type="+returnType+" request id="+mRequest.get("id")+", awake from condition "+mCondition+" thread "+Thread.currentThread()+", timeLeft="+(timeout - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    LOG.trace("waitForResponse type="+returnType+" request id="+mRequest.get("id")+", interrupted exception: "+e+" thread "+Thread.currentThread()+", timeLeft="+(timeout - System.currentTimeMillis()));
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

            mTimeFinished = System.currentTimeMillis();

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
            if (mTimeFinished == 0) {
                mTimeFailed = System.currentTimeMillis();
            }
            LOG.trace("waitForResponse type="+returnType+" request id='"+mRequest.get("id")+"', unlocking lock "+mLock+" thread "+Thread.currentThread());
            mLock.unlock();
        }
        return null;
    }

}