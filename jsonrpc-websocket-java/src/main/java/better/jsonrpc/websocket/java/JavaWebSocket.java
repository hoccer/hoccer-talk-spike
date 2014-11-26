package better.jsonrpc.websocket.java;

import better.jsonrpc.websocket.JsonRpcWebSocket;
import better.jsonrpc.websocket.JsonRpcWebSocketHandler;
import org.apache.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.HashMap;

public class JavaWebSocket implements JsonRpcWebSocket {

    private static final Logger LOG = Logger.getLogger(JavaWebSocket.class);

    private JsonRpcWebSocketHandler mHandler;
    private Client mClient;
    private KeyStore mKeyStore;

    public JavaWebSocket(KeyStore keyStore) {
        mKeyStore = keyStore;
    }

    public void open(URI serviceUri, String protocol) throws InterruptedException {
        mClient = new Client(serviceUri, protocol);

        if (mKeyStore != null) {
            try {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(mKeyStore, null);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(mKeyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                SSLSocketFactory factory = sslContext.getSocketFactory();

                mClient.setSocket(factory.createSocket());
            } catch (Exception e) {
                LOG.error("Error creating SSL socket", e);
            }
        }

        mClient.connectBlocking();
    }

    @Override
    public void setHandler(JsonRpcWebSocketHandler handler) {
        mHandler = handler;
    }

    @Override
    public boolean isOpen() {
        return mClient != null && mClient.getConnection().isOpen();
    }

    @Override
    public void close() {
        if (mClient != null) {
            mClient.close();
        } else {
            LOG.error("Trying to close WebSocket that is not open");
        }
    }

    @Override
    public void sendTextMessage(String data) throws IOException {
        if (mClient != null) {
            mClient.send(data);
        } else {
            LOG.error("Trying to send message over WebSocket that is not open");
        }
    }

    @Override
    public void sendBinaryMessage(byte[] data) throws IOException {
        if (mClient != null) {
            mClient.send(data);
        } else {
            LOG.error("Trying to send message over WebSocket that is not open");
        }
    }

    private class Client extends WebSocketClient {
        public Client(URI serviceUri, String protocol) {
            super(serviceUri, new Draft_17(), createHeaders(protocol), 0);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            LOG.debug("onOpen()");

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
        public void onMessage(ByteBuffer bytes) {
            if (mHandler != null) {
                if (bytes.hasArray()) {
                    // If possible, pass data without copying
                    mHandler.handleBinaryMessage(bytes.array(), bytes.arrayOffset(), bytes.remaining());
                } else {
                    byte[] data = new byte[bytes.remaining()];
                    bytes.get(data);
                    mHandler.handleBinaryMessage(data, 0, data.length);
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            LOG.debug("onClose(" + code + ", " + reason + ", " + remote + ")");

            if (mHandler != null) {
                mHandler.handleClose();
            }

            mClient = null;
        }

        @Override
        public void onError(Exception e) {
            LOG.error("WebSocket error", e);
        }
    }

    private static HashMap<String, String> createHeaders(String protocol) {
        HashMap<String, String> headers = new HashMap<String, String>(1);
        headers.put("Sec-WebSocket-Protocol", protocol);
        return headers;
    }
}
