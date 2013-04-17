package com.hoccer.talk.rpc;

import com.hoccer.talk.model.*;

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

    /**
     * Generate a new client ID for registration
     *
     * The ID will be remembered as part of connection state,
     * so SRP setup must happen on the same connection.
     *
     * @return a new client id
     */
    String generateId();

    /**
     * Register a new client with the given SRP parameters
     */
    String srpRegister(String verifier, String salt);

    /**
     * Initiate SRP login
     *
     * Client passes public value A.
     *
     * After this call we have a shared secret K.
     *
     * @return public value B
     */
    String srpPhase1(String clientId, String A);

    /**
     * Finish SRP login
     *
     * Client passes evidence that it has K, called M1.
     *
     * Iff M1 could be verified to be proof of K,
     * the server will prove knowledge of K by returning M2.
     *
     * Authentication has succeeded if M2 is returned
     * and can be verified by the client to be proof of K.
     *
     * If authentication fails then either null will be
     * returned or an error response will be sent.
     *
     * @return evidence value M2
     */
    String srpPhase2(String M1);

    /** Retrieves established relationships changes after given date */
    TalkRelationship[] getRelationships(Date lastKnown);

    /** Register for GCM push with the given parameters */
    void registerGcm(String registeredPackage, String registrationId);

    /** Clear GCM registration */
    void unregisterGcm();

    /** Register for APNS push with the given token */
    void registerApns(String registrationToken);

    /** Clear APNS registration */
    void unregisterApns();

    /** Update key */
    void updateKey(TalkKey key);

    /** Get key for given client with given keyid */
    TalkKey getKey(String clientId, String keyId);

    /** Update client presence */
    void updatePresence(TalkPresence presence);

    /** Retrieve presences changed after given date */
    TalkPresence[] getPresences(Date lastKnown);

    /** Generate a secret token for the given purpose */
    String generateToken(String tokenPurpose, int secondsValid);

    /** Perform token-based client pairing */
    boolean pairByToken(String secret);

    /** Block the given client */
    void blockClient(String clientId);

    /** Unblock the given client */
    void unblockClient(String clientId);

    /** Depair the given client */
    void depairClient(String clientId);

    /** Request delivery of the given message */
	TalkDelivery[] deliveryRequest(TalkMessage m, TalkDelivery[] d);

    /** Confirm reception of the message with the given id */
	TalkDelivery   deliveryConfirm(String messageId);

    /** Confirm reception of outgoing message delivery confirmation */
    TalkDelivery   deliveryAcknowledge(String messageId, String recipientId);

    /**
     * Reject/abort a delivery
     *
     * The receiverId can be the ID of the connected client, in which
     * case this method will abort an incoming delivery that got stuck.
     *
     * It can also be the ID of another client, in which case the
     * corresponding outgoing message will be aborted.
     */
    TalkDelivery deliveryAbort(String receiverId, String messageId);
	
}
