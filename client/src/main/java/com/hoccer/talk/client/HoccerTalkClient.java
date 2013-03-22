package com.hoccer.talk.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.util.ProxyUtil;

import better.jsonrpc.websocket.JsonRpcWsClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.logging.HoccerLoggers;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;
import com.hoccer.talk.rpc.ITalkRpcClient;
import com.hoccer.talk.rpc.ITalkRpcServer;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

public class HoccerTalkClient implements JsonRpcConnection.Listener {

	private static final Logger LOG = HoccerLoggers.getLogger(HoccerTalkClient.class);

    WebSocketClientFactory mClientFactory;

	JsonRpcWsClient mConnection;

    ITalkClientDatabase mDatabase;
	
	TalkRpcClientImpl mHandler;
	
	ITalkRpcServer mServerRpc;

    ScheduledExecutorService mExecutor;

    ScheduledFuture<?> mDisconnectFuture;

    Vector<ITalkClientListener> mListeners = new Vector<ITalkClientListener>();

    /**
     * Create a Hoccer Talk client using the given client database
     * @param database
     */
	public HoccerTalkClient(ScheduledExecutorService backgroundExecutor, ITalkClientDatabase database) {
        // remember client database and background executor
        mExecutor = backgroundExecutor;
        mDatabase = database;

        // create URI object referencing the server
        URI uri = null;
        try {
            uri = new URI("ws://192.168.2.65:8080/");
        } catch (URISyntaxException e) {
            // won't happen
        }

        // create superfluous client factory
        mClientFactory = new WebSocketClientFactory();
        try {
            mClientFactory.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // create JSON-RPC client
        mConnection = new JsonRpcWsClient(
                mClientFactory,
                createObjectMapper(),
                uri);

        // create client-side RPC handler object
        mHandler = new TalkRpcClientImpl();

        // create JSON-RPC server object
        JsonRpcServer srv = new JsonRpcServer(ITalkRpcClient.class);
        mConnection.setHandler(getHandler());
        mConnection.addListener(this);
        mConnection.setServer(srv);

        // create RPC proxy
		mServerRpc = ProxyUtil.createClientProxy(
				ITalkRpcServer.class.getClassLoader(),
				ITalkRpcServer.class,
				mConnection);

        // XXX this should really be done by the class user
        tryToConnect();
	}

    private ObjectMapper createObjectMapper() {
        ObjectMapper result = new ObjectMapper();
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return result;
    }

    /**
     * Get the RPC interface to the server
     * @return
     */
	public ITalkRpcServer getServerRpc() {
		return mServerRpc;
	}

    /**
     * Get the handler object implementing the client RPC interface
     * @return
     */
	public ITalkRpcClient getHandler() {
		return mHandler;
	}

    public void registerListener(ITalkClientListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(ITalkClientListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Called when the connection is opened
     * @param connection
     */
	@Override
	public void onOpen(JsonRpcConnection connection) {
		LOG.info("connection opened");
        rescheduleAutomaticDisconnect();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                LOG.info("logging in");
                mServerRpc.identify(mDatabase.getClient().getClientId());

                LOG.info("fetching client list");
                String[] clnts = mServerRpc.getAllClients();
                LOG.info("found " + clnts.length + " clients: " + clnts);
            }
        });
	}

    /**
     * Called when the connection is closed
     * @param connection
     */
	@Override
	public void onClose(JsonRpcConnection connection) {
		LOG.info("connection closed");
        shutdownAutomaticDisconnect();
	}

    private void doConnect() {
        LOG.info("performing connect");
        mConnection.connect();
    }

    private void doDisconnect() {
        LOG.info("performing disconnect");
        mConnection.disconnect();
    }

    private void shutdownAutomaticDisconnect() {
        if(mDisconnectFuture != null) {
            mDisconnectFuture.cancel(false);
            mDisconnectFuture = null;
        }
    }

    private void rescheduleAutomaticDisconnect() {
        shutdownAutomaticDisconnect();
        mDisconnectFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                doDisconnect();
            }
        }, 60, TimeUnit.SECONDS);
    }

    private void tryToConnect() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doConnect();
            }
        });
    }

    public void tryToDeliver(final String messageTag) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                TalkMessage m = null;
                TalkDelivery[] d = null;
                try {
                    m = mDatabase.getMessageByTag(messageTag);
                    d = mDatabase.getDeliveriesByTag(messageTag);
                } catch (Exception e) {
                    // XXX fail horribly
                    e.printStackTrace();
                    return;
                }
                mServerRpc.deliveryRequest(m, d);
            }
        });
    }

    /**
     * Client-side RPC implementation
     */
	public class TalkRpcClientImpl implements ITalkRpcClient {

		@Override
		public void incomingDelivery(TalkDelivery d, TalkMessage m) {
			LOG.info("call incomingDelivery()");
		}

		@Override
		public void outgoingDelivery(TalkDelivery d) {
			LOG.info("call outgoingDelivery()");
		}
		
	}
	
}
