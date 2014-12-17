package better.jsonrpc.websocket;

import java.io.IOException;

public interface JsonRpcWebSocket {
    void setHandler(JsonRpcWebSocketHandler messageHandler);

    boolean isOpen();
    void close();

    void sendTextMessage(String data) throws IOException;
    void sendBinaryMessage(byte[] data) throws IOException;
}
