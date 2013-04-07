package com.hoccer.talk.rpc;

import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;
import com.hoccer.talk.model.TalkRelationship;

import java.util.Date;

/**
 * This is the RPC interface exposed by the talk server
 * 
 * It contains all methods that the client can invoke
 * while connected to the server.
 * 
 * @author ingo
 */
public interface ITalkRpcServer {

	/** Unauthenticated login for initial development */
	void identify(String clientId);

    /** Temporary hack which better not be documented
         XXX this MUST be removed before release
     */
    String[] getAllClients();

    /** Register for GCM push with the given parameters */
    void registerGcm(String registeredPackage, String registrationId);

    /** Clear GCM registration */
    void unregisterGcm();

    /** Register for APNS push with the given token */
    void registerApns(String registrationToken);

    /** Clear APNS registration */
    void unregisterApns();

    /** Generate a secret token for the given purpose */
    String generateToken(String tokenPurpose, int secondsValid);

    /** Perform token-based client pairing */
    boolean pairByToken(String secret);

    /** Request delivery of the given message */
	TalkDelivery[] deliveryRequest(TalkMessage m, TalkDelivery[] d);

    /** Confirm reception of the message with the given id */
	TalkDelivery   deliveryConfirm(String messageId);
	
}
