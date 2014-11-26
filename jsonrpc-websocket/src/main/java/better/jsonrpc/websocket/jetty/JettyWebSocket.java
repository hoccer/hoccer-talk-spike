package better.jsonrpc.websocket.jetty;

import better.jsonrpc.websocket.JsonRpcWebSocket;
import better.jsonrpc.websocket.JsonRpcWebSocketHandler;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JettyWebSocket implements JsonRpcWebSocket, WebSocket, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

    private static final Logger LOG = Logger.getLogger(JettyWebSocket.class);

    public static final int MAX_TEXT_MESSAGE_SIZE = 1 << 16;
    public static final int MAX_BINARY_MESSAGE_SIZE = 1 << 16;

    private int mMaxIdleTime = 300 * 1000;
    private JsonRpcWebSocketHandler mHandler;
    private Connection mConnection;

    @Override
    public void setHandler(JsonRpcWebSocketHandler handler) {
        mHandler = handler;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        mMaxIdleTime = maxIdleTime;
    }

    public void open(URI serviceUri, String protocol, int timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException, IOException {
        WebSocketClientFactory clientFactory = new WebSocketClientFactory();

        try {
            clientFactory.start();
        } catch (Exception e) {
            LOG.error("Error starting WebSocketClientFactory", e);
        }

        WebSocketClient client = clientFactory.newWebSocketClient();
        open(client, serviceUri, protocol, timeout, timeoutUnit);
    }

    public void open(WebSocketClient client, URI serviceUri, String protocol, int timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException, IOException {
        LOG.info("connecting to '" + serviceUri + "' with timeout of " + timeout + " " + timeoutUnit);
        client.setProtocol(protocol);
        client.open(serviceUri, this, timeout, timeoutUnit);
    }

    @Override
    public void close() {
        if (mConnection != null) {
            mConnection.close();
        } else {
            LOG.error("Trying to close WebSocket that is not open");
        }
    }

    @Override
    public boolean isOpen() {
        return mConnection != null && mConnection.isOpen();
    }

    @Override
    public void sendMessage(String data) throws IOException {
        if (mConnection != null) {
            mConnection.sendMessage(data);
        } else {
            LOG.error("Trying to send message over WebSocket that is not open");
        }
    }

    @Override
    public void sendMessage(byte[] data, int offset, int length) throws IOException {
        if (mConnection != null) {
            mConnection.sendMessage(data, offset, length);
        } else {
            LOG.error("Trying to send message over WebSocket that is not open");
        }
    }

    @Override
    public void onOpen(Connection connection) {
        LOG.debug("onOpen()");

        mConnection = connection;
        mConnection.setMaxIdleTime(mMaxIdleTime);
        mConnection.setMaxTextMessageSize(MAX_TEXT_MESSAGE_SIZE);
        mConnection.setMaxBinaryMessageSize(MAX_BINARY_MESSAGE_SIZE);

        if (mHandler != null) {
            mHandler.handleOpen();
        }
    }

    @Override
    public void onMessage(String data) {
        if (mHandler != null) {
            mHandler.handleTextMessage(data);
        }
    }

    @Override
    public void onMessage(byte[] data, int offset, int length) {
        if (mHandler != null) {
            mHandler.handleBinaryMessage(data, offset, length);
        }
    }

    @Override
    public void onClose(int closeCode, String message) {
        LOG.debug("onClose(" + closeCode + ", " + message + ")");

        if (mHandler != null) {
            mHandler.handleClose();
        }

        mConnection = null;
    }
}
