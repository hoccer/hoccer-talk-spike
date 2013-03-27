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
public interface ITalkRpcServer {
	
	void identify(String clientId);

    // XXX temporary for spiking
    String[] getAllClients();

    void registerGcm(String registeredPackage, String registrationId);
    void unregisterGcm();

    void registerApns(String registrationToken);
    void unregisterApns();

	TalkDelivery[] deliveryRequest(TalkMessage m, TalkDelivery[] d);
	TalkDelivery   deliveryConfirm(String messageId);
	
}
