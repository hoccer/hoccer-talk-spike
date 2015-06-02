package com.hoccer.talk.server.filecache;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.websocket.jetty.JettyWebSocket;
import better.jsonrpc.websocket.JsonRpcWsConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.filecache.rpc.ICacheControl;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper for filecache control operations
 *
 * This automatically connects to the filecache when needed
 * and performs the requested operations synchronously.
 */
public class FilecacheClient {

    private static final Logger LOG = Logger.getLogger(FilecacheClient.class);
    public static final String FILECACHE_PROTOCOL = "com.hoccer.talk.filecache.control.v1";

    private TalkServerConfiguration mConfig;
    private JettyWebSocket mWebSocket;
    private ICacheControl mRpc;

    public FilecacheClient(TalkServerConfiguration config) {
        mConfig = config;
        mWebSocket = new JettyWebSocket();

        JsonRpcWsConnection connection = new JsonRpcWsConnection(mWebSocket, new ObjectMapper());
        JsonRpcClient client = new JsonRpcClient();
        client.setRequestTimeout(3000);
        connection.bindClient(client);

        mRpc = connection.makeProxy(ICacheControl.class);
    }

    private synchronized void ensureConnected() {
        if(!mWebSocket.isOpen()) {
            LOG.info("filecache not connected, trying to connect");
            try {
                URI serviceUri = mConfig.getFilecacheControlUrl();
                mWebSocket.open(serviceUri, FILECACHE_PROTOCOL, 5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Timeout connecting to filecache", e);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while connecting to filecache", e);
            } catch (IOException e) {
                throw new RuntimeException("Could not connect to filecache", e);
            }
        }
    }

    public ITalkRpcServer.FileHandles createFileForStorage(String accountId, String contentType, int contentLength) {
        ensureConnected();
        ICacheControl.FileHandles cacheHandles = mRpc.createFileForStorage(accountId, contentType, contentLength);
        LOG.debug("created storage file with handles (fileId: '" + cacheHandles.fileId + "', "
                 + " uploadId: '" + cacheHandles.uploadId + "', "
                 + " downloadId: '" + cacheHandles.downloadId + "')");
        ITalkRpcServer.FileHandles serverHandles = new ITalkRpcServer.FileHandles();
        serverHandles.fileId = cacheHandles.fileId;
        serverHandles.uploadUrl = mConfig.getFilecacheUploadBase() + cacheHandles.uploadId;
        serverHandles.downloadUrl = mConfig.getFilecacheDownloadBase() + cacheHandles.downloadId;
        return serverHandles;
    }

    public ITalkRpcServer.FileHandles createFileForTransfer(String accountId, String contentType, int contentLength) {
        ensureConnected();
        ICacheControl.FileHandles cacheHandles = mRpc.createFileForTransfer(accountId, contentType, contentLength);
        LOG.debug("created transfer file with handles (fileId: '" + cacheHandles.fileId + "', "
                 + " uploadId: '" + cacheHandles.uploadId + "', "
                 + " downloadId: '" + cacheHandles.downloadId + "')");
        ITalkRpcServer.FileHandles serverHandles = new ITalkRpcServer.FileHandles();
        serverHandles.fileId = cacheHandles.fileId;
        serverHandles.uploadUrl = mConfig.getFilecacheUploadBase() + cacheHandles.uploadId;
        serverHandles.downloadUrl = mConfig.getFilecacheDownloadBase() + cacheHandles.downloadId;
        return serverHandles;
    }

    public void deleteFile(String fileId) {
        ensureConnected();
        LOG.debug("deleting file with id: '" + fileId + "'");
        mRpc.deleteFile(fileId);
    }

    public void deleteAccount(String accountId) {
        ensureConnected();
        LOG.info("deleting account with id: '" + accountId + "'");
        mRpc.deleteAccount(accountId);
    }

}
