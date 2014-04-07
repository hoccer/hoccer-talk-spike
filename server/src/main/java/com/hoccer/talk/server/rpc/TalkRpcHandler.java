package com.hoccer.talk.server.rpc;

import com.hoccer.talk.model.*;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.ITalkServerStatistics;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingServer;
import com.hoccer.talk.util.MapUtil;
import com.sun.tools.javac.util.Pair;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

/**
 * RPC handler for talk protocol communications
 * <p/>
 * Calls are exposed directly to the client, so essentially
 * this also has to take care of protocol-level security...
 * <p/>
 * This class does not hold any state except for login state.
 */
public class TalkRpcHandler implements ITalkRpcServer {

    private static final Logger LOG =
            Logger.getLogger(TalkRpcHandler.class);

    private static Hex HEX = new Hex();
    private final Digest SRP_DIGEST = new SHA256Digest();
    private static final SecureRandom SRP_RANDOM = new SecureRandom();
    private static final SRP6Parameters SRP_PARAMETERS = SRP6Parameters.CONSTANTS_1024;

    /**
     * Reference to server
     */
    private TalkServer mServer;

    /**
     * Reference to database accessor
     */
    private ITalkServerDatabase mDatabase;

    /**
     * Reference to stats collector
     */
    private ITalkServerStatistics mStatistics;

    /**
     * Reference to connection object
     */
    private TalkRpcConnection mConnection;

    /**
     * SRP authentication state
     */
    private SRP6VerifyingServer mSrpServer;

    /**
     * SRP authenticating user
     */
    private TalkClient mSrpClient;

    public TalkRpcHandler(TalkServer pServer, TalkRpcConnection pConnection) {
        mServer = pServer;
        mConnection = pConnection;
        mDatabase = mServer.getDatabase();
        mStatistics = mServer.getStatistics();
    }

    private void requireIdentification() {
        if (!mConnection.isLoggedIn()) {
            throw new RuntimeException("Not logged in");
        }
    }

    private void logCall(String message) {
        if (TalkServerConfiguration.LOG_ALL_CALLS || mConnection.isSupportMode()) {
            LOG.info("[" + mConnection.getConnectionId() + "] " + message);
        }
    }

    @Override
    public TalkServerInfo hello(TalkClientInfo clientInfo) {
        logCall("hello()");

        String tag = clientInfo.getSupportTag();
        if (tag != null && !tag.isEmpty()) {
            if (tag.equals(mServer.getConfiguration().getSupportTag())) {
                mConnection.activateSupportMode();
            } else {
                LOG.info("[" + mConnection.getConnectionId() + "] sent invalid support tag \"" + tag + "\"");
            }
        }

        TalkServerInfo serverInfo = new TalkServerInfo();
        serverInfo.setServerTime(new Date());
        serverInfo.setSupportMode(mConnection.isSupportMode());

        return serverInfo;
    }

    @Override
    public String generateId() {
        logCall("generateId()");

        if (mConnection.isLoggedIn()) {
            throw new RuntimeException("Can't register while logged in");
        }
        if (mConnection.isRegistering()) {
            throw new RuntimeException("Can't register more than one identity per connection");
        }

        String clientId = UUID.randomUUID().toString();

        mConnection.beginRegistration(clientId);

        return clientId;
    }

    @Override
    public String srpRegister(String verifier, String salt) {
        logCall("srpRegister(" + verifier + "," + salt + ")");

        if (mConnection.isLoggedIn()) {
            throw new RuntimeException("Can't register while logged in");
        }

        String clientId = mConnection.getUnregisteredClientId();

        if (clientId == null) {
            throw new RuntimeException("You need to generate an id before registering");
        }

        // XXX check verifier and salt for viability

        TalkClient client = new TalkClient();
        client.setClientId(clientId);
        client.setSrpSalt(salt);
        client.setSrpVerifier(verifier);
        client.setTimeRegistered(new Date());

        try {
            mDatabase.saveClient(client);
            mStatistics.signalClientRegisteredSucceeded();
        } catch (RuntimeException e) {
            mStatistics.signalClientRegisteredFailed();
            throw e;
        }
        return clientId;
    }

    @Override
    public String srpPhase1(String clientId, String A) {
        logCall("srpPhase1(" + clientId + "," + A + ")");

        // check if we aren't logged in already
        if (mConnection.isLoggedIn()) {
            throw new RuntimeException("Can't authenticate while logged in");
        }
        try {

            // create SRP state
            if (mSrpServer == null) {
                mSrpServer = new SRP6VerifyingServer();
            } else {
                throw new RuntimeException("Can only attempt SRP once per connection");
            }

            // get client object
            mSrpClient = mDatabase.findClientById(clientId);
            if (mSrpClient == null) {
                throw new RuntimeException("No such client");
            }

            // verify SRP registration
            if (mSrpClient.getSrpVerifier() == null || mSrpClient.getSrpSalt() == null) {
                throw new RuntimeException("No such client");
            }

            // parse the salt from DB
            byte[] salt = null;
            try {
                salt = (byte[]) HEX.decode(mSrpClient.getSrpSalt());
            } catch (DecoderException e) {
                throw new RuntimeException("Bad salt", e);
            }

            // initialize SRP state
            mSrpServer.initVerifiable(
                    SRP_PARAMETERS.N, SRP_PARAMETERS.g,
                    new BigInteger(mSrpClient.getSrpVerifier(), 16),
                    clientId.getBytes(),
                    salt,
                    SRP_DIGEST, SRP_RANDOM
            );

            // generate server credentials
            BigInteger credentials = mSrpServer.generateServerCredentials();

            // computer secret / verify client credentials
            try {
                mSrpServer.calculateSecret(new BigInteger(A, 16));
            } catch (CryptoException e) {
                throw new RuntimeException("Authentication failed", e);
            }
            mStatistics.signalClientLoginSRP1Succeeded();
            // return our credentials for the client
            return credentials.toString(16);
        } catch (RuntimeException e) {
            mStatistics.signalClientLoginSRP1Failed();
            throw e;
        }

    }

    @Override
    public String srpPhase2(String M1) {
        logCall("srpPhase2(" + M1 + ")");

        // check if we aren't logged in already
        if (mConnection.isLoggedIn()) {
            throw new RuntimeException("Can't authenticate while logged in");
        }

        try {
            // verify we are in a good state to do phase2
            if (mSrpServer == null) {
                throw new RuntimeException("Need to perform phase 1 first");
            }
            if (mSrpClient == null) {
                throw new RuntimeException("Internal error in SRP phase 2");
            }

            // parse the string given by the client
            byte[] M1b = null;
            try {
                M1b = (byte[]) HEX.decode(M1);
            } catch (DecoderException e) {
                throw new RuntimeException(e);
            }

            // perform the verification
            byte[] M2;
            try {
                M2 = mSrpServer.verifyClient(M1b);
            } catch (CryptoException e) {
                throw new RuntimeException("Verification failed", e);
            }

            // we are now logged in
            mConnection.identifyClient(mSrpClient.getClientId());
            mStatistics.signalClientLoginSRP2Succeeded();
            // clear SRP state
            mSrpClient = null;
            mSrpServer = null;

            // return server evidence for client to check
            //        return Hex.encodeHexString(M2);
            return new String(Hex.encodeHex(M2));
        } catch (RuntimeException e) {
            mStatistics.signalClientLoginSRP2Failed();
            throw e;
        }

    }

    @Override
    public void registerGcm(String registeredPackage, String registrationId) {
        requireIdentification();
        logCall("registerGcm(" + registeredPackage + "," + registrationId + ")");
        TalkClient client = mConnection.getClient();
        client.setGcmPackage(registeredPackage);
        client.setGcmRegistration(registrationId);
        mDatabase.saveClient(client);
    }

    @Override
    public void unregisterGcm() {
        requireIdentification();
        logCall("unregisterGcm()");
        TalkClient client = mConnection.getClient();
        client.setGcmPackage(null);
        client.setGcmRegistration(null);
        mDatabase.saveClient(client);
    }

    @Override
    public void registerApns(String registrationToken) {
        requireIdentification();
        logCall("registerApns(" + registrationToken + ")");
        // APNS occasionally returns these for no good reason
        if (registrationToken.length() == 0) {
            return;
        }
        // set and save the token
        TalkClient client = mConnection.getClient();
        client.setApnsToken(registrationToken);
        mDatabase.saveClient(client);
    }

    @Override
    public void unregisterApns() {
        requireIdentification();
        logCall("unregisterApns()");
        TalkClient client = mConnection.getClient();
        client.setApnsToken(null);
        mDatabase.saveClient(client);
    }

    @Override
    public void hintApnsUnreadMessage(int numUnreadMessages) {
        requireIdentification();
        logCall("hintApnsUnreadMessages(" + numUnreadMessages + ")");
        TalkClient client = mConnection.getClient();
        client.setApnsUnreadMessages(numUnreadMessages);
        mDatabase.saveClient(client);
    }

    @Override
    public TalkRelationship[] getRelationships(Date lastKnown) {
        requireIdentification();

        logCall("getRelationships(" + lastKnown.toString() + ")");

        // query the db
        List<TalkRelationship> relationships =
                mDatabase.findRelationshipsChangedAfter(mConnection.getClientId(), lastKnown);

        // build the result array
        TalkRelationship[] res = new TalkRelationship[relationships.size()];
        int idx = 0;
        for (TalkRelationship r : relationships) {
            res[idx++] = r;
        }

        // return the bunch
        return res;
    }

    @Override
    public void updatePresence(TalkPresence presence) {
        requireIdentification();

        logCall("updatePresence()");

        // find existing presence or create one
        TalkPresence existing = mDatabase.findPresenceForClient(mConnection.getClientId());
        if (existing == null) {
            existing = new TalkPresence();
        }

        // update the presence with what we got
        existing.setClientId(mConnection.getClientId());
        existing.setClientName(presence.getClientName());
        existing.setClientStatus(presence.getClientStatus());
        existing.setTimestamp(new Date());
        existing.setAvatarUrl(presence.getAvatarUrl());
        existing.setKeyId(presence.getKeyId());

        // save the thing
        mDatabase.savePresence(existing);

        // start updating other clients
        mServer.getUpdateAgent().requestPresenceUpdate(mConnection.getClientId());
    }

    @Override
    public TalkPresence[] getPresences(Date lastKnown) {
        requireIdentification();

        logCall("getPresences(" + lastKnown + ")");

        // perform the query
        List<TalkPresence> pres = mDatabase.findPresencesChangedAfter(mConnection.getClientId(), lastKnown);

        // update connection status and convert results to array
        TalkPresence[] res = new TalkPresence[pres.size()];
        for (int i = 0; i < res.length; i++) {
            TalkPresence p = pres.get(i);
            p.setConnectionStatus(mServer.isClientConnected(p.getClientId())
                    ? TalkPresence.CONN_STATUS_ONLINE : TalkPresence.CONN_STATUS_OFFLINE);
            res[i] = pres.get(i);
        }

        // return it
        return res;
    }

    @Override
    public void updateKey(TalkKey key) {
        requireIdentification();

        logCall("updateKey()");

        TalkKey k = mDatabase.findKey(mConnection.getClientId(), key.getKeyId());
        if (k != null) {
            return;
        }

        key.setClientId(mConnection.getClientId());
        key.setTimestamp(new Date());

        // XXX should check if the content is ok

        mDatabase.saveKey(key);
    }

    @Override
    public TalkKey getKey(String clientId, String keyId) {
        requireIdentification();

        logCall("getKey(" + clientId + "," + keyId + ")");

        TalkKey res = null;

        TalkRelationship rel = mDatabase.findRelationshipBetween(mConnection.getClientId(), clientId);
        if (rel != null && rel.isFriend()) {
            res = mDatabase.findKey(clientId, keyId);
        } else {
            List<TalkGroupMember> members = mDatabase.findGroupMembersForClient(mConnection.getClientId());
            for (TalkGroupMember member : members) {
                if (member.isJoined() || member.isInvited()) {
                    TalkGroupMember otherMember = mDatabase.findGroupMemberForClient(member.getGroupId(), clientId);
                    if (otherMember != null && (otherMember.isJoined() || otherMember.isInvited())) {
                        res = mDatabase.findKey(clientId, keyId);
                        break;
                    }
                }
            }
        }

        if (res == null) {
            throw new RuntimeException("Given client is not your friend or key does not exist");
        }

        return res;
    }

    @Override
    public String generateToken(String tokenPurpose, int secondsValid) {
        requireIdentification();

        logCall("generateToken(" + tokenPurpose + "," + secondsValid + ")");

        // verify request
        if (!TalkToken.isValidPurpose(tokenPurpose)) {
            throw new RuntimeException("Invalid token purpose");
        }

        // constrain validity period (XXX constants)
        secondsValid = Math.max(60, secondsValid);            // at least 1 minute
        secondsValid = Math.min(7 * 24 * 3600, secondsValid); // at most 1 week

        // get the current time
        Date time = new Date();
        // compute expiry time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        calendar.add(Calendar.SECOND, Math.round(secondsValid));

        // generate the secret
        int attempt = 0;
        String secret = null;
        do {
            if (secret != null) {
                LOG.warn("Token generator returned existing token - regenerating");
            }
            if (attempt++ > 3) {
                throw new RuntimeException("Could not generate a token");
            }
            secret = genPw();
        } while (mDatabase.findTokenByPurposeAndSecret(tokenPurpose, secret) != null);

        // create the token object
        TalkToken token = new TalkToken();
        token.setClientId(mConnection.getClientId());
        token.setPurpose(tokenPurpose);
        token.setState(TalkToken.STATE_UNUSED);
        token.setSecret(secret);
        token.setGenerationTime(time);
        token.setExpiryTime(calendar.getTime());
        token.setUseCount(0);
        token.setMaxUseCount(1);

        // save the token
        mDatabase.saveToken(token);

        // return the secret
        return token.getSecret();
    }

    @Override
    public String generatePairingToken(int maxUseCount, int secondsValid) {
        requireIdentification();

        logCall("generatePairingToken(" + maxUseCount + "," + secondsValid + ")");

        // constrain validity period (XXX constants)
        secondsValid = Math.max(60, secondsValid);            // at least 1 minute
        secondsValid = Math.min(7 * 24 * 3600, secondsValid); // at most 1 week

        // constrain use count
        maxUseCount = Math.max(1, maxUseCount);
        maxUseCount = Math.min(50, maxUseCount);

        // get the current time
        Date time = new Date();
        // compute expiry time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        calendar.add(Calendar.SECOND, Math.round(secondsValid));

        // generate the secret
        int attempt = 0;
        String secret = null;
        do {
            if (secret != null) {
                LOG.warn("Token generator returned existing token - regenerating");
            }
            if (attempt++ > 3) {
                throw new RuntimeException("Could not generate a token");
            }
            secret = genPw();
        } while (mDatabase.findTokenByPurposeAndSecret(TalkToken.PURPOSE_PAIRING, secret) != null);

        // create the token object
        TalkToken token = new TalkToken();
        token.setClientId(mConnection.getClientId());
        token.setPurpose(TalkToken.PURPOSE_PAIRING);
        token.setState(TalkToken.STATE_UNUSED);
        token.setSecret(secret);
        token.setGenerationTime(time);
        token.setExpiryTime(calendar.getTime());
        token.setUseCount(0);
        token.setMaxUseCount(maxUseCount);

        // save the token
        mDatabase.saveToken(token);

        // return the secret
        return token.getSecret();
    }

    // TODO: extract as generic TokenGenerator for the server in general?!
    // TODO: Do not use a command line tool for this! Use a library or so...
    private String genPw() {
        String result = null;
        ProcessBuilder pb = new ProcessBuilder("pwgen", "10", "1");
        try {
            Process p = pb.start();
            InputStream s = p.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            String line = r.readLine();
            LOG.debug("pwline " + line);
            if (line.length() == 10) {
                result = line;
            }
        } catch (IOException ioe) {
            LOG.error("Error in running 'pwgen'!", ioe);
        }
        return result;
    }

    @Override
    public boolean pairByToken(String secret) {
        requireIdentification();
        logCall("pairByToken(" + secret + ")");

        TalkToken token = mDatabase.findTokenByPurposeAndSecret(
                TalkToken.PURPOSE_PAIRING, secret);

        // check if token exists
        if (token == null) {
            LOG.info("no token could be found");
            return false;
        }

        // check if token is unused
        if (!token.isUsable()) {
            LOG.info("token can no longer be used");
            return false;
        }

        // check if token is still valid
        if (token.getExpiryTime().before(new Date())) {
            LOG.info("token has expired");
            return false;
        }

        // get relevant client IDs
        String myId = mConnection.getClientId();
        String otherId = token.getClientId();

        // reject self-pairing
        if (token.getClientId().equals(myId)) {
            LOG.info("self-pairing rejected");
            return false;
        }

        // log about it
        LOG.info("performing token-based pairing between " + mConnection.getClientId() + " and " + token.getClientId());

        // set up relationships
        setRelationship(myId, otherId, TalkRelationship.STATE_FRIEND, true);
        setRelationship(otherId, myId, TalkRelationship.STATE_FRIEND, true);

        // invalidate the token
        token.setUseCount(token.getUseCount() + 1);
        if (token.getUseCount() < token.getMaxUseCount()) {
            token.setState(TalkToken.STATE_USED);
        } else {
            token.setState(TalkToken.STATE_SPENT);
        }
        mDatabase.saveToken(token);

        // give both users an initial presence
        mServer.getUpdateAgent().requestPresenceUpdateForClient(otherId, myId);
        mServer.getUpdateAgent().requestPresenceUpdateForClient(myId, otherId);

        return true;
    }

    @Override
    public void blockClient(String clientId) {
        requireIdentification();

        logCall("blockClient(" + clientId + ")");

        TalkRelationship rel = mDatabase.findRelationshipBetween(mConnection.getClientId(), clientId);
        if (rel == null) {
            throw new RuntimeException("You are not paired with client " + clientId);
        }

        String oldState = rel.getState();

        if (oldState.equals(TalkRelationship.STATE_FRIEND)) {
            setRelationship(mConnection.getClientId(), clientId, TalkRelationship.STATE_BLOCKED, true);
            return;
        }
        if (oldState.equals(TalkRelationship.STATE_BLOCKED)) {
            return;
        }

        throw new RuntimeException("You are not paired with client " + clientId);
    }

    @Override
    public void unblockClient(String clientId) {
        requireIdentification();

        logCall("unblockClient(" + clientId + ")");

        TalkRelationship rel = mDatabase.findRelationshipBetween(mConnection.getClientId(), clientId);
        if (rel == null) {
            throw new RuntimeException("You are not paired with client " + clientId);
        }

        String oldState = rel.getState();

        if (oldState.equals(TalkRelationship.STATE_FRIEND)) {
            return;
        }
        if (oldState.equals(TalkRelationship.STATE_BLOCKED)) {
            setRelationship(mConnection.getClientId(), clientId, TalkRelationship.STATE_FRIEND, true);
            return;
        }

        throw new RuntimeException("You are not paired with client " + clientId);
    }

    @Override
    public void depairClient(String clientId) {
        requireIdentification();

        logCall("depairClient(" + clientId + ")");

        TalkRelationship rel = mDatabase.findRelationshipBetween(mConnection.getClientId(), clientId);
        if (rel == null) {
            return;
        }

        setRelationship(mConnection.getClientId(), clientId, TalkRelationship.STATE_NONE, true);
        setRelationship(clientId, mConnection.getClientId(), TalkRelationship.STATE_NONE, true);
    }

    private void setRelationship(String thisClientId, String otherClientId, String state, boolean notify) {
        if (!TalkRelationship.isValidState(state)) {
            throw new RuntimeException("Invalid state " + state);
        }
        LOG.info("relationship between " + thisClientId + " and " + otherClientId + " is now " + state);
        TalkRelationship relationship = mDatabase.findRelationshipBetween(thisClientId, otherClientId);
        if (relationship == null) {
            relationship = new TalkRelationship();
        }
        relationship.setClientId(thisClientId);
        relationship.setOtherClientId(otherClientId);
        relationship.setState(state);
        relationship.setLastChanged(new Date());
        mDatabase.saveRelationship(relationship);
        if (notify) {
            mServer.getUpdateAgent().requestRelationshipUpdate(relationship);
        }
    }

    @Override
    public TalkDelivery[] deliveryRequest(TalkMessage message, TalkDelivery[] deliveries) {
        requireIdentification();

        logCall("deliveryRequest(" + deliveries.length + " deliveries)");

        // who is doing this again?
        String clientId = mConnection.getClientId();

        // generate and assign message id
        String messageId = UUID.randomUUID().toString();
        message.setMessageId(messageId);

        // guarantee correct sender
        message.setSenderId(clientId);

        // walk deliveries and determine which to accept,
        // filling in missing things as we go
        Vector<TalkDelivery> acceptedDeliveries = new Vector<TalkDelivery>();
        for (TalkDelivery d : deliveries) {
            // fill out various fields
            d.setMessageId(message.getMessageId());
            d.setSenderId(clientId);
            // perform the delivery request
            acceptedDeliveries.addAll(requestOneDelivery(message, d));
        }

        // update number of deliveries
        message.setNumDeliveries(acceptedDeliveries.size());

        // process all accepted deliveries
        if (!acceptedDeliveries.isEmpty()) {
            // save deliveries first so messages get collected
            for (TalkDelivery ds : acceptedDeliveries) {
                mDatabase.saveDelivery(ds);
            }
            // save the message
            mDatabase.saveMessage(message);
            // initiate delivery for all recipients
            for (TalkDelivery ds : acceptedDeliveries) {
                mServer.getDeliveryAgent().requestDelivery(ds.getReceiverId());
            }
        }

        mStatistics.signalMessageAcceptedSucceeded();

        // done - return whatever we are left with
        return deliveries;
    }

    private Vector<TalkDelivery> requestOneDelivery(TalkMessage m, TalkDelivery d) {
        Vector<TalkDelivery> result = new Vector<TalkDelivery>();

        // get the current date for stamping
        Date currentDate = new Date();
        // who is doing this again?
        String clientId = mConnection.getClientId();
        // get the receiver
        String receiverId = d.getReceiverId();

        // if we don't have a receiver try group delivery
        if (receiverId == null) {
            String groupId = d.getGroupId();
            // if
            if (groupId == null) {
                LOG.info("delivery rejected: no receiver");
                d.setState(TalkDelivery.STATE_FAILED);
                return result;
            }
            // check that group exists
            TalkGroup group = mDatabase.findGroupById(groupId);
            if (group == null) {
                LOG.info("delivery rejected: no such group");
                d.setState(TalkDelivery.STATE_FAILED);
                return result;
            }
            // check that sender is member of group
            TalkGroupMember clientMember = mDatabase.findGroupMemberForClient(groupId, clientId);
            if (clientMember == null || !clientMember.isMember()) {
                LOG.info("delivery rejected: not a member of group");
                d.setState(TalkDelivery.STATE_FAILED);
                return result;
            }
            // deliver to each group member
            List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
            for (TalkGroupMember member : members) {
                if (member.getClientId().equals(clientId)) {
                    continue;
                }
                if (!member.isJoined()) {
                    continue;
                }
                TalkDelivery memberDelivery = new TalkDelivery();
                memberDelivery.setMessageId(m.getMessageId());
                memberDelivery.setMessageTag(d.getMessageTag());
                memberDelivery.setGroupId(groupId);
                memberDelivery.setSenderId(clientId);
                memberDelivery.setKeyId(member.getMemberKeyId());
                memberDelivery.setKeyCiphertext(member.getEncryptedGroupKey());
                memberDelivery.setReceiverId(member.getClientId());
                memberDelivery.setState(TalkDelivery.STATE_DELIVERING);
                memberDelivery.setTimeAccepted(currentDate);

                boolean success = performOneDelivery(m, memberDelivery);
                if (success) {
                    result.add(memberDelivery);
                    // group deliveries are confirmed from acceptance
                    d.setState(TalkDelivery.STATE_CONFIRMED);
                    // set delivery timestamps
                    d.setTimeAccepted(currentDate);
                    d.setTimeChanged(currentDate);
                }
            }

        } else {
            // find relationship between clients, if there is one
            TalkRelationship relationship = mDatabase.findRelationshipBetween(receiverId, clientId);

            // reject if there is no relationship
            if (relationship == null) {
                LOG.info("delivery rejected: client " + receiverId + " has no relationship with sender");
                d.setState(TalkDelivery.STATE_FAILED);
                return result;
            }

            // reject unless befriended
            if (!relationship.getState().equals(TalkRelationship.STATE_FRIEND)) {
                LOG.info("delivery rejected: client " + receiverId
                        + " is not a friend of sender (relationship is " + relationship.getState() + ")");
                d.setState(TalkDelivery.STATE_FAILED);
                return result;
            }

            boolean success = performOneDelivery(m, d);
            if (success) {
                result.add(d);
                // mark delivery as in progress
                d.setState(TalkDelivery.STATE_DELIVERING);
                // set delivery timestamps
                d.setTimeAccepted(currentDate);
                d.setTimeChanged(currentDate);
            }
        }
        return result;
    }

    private boolean performOneDelivery(TalkMessage m, TalkDelivery d) {
        // get the current date for stamping
        Date currentDate = new Date();
        // who is doing this again?
        String clientId = mConnection.getClientId();
        // get the receiver
        String receiverId = d.getReceiverId();

        // reject messages to self
        if (receiverId.equals(clientId)) {
            LOG.info("delivery rejected: send to self");
            // mark delivery failed
            d.setState(TalkDelivery.STATE_FAILED);
            return false;
        }

        // reject messages to nonexisting clients
        //   XXX this check does not currently work because findClient() creates instances
        TalkClient receiver = mDatabase.findClientById(receiverId);
        if (receiver == null) {
            LOG.info("delivery rejected: client " + receiverId + " does not exist");
            // mark delivery failed
            d.setState(TalkDelivery.STATE_FAILED);
            return false;
        }

        // all fine, delivery accepted
        LOG.info("delivery accepted: client " + receiverId);
        // return
        return true;
    }

    @Override
    public TalkDelivery deliveryConfirm(String messageId) {
        requireIdentification();
        logCall("deliveryConfirm(" + messageId + ")");
        String clientId = mConnection.getClientId();
        TalkDelivery d = mDatabase.findDelivery(messageId, clientId);
        if (d != null) {
            if (d.getState().equals(TalkDelivery.STATE_DELIVERING)) {
                LOG.info("confirmed " + messageId + " for " + clientId);
                setDeliveryState(d, TalkDelivery.STATE_DELIVERED);
                mStatistics.signalMessageConfirmedSucceeded();
            }
        }
        return d;
    }

    @Override
    public TalkDelivery deliveryAcknowledge(String messageId, String recipientId) {
        requireIdentification();
        logCall("deliveryAcknowledge(" + messageId + "," + recipientId + ")");
        TalkDelivery d = mDatabase.findDelivery(messageId, recipientId);
        if (d != null) {
            if (d.getState().equals(TalkDelivery.STATE_DELIVERED)) {
                LOG.info("acknowledged " + messageId + " for " + recipientId);
                setDeliveryState(d, TalkDelivery.STATE_CONFIRMED);
                mStatistics.signalMessageAcknowledgedSucceeded();
            }
        }
        return d;
    }

    // TODO: do not allow abortion of message that are already delivered or confirmed
    @Override
    public TalkDelivery deliveryAbort(String messageId, String recipientId) {
        requireIdentification();
        logCall("deliveryAbort(" + messageId + "," + recipientId);
        String clientId = mConnection.getClientId();
        TalkDelivery delivery = mDatabase.findDelivery(messageId, recipientId);
        if (delivery != null) {
            if (recipientId.equals(clientId)) {
                // abort incoming delivery, regardless of sender
                setDeliveryState(delivery, TalkDelivery.STATE_ABORTED);
            } else {
                // abort outgoing delivery iff we are the actual sender
                if (delivery.getSenderId().equals(clientId)) {
                    setDeliveryState(delivery, TalkDelivery.STATE_ABORTED);
                }
            }
        }
        return delivery;
    }

    private void setDeliveryState(TalkDelivery delivery, String state) {
        delivery.setState(state);
        delivery.setTimeChanged(new Date());
        mDatabase.saveDelivery(delivery);
        if (state.equals(TalkDelivery.STATE_DELIVERED)) {
            mServer.getDeliveryAgent().requestDelivery(delivery.getSenderId());
        } else if (state.equals(TalkDelivery.STATE_DELIVERING)) {
            mServer.getDeliveryAgent().requestDelivery(delivery.getReceiverId());
        } else if (delivery.isFinished()) {
            mServer.getCleaningAgent().cleanFinishedDelivery(delivery);
        }
    }

    @Override
    public String createGroup(TalkGroup group) {
        requireIdentification();
        logCall("createGroup(" + group.getGroupTag() + ")");
        group.setGroupId(UUID.randomUUID().toString());
        group.setState(TalkGroup.STATE_EXISTS);
        TalkGroupMember groupAdmin = new TalkGroupMember();
        groupAdmin.setClientId(mConnection.getClientId());
        groupAdmin.setGroupId(group.getGroupId());
        groupAdmin.setRole(TalkGroupMember.ROLE_ADMIN);
        groupAdmin.setState(TalkGroupMember.STATE_JOINED);
        changedGroup(group);
        changedGroupMember(groupAdmin);
        return group.getGroupId();
    }

    @Override
    public TalkGroup[] getGroups(Date lastKnown) {
        requireIdentification();
        logCall("getGroups(" + lastKnown + ")");
        List<TalkGroup> groups = mDatabase.findGroupsByClientIdChangedAfter(mConnection.getClientId(), lastKnown);
        TalkGroup[] res = new TalkGroup[groups.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = groups.get(i);
        }
        return res;
    }

    @Override
    public void updateGroupName(String groupId, String name) {
        requireIdentification();
        requiredGroupAdmin(groupId);
        logCall("updateGroupName(" + groupId + "," + name + ")");
        TalkGroup targetGroup = mDatabase.findGroupById(groupId);
        targetGroup.setGroupName(name);
        changedGroup(targetGroup);
    }

    @Override
    public void updateGroupAvatar(String groupId, String avatarUrl) {
        requireIdentification();
        requiredGroupAdmin(groupId);
        logCall("updateGroupAvatar(" + groupId + "," + avatarUrl + ")");
        TalkGroup targetGroup = mDatabase.findGroupById(groupId);
        targetGroup.setGroupAvatarUrl(avatarUrl);
        changedGroup(targetGroup);
    }

    @Override
    public void updateGroup(TalkGroup group) {
        requireIdentification();
        requiredGroupAdmin(group.getGroupId());
        logCall("updateGroup(" + group.getGroupId() + ")");
        TalkGroup targetGroup = mDatabase.findGroupById(group.getGroupId());
        targetGroup.setGroupName(group.getGroupName());
        targetGroup.setGroupAvatarUrl(group.getGroupAvatarUrl());
        changedGroup(targetGroup);
    }

    @Override
    public void deleteGroup(String groupId) {
        requireIdentification();
        requiredGroupAdmin(groupId);
        logCall("deleteGroup(" + groupId + ")");

        TalkGroup group = mDatabase.findGroupById(groupId);
        if (group == null) {
            throw new RuntimeException("Group does not exist");
        }

        // mark the group as deleted
        group.setState(TalkGroup.STATE_NONE);
        changedGroup(group);

        // walk the group and make everyone have a "none" relationship to it
        List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
        for (TalkGroupMember member : members) {
            if (member.isInvited() || member.isJoined()) {
                member.setState(TalkGroupMember.STATE_GROUP_REMOVED);
                changedGroupMember(member);
            }
        }
    }

    // touch presences of all members
    private void touchGroupMemberPresences(String groupId) {
        List<TalkGroupMember> members = mDatabase.findGroupMembersById(groupId);
        for (TalkGroupMember m : members) {
            if (m.isInvited() || m.isJoined()) {
                TalkPresence p = mDatabase.findPresenceForClient(m.getClientId());
                if (p != null) {
                    p.setTimestamp(new Date());
                    mDatabase.savePresence(p);
                }
            }
        }
    }

    @Override
    public void inviteGroupMember(String groupId, String clientId) {
        requireIdentification();
        requiredGroupAdmin(groupId);
        logCall("inviteGroupMember(" + groupId + "/" + clientId + ")");
        // check that the client exists
        TalkClient client = mDatabase.findClientById(clientId);
        if (client == null) {
            throw new RuntimeException("No such client");
        }
        // XXX need to apply blocklist here?
        // get or create the group member
        TalkGroupMember member = mDatabase.findGroupMemberForClient(groupId, clientId);
        if (member == null) {
            member = new TalkGroupMember();
        }
        // perform the invite
        if (member.getState().equals(TalkGroupMember.STATE_NONE)) {
            // set up the member
            member.setGroupId(groupId);
            member.setClientId(clientId);
            member.setState(TalkGroupMember.STATE_INVITED);
            changedGroupMember(member);
            //  NOTE if this gets removed then the invited users presence might
            //       need touching depending on what the solution to the update problem is
             // notify various things
            touchGroupMemberPresences(groupId);
            mServer.getUpdateAgent().requestGroupUpdate(groupId, clientId);
            mServer.getUpdateAgent().requestPresenceUpdateForGroup(clientId, groupId);
            mServer.getUpdateAgent().requestPresenceUpdate(clientId);
        } else {
            throw new RuntimeException("Already invited or member to group");
        }
    }

    @Override
    public void joinGroup(String groupId) {
        requireIdentification();
        logCall("joinGroup(" + groupId + ")");

        String clientId = mConnection.getClientId();

        TalkGroupMember member = mDatabase.findGroupMemberForClient(groupId, clientId);
        if (member == null) {
            throw new RuntimeException("Group does not exist");
        }

        if (!member.getState().equals(TalkGroupMember.STATE_INVITED)) {
            throw new RuntimeException("Not invited to group");
        }

        member.setState(TalkGroupMember.STATE_JOINED);

        changedGroupMember(member);
    }

    @Override
    public void leaveGroup(String groupId) {
        requireIdentification();
        TalkGroupMember member = requiredGroupInvitedOrMember(groupId);
        logCall("leaveGroup(" + groupId + ")");
        // set membership state to NONE
        member.setState(TalkGroupMember.STATE_NONE);
        // degrade anyone who leaves to member
        member.setRole(TalkGroupMember.ROLE_MEMBER);
        // save the whole thing
        changedGroupMember(member);
    }

    @Override
    public void removeGroupMember(String groupId, String clientId) {
        requireIdentification();
        logCall("removeGroupMember(" + groupId + "/" + clientId + ")");
        requiredGroupAdmin(groupId);
        TalkGroupMember targetMember = mDatabase.findGroupMemberForClient(groupId, clientId);
        if (targetMember == null) {
            throw new RuntimeException("Client is not a member of group");
        }
        // set membership state to NONE
        targetMember.setState(TalkGroupMember.STATE_NONE);
        // degrade removed users to member
        targetMember.setRole(TalkGroupMember.ROLE_MEMBER);
        changedGroupMember(targetMember);
    }

    @Override
    public void updateGroupRole(String groupId, String clientId, String role) {
        requireIdentification();
        logCall("updateGroupRole(" + groupId + "/" + clientId + "," + role + ")");
        requiredGroupAdmin(groupId);
        TalkGroupMember targetMember = mDatabase.findGroupMemberForClient(groupId, clientId);
        if (targetMember == null) {
            throw new RuntimeException("Client is not a member of group");
        }
        if (!TalkGroupMember.isValidRole(role)) {
            throw new RuntimeException("Invalid role");
        }
        targetMember.setRole(role);
        changedGroupMember(targetMember);
    }

    @Override
    public void updateGroupKey(String groupId, String clientId, String keyId, String key) {
        requireIdentification();
        logCall("updateGroupKey(" + groupId + "/" + clientId + "," + keyId + ")");
        TalkGroupMember selfMember = mDatabase.findGroupMemberForClient(groupId, mConnection.getClientId());
        if (selfMember == null || !(selfMember.isAdmin() || (selfMember.isMember() && clientId.equals(mConnection.getClientId())))) {
            throw new RuntimeException("updateGroupKeys: insufficient permissions");
        }
        TalkGroupMember targetMember = mDatabase.findGroupMemberForClient(groupId, clientId);
        if (targetMember == null) {
            throw new RuntimeException("Client is not a member of group");
        }
        targetMember.setMemberKeyId(keyId);
        targetMember.setEncryptedGroupKey(key);
        changedGroupMember(targetMember);
    }

    @Override
    public String[] updateGroupKeys(String groupId, String sharedKeyId, String sharedKeyIdSalt, String[] clientIds, String[] publicKeyIds, String[] cryptedSharedKeys) {
        requireIdentification();
        logCall("updateGroupKeys(" + groupId + "/" + sharedKeyId + "," + sharedKeyIdSalt + clientIds.length + " " +publicKeyIds.length + " " +cryptedSharedKeys.length+ ")");
        if (!(clientIds.length == publicKeyIds.length && clientIds.length == cryptedSharedKeys.length)) {
            throw new RuntimeException("array length mismatches");
        }
        TalkGroupMember selfMember = mDatabase.findGroupMemberForClient(groupId, mConnection.getClientId());
        if (selfMember == null || !(selfMember.isAdmin() || selfMember.isMember()) ) {
            throw new RuntimeException("updateGroupKeys: insufficient permissions");
        }
        for (int i = 0; i < clientIds.length; ++i) {
            TalkGroupMember targetMember = mDatabase.findGroupMemberForClient(groupId, clientIds[i]);
            if (targetMember == null) {
                throw new RuntimeException("Client is not a member of group");
            }
            targetMember.setMemberKeyId(publicKeyIds[i]);
            targetMember.setEncryptedGroupKey(cryptedSharedKeys[i]);
            targetMember.setSharedKeyId(sharedKeyId);
            targetMember.setSharedKeyIdSalt(sharedKeyIdSalt);
            targetMember.setKeySupplier(mConnection.getClientId());
            changedGroupMember(targetMember);
        }

        TalkGroup group = mDatabase.findGroupById(groupId);
        group.setSharedKeyId(sharedKeyId);
        group.setSharedKeyIdSalt(sharedKeyIdSalt);
        group.setKeySupplier(mConnection.getClientId());
        changedGroup(group);

        String[] activeStates = {TalkGroupMember.STATE_INVITED, TalkGroupMember.STATE_JOINED};
        List<TalkGroupMember> members = mDatabase.findGroupMembersByIdWithStates(groupId, activeStates);
        List<String> outOfDateMembers = new ArrayList<String>();
        for (int i = 0; i < members.size(); ++i) {
            if (!sharedKeyId.equals(members.get(i).getSharedKeyId())) {
                outOfDateMembers.add(members.get(i).getClientId());
            }
        }

        String[] res = new String[outOfDateMembers.size()];
        for (int i = 0; i < res.length; ++i) {
            res[i] = outOfDateMembers.get(i);
        }
        return res;

    }

    @Override
    public void updateGroupMember(TalkGroupMember member) {
        requireIdentification();
        logCall("updateGroupMember(" + member.getGroupId() + "/" + member.getClientId() + ")");
        requiredGroupAdmin(member.getGroupId());
        TalkGroupMember targetMember = mDatabase.findGroupMemberForClient(member.getGroupId(), member.getClientId());
        if (targetMember == null) {
            throw new RuntimeException("Client is not a member of group");
        }
        targetMember.setRole(member.getRole());
        targetMember.setEncryptedGroupKey(member.getEncryptedGroupKey());
        changedGroupMember(targetMember);
    }

    @Override
    public TalkGroupMember[] getGroupMembers(String groupId, Date lastKnown) {
        requireIdentification();
        logCall("getGroupMembers(" + groupId + "/" + lastKnown + ")");
        requiredGroupInvitedOrMember(groupId);

        List<TalkGroupMember> members = mDatabase.findGroupMembersByIdChangedAfter(groupId, lastKnown);
        TalkGroupMember[] res = new TalkGroupMember[members.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = members.get(i);
        }
        return res;
    }

    private void changedGroup(TalkGroup group) {
        group.setLastChanged(new Date());
        mDatabase.saveGroup(group);
        mServer.getUpdateAgent().requestGroupUpdate(group.getGroupId());
    }

    private void changedGroupMember(TalkGroupMember member) {
        member.setLastChanged(new Date());
        mDatabase.saveGroupMember(member);
        mServer.getUpdateAgent().requestGroupMembershipUpdate(member.getGroupId(), member.getClientId());
    }

    private TalkGroupMember requiredGroupAdmin(String groupId) {
        TalkGroupMember gm = mDatabase.findGroupMemberForClient(groupId, mConnection.getClientId());
        if (gm != null && gm.isAdmin()) {
            return gm;
        }
        throw new RuntimeException("Client is not an admin in group " + groupId);
    }

    private TalkGroupMember requiredGroupInvitedOrMember(String groupId) {
        TalkGroupMember gm = mDatabase.findGroupMemberForClient(groupId, mConnection.getClientId());
        if (gm != null && (gm.isInvited() || gm.isMember())) {
            return gm;
        }
        throw new RuntimeException("Client is not an member in group " + groupId);
    }

    @Override
    public FileHandles createFileForStorage(int contentLength) {
        requireIdentification();
        logCall("createFileForStorage(" + contentLength + ")");
        return mServer.getFilecacheClient()
                .createFileForStorage(mConnection.getClientId(), "application/octet-stream", contentLength);
    }

    @Override
    public FileHandles createFileForTransfer(int contentLength) {
        requireIdentification();
        logCall("createFileForTransfer(" + contentLength + ")");
        return mServer.getFilecacheClient()
                .createFileForTransfer(mConnection.getClientId(), "application/octet-stream", contentLength);
    }

    private void createGroupWithEnvironment(TalkEnvironment environment) {
        LOG.info("updateEnvironment: creating new group for client " + mConnection.getClientId());
        TalkGroup group = new TalkGroup();
        group.setGroupTag(UUID.randomUUID().toString());
        group.setGroupId(UUID.randomUUID().toString());
        group.setState(TalkGroup.STATE_EXISTS);
        group.setGroupName("Nearby" + "-" + group.getGroupId().substring(group.getGroupId().length() - 8));
        group.setGroupType(TalkGroup.GROUP_TYPE_NEARBY);
        TalkGroupMember groupAdmin = new TalkGroupMember();
        groupAdmin.setClientId(mConnection.getClientId());
        groupAdmin.setGroupId(group.getGroupId());
        groupAdmin.setRole(TalkGroupMember.ROLE_ADMIN);
        groupAdmin.setState(TalkGroupMember.STATE_JOINED);
        changedGroup(group);
        changedGroupMember(groupAdmin);

        environment.setGroupId(group.getGroupId());
        environment.setClientId(mConnection.getClientId());
        mDatabase.saveEnvironment(environment);
    }

    private void joinGroupWithEnvironment(TalkGroup group, TalkEnvironment environment) {
        LOG.info("updateEnvironment: creating new group for client " + mConnection.getClientId());

        TalkGroupMember groupAdmin = mDatabase.findGroupMemberForClient(group.getGroupId(), mConnection.getClientId());
        if (groupAdmin == null) {
            groupAdmin= new TalkGroupMember();
        }
        groupAdmin.setClientId(mConnection.getClientId());
        groupAdmin.setGroupId(group.getGroupId());
        groupAdmin.setRole(TalkGroupMember.ROLE_ADMIN);
        groupAdmin.setState(TalkGroupMember.STATE_JOINED);
        changedGroup(group);
        changedGroupMember(groupAdmin);

        environment.setGroupId(group.getGroupId());
        environment.setClientId(mConnection.getClientId());
        mDatabase.saveEnvironment(environment);
        touchGroupMemberPresences(group.getGroupId());
        mServer.getUpdateAgent().requestPresenceUpdateForGroup(mConnection.getClientId(), group.getGroupId());
        mServer.getUpdateAgent().requestPresenceUpdate(mConnection.getClientId());
    }

    public ArrayList<Pair<String, Integer>> findGroupSortedBySize(List<TalkEnvironment> matchingEnvironments) {

        Map<String, Integer> environmentsPerGroup = new HashMap<String, Integer>();
        for (int i = 0; i < matchingEnvironments.size(); ++i) {
            String key = matchingEnvironments.get(i).getGroupId();
            if (environmentsPerGroup.containsKey(key)) {
                environmentsPerGroup.put(key, environmentsPerGroup.get(key) + 1);
            } else {
                environmentsPerGroup.put(key, 1);
            }
        }
        environmentsPerGroup = MapUtil.sortByValueDescending(environmentsPerGroup);

        ArrayList<Pair<String, Integer>> result = new ArrayList<Pair<String, Integer>>();
        for (Map.Entry<String, Integer> entry : environmentsPerGroup.entrySet()) {
            result.add(new Pair<String, Integer>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @Override
    public String updateEnvironment(TalkEnvironment environment) {
        logCall("updateEnvironment(" + mConnection.getClientId() + ")");
        requireIdentification();
        environment.setTimeReceived(new Date());
        List<TalkEnvironment> matching = mDatabase.findEnvironmentsMatching(environment);
        TalkEnvironment myEnvironment = mDatabase.findEnvironmentByClientId(mConnection.getClientId());
        ArrayList<Pair<String, Integer>> environmentsPerGroup = findGroupSortedBySize(matching);
        for (TalkEnvironment te : matching) {
            if (te.getClientId().equals(mConnection.getClientId())) {
                // there is already a matching environment for us
                TalkGroupMember myMemberShip = mDatabase.findGroupMemberForClient(te.getGroupId(), te.getClientId());
                TalkGroup myGroup = mDatabase.findGroupById(te.getGroupId());
                if (myMemberShip != null && myGroup != null) {
                    if (myMemberShip.isAdmin() && myMemberShip.isJoined() && myGroup.getState().equals(TalkGroup.STATE_EXISTS)) {
                        // everything seems fine, but are we in the largest group?
                        if (environmentsPerGroup.size() > 1) {
                            if (!environmentsPerGroup.get(0).fst.equals(te.getGroupId())) {
                                // we are not in the largest group, lets move over
                                destroyEnvironment(myEnvironment);
                                // join the largest group
                                TalkGroup largestGroup = mDatabase.findGroupById(environmentsPerGroup.get(0).fst);
                                joinGroupWithEnvironment(largestGroup, environment);
                                return largestGroup.getGroupId();
                            }
                        }
                        // group membership has not changed, we are still fine
                        // just update enviroment
                        myEnvironment.updateWith(environment);
                        mDatabase.saveEnvironment(myEnvironment);
                        return myGroup.getGroupId();
                    }
                    // there is a group and a membership, but they seem to be tombstones, so lets ignore them, just get rid of the bad environment
                    mDatabase.deleteEnvironment(te);
                    TalkGroup largestGroup = mDatabase.findGroupById(environmentsPerGroup.get(0).fst);
                    joinGroupWithEnvironment(largestGroup, environment);
                    return largestGroup.getGroupId();
                }
            }
        }
        // when we got here, there is no environment for us in the matching list
        if (myEnvironment != null) {
            // we have an environment for another location that does not match, lets get rid of it
            destroyEnvironment(myEnvironment);
        }
        if (matching.size() > 0) {
            // join the largest group
            TalkGroup largestGroup = mDatabase.findGroupById(environmentsPerGroup.get(0).fst);
            joinGroupWithEnvironment(largestGroup, environment);
            return largestGroup.getGroupId();
        }
        // we are alone or first at the location, lets create a new group
        createGroupWithEnvironment(environment);
        return environment.getGroupId();
    }

    private void destroyEnvironment(TalkEnvironment environment) {
        logCall("destroyEnvironment(" + environment + ")");
        TalkGroup group = mDatabase.findGroupById(environment.getGroupId());
        TalkGroupMember member = mDatabase.findGroupMemberForClient(environment.getGroupId(), environment.getClientId());
        if (member != null && member.getState().equals(TalkGroupMember.STATE_JOINED)) {
            // remove my membership
            // set membership state to NONE
            member.setState(TalkGroupMember.STATE_NONE);
            // degrade removed users to member
            member.setRole(TalkGroupMember.ROLE_MEMBER);
            changedGroupMember(member);
            String[] states = {TalkGroupMember.STATE_JOINED};
            List<TalkGroupMember> membersLeft = mDatabase.findGroupMembersByIdWithStates(environment.getGroupId(), states);
            if (membersLeft.size() == 0) {
                // last member removed, remove group
                group.setState(TalkGroup.STATE_NONE);
                changedGroup(group);
            }
        }
        mDatabase.deleteEnvironment(environment);
    }

    @Override
    // TODO: possibly remove parameters or do something with them
    public void destroyEnvironment(String clientId, String groupId) {
        logCall("destroyEnvironment(" + mConnection.getClientId() + ")");
        requireIdentification();
        TalkEnvironment myEnvironment = mDatabase.findEnvironmentByClientId(mConnection.getClientId());
        if (myEnvironment != null) {
            destroyEnvironment(myEnvironment);
        }
    }
}
