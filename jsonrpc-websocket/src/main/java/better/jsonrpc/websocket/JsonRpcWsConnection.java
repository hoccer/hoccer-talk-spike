package better.jsonrpc.websocket;

import better.jsonrpc.core.JsonRpcConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JsonRpcWsConnection extends JsonRpcConnection implements JsonRpcWebSocketHandler {

    private static final String KEEPALIVE_REQUEST_STRING = "k";
    private static final byte[] KEEPALIVE_REQUEST_BINARY = new byte[]{'k'};
    private static final String KEEPALIVE_RESPONSE_STRING = "a";
    private static final byte[] KEEPALIVE_RESPONSE_BINARY = new byte[]{'a'};

    /**
     * Currently active websocket connection
     */
    private JsonRpcWebSocket mWebSocket;

    /**
     * Whether to accept binary messages
     */
    private boolean mAcceptBinaryMessages = true;

    /**
     * Whether to accept text messages
     */
    private boolean mAcceptTextMessages = true;

    /**
     * Whether to send binary messages (text is the default)
     */
    private boolean mSendBinaryMessages = false;

    /**
     * Whether to send keep-alive frames
     */
    private boolean mSendKeepAlives = false;

    /**
     * Whether to answer keep-alive requests
     */
    private boolean mAnswerKeepAlives = false;

    public JsonRpcWsConnection(JsonRpcWebSocket webSocket, ObjectMapper mapper) {
        super(mapper);
        mWebSocket = webSocket;
        mWebSocket.setHandler(this);
    }

    public boolean isAcceptBinaryMessages() {
        return mAcceptBinaryMessages;
    }

    public void setAcceptBinaryMessages(boolean acceptBinaryMessages) {
        this.mAcceptBinaryMessages = acceptBinaryMessages;
    }

    public boolean isAcceptTextMessages() {
        return mAcceptTextMessages;
    }

    public void setAcceptTextMessages(boolean acceptTextMessages) {
        this.mAcceptTextMessages = acceptTextMessages;
    }

    public boolean isSendBinaryMessages() {
        return mSendBinaryMessages;
    }

    public void setSendBinaryMessages(boolean sendBinaryMessages) {
        this.mSendBinaryMessages = sendBinaryMessages;
    }

    public boolean isSendKeepAlives() {
        return mSendKeepAlives;
    }

    public void setSendKeepAlives(boolean sendKeepAlives) {
        this.mSendKeepAlives = sendKeepAlives;
    }

    public boolean isAnswerKeepAlives() {
        return mAnswerKeepAlives;
    }

    public void setAnswerKeepAlives(boolean answerKeepAlives) {
        this.mAnswerKeepAlives = answerKeepAlives;
    }

    @Override
    public boolean isConnected() {
        return mWebSocket.isOpen();
    }

    @Override
    public boolean disconnect() {
        if (mWebSocket.isOpen()) {
            mWebSocket.close();
            return true;
        } else {
            // call listeners again to notify them of connection closure
            onClose();
        }
        return false;
    }

    public void transmit(String data) throws IOException {
        if (mWebSocket.isOpen()) {
            mWebSocket.sendMessage(data);
        } else {
            throw new IOException("Websocket not open");
        }
    }

    public void transmit(byte[] data, int offset, int length) throws IOException {
        if (mWebSocket.isOpen()) {
            mWebSocket.sendMessage(data, offset, length);
        } else {
            throw new IOException("Websocket not open");
        }
    }

    public void transmit(JsonNode node) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + mConnectionId + "] transmitting \"" + node.toString() + "\"");
        }
        if (mSendBinaryMessages) {
            byte[] data = getMapper().writeValueAsBytes(node);
            transmit(data, 0, data.length);
        } else {
            String data = getMapper().writeValueAsString(node);
            transmit(data);
        }
    }

    @Override
    public void handleOpen() {
        onOpen();
    }

    @Override
    public void handleClose() {
        onClose();
    }

    @Override
    public void handleTextMessage(String data) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + mConnectionId + "] received string data \"" + data + "\"");
        }
        if (mAcceptTextMessages) {
            // answer keep-alive requests
            if (mAnswerKeepAlives) {
                if (data.equals(KEEPALIVE_REQUEST_STRING)) {
                    try {
                        transmit(KEEPALIVE_RESPONSE_STRING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            // handle normal payload
            try {
                handleJsonMessage(getMapper().readTree(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleBinaryMessage(byte[] data, int offset, int length) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + mConnectionId + "] received binary data \"" + data.toString() + "\", offset="+offset+", length="+length);
        }
        if (mAcceptBinaryMessages) {
            // handle keep-alive frames
            if (length == 1) {
                if (data[offset] == 'k') {
                    if (mAnswerKeepAlives) {
                        try {
                            transmit(KEEPALIVE_RESPONSE_BINARY, 0, KEEPALIVE_RESPONSE_BINARY.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (data[offset] == 'a') {
                    // ignore for now
                }
                return;
            }
            // handle normal payload
            InputStream is = new ByteArrayInputStream(data, offset, length);
            try {
                handleJsonMessage(getMapper().readTree(is));
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleJsonMessage(JsonNode message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("[" + mConnectionId + "] received \"" + message.toString() + "\"");
        }
        if (message.isObject()) {
            ObjectNode messageObj = ObjectNode.class.cast(message);

            // requests and notifications
            if (messageObj.has("method")) {
                if (messageObj.has("id")) {
                    handleRequest(messageObj);
                } else {
                    handleNotification(messageObj);
                }
            }
            // responses
            if (messageObj.has("result") || messageObj.has("error")) {
                if (messageObj.has("id")) {
                    handleResponse(messageObj);
                }
            }
        }
    }

    public void sendKeepAlive() throws IOException {
        if (mSendKeepAlives) {
            if (mSendBinaryMessages) {
                transmit(KEEPALIVE_REQUEST_BINARY, 0, KEEPALIVE_REQUEST_BINARY.length);
            } else {
                transmit(KEEPALIVE_REQUEST_STRING);
            }
        }
    }

    @Override
    public void sendRequest(ObjectNode request) throws IOException {
        transmit(request);
    }

    @Override
    public void sendResponse(ObjectNode response) throws IOException {
        transmit(response);
    }

    @Override
    public void sendNotification(ObjectNode notification) throws IOException {
        transmit(notification);
    }

}
