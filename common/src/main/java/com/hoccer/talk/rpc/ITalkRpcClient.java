package com.hoccer.talk.rpc;

import better.jsonrpc.annotations.JsonRpcNotification;

import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;

/**
 * This is the RPC interface exposed by clients.
 * 
 * Using it, clients get notifications for things
 * they requested using the matching "request*"
 * methods in the server interface.
 * 
 * @author ingo
 *
 */
public interface ITalkRpcClient {

    @JsonRpcNotification
    void pushNotRegistered();

	@JsonRpcNotification
	void incomingDelivery(TalkDelivery d, TalkMessage m);
	
	@JsonRpcNotification
	void outgoingDelivery(TalkDelivery d);
	
}
