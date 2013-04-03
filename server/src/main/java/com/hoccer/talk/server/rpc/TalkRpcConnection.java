package com.hoccer.talk.server.rpc;

import com.hoccer.talk.logging.HoccerLoggers;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.rpc.ITalkRpcClient;

import java.util.logging.Logger;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.util.ProxyUtil;

import com.hoccer.talk.server.TalkServer;

/**
 * Connection object representing one JSON-RPC connection each
 *
 * This class is responsible for the connection lifecycle as well
 * as for binding JSON-RPC handlers and Talk messaging state to
 * the connection based on user identity and state.
 *
 */
public class TalkRpcConnection implements JsonRpcConnection.Listener {

    /** Logger for connection-related things */
	private static final Logger log = HoccerLoggers.getLogger(TalkRpcConnection.class);
	
	/** Server this connection belongs to */
	TalkServer mServer;

	/** JSON-RPC connection object */
	JsonRpcConnection mConnection;
	
	/** JSON-RPC handler object */
    TalkRpcHandler mHandler;
	
	/** RPC interface to client */
	ITalkRpcClient mClientRpc;
	
	/** Last time we have seen client activity (connection or message) */
	long mLastActivity;

    /** Client object (if logged in) */
    TalkClient mClient;

    /**
     * Construct a connection for the given server using the given connection
     *
     * @param server that we are part of
     * @param connection that we should handle
     */
	public TalkRpcConnection(TalkServer server, JsonRpcConnection connection) {
        // remember stuff
		mServer = server;
		mConnection = connection;
        // create a json-rpc proxy for client notifications
		mClientRpc = ProxyUtil.createClientProxy(
				ITalkRpcClient.class.getClassLoader(),
				ITalkRpcClient.class,
				mConnection);
        // register ourselves for connection events
		mConnection.addListener(this);
	}

    /**
     * Indicate if the connection is currently connected
     *
     * @return true if connected
     */
	public boolean isConnected() {
		return mConnection.isConnected();
	}

    /**
     * Indicate if the connection is currently logged in
     */
    public boolean isLoggedIn() {
        return isConnected() && mClient != null;
    }

    /**
     * Returns the logged-in client or null
     * @return
     */
    public TalkClient getClient() {
        return mClient;
    }

    /**
     * Returns the logged-in clients id or null
     * @return
     */
    public String getClientId() {
        if(mClient != null) {
            return mClient.getClientId();
        }
        return null;
    }

    /**
     * Returns the RPC interface to the client
     */
    public ITalkRpcClient getClientRpc() {
        return mClientRpc;
    }

    /**
     * Callback: underlying connection is now open
     * @param connection
     */
	@Override
	public void onOpen(JsonRpcConnection connection) {
        // reset the time of last activity
		mLastActivity = System.currentTimeMillis();
        // tell the server about the connection
		mServer.connectionOpened(this);
        // create a fresh handler object
        mHandler = new TalkRpcHandler(mServer, this);
        // register the handler with the connection
        mConnection.setHandler(mHandler);
    }

    /**
     * Callback: underlying connection is now closed
     * @param connection
     */
	@Override
	public void onClose(JsonRpcConnection connection) {
        // invalidate the time of last activity
		mLastActivity = -1;
        // tell the server about the disconnect
		mServer.connectionClosed(this);
	}

    /**
     * Called by handler when the client has logged in
     */
    public void identifyClient(String clientId) {
        mClient = mServer.getDatabase().findClientById(clientId);
        if(mClient != null) {
            mServer.identifyClient(mClient, this);
        }
    }

}
