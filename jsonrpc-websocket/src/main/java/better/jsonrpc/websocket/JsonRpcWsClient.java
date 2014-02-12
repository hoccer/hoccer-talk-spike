package better.jsonrpc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import better.jsonrpc.client.JsonRpcClient;

public class JsonRpcWsClient extends JsonRpcWsConnection
        implements WebSocket, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

    private Executor mExecutor;

    /** URI for the service used */
	private URI mServiceUri;
    private String mServiceProtocol;

	/** Websocket client */
	private WebSocketClient mClient;

    public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClient client, ObjectMapper mapper) {
        super(mapper);
        mServiceUri = serviceUri;
        mServiceProtocol = protocol;
        mClient = client;
    }

    public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClient client, ObjectMapper mapper, Executor executor) {
        super(mapper);
        mServiceUri = serviceUri;
        mServiceProtocol = protocol;
        mClient = client;
        mExecutor = executor;
    }

	public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClient client) {
		this(serviceUri, protocol, client, new ObjectMapper());
	}

    public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClientFactory clientFactory) {
        this(serviceUri, protocol, clientFactory.newWebSocketClient(), new ObjectMapper());
    }

    public JsonRpcWsClient(URI serviceUri, String protocol) {
        this(serviceUri, protocol, null, new ObjectMapper());
        WebSocketClientFactory clientFactory = new WebSocketClientFactory();
        try {
            clientFactory.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mClient = clientFactory.newWebSocketClient();
    }

    public WebSocketClient getWebSocketClient() {
        return mClient;
    }

    public URI getServiceUri() {
        return mServiceUri;
    }

    public void setServiceUri(URI serviceUri) {
        this.mServiceUri = serviceUri;
    }

    public String getServiceProtocol() {
        return mServiceProtocol;
    }

    public void setServiceProtocol(String serviceProtocol) {
        this.mServiceProtocol = serviceProtocol;
    }

    public void connect() throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("[" + mConnectionId + "] connecting");
        }
        mClient.setProtocol(mServiceProtocol);
		mClient.open(mServiceUri, this);
	}

    public void connect(long maxWait, TimeUnit maxWaitUnit)
            throws TimeoutException, IOException, InterruptedException
    {
        if(LOG.isDebugEnabled()) {
            LOG.debug("[" + mConnectionId + "] connecting with timeout of " + maxWait + " " + maxWaitUnit);
        }
        mClient.setProtocol(mServiceProtocol);
        mClient.open(mServiceUri, this, maxWait, maxWaitUnit);
    }

    /** @return the executor used to decouple this connection */
    public Executor getExecutor() {
        return mExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public void sendRequest(final ObjectNode request) throws IOException {
        if(mExecutor == null) {
            super.sendRequest(request);
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonRpcWsClient.super.sendRequest(request);
                } catch (Exception e) {
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void sendResponse(final ObjectNode response) throws IOException {
        if(mExecutor == null) {
            super.sendResponse(response);
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonRpcWsClient.super.sendResponse(response);
                } catch (Exception e) {
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void sendNotification(final ObjectNode notification) throws IOException {
        if(mExecutor == null) {
            super.sendNotification(notification);
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonRpcWsClient.super.sendNotification(notification);
                } catch (Exception e) {
                }
            }
        });
    }

}
