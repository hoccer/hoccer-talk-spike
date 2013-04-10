package com.hoccer.talk.server;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hoccer.talk.logging.HoccerLoggers;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.server.database.MemoryDatabase;
import com.hoccer.talk.server.delivery.DeliveryAgent;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.server.presence.PresenceAgent;
import com.hoccer.talk.server.push.PushAgent;
import com.hoccer.talk.server.push.PushRequest;
import com.hoccer.talk.server.rpc.TalkRpcConnection;

import better.jsonrpc.server.JsonRpcServer;

/**
 * Main object of the Talk server
 *
 * This holds global state such as the list of active connections,
 * references to common database mapping helpers and so on.
 */
public class TalkServer {

    /** Logger for changes in global server state */
	private static final Logger log = HoccerLoggers.getLogger(TalkServer.class);

    /** server-global JSON mapper */
	ObjectMapper mMapper;

    /** JSON-RPC server instance */
	JsonRpcServer mRpcServer;

    /** Database accessor */
    ITalkServerDatabase mDatabase;

    /** Delivery agent */
    DeliveryAgent mDeliveryAgent;

    /** Push service agent */
	PushAgent mPushAgent;

    /** Presence update agent */
    PresenceAgent mPresenceAgent;

    /** All connections (every connected websocket) */
	Vector<TalkRpcConnection> mConnections =
			new Vector<TalkRpcConnection>();

    /** All logged-in connections by client ID */
	Hashtable<String, TalkRpcConnection> mConnectionsByClientId =
			new Hashtable<String, TalkRpcConnection>();

    /**
     * Create and initialize a Hoccer Talk server
     */
	public TalkServer(ITalkServerDatabase database) {
        mDatabase = database;
		mMapper = createObjectMapper();
		mRpcServer = new JsonRpcServer(ITalkRpcServer.class);
        mDeliveryAgent = new DeliveryAgent(this);
		mPushAgent = new PushAgent();
        mPresenceAgent = new PresenceAgent(this);
    }
	
	public ObjectMapper getMapper() {
		return mMapper;
	}
	
	public JsonRpcServer getRpcServer() {
		return mRpcServer;
	}

    public ITalkServerDatabase getDatabase() {
        return mDatabase;
    }

    public PushAgent getPushAgent() {
        return mPushAgent;
    }

    public DeliveryAgent getDeliveryAgent() {
        return mDeliveryAgent;
    }

    public PresenceAgent getPresenceAgent() {
        return mPresenceAgent;
    }

    public boolean isClientConnected(String clientId) {
        return getClientConnection(clientId) != null;
    }

    public TalkRpcConnection getClientConnection(String clientId) {
        return mConnectionsByClientId.get(clientId);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper result = new ObjectMapper();
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return result;
    }

    /** XXX highly temporary */
    public List<String> getAllClients() {
        Enumeration<String> k = mConnectionsByClientId.keys();
        List<String> r = new ArrayList<String>();
        while(k.hasMoreElements()) {
            r.add(k.nextElement());
        }
        return r;
    }
	
	public void identifyClient(TalkClient client, TalkRpcConnection connection) {
		mConnectionsByClientId.put(client.getClientId(), connection);
	}
	
	public void connectionOpened(TalkRpcConnection connection) {
		mConnections.add(connection);
	}

	public void connectionClosed(TalkRpcConnection connection) {
        // remove connection from list
		mConnections.remove(connection);
        // remove connection from table (being extra careful not to mess it up)
        if(connection.isLoggedIn()) {
            String clientId = connection.getClientId();
            if(mConnectionsByClientId.contains(clientId)) {
                TalkRpcConnection storedConnection = mConnectionsByClientId.get(clientId);
                if(storedConnection == connection) {
                    mConnectionsByClientId.remove(clientId);
                }
            }
        }
	}
	
}
