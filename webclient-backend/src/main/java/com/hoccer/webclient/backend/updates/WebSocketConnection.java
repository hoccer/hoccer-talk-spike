package com.hoccer.webclient.backend.updates;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WebSocketConnection implements WebSocket, WebSocket.OnTextMessage, IXoDownloadListener {

    private static final Logger LOG = Logger.getLogger(WebSocketConnection.class);

    public static final String SUBSCRIBE_COMMAND = "subscribe";
    public static final String UNSUBSCRIBE_COMMAND = "unsubscribe";

    public static final String DOWNLOADS_PATH = "/api/downloads";

    private static final ScheduledExecutorService sExecutor = Executors.newSingleThreadScheduledExecutor();

    private XoClientDatabase mClientDatabase;
    private Connection mConnection;
    private ObjectMapper mJsonMapper;
    private ScheduledFuture<?> mKeepaliveFuture;
    private HashSet<String> mSubscribedPaths;

    public WebSocketConnection(XoClientDatabase database) {
        mClientDatabase = database;
        mConnection = null;
        mJsonMapper = new ObjectMapper(new JsonFactory());
        mKeepaliveFuture = null;
        mSubscribedPaths = new HashSet<String>();
    }

    @Override
    public void onOpen(Connection connection) {
        LOG.info("onOpen");

        mConnection = connection;
        mKeepaliveFuture = sExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                sendMessage("/keepalive", null);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void onMessage(String data) {
        try {
            CommandMessage message = mJsonMapper.readValue(data, CommandMessage.class);
            String command = message.getCommand();
            String path = message.getPath();

            if (SUBSCRIBE_COMMAND.equals(command)) {
                subscribe(path);
            } else if (UNSUBSCRIBE_COMMAND.equals(command)) {
                unsubscribe(path);
            } else {
                LOG.info("Unknown command: " + command);
            }
        } catch (JsonMappingException e) {
            LOG.info("JSON message could not be mapped to Command: " + e.getMessage());
        } catch (JsonParseException e) {
            LOG.info("WebSocket message is no valid JSON: " + e.getMessage());
        } catch (IOException e) {
            LOG.warn("Error while parsing WebSocket message: " + e.getMessage());
        }
    }

    private void subscribe(String path) {
        if (DOWNLOADS_PATH.equals(path)) {
            LOG.info("Subscribing to " + DOWNLOADS_PATH);
            mClientDatabase.registerDownloadListener(this);
        }

        mSubscribedPaths.add(path);
    }

    private void unsubscribe(String path) {
        if (DOWNLOADS_PATH.equals(path)) {
            LOG.info("Unsubscribing from " + DOWNLOADS_PATH);
            mClientDatabase.unregisterDownloadListener(this);
        }

        mSubscribedPaths.remove(path);
    }

    private void sendMessage(String path, Object data) {
        try {
            UpdateMessage message = new UpdateMessage(path, data);
            String json = mJsonMapper.writeValueAsString(message);
            mConnection.sendMessage(json);
        } catch (IOException e) {
            LOG.error("Could not send update message over WebSocket", e);
        }
    }

    @Override
    public void onDownloadCreated(TalkClientDownload download) {
        onDownloadChanged(download);
    }

    @Override
    public void onDownloadUpdated(TalkClientDownload download) {
        onDownloadChanged(download);
    }

    private void onDownloadChanged(TalkClientDownload download) {
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            sendMessage(DOWNLOADS_PATH, download);
        }
    }

    @Override
    public void onDownloadDeleted(TalkClientDownload download) {}

    @Override
    public void onClose(int closeCode, String message) {
        LOG.info("onClose");

        for (String path : mSubscribedPaths) {
            unsubscribe(path);
        }

        mKeepaliveFuture.cancel(true);
        mConnection = null;
    }
}
