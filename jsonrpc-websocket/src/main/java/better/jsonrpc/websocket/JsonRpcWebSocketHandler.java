package better.jsonrpc.websocket;

public interface JsonRpcWebSocketHandler {
    void handleOpen();
    void handleClose();

    void handleTextMessage(String data);
    void handleBinaryMessage(byte[] data, int offset, int length);
}
