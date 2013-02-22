package com.hoccer.talk.rpc;

import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;

/**
 * This is the RPC interface exposed by the talk server
 * 
 * It contains all methods that the client can invoke
 * while connected to the server.
 * 
 * @author ingo
 */
public interface TalkRpcServer {
	
	void identify(String clientId);
	
	void requestOutgoingDelivery(boolean wanted);
	void requestIncomingDelivery(boolean wanted);
	
	void deliveryRequest(TalkMessage m, TalkDelivery[] d);
	void deliveryConfirm(TalkDelivery d);
	
}
