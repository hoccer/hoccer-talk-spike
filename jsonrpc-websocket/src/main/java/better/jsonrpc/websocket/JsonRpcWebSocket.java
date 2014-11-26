package better.jsonrpc.websocket;

import java.io.IOException;

public interface JsonRpcWebSocket {
    void setHandler(JsonRpcWebSocketHandler messageHandler);

    boolean isOpen();
    void close();

    void sendMessage(String data) throws IOException;
    void sendMessage(byte[] data, int offset, int length) throws IOException;
}
