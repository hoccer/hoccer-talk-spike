package com.hoccer.talk.server.rpc;

import com.hoccer.talk.model.*;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.ITalkServerStatistics;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.message.StaticSystemMessage;
import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingServer;
import com.hoccer.talk.util.MapUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
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

    private static final Logger LOG = Logger.getLogger(TalkRpcHandler.class);

    private static final Hex HEX = new Hex();
    private final Digest SRP_DIGEST = new SHA256Digest();
    private static final SecureRandom SRP_RANDOM = new SecureRandom();
    private static final SRP6Parameters SRP_PARAMETERS = SRP6Parameters.CONSTANTS_1024;
   /*
    // TODO: make configurable via TalkServerConfiguration
    private static final int TOKEN_LIFETIME_MIN = 60; // (seconds) at least 1 minute
    private static final int TOKEN_LIFETIME_MAX = 7 * 24 * 3600; // (seconds) at most 1 week
    private static final int TOKEN_MAX_USAGE = 1;
    private static final int PAIRING_TOKEN_MAX_USAGE_RANGE_MIN = 1;
    private static final int PAIRING_TOKEN_MAX_USAGE_RANGE_MAX = 50;

    // TODO: make configurable via TalkServerConfiguration
    private static final int MIN_WORLD_WIDE_GROUP_SIZE = 10;
    private static final int MAX_WORLD_WIDE_GROUP_SIZE = 20;
    */
    private final int TOKEN_LIFETIME_MIN;
    private final int TOKEN_LIFETIME_MAX;
    private final int TOKEN_MAX_USAGE;
    private final int PAIRING_TOKEN_MAX_USAGE_RANGE_MIN;
    private final int PAIRING_TOKEN_MAX_USAGE_RANGE_MAX;
    private final int MIN_WORLD_WIDE_GROUP_SIZE;
    private final int MAX_WORLD_WIDE_GROUP_SIZE;

    /**
     * Reference to server
     */
    final private TalkServer mServer;

    /**
     * Reference to database accessor
     */
    final private ITalkServerDatabase mDatabase;

    /**
     * Reference to stats collector
     */
    final private ITalkServerStatistics mStatistics;

    /**
     * Reference to connection object
     */
    final private TalkRpcConnection mConnection;

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

        TOKEN_LIFETIME_MIN = mServer.getConfiguration().getTokenLifeTimeMin();
        TOKEN_LIFETIME_MAX = mServer.getConfiguration().getTokenLifeTimeMax();
        TOKEN_MAX_USAGE = mServer.getConfiguration().getTokenMaxUsage();
        PAIRING_TOKEN_MAX_USAGE_RANGE_MIN = mServer.getConfiguration().getTokenMaxUsageRangeMin();
        PAIRING_TOKEN_MAX_USAGE_RANGE_MAX = mServer.getConfiguration().getTokenMaxUsageRangeMax();
        MIN_WORLD_WIDE_GROUP_SIZE = mServer.getConfiguration().getMinWorldwideGroupSize();
        MAX_WORLD_WIDE_GROUP_SIZE = mServer.getConfiguration().getMaxWorldwideGroupSize();
    }

    private void requireIsNotOutdated() {
        if (mConnection.isLegacyMode()) {
            throw new RuntimeException("Client too old");
        }
    }

    private void requireIdentification(boolean flagCheckOutdated) {
        LOG.debug("requireIdentification : flag is " + flagCheckOutdated);
        if (flagCheckOutdated) {
            requireIsNotOutdated();
        }
        if (!mConnection.isLoggedIn()) {
            throw new RuntimeException("Not logged in");
        }
    }

    private void requirePastIdentification() {
        if (!mConnection.wasLoggedIn()) {
            throw new RuntimeException("Was not logged in");
        }
    }

    private void logCall(String message) {
        if (mServer.getConfiguration().getLogAllCalls() || mConnection.isSupportMode()) {
            LOG.debug("[connectionId: '" + mConnection.getConnectionId() + "'] " + message);
        }
    }

    @Override
    public void bing() {
        logCall("bing()");
        requireIdentification(true);
    }

    @Override
    public void ready() {
        logCall("ready()");
        requireIdentification(true);
        mConnection.readyClient();
        // check if all our memberships have the right key and issue rekeying if not
        checkMembershipKeysForClient(mConnection.getClientId());
    }

    @Override
    public void finishedIncoming() {
        logCall("finishedIncoming()");
        requireIdentification(true);
        mServer.getDeliveryAgent().requestDelivery(mConnection.getClientId(), false);
    }


    @Override
    public Date getTime() {
        logCall("getTime()");
        return new Date();
    }

    @Override
    public TalkServerInfo hello(TalkClientInfo clientInfo) {
        logCall("hello()");
        requireIdentification(false);

        String tag = clientInfo.getSupportTag();
        if (tag != null && !tag.isEmpty()) {
            if (tag.equals(mServer.getConfiguration().getSupportTag())) {
                mConnection.activateSupportMode();
            } else {
                LOG.warn("[connectionId: '" + mConnection.getConnectionId() + "'] sent invalid support tag '" + tag + "'.");
            }
        } else {
            if (mConnection.isSupportMode()) {
                mConnection.deactivateSupportMode();
            }
        }

        updateClientHostInfo(clientInfo);

        TalkServerInfo serverInfo = new TalkServerInfo();
        serverInfo.setServerTime(new Date());
        serverInfo.setSupportMode(mConnection.isSupportMode());
        serverInfo.setVersion(mServer.getConfiguration().getVersion());
        serverInfo.setCommitId(mServer.getConfiguration().getGitInfo().commitId);

        List<String> protcolVersions = TalkRpcConnectionHandler.getCurrentProtocolVersions();
        for (String protcolVersion : protcolVersions) {
            serverInfo.addProtocolVersion(protcolVersion);
        }

        // This is the moment now where have sufficient information about the client to
        // send customized and potentially even personalized system messages.
        // *Note* Currently the only mechanism to send system messages is with alertUser()
        if (mConnection.isLegacyMode()) {
            mServer.getUpdateAgent().requestUserAlert(
                    mConnection.getClientId(),
                    StaticSystemMessage.Message.UPDATE_NAGGING);
        }
        requireIsNotOutdated();

        // keep disabled until issues with Apple are resolved - no call ist actually made right now, the following is just for testing purposes
        if ("iPhone OS".equals(clientInfo.getSystemName()) && clientInfo.getClientBuildNumber() >= 14528 && false) {
            mServer.getUpdateAgent().requestSettingUpdate(mConnection.getClientId(), "mpMediaAccess", "0", StaticSystemMessage.Message.UPDATE_SETTING_ENABLE_MP_MEDIA_ACCESS);
            //mServer.getUpdateAgent().requestSettingUpdate(mConnection.getClientId(), "mpMediaAccess", "1", StaticSystemMessage.Message.UPDATE_SETTING_ENABLE_MP_MEDIA_ACCESS);
        }

        return serverInfo;
    }

    private void updateClientHostInfo(TalkClientInfo clientInfo) {
        final String clientId = mConnection.getClientId();
        TalkClientHostInfo existing = mDatabase.findClientHostInfoForClient(clientId);
        if (existing == null) {
            LOG.debug("clientHostInfo for clientId '" + clientId + "' does not exist yet - creating a new one...");
            existing = new TalkClientHostInfo();
        } else {
            LOG.debug("clientHostInfo for clientId '" + clientId + "' already exists - using that...");
        }

        existing.updateWith(clientInfo);
        existing.setClientId(clientId);
        existing.setServerTime(new Date());

        mDatabase.saveClientHostInfo(existing);
    }

    @Override
    public String generateId() {
        logCall("generateId()");

        if (mConnection.isLoggedIn()) {
            LOG.error("generateId: Can't register while logged in, disconnecting: clientId="+mConnection.getClientId());
            mConnection.disconnectAfterRequest();
            throw new RuntimeException("Can't register while logged in");
        }
        if (mConnection.isRegistering()) {
            LOG.error("generateId: Can't register more than one identity per connection: clientId="+mConnection.getClientId());
            mConnection.disconnectAfterRequest();
            throw new RuntimeException("Can't register more than one identity per connection");
        }

        String clientId = UUID.randomUUID().toString();
        mConnection.beginRegistration(clientId);
        return clientId;
    }

    @Override
    public String srpRegister(String verifier, String salt) {
        logCall("srpRegister(verifier: '" + verifier + "', salt: '" + salt + "')");

        if (mConnection.isLoggedIn()) {
            LOG.error("srpRegister: Can't register while logged in, disconnecting: clientId="+mConnection.getClientId());
            mConnection.disconnectAfterRequest();
            throw new RuntimeException("Can't register while logged in");
        }

        String clientId = mConnection.getUnregisteredClientId();

        if (clientId == null) {
            throw new RuntimeException("You need to generate an id before registering");
        }

        // TODO: check verifier and salt for viability

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
    public String srpChangeVerifier(String verifier, String salt) {
        logCall("srpChangeVerifier(verifier: '" + verifier + "', salt: '" + salt + "')");
        synchronized (mConnection) {

            if (!mConnection.isLoggedIn()) {
                throw new RuntimeException("Must be logged in to change verifier");
            }

            String clientId = mConnection.getClientId();

            if (clientId == null) {
                throw new RuntimeException("You need to generate an id before registering");
            }

            // TODO: check verifier and salt for viability

            TalkClient client = mConnection.getClient();
            client.setSrpSalt(salt);
            client.setSrpVerifier(verifier);

            try {
                mDatabase.saveClient(client);
                //mStatistics.signalClientRegisteredSucceeded();
            } catch (RuntimeException e) {
                //mStatistics.signalClientRegisteredFailed();
                throw e;
            }
            return clientId;
        }
    }

    @Override
    public String srpSetRescueCode(String rescueCode) {
        logCall("srpSetRescueCode(rescueCode: '" + rescueCode +"')");
        synchronized (mConnection) {

            if (!mConnection.isLoggedIn()) {
                throw new RuntimeException("Must be logged in to change verifier");
            }

            String clientId = mConnection.getClientId();

            if (clientId == null) {
                throw new RuntimeException("You need to generate an id before registering");
            }

            // TODO: check verifier and salt for viability

            TalkClient client = mConnection.getClient();
            client.setRescueCode(rescueCode);
            client.setRescueCodeFails(0);

            try {
                mDatabase.saveClient(client);
            } catch (RuntimeException e) {
                throw e;
            }
            return clientId;
        }
    }

    @Override
    public String srpRescue(String clientId, String verifier, String salt, String rescueCode) {
        logCall("srpRescue(verifier: '" + verifier + "', salt: '" + salt + "')");
        synchronized (mConnection) {

            if (mConnection.isLoggedIn()) {
                throw new RuntimeException("Must not be logged in to rescue verifier");
            }
            TalkClient client = mDatabase.findClientById(clientId);
            if (client == null) {
                if (mDatabase.findDeletedClientById(clientId) != null) {
                    throw new RuntimeException("Client deleted");  // must not change this string, is checked on client side
                } else {
                    throw new RuntimeException("No such client");  // must not change this string, is checked on client side
                }
            }

            // verify SRP registration
            if (client.getSrpVerifier() == null || client.getSrpSalt() == null) {
                throw new RuntimeException("Not registered");   // must not change this string, is checked on client side
            }

            if (client.getRescueCode() == null | client.getRescueCode().length() == 0) {
                throw new RuntimeException("No rescue code");   // must not change this string, is checked on client side
            }
            if (!client.getRescueCode().equals(rescueCode)) {
                client.setRescueCodeFails(client.getRescueCodeFails() + 1);
                if (client.getRescueCodeFails() > 3) {
                    client.setRescueCode(null);
                    try {
                        mDatabase.saveClient(client);
                    } catch (RuntimeException e) {
                        throw e;
                    }
                    throw new RuntimeException("Wrong rescue failed too often");   // must not change this string, is checked on client side
                }
                try {
                    mDatabase.saveClient(client);
                } catch (RuntimeException e) {
                    throw e;
                }
                throw new RuntimeException("Wrong rescue code");   // must not change this string, is checked on client side
            }

            // TODO: check verifier and salt for viability
            client.setSrpSalt(salt);
            client.setSrpVerifier(verifier);

            try {
                mDatabase.saveClient(client);
            } catch (RuntimeException e) {
                throw e;
            }
            return clientId;
        }
    }


    @Override
    public String srpPhase1(String clientId, String A) {
        logCall("srpPhase1(clientId: '" + clientId + "', '" + A + "')");
        synchronized (mConnection) {
            LOG.info("srpPhase1: starting login for client " + clientId + " with [connectionId: '" + mConnection.getConnectionId() + "'] from "+mConnection.getRemoteAddress());
            try {
                // check if we aren't logged in already
                if (mConnection.isLoggedIn()) {
                    if (clientId == null || !mConnection.getClientId().equals(clientId)) {
                        LOG.error("srpPhase1: Bad authentication request while logged in, account clientId=" + mConnection.getClientId()
                                + ", incoming clientId=" + clientId
                                + " [connectionId: '" + mConnection.getConnectionId() + "']");
                        throw new RuntimeException("Bad client id while authenticating while logged in");
                    } else {
                        mConnection.disconnectAfterRequest();
                        throw new RuntimeException("Can not authenticate while logged in, disconnecting");
                    }
                }
                TalkRpcConnection otherConnection = mServer.getClientConnection(clientId);
                if (otherConnection != null) {
                    LOG.warn("srpPhase1: Client already/still logged in on other [connectionId: '" + otherConnection.getConnectionId() + "'], clientId=" + clientId + " [connectionId: '" + mConnection.getConnectionId() + "'] from "+mConnection.getRemoteAddress());
                    //mConnection.disconnectAfterRequest();
                    //throw new RuntimeException("Can not authenticate while logged in with other connection");
                }

                // create SRP state
                if (mSrpServer == null) {
                    mSrpServer = new SRP6VerifyingServer();
                } else {
                    mConnection.disconnectAfterRequest();
                    throw new RuntimeException("Can only attempt SRP once per connection, disconnecting");
                }

                // get client object
                mSrpClient = mDatabase.findClientById(clientId);
                if (mSrpClient == null) {
                    if (mDatabase.findDeletedClientById(clientId) != null) {
                        throw new RuntimeException("Client deleted");  // must not change this string, is checked on client side
                    } else {
                        throw new RuntimeException("No such client");  // must not change this string, is checked on client side
                    }
                }

                // verify SRP registration
                if (mSrpClient.getSrpVerifier() == null || mSrpClient.getSrpSalt() == null) {
                    throw new RuntimeException("Not registered");   // must not change this string, is checked on client side
                }

                // parse the salt from DB
                byte[] salt;
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
                LOG.info("srpPhase1: done Srp1 for client " + clientId + " with [connectionId: '" + mConnection.getConnectionId() + "']");
                // return our credentials for the client
                return credentials.toString(16);
            } catch (RuntimeException e) {
                mStatistics.signalClientLoginSRP1Failed();
                LOG.error("srpPhase1: Failed: '" + e.getMessage() + "' clientId = " + clientId + " with [connectionId: '" + mConnection.getConnectionId() + "']");
                throw e;
            }
        }
    }

    @Override
    public String srpPhase2(String M1) {
        logCall("srpPhase2('" + M1 + "')");
        synchronized (mConnection) {
            LOG.info("srpPhase2: continuing login for client " + mSrpClient.getClientId() + " with [connectionId: '" + mConnection.getConnectionId() + "']");

            try {
                // check if we aren't logged in already
                if (mConnection.isLoggedIn()) {
                    mConnection.disconnectAfterRequest();
                    throw new RuntimeException("Can't authenticate while logged in");
                }

                // verify we are in a good state to do phase2
                if (mSrpServer == null) {
                    throw new RuntimeException("Need to perform phase 1 first");
                }
                if (mSrpClient == null) {
                    throw new RuntimeException("Internal error in SRP phase 2");
                }

                // parse the string given by the client
                byte[] M1b;
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
                String clientId = mSrpClient != null ? mSrpClient.getClientId() : "unknown";
                LOG.error("srpPhase2: Failed: '"+e.getMessage()+"' clientId=" + clientId + " with [connectionId: '" + mConnection.getConnectionId() + "']");
                mConnection.disconnectAfterRequest();
                throw e;
            }
        }
    }

    @Override
    public void deleteAccount(String reason) {
        requireIdentification(true);
        logCall("deleteAccount(id: '" + mConnection.getClientId() + "', reason: '" + reason + "')");
        final String clientId = mConnection.getClientId();
        TalkClient client = mConnection.getClient();

        // make sure client can no longer log in and won't be found again for delivery stuff
        mDatabase.markClientDeleted(client, reason);
        mConnection.setDeleted(true, clientId);

        // handle deletion after we returned rpc call status to client
        mServer.getUpdateAgent().requestAccountDeletion(clientId);
    }

    @Override
    public void registerGcm(String registeredPackage, String registrationId) {
        requireIdentification(true);
        logCall("registerGcm(registeredPackage: '" + registeredPackage + "', registrationId: '" + registrationId + "')");
        TalkClient client = mConnection.getClient();
        client.setGcmPackage(registeredPackage);
        client.setGcmRegistration(registrationId);
        mDatabase.saveClient(client);
    }

    @Override
    public void unregisterGcm() {
        requireIdentification(true);
        logCall("unregisterGcm()");
        TalkClient client = mConnection.getClient();
        client.setGcmPackage(null);
        client.setGcmRegistration(null);
        mDatabase.saveClient(client);
    }

    @Override
    public void registerApns(String registrationToken) {
        requireIdentification(true);
        logCall("registerApns(registrationToken: '" + registrationToken + "')");
        // APNS occasionally returns these for no good reason
        if (registrationToken.isEmpty()) {
            return;
        }
        TalkClient client = mConnection.getClient();
        client.setApnsToken(registrationToken);
        mDatabase.saveClient(client);
    }

    @Override
    public void unregisterApns() {
        requireIdentification(true);
        logCall("unregisterApns()");
        TalkClient client = mConnection.getClient();
        client.setApnsToken(null);
        mDatabase.saveClient(client);
    }

    @Override
    public void hintApnsUnreadMessage(int numUnreadMessages) {
        requireIdentification(true);
        logCall("hintApnsUnreadMessages('" + numUnreadMessages + "' unread messages)");
        TalkClient client = mConnection.getClient();
        client.setApnsUnreadMessages(numUnreadMessages);
        client.setLastPushMessage(null);
        mDatabase.saveClient(client);
    }

    @Override
    public void setApnsMode(String mode) {
        requireIdentification(true);
        logCall("setApnsMode('" + mode + "')");
        if (TalkClient.APNS_MODE_DEFAULT.equals(mode) || TalkClient.APNS_MODE_BACKGROUND.equals(mode)) {
            TalkClient client = mConnection.getClient();
            client.setApnsMode(mode);
            mDatabase.saveClient(client);
        } else {
            throw new RuntimeException("Illegal apns mode:"+mode);
        }
    }


    @Override
    public TalkRelationship[] getRelationships(Date lastKnown) {
        requireIdentification(true);
        logCall("getRelationships(lastKnown: '" + lastKnown + "')");

        List<TalkRelationship> relationships =
                mDatabase.findRelationshipsChangedAfter(mConnection.getClientId(), lastKnown);

        // build the result array
        TalkRelationship[] result = new TalkRelationship[relationships.size()];
        int idx = 0;
        for (TalkRelationship r : relationships) {
            result[idx++] = r;
        }
        return result;
    }

    @Override
    public void updatePresence(TalkPresence presence) {
        requireIdentification(true);
        logCall("updatePresence()");
        updatePresence(presence, null);
    }

    @Override
    public void modifyPresence(TalkPresence presence) {
        requireIdentification(true);
        logCall("modifyPresence(presence: '" + presence + "')");
        Set<String> fields = presence.nonNullFields();
        updatePresence(presence, fields);
    }

    private void updatePresence(TalkPresence presence, Set<String> fields) {
        // find existing presence or create one
        TalkPresence existing = mDatabase.findPresenceForClient(mConnection.getClientId());
        if (existing == null) {
            existing = new TalkPresence();
        }

        boolean keyIdChanged = false;
        if (fields == null || fields.contains(TalkPresence.FIELD_KEY_ID)) {
            if (presence.getKeyId() != null && !presence.getKeyId().equals(existing.getKeyId())) {
                keyIdChanged = true;
            }
        }

        // update the presence with what we got
        existing.updateWith(presence, fields);
        existing.setClientId(mConnection.getClientId());
        existing.setTimestamp(new Date());

        if (fields == null || fields.contains(TalkPresence.FIELD_AVATAR_URL)) {
            // iOS clients did not migrate their avatar URLs when the Filecache URL changed.
            // These URLs can cause problems on Android clients. This workaround "redirects"
            // all avatar URLs to the current Filecache.
            // TODO: Remove this when all iOS clients migrated their avatar URLs
            existing.setAvatarUrl(urlOnCurrentFilecache(presence.getAvatarUrl()));
        }

        if (fields != null) {
            fields.add(TalkPresence.FIELD_CLIENT_ID);
            // if we do not send time stamp updates on presenceModified, we are more conservative and cause a full presence sync after login
            // fields.add(TalkPresence.FIELD_TIMESTAMP);
        }

        if (fields == null || fields.contains(TalkPresence.FIELD_CONNECTION_STATUS)) {
            if (presence.isOffline()) {
                // client os lying about it's presence
                existing.setConnectionStatus(TalkPresence.STATUS_ONLINE);
            } else if (presence.isConnected()) {
                existing.setConnectionStatus(presence.getConnectionStatus());
            } else {
                LOG.error("undefined connectionStatus in presence:" + presence.getConnectionStatus());
                existing.setConnectionStatus(TalkPresence.STATUS_ONLINE);
            }
        }

        mDatabase.savePresence(existing);

        mServer.getUpdateAgent().requestPresenceUpdate(mConnection.getClientId(), fields);
        if (keyIdChanged) {
            checkMembershipKeysForClient(mConnection.getClientId());
        }
    }

    private String urlOnCurrentFilecache(String urlString) {
        try {
            URL url = new URL(urlString);
            String downloadId = url.getPath().replace("/download/", "");
            return mServer.getConfiguration().getFilecacheDownloadBase() + downloadId;
        } catch (MalformedURLException e) {
            return urlString;
        }
    }

    @Override
    public TalkPresence[] getPresences(Date lastKnown) {
        requireIdentification(true);
        logCall("getPresences(lastKnown: '" + lastKnown + "')");

        if (lastKnown == null) {
            lastKnown = new Date(0);
        }

        List<TalkPresence> presences = mDatabase.findPresencesChangedAfter(mConnection.getClientId(), lastKnown);
        // update connection status and convert results to array
        TalkPresence[] result = new TalkPresence[presences.size()];
        for (int i = 0; i < result.length; i++) {
            TalkPresence presence = presences.get(i);
            if (presence.getConnectionStatus() == null) {
                presence.setConnectionStatus(mServer.isClientConnected(presence.getClientId())
                        ? TalkPresence.STATUS_ONLINE : TalkPresence.STATUS_OFFLINE);
            }
            result[i] = presences.get(i);
        }

        return result;
    }

    private void checkMembershipKeysForClient(String clientId) {
        LOG.debug("checkMembershipKeysForClient id "+clientId);
        TalkPresence myPresence = mDatabase.findPresenceForClient(clientId);
        if (myPresence == null) {
            throw new RuntimeException("no presence for client "+clientId);
        }
        if (myPresence.getKeyId() == null || myPresence.getKeyId().length() == 0) {
            throw new RuntimeException("no keyId in presence for client "+clientId);
        }
        checkMembershipKeysForClient(clientId, myPresence.getKeyId());
    }


    private void checkMembershipKeysForClient(String clientId, String keyId) {
        LOG.debug("checkMembershipKeysForClient id "+clientId+", keyid "+keyId);
        final List<TalkGroupMembership> myMemberships = mDatabase.findGroupMembershipsForClientWithStates(clientId, TalkGroupMembership.ACTIVE_STATES);

        if (myMemberships != null && myMemberships.size()>0) {
            final TalkKey key = mDatabase.findKey(mConnection.getClientId(), keyId);
            if (key == null) {
                throw new RuntimeException("checkMembershipKeyForClient: key with id "+keyId+" not found for client "+clientId);
            }
            if (key.getKey() == null || key.getKey().length() == 0) {
                throw new RuntimeException("checkMembershipKeyForClient: key with id "+keyId+" is empty for client "+clientId);
            }

            for (TalkGroupMembership membership : myMemberships) {
                TalkGroupPresence groupPresence = mDatabase.findGroupPresenceById(membership.getGroupId());
                boolean outDated = false;
                if (!keyId.equals(membership.getMemberKeyId())) {
                    outDated = true;
                } else if (groupPresence.getSharedKeyId() == null || groupPresence.getSharedKeyId().length() == 0) {
                    outDated = true;
                } else if (!groupPresence.getSharedKeyId().equals(membership.getSharedKeyId())) {
                    outDated = true;
                }
                if (outDated) {
                    LOG.debug("checkMembershipKeysForClient id "+clientId+", outdated keyid "+keyId+", requesting key update for group "+membership.getGroupId());
                    mServer.getUpdateAgent().checkAndRequestGroupMemberKeys(membership.getGroupId());
                } else {
                    LOG.debug("checkMembershipKeysForClient id "+clientId+", keyid "+keyId+", key ok for group "+membership.getGroupId());
                }
            }
        }
    }

    @Override
    public void updateKey(TalkKey key) {
        requireIdentification(true);
        logCall("updateKey()");
        if (verifyKey(key.getKeyId())) {
            LOG.info("updateKey for client " + mConnection.getClientId() + " ok with same valid keyid " + key.getKeyId());
            return;
        }
        if (key.getKeyId().equals(key.calcKeyId())) {
            key.setClientId(mConnection.getClientId());
            key.setTimestamp(new Date());
            LOG.info("updateKey for client " + mConnection.getClientId() + " ok with new valid keyid " + key.getKeyId());
            mDatabase.saveKey(key);
        } else {
            throw new RuntimeException("updateKey: keyid "+key.getKey()+" is not the id of "+key.getKey());
        }
    }

    @Override
    public boolean verifyKey(String keyId) {
        requireIdentification(true);
        logCall("verifyKey( keyId: '" + keyId + "')");

        TalkKey key = mDatabase.findKey(mConnection.getClientId(), keyId);
        if (key != null) {
            String storedKeyId = key.getKeyId();
            String realKeyId = key.calcKeyId();
            if (storedKeyId != null && storedKeyId.equals(keyId)) {
                if (storedKeyId.equals(realKeyId)) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public TalkKey getKey(String clientId, String keyId) {
        requireIdentification(true);
        logCall("getKey(clientId: '" + clientId + "', keyId: '" + keyId + "')");

        TalkKey key = null;

        TalkRelationship relationship = mDatabase.findRelationshipBetween(mConnection.getClientId(), clientId);
        //TalkRelationship otherRelationship = mDatabase.findRelationshipBetween(clientId, mConnection.getClientId());
        if ((relationship != null && relationship.isRelated()) /*|| (otherRelationship != null && otherRelationship.isRelated())*/) {
            key = mDatabase.findKey(clientId, keyId);
            if (key == null) {
                throw new RuntimeException("No key for clientId " + clientId + " with keyId " + keyId + " found");
            }
        } else {
            List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsForClient(mConnection.getClientId());
            for (TalkGroupMembership membership : memberships) {
                if (membership.isJoined() || membership.isInvited() || membership.isSuspended()) {
                    TalkGroupMembership otherMembership = mDatabase.findGroupMembershipForClient(membership.getGroupId(), clientId);
                    if (otherMembership != null && (otherMembership.isJoined() || otherMembership.isInvited() || otherMembership.isSuspended())) {
                        key = mDatabase.findKey(clientId, keyId);
                        if (key == null) {
                            throw new RuntimeException("No key for clientId " + clientId + " with keyId " + keyId + " found (2)");
                        }
                        break;
                    }
                }
            }
            if (key == null) {
                // treat former senders with unfinished deliveries as contact
                final List<TalkDelivery> deliveries = mDatabase.findDeliveriesForClientInState(mConnection.getClientId(), TalkDelivery.STATE_DELIVERING);
                for (TalkDelivery delivery : deliveries) {
                    if (clientId.equals(delivery.getSenderId())) {
                        key = mDatabase.findKey(clientId, keyId);
                        if (key == null) {
                            throw new RuntimeException("No key for clientId " + clientId + " with keyId " + keyId + " found (3)");
                        }
                        break;
                    }
                }
            }
            if (key == null) {
                final List<TalkDelivery> attachmentDeliveries =
                        mDatabase.findDeliveriesForClientInDeliveryAndAttachmentStates(mConnection.getClientId(),
                                TalkDelivery.IN_ATTACHMENT_DELIVERY_STATES, TalkDelivery.IN_ATTACHMENT_STATES);
                for (TalkDelivery delivery : attachmentDeliveries) {
                    if (clientId.equals(delivery.getSenderId())) {
                        key = mDatabase.findKey(clientId, keyId);
                        if (key == null) {
                            throw new RuntimeException("No key for clientId " + clientId + " with keyId " + keyId + " found");
                        }
                        break;
                    }
                }
            }
        }

        if (key == null) {
            throw new RuntimeException("No relationship exist with client "+clientId+" allowing to retrieve key");
        }

        return key;
    }

    @Override
    public String generateToken(String tokenPurpose, int secondsValid) {
        requireIdentification(true);
        logCall("generateToken(tokenPurpose: '" + tokenPurpose + "', secondsValid: '" + secondsValid + "')");

        // verify request
        if (!TalkToken.isValidPurpose(tokenPurpose)) {
            throw new RuntimeException("Invalid token purpose");
        }

        // constrain validity period
        secondsValid = Math.max(TOKEN_LIFETIME_MIN, secondsValid);
        secondsValid = Math.min(TOKEN_LIFETIME_MAX, secondsValid);

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
        token.setMaxUseCount(TOKEN_MAX_USAGE);

        // save the token
        mDatabase.saveToken(token);

        // return the secret
        return token.getSecret();
    }

    @Override
    public String generatePairingToken(int maxUseCount, int secondsValid) {
        requireIdentification(true);

        logCall("generatePairingToken(maxUseCount: '" + maxUseCount + "', secondsValid: '" + secondsValid + "')");

        // constrain validity period
        secondsValid = Math.max(TOKEN_LIFETIME_MIN, secondsValid);
        secondsValid = Math.min(TOKEN_LIFETIME_MAX, secondsValid);

        // constrain use count
        maxUseCount = Math.max(PAIRING_TOKEN_MAX_USAGE_RANGE_MIN, maxUseCount);
        maxUseCount = Math.min(PAIRING_TOKEN_MAX_USAGE_RANGE_MAX, maxUseCount);

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
        return createPasswordLowerChars(10);
        /*
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
        */
    }

    public static String createPasswordLowerChars(int n) {
        char[] pw = new char[n];
        for (int i=0; i < n; i++) {
            int c = 'a' +  (int)(Math.random() * 26);
            pw[i] = (char)c;
        }
        return new String(pw);
    }

    public static String createPassword(int n) {
        char[] pw = new char[n];
        int c  = 'A';
        int  r1 = 0;
        for (int i=0; i < n; i++)
        {
            r1 = (int)(Math.random() * 3);
            switch(r1) {
                case 0: c = '0' +  (int)(Math.random() * 10); break;
                case 1: c = 'a' +  (int)(Math.random() * 26); break;
                case 2: c = 'A' +  (int)(Math.random() * 26); break;
            }
            pw[i] = (char)c;
        }
        return new String(pw);
    }


    @Override
    public boolean pairByToken(String secret) {
        requireIdentification(true);
        logCall("pairByToken(secret: '" + secret + "')");

        TalkToken token = mDatabase.findTokenByPurposeAndSecret(
                TalkToken.PURPOSE_PAIRING, secret);

        // check if token exists
        if (token == null) {
            LOG.warn("pairByToken: no token could be found");
            return false;
        }

        // check if token is unused
        if (!token.isUsable()) {
            LOG.warn("pairByToken: token can no longer be used");
            return false;
        }

        // check if token is still valid
        if (token.getExpiryTime().before(new Date())) {
            LOG.warn("pairByToken: token has expired");
            return false;
        }

        // get relevant client IDs
        String myId = mConnection.getClientId();
        String otherId = token.getClientId();

        // reject self-pairing
        if (token.getClientId().equals(myId)) {
            LOG.warn("self-pairing rejected");
            return false;
        }

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, otherId)) {
            LOG.debug("performing token-based pairing between clients with id '" + myId + "' and '" + token.getClientId() + "'");

            // set up relationships
            setRelationship(myId, otherId, TalkRelationship.STATE_FRIEND, TalkRelationship.STATE_NONE, true);
            setRelationship(otherId, myId, TalkRelationship.STATE_FRIEND, TalkRelationship.STATE_NONE, true);

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
        }

        return true;
    }

    // This discussion is left here for better understanding the history of some changes that wer made:
    //
    //  Blocking can cause unidirectional relationsShips to be created that caused problems in some cases
    //  e.g. we got presence updates causing a blocked contact to be created, but when requesting isContactOf they will be
    //  immediately deleted; we might either not send out presence information for blocked contacts, but this might cause other
    //  problems, e.g. a stuck online indicator or not being able to unblock a contact after a client database loss
    //  Possible alternative solutions:
    //     a) always maintain bidirectional relationships
    //     b) make sure unidirectional relationships are properly handled everywhere (e.g. getPresences(), isContactOf, delivery, invitations, etc...)
    //  a) seems to be much easier
    //
    // However, b) actually turned out to be the way to go. The problem with the presence updates in getPresences() was
    // that there were presences sent out when there was a unidirectional relationship blocking *us*, which was wrong.
    // The problem was caused because initially someone thought that the same code as in performPresenceUpdate()
    // should be used, but the case is different there because performPresenceUpdate actually sees the clients from
    // the other side of a relationship.

    @Override
    public void blockClient(String clientId) {
        requireIdentification(true);
        logCall("blockClient(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {
            logCall("performing blockClient(id '" + clientId + "')");
            TalkRelationship rel = mDatabase.findRelationshipBetween(mConnection.getClientId(), clientId);
            if (rel == null || rel.isNone()) {
                setRelationship(mConnection.getClientId(), clientId, TalkRelationship.STATE_BLOCKED, TalkRelationship.STATE_NONE, true);
            } else if (rel.isFriend()) {
                setRelationship(mConnection.getClientId(), clientId, TalkRelationship.STATE_BLOCKED, TalkRelationship.STATE_FRIEND, true);
            } else if (rel.invitedMe() || rel.isInvited()) {
                // also take back or refuse invitation when blocking a client where an invitation from either side is pending
                // it also would be possible to restore the invitation state after unblocking, but this might cause problems
                // when the other side also blocks or disinvites, so this here is the most robust way of handling it
                setRelationship(mConnection.getClientId(), clientId, TalkRelationship.STATE_BLOCKED, TalkRelationship.STATE_NONE, true);
                setRelationship(clientId, mConnection.getClientId(), TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);
            } else if (rel.isBlocked()) {
                throw new RuntimeException("already blocked");
            } else {
                throw new RuntimeException("illegal state");
            }
        }
    }

    // This function must also work with unidirectional relationships, and it easily does
    // because it deals with one relationship only.
    @Override
    public void unblockClient(String clientId) {
        requireIdentification(true);
        logCall("unblockClient(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {
            logCall("performing unblockClient(id '" + clientId + "')");
            TalkRelationship rel = mDatabase.findRelationshipBetween(myId, clientId);
            if (rel == null) {
                throw new RuntimeException("You are not paired with client with id '" + clientId + "'");
            }

            if (rel.isBlocked()) {
                String unblockState = rel.getUnblockState();
                if (!TalkRelationship.isValidState(unblockState)) {
                    LOG.warn("unblockClient "+clientId+", invalid unblockState '"+unblockState+"', setting to state to 'none'");
                    unblockState = TalkRelationship.STATE_NONE;
                }
                setRelationship(myId, clientId, unblockState, unblockState, true);
                return;
            } else {
                throw new RuntimeException("You have not blocked the client with id '" + clientId + "'");
            }
        }
    }

    //
    // This function must also work with unidirectional relationships, and it is a bit harder
    // because it deals with both sides of the relationship and we have at least.
    //
    @Override
    public void inviteFriend(String clientId) {
        requireIdentification(true);
        logCall("inviteFriend(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {

            TalkRelationship rel = mDatabase.findRelationshipBetween(myId, clientId);
            TalkRelationship reverse_rel = mDatabase.findRelationshipBetween(clientId,myId);

            if (rel != null) {
                logCall("inviteFriend(id '" + clientId + "'), found relationsShip in state '"+rel.getState()+"'");
                /*
                if (reverse_rel == null) {
                    throw new RuntimeException("Server error: Relationship exists, but no reverse relationship found");
                }
                */
                if (rel.isFriend()) {
                    throw new RuntimeException("Contact is already a friend");
                }
                if (rel.isBlocked()) {
                    throw new RuntimeException("Caller has blocked client, client must unblock contact first");
                }
                if (rel.invitedMe()) {
                    throw new RuntimeException("Invited client has already invited me, must accept invitation");
                }
            }
            if (reverse_rel != null) {
                logCall("inviteFriend(id '" + clientId + "'), found reverse relationsShip in state '"+reverse_rel.getState()+"'");
                /*
                if (rel == null) {
                    throw new RuntimeException("Server error: Reverse Relationship exists, but relationship to client not found");
                }
                */
                if (reverse_rel.isBlocked()) {
                    throw new RuntimeException("Contact has blocked client"); // TODO: leaks information about blocking to client, how shall we deal with it?
                }
                if (reverse_rel.isFriend()) {
                    throw new RuntimeException("Server Error: Contact is already a friend, but not friend relationship to caller exists");
                }
                if (reverse_rel.isInvited()) {
                    throw new RuntimeException("Server Error: Invited client has already invited me, but relationship does not have proper state");
                }
            }

            setRelationship(myId, clientId, TalkRelationship.STATE_INVITED, TalkRelationship.STATE_NONE, true);
            setRelationship(clientId, myId, TalkRelationship.STATE_INVITED_ME, TalkRelationship.STATE_NONE, true);
        }
        // give only invited user a presence update from inviting client
        //mServer.getUpdateAgent().requestPresenceUpdateForClient(clientId, myId);
        mServer.getUpdateAgent().requestPresenceUpdateForClient(myId, clientId);
    }

    @Override
    public void disinviteFriend(String clientId) {
        requireIdentification(true);
        logCall("disinviteFriend(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {
            logCall("performing disinviteFriend(id '" + clientId + "')");

            TalkRelationship rel = mDatabase.findRelationshipBetween(myId, clientId);
            TalkRelationship reverse_rel = mDatabase.findRelationshipBetween(clientId,myId);

            if (rel == null) {
                throw new RuntimeException("No relationship exists with client with id '" + clientId + "'");
            }
            if (reverse_rel == null) {
                throw new RuntimeException("No reverse relationship exists with client with id '" + clientId + "'");
            }
            if (!rel.isInvited()) {
                throw new RuntimeException("Client with id '" + clientId + "'" +"is not invited");
            }
            setRelationship(myId, clientId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);
            if (reverse_rel.invitedMe()) {
                setRelationship(clientId, myId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);
            }
        }
    }

    @Override
    public void acceptFriend(String clientId) {
        requireIdentification(true);
        logCall("acceptFriend(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {
            logCall("performing acceptFriend(id '" + clientId + "')");
            TalkRelationship rel = mDatabase.findRelationshipBetween(myId, clientId);
            TalkRelationship reverse_rel = mDatabase.findRelationshipBetween(clientId,myId);

            if (rel == null) {
                throw new RuntimeException("No relationship exists with client with id '" + clientId + "'");
            }
            if (reverse_rel == null) {
                throw new RuntimeException("No reverse relationship exists with client with id '" + clientId + "'");
            }
            if (!rel.invitedMe()) {
                throw new RuntimeException("Relationship to client with id '" + clientId + "'" +"is not in invitedMe state");
            }
            if (!reverse_rel.isInvited()) {
                throw new RuntimeException("Relationship from client with id '" + clientId + "'" +"is not in invited state");
            }
            setRelationship(clientId, myId, TalkRelationship.STATE_FRIEND, TalkRelationship.STATE_NONE, true);
            setRelationship(myId, clientId, TalkRelationship.STATE_FRIEND, TalkRelationship.STATE_NONE, true);

            // send each other current presence to new befriended pair
            mServer.getUpdateAgent().requestPresenceUpdateForClient(clientId, myId);
            mServer.getUpdateAgent().requestPresenceUpdateForClient(myId, clientId);
        }
    }

    @Override
    public void refuseFriend(String clientId) {
        requireIdentification(true);
        logCall("acceptFriend(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {
            logCall("performing acceptFriend(id '" + clientId + "')");
            TalkRelationship rel = mDatabase.findRelationshipBetween(myId, clientId);
            TalkRelationship reverse_rel = mDatabase.findRelationshipBetween(clientId,myId);

            if (rel == null) {
                throw new RuntimeException("No relationship exists with client with id '" + clientId + "'");
            }
            if (reverse_rel == null) {
                throw new RuntimeException("No reverse relationship exists with client with id '" + clientId + "'");
            }
            if (!rel.invitedMe()) {
                throw new RuntimeException("Relationship to client with id '" + clientId + "'" +"is not in invitedMe state");
            }
            if (!reverse_rel.isInvited()) {
                throw new RuntimeException("Relationship from client with id '" + clientId + "'" +"is not in invited state");
            }
            setRelationship(clientId, myId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);
            setRelationship(myId, clientId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);
        }
    }

    /* The old version of this function had a security hole;
    A blocked client could have

     */

    @Override
    public void depairClient(String clientId) {
        requireIdentification(true);

        logCall("depairClient(id '" + clientId + "')");

        String myId = mConnection.getClientId();

        synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, clientId)) {
            logCall("performing depairClient(id '" + clientId + "')");
            TalkRelationship rel = mDatabase.findRelationshipBetween(myId, clientId);
            if (rel == null) {
                return;
            }
            setRelationship(myId, clientId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);

            /* The old version of this function had a security hole:
               A blocked client could have called blockClient() itself and then depairClient() to
                get rid of the block. We check for this now.
                We also avoid creating a new reverse relationship if there is none.
             */
            TalkRelationship reverse_rel = mDatabase.findRelationshipBetween(clientId, myId);
            if (reverse_rel != null) {
                if (reverse_rel.isBlocked()) {
                    setRelationship(clientId, myId, TalkRelationship.STATE_BLOCKED, TalkRelationship.STATE_NONE, true);
                } else {
                    setRelationship(clientId, myId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, true);
                }
            }
        }
    }

    @Override
    public void setClientNotifications(String otherClientId, String preference) {
        logCall("setClientNotifications(otherClientId: '" + otherClientId + ","+preference+"')");
        requireIdentification(true);
        if (TalkGroupMembership.isValidNotificationPreference(preference)) {
            String myId = mConnection.getClientId();

            synchronized (mServer.dualIdLock(TalkRelationship.LOCK_PREFIX, myId, otherClientId)) {
                logCall("performing setClientNotifications(otherClientId: '" + otherClientId + ","+preference+"')");
                TalkRelationship relationship = mDatabase.findRelationshipBetween(myId, otherClientId);

                if (relationship == null) {
                    relationship = setRelationship(myId, otherClientId, TalkRelationship.STATE_NONE, TalkRelationship.STATE_NONE, false);
                }

                relationship.setLastChanged(new Date());
                relationship.setNotificationPreference(preference);
                mDatabase.saveRelationship(relationship);
                mServer.getUpdateAgent().requestRelationshipUpdate(relationship);
            }
        } else {
            throw new RuntimeException("Illegal notification preference:"+preference);
        }
    }

    private TalkRelationship setRelationship(String thisClientId, String otherClientId, String state, String unblockState, boolean notify) {
        if (!TalkRelationship.isValidState(state)) {
            throw new RuntimeException("Invalid state '" + state + "'");
        }
        TalkClient otherClient = mDatabase.findClientById(otherClientId);
        TalkRelationship relationship = mDatabase.findRelationshipBetween(thisClientId, otherClientId);
        if (otherClient == null && relationship == null) {
            throw new RuntimeException("Invalid client to relate to - does not exist!");
        }

        if (relationship == null) {
            relationship = new TalkRelationship();
        }

        relationship.setClientId(thisClientId);
        relationship.setOtherClientId(otherClientId);
        relationship.setState(state);
        relationship.setUnblockState(unblockState);
        relationship.setLastChanged(new Date());

        // always save and notify
        mDatabase.saveRelationship(relationship);
        LOG.debug("relationship between clients with id '" + thisClientId + "' and '" + otherClientId + "' is now in state '" + state + "'");
        if (notify) {
            mServer.getUpdateAgent().requestRelationshipUpdate(relationship);
        }
        return relationship;
    }

    @Override
    public TalkDelivery[] outDeliveryRequest(TalkMessage message, TalkDelivery[] deliveries) {
        requireIdentification(true);
        logCall("outDeliveryRequest(" + deliveries.length + " deliveries)");

        String clientId = mConnection.getClientId();
        if (clientId == null) {
            LOG.error("outDeliveryRequest null clientId on connection: '" + mConnection.getConnectionId() + "', address " + mConnection.getRemoteAddress());
        }
        boolean attachmentError = false;
        String attachmentErrorReason = "no error";

        boolean hasAttachment = message.getAttachment() != null && message.getAttachment().length() > 0;
        boolean hasAttachmentFileId = message.getAttachmentFileId() != null && message.getAttachmentFileId().length() > 0;

        if (hasAttachment && !hasAttachmentFileId) {
            attachmentError = true;
            attachmentErrorReason = "message is missing attachment file id while having attachment";
        }
        if (hasAttachmentFileId && !hasAttachment) {
            attachmentError = true;
            attachmentErrorReason = "message is missing attachment while having attachment file id";
        }

        // generate and assign message id
        String messageId = UUID.randomUUID().toString();
        message.setMessageId(messageId);

        // guarantee correct sender
        message.setSenderId(clientId);

        // walk deliveries and determine which to accept,
        // filling in missing things as we go
        Vector<TalkDelivery> acceptedDeliveries = new Vector<TalkDelivery>();
        Vector<TalkDelivery> resultDeliveries = new Vector<TalkDelivery>();
        for (TalkDelivery delivery : deliveries) {
            // fill out various fields
            delivery.ensureDates();
            delivery.setMessageId(message.getMessageId());
            delivery.setSenderId(clientId);

            boolean hasAttachmentState = delivery.getAttachmentState() != null && !TalkDelivery.ATTACHMENT_STATE_NONE.equals(delivery.getAttachmentState());

            if (!attachmentError) {
                if (hasAttachment && !hasAttachmentState) {
                    // TODO: The Android client does not properly set attachmentState on outgoing deliveries,
                    // see https://github.com/hoccer/hoccer-talk-spike/issues/1092
                    //attachmentError = true;
                    //attachmentErrorReason = "message has attachment but attachment state in delivery does not indicate that";
                    // TODO: remove fix below and uncomment the two lines above when client is fixed and update is forced
                }
                if (hasAttachmentState && !hasAttachment) {
                    attachmentError = true;
                    attachmentErrorReason = "attachment state in delivery indicates attachment but there is no attachment in this message";
                }
            }

            if (!attachmentError) {
                if (message.getAttachmentFileId() == null) {
                    delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_NONE);
                } else {
                    // TODO: remove this fix when clients will behave properly and the above error checking is fully enabled
                    if (delivery.getAttachmentState() == null) {
                        delivery.setAttachmentState(TalkDelivery.ATTACHMENT_STATE_NEW);
                    }
                }
            } else {
                delivery.setAttachmentStateUnchecked(TalkDelivery.ATTACHMENT_STATE_NONE);
            }

            Vector<TalkDelivery> processedDeliveries;
            if (!attachmentError) {
                processedDeliveries = processNewDelivery(message, delivery);
            } else {
                Date currentDate = new Date();
                LOG.warn("processNewDelivery: delivery failed by server because of bad incoming attachment data: '"+attachmentErrorReason+"', sender "+clientId);
                delivery.setState(TalkDelivery.STATE_FAILED);
                delivery.setReason(attachmentErrorReason);
                delivery.setTimeAccepted(currentDate);
                delivery.setTimeChanged(currentDate);
                processedDeliveries = new Vector<TalkDelivery>();
                processedDeliveries.add(delivery);
            }

            for (TalkDelivery processedDelivery : processedDeliveries) {
                // delivery will be returned as result, so mark outgoing time
                processedDelivery.setTimeUpdatedOut(new Date(processedDelivery.getTimeChanged().getTime() + 1));
                acceptedDeliveries.add(processedDelivery);

                TalkDelivery resultDelivery = new TalkDelivery();
                resultDelivery.updateWith(processedDelivery, TalkDelivery.REQUIRED_OUT_RESULT_FIELDS_SET);
                resultDeliveries.add(resultDelivery);
            }
         }

        // update number of deliveries
        message.setNumDeliveries(acceptedDeliveries.size());

        // process all accepted deliveries
        if (!acceptedDeliveries.isEmpty()) {
            // save deliveries first so messages get collected
            for (TalkDelivery ds : acceptedDeliveries) {
                mDatabase.saveDelivery(ds);
            }
            mDatabase.saveMessage(message);
            // initiate delivery for all recipients
            for (TalkDelivery ds : acceptedDeliveries) {
                if (!ds.isFailure()) {
                    mServer.getDeliveryAgent().requestDelivery(ds.getReceiverId(), false);
                }
            }
        }

        mStatistics.signalMessageAcceptedSucceeded();
        return resultDeliveries.toArray(new TalkDelivery[0]);
    }


    // this function checks if a message can be delivered and returns one or more deliveries
    // with the apropriate states.
    // If delivery is a client delivery, only one delivery is returned
    // if delivery is a group delivery, one delivery for each group member will be returned

    private Vector<TalkDelivery> processNewDelivery(TalkMessage message, TalkDelivery delivery) {
        Vector<TalkDelivery> result = new Vector<TalkDelivery>();

        Date currentDate = new Date();
        String senderId = mConnection.getClientId();

        if (!delivery.hasValidRecipient()) {
            LOG.warn("processNewDelivery: delivery rejected: no valid recipient (neither group nor client delivery)");
            delivery.setState(TalkDelivery.STATE_FAILED);
            delivery.setReason("no valid recipient (neither group nor client delivery)");
        }

        if (delivery.isGroupDelivery()) {
            String groupId = delivery.getGroupId();

            // check that group exists
            TalkGroupPresence groupPresence = mDatabase.findGroupPresenceById(groupId);
            if (groupPresence == null) {
                LOG.warn("processNewDelivery: delivery rejected: no such group");
                delivery.setState(TalkDelivery.STATE_FAILED);
                delivery.setReason("no such group");
            } else {
                // check that sender is member of group
                TalkGroupMembership clientMembership = mDatabase.findGroupMembershipForClient(groupId, senderId);
                if (clientMembership != null && clientMembership.isSuspended()) {
                    LOG.warn("processNewDelivery: delivery rejected: sender is suspended group member");
                    delivery.setState(TalkDelivery.STATE_FAILED);
                    delivery.setReason("sender is suspended group member");
                } else if (clientMembership == null || !clientMembership.isMember()) {
                    LOG.warn("processNewDelivery: delivery rejected: sender is not group member");
                    delivery.setState(TalkDelivery.STATE_FAILED);
                    delivery.setReason("sender is not group member");
                }
            }

            if (!TalkDelivery.STATE_FAILED.equals(delivery.getState())) {
                // deliver to each group member
                List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsById(groupId);
                for (TalkGroupMembership membership : memberships) {
                    if (membership.getClientId().equals(senderId)) {
                        continue;
                    }
                    if (!membership.isJoined()) {
                        continue;
                    }
                    if (membership.isSuspended()) {
                        continue;
                    }

                    TalkDelivery memberDelivery = new TalkDelivery(true);
                    memberDelivery.setMessageId(message.getMessageId());
                    memberDelivery.setMessageTag(delivery.getMessageTag());
                    memberDelivery.setGroupId(groupId);
                    memberDelivery.setSenderId(senderId);
                    memberDelivery.setKeyId(membership.getMemberKeyId());
                    memberDelivery.setKeyCiphertext(membership.getEncryptedGroupKey());
                    memberDelivery.setReceiverId(membership.getClientId());
                    memberDelivery.setAttachmentState(delivery.getAttachmentState());
                    memberDelivery.setTimeAccepted(currentDate);

                    if (membership.getEncryptedGroupKey() == null) {
                        LOG.warn("have no group key, discarding group message " + message.getMessageId() + " for client " + membership.getClientId() + " group " + groupId);
                        memberDelivery.setState(TalkDelivery.STATE_FAILED);
                        memberDelivery.setReason("no group key for receiver available");
                    } else if (membership.getSharedKeyId() != null && message.getSharedKeyId() != null && !membership.getSharedKeyId().equals(message.getSharedKeyId())) {
                        LOG.warn("message key id and member shared key id mismatch, discarding group message " + message.getMessageId() + " for client " + membership.getClientId() + " group " + groupId);
                        LOG.warn("message.sharedKeyId=" + message.getSharedKeyId() + " member.sharedKeyId= " + membership.getSharedKeyId() + " group.sharedKeyId= " + groupPresence.getSharedKeyId());
                        memberDelivery.setState(TalkDelivery.STATE_FAILED);
                        memberDelivery.setReason("group key for receiver is not current");
                    } else {
                        if (groupPresence.isTypeWorldwide()) {
                            // check for expired environment
                            TalkEnvironment environment = mDatabase.findEnvironmentByClientIdForGroup(memberDelivery.getReceiverId(), memberDelivery.getGroupId());
                            if (environment == null || environment.hasExpired()) {
                                memberDelivery.setState(TalkDelivery.STATE_FAILED);
                                memberDelivery.setReason("worldwide membership with expired on non-existing environment");
                                LOG.warn("not delivering (failing) message to worldwide group member with expired or non-existing environment " + message.getMessageId() + " for client " + membership.getClientId() + " group " + groupId);
                                suspendGroupMember(mServer, membership.getGroupId(), membership.getClientId());
                            }
                        }

                        if (!TalkDelivery.STATE_FAILED.equals(memberDelivery.getState())) {
                            boolean success = checkOneDelivery(message, memberDelivery);
                            if (success) {
                                memberDelivery.setState(TalkDelivery.STATE_DELIVERING);
                                LOG.debug("delivering message " + message.getMessageId() + " for client " + membership.getClientId() + " group " + groupId + " sharedKeyId=" + message.getSharedKeyId() + ", member sharedKeyId=" + membership.getSharedKeyId());
                            } else {
                                LOG.warn("failed message " + message.getMessageId() + " for client " + membership.getClientId() + " group " + groupId + " sharedKeyId=" + message.getSharedKeyId() + ", member sharedKeyId=" + membership.getSharedKeyId());
                            }
                        }
                    }
                    // set delivery timestamps
                    memberDelivery.setTimeAccepted(currentDate);
                    memberDelivery.setTimeChanged(currentDate);
                    mDatabase.saveDelivery(memberDelivery);
                    result.add(memberDelivery);
                }
                return result;
            }
        } else if (delivery.isClientDelivery()) {
            /*
            1) if a client is blocked we are done - message delivery is disallowed.
            2) Otherwise check if the sender has a valid relationship to the recipient that allows message delivery
            */
            String recipientId = delivery.getReceiverId();
            final TalkRelationship relationship = mDatabase.findRelationshipBetween(recipientId, senderId);

            if (isBlocking(relationship)) {
                LOG.debug("Recipient: '" + recipientId + "' blocks sender: '" + senderId + "' -> Blocking delivery");
                delivery.setState(TalkDelivery.STATE_FAILED);
                delivery.setReason("recipient blocked sender");
            } else if (areBefriended(relationship, recipientId, senderId) ||
                    areRelatedViaGroupMembership(senderId, recipientId))  // we need this for nearby group direct messages
            {
                // check for even more failure reasons
                if (checkOneDelivery(message, delivery)) {
                    // mark delivery as in progress if everything is fine
                    delivery.setState(TalkDelivery.STATE_DELIVERING);
                }
            } else {
                LOG.warn("Message delivery rejected since no permissive relationship via group or friendship exists. (" + senderId + ", " + recipientId + ")");
                delivery.setState(TalkDelivery.STATE_FAILED);
                delivery.setReason("neither friends nor joint group members");
            }
        }
        delivery.setTimeAccepted(currentDate);
        delivery.setTimeChanged(currentDate);
        result.add(delivery);

        return result;
    }

    private boolean areRelatedViaGroupMembership(String clientId1, String clientId2) {
        final List<TalkGroupMembership> client1GroupMemberships = mDatabase.findGroupMembershipsForClient(clientId1);
        final List<String> client1GroupIds = new ArrayList<String>();
        for (TalkGroupMembership membership : client1GroupMemberships) {
            LOG.trace("  * client1 membership state in group: '" + membership.getGroupId() + "':" + membership.getState());
            if (membership.isJoined()) {
                client1GroupIds.add(membership.getGroupId());
            }
        }

        final List<TalkGroupMembership> client2GroupMemberships = mDatabase.findGroupMembershipsForClient(clientId2);
        for (TalkGroupMembership membership : client2GroupMemberships) {
            LOG.trace("  * client2 membership state in group: '" + membership.getGroupId() + "':" + membership.getState());
            if (client1GroupIds.indexOf(membership.getGroupId()) != -1 &&
                    membership.isJoined()) {
                LOG.trace("clients '" + clientId1 + "' and '" + clientId2 + "' are both joined in group! (groupId: '" + membership.getGroupId() + "')");
                return true;
            }
        }

        LOG.trace("clients '" + clientId1 + "' and '" + clientId2 + "' are NOT both joined in the same group");
        return false;
    }

    private boolean isBlocking(TalkRelationship relationship) {
        // Check if recipient marked his relationship to the sender as BLOCKED
        return relationship != null && relationship.isBlocked();
    }

    private boolean areBefriended(TalkRelationship relationship, String clientId1, String clientId2) {
        // reject if there is no relationship
        if (relationship == null) {
            LOG.trace("clients '" + clientId1 + "' and '" + clientId2 + "' have no relationship with each other");
            return false;
        }

        // reject unless befriended
        if (!TalkRelationship.STATE_FRIEND.equals(relationship.getState())) {
            LOG.trace("clients '" + clientId1 + "' and '" + clientId2 +
                    "' are not friends (relationship is '" + relationship.getState() + "')");
            return false;
        }

        LOG.trace("clients '" + clientId1 + "' and '" + clientId2 + "' are friends!");
        return true;
    }

    private boolean checkOneDelivery(TalkMessage m, TalkDelivery delivery) {
        // who is doing this again?
        String clientId = mConnection.getClientId();
        // get the receiver
        String receiverId = delivery.getReceiverId();

        // reject messages to self
        if (receiverId.equals(clientId)) {
            LOG.warn("delivery rejected: send to self");
            // mark delivery failed
            delivery.setState(TalkDelivery.STATE_FAILED);
            delivery.setReason("send to self");
            return false;
        }

        // reject messages to non-existing clients
        TalkClient receiver = mDatabase.findClientById(receiverId);
        if (receiver == null) {
            LOG.warn("delivery rejected: recipient with id '" + receiverId + "' does not exist");
            // mark delivery failed
            delivery.setState(TalkDelivery.STATE_FAILED);
            delivery.setReason("recipient does not exist");
            return false;
        }

        final TalkRelationship relationship = mDatabase.findRelationshipBetween(receiverId, clientId);

        if (isBlocking(relationship)) {
            LOG.warn("delivery rejected: Recipient: '" + receiverId + "' blocks sender: '" + clientId);
            delivery.setState(TalkDelivery.STATE_FAILED);
            delivery.setReason("recipient blocked sender");
            return false;
        }

        // all fine, delivery accepted
        LOG.debug("delivery accepted for recipient with id '" + receiverId + "'");
        // return
        return true;
    }

    private TalkDelivery inDeliveryConfirm(String messageId, String confirmationState) {
        requireIdentification(true);
        synchronized (mServer.idLock(messageId)) {
            String clientId = mConnection.getClientId();
            TalkDelivery delivery = mDatabase.findDelivery(messageId, clientId);
            if (delivery != null) {
                if (delivery.nextStateAllowed(confirmationState)) {
                    LOG.debug("confirmed '" + confirmationState + "' message with id '" + messageId + "' for client with id '" + clientId + "'");
                    setDeliveryState(delivery, confirmationState, true, false);
                    mStatistics.signalMessageConfirmedSucceeded();
                } else {
                    throw new RuntimeException("inDeliveryConfirm: no state change path to '"+confirmationState+"' from current delivery state '"+delivery.getState()+"' : message id '" + messageId + "' client id '" + clientId + "'");
                }
                TalkDelivery result = new TalkDelivery();
                result.updateWith(delivery, TalkDelivery.REQUIRED_IN_UPDATE_FIELDS_SET);
                return result;
            } else {
                throw new RuntimeException("inDeliveryConfirm '"+confirmationState+"': no delivery found for message with id '" + messageId + "' for client with id '" + clientId + "'");
            }
        }
    }

    @Override
    public TalkDelivery inDeliveryConfirmUnseen(String messageId) {
        logCall("inDeliveryConfirmUnseen(messageId: '" + messageId + "')");
        return inDeliveryConfirm(messageId, TalkDelivery.STATE_DELIVERED_UNSEEN);
    }

    @Override
    public TalkDelivery inDeliveryConfirmSeen(String messageId) {
        logCall("inDeliveryConfirmSeen(messageId: '" + messageId + "')");
        return inDeliveryConfirm(messageId, TalkDelivery.STATE_DELIVERED_SEEN);
    }

    @Override
    public TalkDelivery inDeliveryConfirmPrivate(String messageId) {
        logCall("inDeliveryConfirmPrivate(messageId: '" + messageId + "')");
        return inDeliveryConfirm(messageId, TalkDelivery.STATE_DELIVERED_PRIVATE);
    }

    private TalkDelivery outDeliveryAcknowledge(String messageId, String recipientId, String acknowledgeState, String acknowledgedState) {
        requireIdentification(true);
        logCall("outDeliveryAcknowledge '"+acknowledgeState+"' (messageId: '" + messageId + "', recipientId: '" + recipientId + "')");
        synchronized (mServer.idLock(messageId)) {
            TalkDelivery delivery = findDelivery(messageId, recipientId);
            TalkDelivery result = new TalkDelivery();
            if (delivery != null) {
                String state = delivery.getState();
                if (acknowledgeState.equals(state) || acknowledgedState.equals(state)) {
                    LOG.debug("acknowledged '" + acknowledgeState + "' message with id '" + messageId + "' for recipient with id '" + recipientId + "'");
                    setDeliveryState(delivery, acknowledgedState , false, true);
                    mStatistics.signalMessageAcknowledgedSucceeded();
                }  else {
                    LOG.error("outDeliveryAcknowledge '"+acknowledgeState+"' received for delivery not in state 'delivered' (state =" + delivery.getState() + ") : message id '" + messageId + "' recipientId '" + recipientId + "'");
                }
                result.updateWith(delivery, TalkDelivery.REQUIRED_OUT_UPDATE_FIELDS_SET);
            }  else {
                LOG.error("outDeliveryAcknowledge '"+acknowledgeState+"' : no delivery found for message with id '" + messageId + "' for recipient with id '" + recipientId + "'");
                throw new RuntimeException("outDeliveryAcknowledge '"+acknowledgeState+"' : no delivery found for message with id '" + messageId + "' for recipient with id '" + recipientId + "'");
            }
            return result;
        }
    }

    @Override
    public TalkDelivery outDeliveryAcknowledgeUnseen(String messageId, String recipientId) {
        return outDeliveryAcknowledge(messageId, recipientId, TalkDelivery.STATE_DELIVERED_UNSEEN, TalkDelivery.STATE_DELIVERED_UNSEEN_ACKNOWLEDGED);
    }
    @Override
    public TalkDelivery outDeliveryAcknowledgeSeen(String messageId, String recipientId) {
        return outDeliveryAcknowledge(messageId, recipientId, TalkDelivery.STATE_DELIVERED_SEEN, TalkDelivery.STATE_DELIVERED_SEEN_ACKNOWLEDGED);
    }
    @Override
    public TalkDelivery outDeliveryAcknowledgePrivate(String messageId, String recipientId) {
        return outDeliveryAcknowledge(messageId, recipientId, TalkDelivery.STATE_DELIVERED_PRIVATE, TalkDelivery.STATE_DELIVERED_PRIVATE_ACKNOWLEDGED);
    }

    // handle finding failed group deliveries without a recipientId
    private TalkDelivery findDelivery(String messageId, String recipientId) {
        if (messageId == null) {
            throw new RuntimeException("messageId is null");
        }
        if (recipientId == null) {
            throw new RuntimeException("recipientId is null");
        }
        if (recipientId.length() > 0) {
            return mDatabase.findDelivery(messageId, recipientId);
        } else {
            String clientId = mConnection.getClientId();
            List<TalkDelivery> deliveries = mDatabase.findDeliveriesFromClientForMessage(clientId, messageId);
            if (deliveries.size() == 1) {
                return deliveries.get(0);
            }
            if (deliveries.size() == 0) {
                return null;
            }
            throw new RuntimeException("multiple deliveries found for this messageId, must supply a recipient id");
        }
    }

    private TalkDelivery deliverySenderChangeState(String messageId, String recipientId, String newState) {
        synchronized (mServer.idLock(messageId)) {
            String clientId = mConnection.getClientId();
            TalkDelivery delivery = findDelivery(messageId, recipientId);
            if (delivery != null) {
                if (delivery.getSenderId().equals(clientId)) {
                    setDeliveryState(delivery, newState, false, true);
                } else {
                    // TODO: remove this fix in 2015 or after next forced update
                    // temporary fix for bug in iOS-Client 2.2.12
                    if (TalkDelivery.STATE_ABORTED_ACKNOWLEDGED.equals(newState)) {
                        return inDeliveryReject(messageId, "no key or private key not found");
                    }
                    // end of fix
                    throw new RuntimeException("you are not the sender");
                }
                TalkDelivery result = new TalkDelivery();
                result.updateWith(delivery, TalkDelivery.REQUIRED_OUT_UPDATE_FIELDS_SET);
                return result;
            } else {
                throw new RuntimeException("no delivery found for message with id '" + messageId + "' for recipient with id '" + recipientId + "'");
            }
        }
    }

    @Override
    public TalkDelivery outDeliveryAbort(String messageId, String recipientId) {
        requireIdentification(true);
        logCall("outDeliveryAbort(messageId: '" + messageId + "', recipientId: '" + recipientId + "'");
        return deliverySenderChangeState(messageId, recipientId, TalkDelivery.STATE_ABORTED_ACKNOWLEDGED);
    }

    @Override
    public void outDeliveryUnknown(String messageId, String recipientId) {
        requireIdentification(true);
        logCall("outDeliveryUnknown(messageId: '" + messageId + "', recipientId: '" + recipientId + "'");
        synchronized (mServer.idLock(messageId)) {
            String clientId = mConnection.getClientId();
            TalkDelivery delivery = findDelivery(messageId, recipientId);
            if (delivery != null) {
                if (delivery.getSenderId().equals(clientId)) {
                    // make sure the delivery will be set to a good possibly final state and the clients won't be bothered again
                    String finalAttachmentState = TalkDelivery.findNextUnknownState(TalkDelivery.nextUnknownOutAttachmentState, delivery.getAttachmentState());
                    boolean changed = false;
                    if (!finalAttachmentState.equals(delivery.getAttachmentState())) {
                        LOG.debug("outDeliveryUnknown: setting delivery from state=" + delivery.getState() + ", attachmentState=" + delivery.getAttachmentState() + " to finalAttachmentState=" + finalAttachmentState);
                        delivery.setAttachmentState(finalAttachmentState);
                        delivery.setTimeChanged(new Date());
                        mDatabase.saveDelivery(delivery);
                        mServer.getDeliveryAgent().requestDelivery(delivery.getReceiverId(), false);
                        changed = true;
                    }
                    String nextGoodState = TalkDelivery.findNextUnknownState(TalkDelivery.nextUnknownOutState, delivery.getState());
                    if (!nextGoodState.equals(delivery.getState())) {
                        LOG.debug("outDeliveryUnknown: setting delivery from state="+delivery.getState()+", attachmentState="+delivery.getAttachmentState()+" to nextGoodState="+nextGoodState);
                        setDeliveryState(delivery, nextGoodState, false, false);
                        changed = true;
                    }
                    if (!changed) {
                        LOG.warn("outDeliveryUnknown: delivery already in suitable state="+delivery.getState()+", attachmentState="+delivery.getAttachmentState());
                    }
                } else {
                    throw new RuntimeException("you are not the sender");
                }
            } else {
                LOG.warn("no delivery found for message with id '" + messageId + "' for recipient with id '" + recipientId + "', probably already also deleted on server");
            }
        }
    }

    @Override
    public void inDeliveryUnknown(String messageId) {
        requireIdentification(true);
        logCall("inDeliveryUnknown(messageId: '" + messageId);
        synchronized (mServer.idLock(messageId)) {
            String clientId = mConnection.getClientId();
            TalkDelivery delivery = mDatabase.findDelivery(messageId, clientId);
            if (delivery != null) {
                // make sure the delivery will be set to a good possibly final state and the clients won't be bothered again
                boolean changed = false;

                String finalAttachmentState = TalkDelivery.findNextUnknownState(TalkDelivery.nextUnknownInAttachmentState, delivery.getAttachmentState());
                 if (!finalAttachmentState.equals(delivery.getAttachmentState())) {
                    LOG.debug("inDeliveryUnknown: setting delivery from state=" + delivery.getState() + ", attachmentState=" + delivery.getAttachmentState() + " to finalAttachmentState=" + finalAttachmentState);
                    delivery.setAttachmentState(finalAttachmentState);
                    delivery.setTimeChanged(new Date());
                    mDatabase.saveDelivery(delivery);
                    mServer.getDeliveryAgent().requestDelivery(delivery.getSenderId(), false);
                    changed = true;
                 }

                // note: there are currently no states in nextUnknownInState,
                // so the following body in the next if-statement
                // currently should never be executed and is in here
                // for symmetry with the out-side code only and in
                // case we might have such states in the future.
                String nextGoodState = TalkDelivery.findNextUnknownState(TalkDelivery.nextUnknownInState, delivery.getState());
                if (!nextGoodState.equals(delivery.getState())) {
                    LOG.debug("inDeliveryUnknown: setting delivery from state="+delivery.getState()+", attachmentState="+delivery.getAttachmentState()+" to nextGoodState="+nextGoodState);
                    setDeliveryState(delivery, nextGoodState, false, false);
                    changed = true;
                }
                if (!changed) {
                    LOG.warn("inDeliveryUnknown: delivery already in suitable state="+delivery.getState()+", attachmentState="+delivery.getAttachmentState());
                }
            } else {
                LOG.warn("no delivery found for message with id '" + messageId + "' for recipient with id '" + clientId + "', probably already also deleted on server");
            }
        }
    }


    @Override
    public TalkDelivery outDeliveryAcknowledgeRejected(String messageId, String recipientId) {
        requireIdentification(true);
        logCall("outDeliveryAcknowledgeRejected(messageId: '" + messageId + "', recipientId: '" + recipientId + "'");
        return deliverySenderChangeState(messageId, recipientId, TalkDelivery.STATE_REJECTED_ACKNOWLEDGED);
    }
    @Override
    public TalkDelivery outDeliveryAcknowledgeFailed(String messageId, String recipientId) {
        requireIdentification(true);
        logCall("outDeliveryAcknowledgeFailed(messageId: '" + messageId + "', recipientId: '" + recipientId + "'");
        return deliverySenderChangeState(messageId, recipientId, TalkDelivery.STATE_FAILED_ACKNOWLEDGED);
    }

    @Override
    public TalkDelivery inDeliveryReject(String messageId, String reason) {
        requireIdentification(true);
        logCall("inDeliveryReject(messageId: '" + messageId+", reason:"+reason);
        synchronized (mServer.idLock(messageId)) {
            String clientId = mConnection.getClientId();
            TalkDelivery delivery = mDatabase.findDelivery(messageId, clientId);
            if (delivery != null) {
                delivery.setReason(reason);
                setDeliveryState(delivery, TalkDelivery.STATE_REJECTED, true, false);
                TalkDelivery result = new TalkDelivery();
                result.updateWith(delivery, TalkDelivery.REQUIRED_OUT_UPDATE_FIELDS_SET);
                return result;
            } else {
                throw new RuntimeException("deliveryReject(): no delivery found for message with id '" + messageId + "' for recipient with id '" + clientId + "'");
            }
        }
    }

    private void setDeliveryState(TalkDelivery delivery, String state, boolean willReturnDeliveryIn, boolean willReturnDeliveryOut) {
        if (delivery.nextStateAllowed(state)) {

            delivery.setState(state);
            delivery.setTimeChanged(new Date());
            if (willReturnDeliveryIn) {
                delivery.setTimeUpdatedIn(new Date(delivery.getTimeChanged().getTime() + 1 ));
            }
            if (willReturnDeliveryOut) {
                delivery.setTimeUpdatedOut(new Date(delivery.getTimeChanged().getTime() + 1 ));
            }
            mDatabase.saveDelivery(delivery);
            if (TalkDelivery.OUT_STATES_SET.contains(state)) {
                mServer.getDeliveryAgent().requestDelivery(delivery.getSenderId(), false);
            } else if (TalkDelivery.IN_STATES_SET.contains(state)) {
                mServer.getDeliveryAgent().requestDelivery(delivery.getReceiverId(), false);
            } else if (delivery.isFinished()) {
                mServer.getCleaningAgent().cleanFinishedDelivery(delivery);
            }
        } else {
            throw new RuntimeException("Setting delivery state from '"+delivery.getState()+" to '"+state+"'not allowed");
        }
    }

    @Override
    public String createGroup(TalkGroupPresence groupPresence) {
        requireIdentification(true);
        logCall("createGroup(groupTag: '" + groupPresence.getGroupTag() + "')");
        groupPresence.setGroupId(UUID.randomUUID().toString());
        groupPresence.setState(TalkGroupPresence.STATE_EXISTS);
        TalkGroupMembership adminMembership = new TalkGroupMembership();
        adminMembership.setClientId(mConnection.getClientId());
        adminMembership.setGroupId(groupPresence.getGroupId());
        adminMembership.setRole(TalkGroupMembership.ROLE_ADMIN);
        adminMembership.setState(TalkGroupMembership.STATE_JOINED);
        changedGroupPresence(groupPresence, new Date());
        changedGroupMembership(adminMembership, groupPresence.getLastChanged());
        return groupPresence.getGroupId();
    }

    @Override
    public TalkGroupPresence createGroupWithMembers(String groupType, String groupTag, String groupName, String[] members, String[] roles) {
        requireIdentification(true);
        logCall("createGroupWithMembers(groupName: '"+groupName +"', groupTag='" + groupTag + "')");
        if (!TalkGroupPresence.GROUP_TYPE_USER.equals(groupType)) {
            throw new RuntimeException("illegal group type:"+groupType);
        }
        if (groupName == null || groupTag == null) {
            throw new RuntimeException("group name or tag missing");
        }
        if (groupName.length() > 32) {
            throw new RuntimeException("group name too long (>32)");
        }
        TalkGroupPresence groupPresence = new TalkGroupPresence();
        groupPresence.setGroupId(UUID.randomUUID().toString());
        groupPresence.setState(TalkGroupPresence.STATE_EXISTS);
        groupPresence.setGroupType(groupType);
        groupPresence.setGroupName(groupName);
        groupPresence.setGroupTag(groupTag);

        if (members.length != roles.length) {
            throw new RuntimeException("number of members != number of roles");
        }

        HashSet<String> deleted = new HashSet<String>();
        for (String memberId : members) {
            TalkClient client = mDatabase.findClientById(memberId);
            if (client == null) {
                client = mDatabase.findDeletedClientById(memberId);
                if (client == null) {
                    throw new RuntimeException("No such client:" + memberId);
                } else {
                    logCall("createGroupWithMembers(groupName: '"+groupName +"', groupTag='" + groupTag + "'), member +"+memberId+" already deleted");
                    deleted.add(memberId);
                }
            }
        }

        for (String role : roles) {
            if (!TalkGroupMembership.isValidRoleForGroupType(role, groupType))  {
                throw new RuntimeException("Invalid role:"+role+" for group type"+groupType);
            }
        }

        Date now = new Date();
        changedGroupPresence(groupPresence, now);

        TalkGroupMembership adminMembership = new TalkGroupMembership();
        adminMembership.setClientId(mConnection.getClientId());
        adminMembership.setGroupId(groupPresence.getGroupId());
        adminMembership.setRole(TalkGroupMembership.ROLE_ADMIN);
        adminMembership.setState(TalkGroupMembership.STATE_JOINED);
        changedGroupMembership(adminMembership, now);

        for (int i = 0; i < members.length;++i) {
            if (!deleted.contains(members[i])) {
                TalkGroupMembership membership = new TalkGroupMembership();
                membership.setGroupId(groupPresence.getGroupId());
                membership.setClientId(members[i]);
                membership.setRole(roles[i]);
                membership.setState(TalkGroupMembership.STATE_INVITED);
                changedGroupMembership(membership, now);
            }
        }
        for (int i = 0; i < members.length;++i) {
            if (!deleted.contains(members[i])) {
                // send the presence of all other group members to the new group member
                mServer.getUpdateAgent().requestPresenceUpdateForClientOfMembersOfGroup(members[i], groupPresence.getGroupId());
            }
        }
        return groupPresence;
    }

    @Override
    public TalkGroupPresence[] getGroups(Date lastKnown) {
        requireIdentification(true);
        logCall("getGroups(lastKnown: '" + lastKnown + "')");
        List<TalkGroupPresence> groupPresences = mDatabase.findGroupPresencesByClientIdChangedAfter(mConnection.getClientId(), lastKnown);
        TalkGroupPresence[] result = new TalkGroupPresence[groupPresences.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = groupPresences.get(i);
        }
        return result;
    }

    @Override
    public TalkGroupPresence getGroup(String groupId) {
        requireIdentification(true);
        logCall("getGroup(id: '" + groupId + "')");
        return mDatabase.findGroupPresenceById(groupId);
    }

    @Override
    public TalkGroupMembership getGroupMember(String groupId, String clientId) {
        requireIdentification(true);
        logCall("getGroupMember(groupId: '" + groupId + ", clientId:" + clientId + "')");
        return mDatabase.findGroupMembershipForClient(groupId, clientId);
    }

    @Override
    public void updateGroupName(String groupId, String name) {
        requireIdentification(true);
        requireGroupAdmin(groupId);
        //requireNotNearbyGroupType(groupId);
        logCall("updateGroupName(groupId: '" + groupId + "', name: '" + name + "')");
        TalkGroupPresence targetGroupPresence = mDatabase.findGroupPresenceById(groupId);
        targetGroupPresence.setGroupName(name);
        changedGroupPresence(targetGroupPresence, new Date());
    }

    @Override
    public void updateGroupAvatar(String groupId, String avatarUrl) {
        requireIdentification(true);
        requireGroupAdmin(groupId);
        //requireNotNearbyGroupType(groupId);
        logCall("updateGroupAvatar(groupId: '" + groupId + "', avatarUrl: '" + avatarUrl + "')");
        TalkGroupPresence targetGroupPresence = mDatabase.findGroupPresenceById(groupId);
        targetGroupPresence.setGroupAvatarUrl(avatarUrl);
        changedGroupPresence(targetGroupPresence, new Date());
    }

    @Override
    public void updateGroup(TalkGroupPresence groupPresence) {
        requireIdentification(true);
        requireGroupAdmin(groupPresence.getGroupId());
        logCall("updateGroup(groupId: '" + groupPresence.getGroupId() + "')");
        TalkGroupPresence targetGroupPresence = mDatabase.findGroupPresenceById(groupPresence.getGroupId());
        targetGroupPresence.setGroupName(groupPresence.getGroupName());
        targetGroupPresence.setGroupAvatarUrl(groupPresence.getGroupAvatarUrl());
        changedGroupPresence(targetGroupPresence, new Date());
    }

    @Override
    public void deleteGroup(String groupId) {
        requireIdentification(true);
        requireGroupAdmin(groupId);
        logCall("deleteGroup(groupId: '" + groupId + "')");

        TalkGroupPresence groupPresence = mDatabase.findGroupPresenceById(groupId);
        if (groupPresence == null) {
            throw new RuntimeException("Group does not exist");
        }

        // mark the group as deleted
        groupPresence.setState(TalkGroupPresence.STATE_DELETED);
        changedGroupPresence(groupPresence, new Date());

        // walk the group and make everyone have a "none" relationship to it
        List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsById(groupId);
        for (TalkGroupMembership membership : memberships) {
            if (membership.isInvited() || membership.isJoined() || membership.isSuspended()) {
                membership.setState(TalkGroupMembership.STATE_GROUP_REMOVED);

                // TODO: check if degrade role of admins is advisable
                /*if (member.isAdmin()) {
                    member.setRole(TalkGroupMember.ROLE_MEMBER);
                }*/
                changedGroupMembership(membership, groupPresence.getLastChanged());
            }
        }
    }

    // touch presences of all members
    private void touchGroupMemberPresences(String groupId) {
        List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsById(groupId);
        for (TalkGroupMembership membership : memberships) {
            if (membership.isInvited() || membership.isJoined() || membership.isSuspended()) {
                TalkPresence p = mDatabase.findPresenceForClient(membership.getClientId());
                if (p != null) {
                    p.setTimestamp(new Date());
                    mDatabase.savePresence(p);
                }
            }
        }
    }

    private static void suspendGroupMember(TalkServer server, String groupId, String clientId) {
        LOG.debug("suspendGroupMember:" + groupId + " client:" + clientId);

        synchronized (server.dualIdLock(TalkGroupMembership.LOCK_PREFIX, groupId, clientId)) {

            TalkGroupMembership membership = server.getDatabase().findGroupMembershipForClient(groupId, clientId);
            if (membership == null) {
                throw new RuntimeException("suspendGroupMember: not membership found for group:" + groupId + " client:" + clientId);
            }
            if (!membership.isInvolved()) {
                throw new RuntimeException("suspendGroupMember: member not involved, state:"+membership.getState()+", group:" + groupId + " client:" + clientId);
            }
            if (!membership.isSuspended()) {
                LOG.debug("suspendGroupMember:  suspending membership for group:" + groupId + " client:" + clientId);
                membership.setState(TalkGroupMembership.STATE_SUSPENDED);
                changedGroupMembership(server, membership, new Date());
                server.getUpdateAgent().requestGroupMembershipUpdate(groupId, clientId);
            } else {
                LOG.warn("suspendGroupMember:  membership already suspended for group:" + groupId + " client:" + clientId);
            }
        }
    }

    @Override
    public void inviteGroupMember(String groupId, String clientId) {
        logCall("inviteGroupMember(groupId: '" + groupId + "' / clientId: '" + clientId + "')");
        requireIdentification(true);
        requireGroupAdmin(groupId);
        //requireNotNearbyGroupType(groupId);

        // check that the client exists
        TalkClient client = mDatabase.findClientById(clientId);
        if (client == null) {
            throw new RuntimeException("No such client");
        }

        // disallow invitation by blocked clients
        String invitingId = mConnection.getClientId();
        final TalkRelationship relationship = mDatabase.findRelationshipBetween(clientId, invitingId);

        if (isBlocking(relationship)) {
            LOG.warn("Inviting contact '" + invitingId + "' blocked by invitee '" + clientId + "'");
            throw new RuntimeException("Inviting contact blocked by invitee");
        }

        // get or create the group member
        TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, clientId);
        if (membership == null) {
            membership = new TalkGroupMembership();
        }
        // perform the invite
        if (membership.getState().equals(TalkGroupMembership.STATE_NONE)) {
            // set up the member
            membership.setGroupId(groupId);
            membership.setClientId(clientId);
            membership.setState(TalkGroupMembership.STATE_INVITED);
            membership.trashPrivate();
            changedGroupMembership(membership, new Date());
            //  NOTE if this gets removed then the invited users presence might
            //       need touching depending on what the solution to the update problem is
            // notify various things
            //touchGroupMemberPresences(groupId);
            mServer.getUpdateAgent().requestGroupUpdate(groupId, clientId);
            mServer.getUpdateAgent().requestGroupMembershipUpdatesForNewMember(groupId, clientId);

            // send the presence of all other group members to the new group member
            mServer.getUpdateAgent().requestPresenceUpdateForClientOfMembersOfGroup(clientId, groupId);

            // send presence updates to all related clients of <clientId>
            mServer.getUpdateAgent().requestPresenceUpdate(clientId, null);
        } else {
            throw new RuntimeException("Already invited or member to group");
        }
    }

    @Override
    public void joinGroup(String groupId) {
        logCall("joinGroup(groupId: '" + groupId + "')");
        requireIdentification(true);

        String clientId = mConnection.getClientId();

        TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, clientId);
        if (membership == null) {
            throw new RuntimeException("Group does not exist");
        }

        if (membership.getState().equals(TalkGroupMembership.STATE_JOINED)) {
            throw new RuntimeException("Already a member of the group");
        }

        if (!membership.getState().equals(TalkGroupMembership.STATE_INVITED)) {
            throw new RuntimeException("Not invited to group");
        }

        membership.setState(TalkGroupMembership.STATE_JOINED);

        changedGroupMembership(membership, new Date());
    }

    @Override
    public void leaveGroup(String groupId) {
        logCall("leaveGroup(groupId: '" + groupId + "')");
        requireIdentification(true);
        doLeaveGroup(groupId);
     }

    public void doLeaveGroup(String groupId) {
        TalkGroupMembership membership = requiredGroupInvitedOrMember(groupId);
        // set membership state to NONE
        membership.setState(TalkGroupMembership.STATE_NONE);
        // degrade anyone who leaves to member
        membership.setRole(TalkGroupMembership.ROLE_MEMBER);
        // trash keys
        membership.trashPrivate();
        // save the whole thing
        changedGroupMembership(membership, new Date());
    }

    @Override
    public void setGroupNotifications(String groupId, String preference) {
        logCall("setGroupNotifications(groupId: '" + groupId + ","+preference+"')");
        requireIdentification(true);
        if (TalkGroupMembership.isValidNotificationPreference(preference)) {
            TalkGroupMembership membership = requiredGroupInvitedOrMember(groupId);
            membership.setNotificationPreference(preference);
            membership.setLastChanged(new Date());
            mDatabase.saveGroupMembership(membership);
            mServer.getUpdateAgent().requestGroupMembershipUpdate(membership);
        } else {
            throw new RuntimeException("Illegal preference:"+preference);
        }
    }


    @Override
    public void removeGroupMember(String groupId, String clientId) {
        requireIdentification(true);
        requireGroupAdmin(groupId);
        //requireNotNearbyGroupType(groupId);
        logCall("removeGroupMember(groupId: '" + groupId + "' / clientId: '" + clientId + "')");

        TalkGroupMembership targetMembership = mDatabase.findGroupMembershipForClient(groupId, clientId);
        if (targetMembership == null) {
            throw new RuntimeException("Client is not a member of group");
        }
        // set membership state to NONE
        targetMembership.setState(TalkGroupMembership.STATE_NONE);
        // degrade removed users to member
        targetMembership.setRole(TalkGroupMembership.ROLE_MEMBER);
        targetMembership.trashPrivate();
        changedGroupMembership(targetMembership, new Date());
    }

    @Override
    public void updateGroupRole(String groupId, String clientId, String role) {
        requireIdentification(true);
        requireGroupAdmin(groupId);
        logCall("updateGroupRole(groupId: '" + groupId + "' / clientId: '" + clientId + "', role: '" + role + "')");
        TalkGroupMembership targetMembership = mDatabase.findGroupMembershipForClient(groupId, clientId);
        TalkGroupPresence targetPresence = mDatabase.findGroupPresenceById(groupId);
        if (targetMembership == null) {
            throw new RuntimeException("Client is not a member of group");
        }
        if (targetPresence == null) {
            throw new RuntimeException("Group does not exist");
        }
        if (!TalkGroupMembership.isValidRoleForGroupType(role, targetPresence.getGroupType())) {
            throw new RuntimeException("Invalid role:"+role+" for group type"+targetPresence.getGroupType());
        }
        targetMembership.setRole(role);
        changedGroupMembership(targetMembership, new Date());
    }

    @Override
    public TalkGroupMembership[] getGroupMembers(String groupId, Date lastKnown) {
        requireIdentification(true);
        requiredGroupInvitedOrMember(groupId);
        logCall("getGroupMembers(groupId: '" + groupId + "' / lastKnown: '" + lastKnown + "')");

        List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsByIdChangedAfter(groupId, lastKnown);
        TalkGroupMembership[] result = new TalkGroupMembership[memberships.size()];
        for (int i = 0; i < result.length; i++) {
            TalkGroupMembership membership = memberships.get(i);
            if (!membership.getClientId().equals(mConnection.getClientId())) {
                membership.setEncryptedGroupKey(null);
            }
            result[i] = membership;
        }
        return result;
    }

    private static void changedGroupPresence(TalkServer server, TalkGroupPresence groupPresence, Date changed) {
        groupPresence.setLastChanged(changed);
        server.getDatabase().saveGroupPresence(groupPresence);
        server.getUpdateAgent().requestGroupUpdate(groupPresence.getGroupId());
    }

    private void changedGroupPresence(TalkGroupPresence groupPresence, Date changed) {
        changedGroupPresence(mServer, groupPresence, changed);
     }

    private static void changedGroupMembership(TalkServer server, TalkGroupMembership membership, Date changed) {
        membership.setLastChanged(changed);
        server.getDatabase().saveGroupMembership(membership);
        server.getUpdateAgent().requestGroupMembershipUpdate(membership.getGroupId(), membership.getClientId());
    }

    private void changedGroupMembership(TalkGroupMembership membership, Date changed) {
        TalkRpcHandler.changedGroupMembership(mServer, membership, changed);
     }

    private void requireGroupAdmin(String groupId) {
        TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, mConnection.getClientId());
        if (membership != null && membership.isAdmin()) {
            return;
        }
        throw new RuntimeException("Client is not an admin in group with id: '" + groupId + "'");
    }

    private TalkGroupMembership requiredGroupInvitedOrMember(String groupId) {
        TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, mConnection.getClientId());
        if (membership != null && (membership.isInvited() || membership.isMember() || membership.isSuspended())) {
            return membership;
        }
        throw new RuntimeException("Client is not a member in group with id: '" + groupId + "'");
    }
/*
    private void requireNotNearbyGroupType(String groupId) {
        // perspectively we should evolve a permission model to enable checking of WHO is allowed to do WHAT in which CONTEXT
        // e.g. client (permission depending on role) inviteGroupMembers to Group (permission depending on type)
        // for now we just check for nearby groups...
        TalkGroup group = mDatabase.findGroupPresenceById(groupId);
        if (group.isTypeNearby()) {
            throw new RuntimeException("Group type is: nearby. not allowed.");
        }
    }
*/
    @Override
    public FileHandles createFileForStorage(int contentLength) {
        requireIdentification(true);
        logCall("createFileForStorage(contentLength: '" + contentLength + "')");
        return mServer.getFilecacheClient()
                .createFileForStorage(mConnection.getClientId(), "application/octet-stream", contentLength);
    }

    @Override
    public FileHandles createFileForTransfer(int contentLength) {
        requireIdentification(true);
        logCall("createFileForTransfer(contentLength: '" + contentLength + "')");
        return mServer.getFilecacheClient()
                .createFileForTransfer(mConnection.getClientId(), "application/octet-stream", contentLength);
    }

    // should be called by the receiver of an transfer file after download; the server can the delete the file in case
    @Override
    public String receivedFile(String fileId) {
        requireIdentification(true);
        logCall("receivedFile(fileId: '" + fileId + "')");
        return processFileDownloadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_RECEIVED);
    }
    // should be called by the receiver of an transfer file if the user has aborted the download
    @Override
    public String abortedFileDownload(String fileId) {
        requireIdentification(true);
        logCall("abortedFileDownload(fileId: '" + fileId + "')");
        return processFileDownloadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_ABORTED);
    }
    // should be called by the receiver of an transfer file if the client has exceeded the download retry count
    @Override
    public String failedFileDownload(String fileId) {
        requireIdentification(true);
        logCall("failedFileDownload(fileId: '" + fileId + "')");
        return processFileDownloadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_FAILED);
    }
    // should be called by the receiver of an transfer file when a final attachment sender set state has been seen
    @Override
    public String acknowledgeAbortedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("failedFileDownload(fileId: '" + fileId + "')");
        return processFileDownloadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOAD_ABORTED_ACKNOWLEDGED);
    }
    // should be called by the receiver of an transfer file when a final attachment sender set state has been seen
    @Override
    public String acknowledgeFailedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("acknowledgeFailedFileUpload(fileId: '" + fileId + "')");
        return processFileDownloadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOAD_FAILED_ACKNOWLEDGED);
    }

    private String processFileDownloadMessage(String fileId, String nextState) {
        if (fileId == null) {
            throw new RuntimeException("illegal parameter: fileId is null");
        }
        final String clientId = mConnection.getClientId();
        logCall("processFileDownloadMessage(fileId: '" + fileId + "') for client "+clientId + ", nextState='"+nextState+"'");

        TalkServer.NonReentrantLock lock = mServer.idLockNonReentrant("deliveryRequest-"+clientId);
        try {
            lock.lock("processFileDownloadMessage");
            List<TalkMessage> messages = mDatabase.findMessagesWithAttachmentFileId(fileId);
            if (messages.isEmpty()) {
                throw new RuntimeException("No message found with file id "+fileId);
            }
            if (messages.size() > 1) {
                LOG.error("Multiple messages ("+messages.size()+") found with file id " + fileId);
            }
            for (TalkMessage message : messages) {
                if (clientId.equals(message.getSenderId())) {
                    throw new RuntimeException("Sender must not mess with download, messageId="+message.getMessageId());
                }
                synchronized (mServer.idLock(message.getMessageId())) {
                    TalkDelivery delivery = mDatabase.findDelivery(message.getMessageId(), clientId);
                    if (delivery != null) {
                        LOG.debug("AttachmentState '" + delivery.getAttachmentState() + "' --> '" + nextState + "' (download), messageId=" + message.getMessageId() + ", delivery=" + delivery.getId());

                        if (!delivery.nextAttachmentStateAllowed(nextState)) {
                            throw new RuntimeException("next state '"+nextState+"'not allowed, delivery already in state '"+delivery.getAttachmentState()+"', messageId="+message.getMessageId()+", delivery="+delivery.getId());
                        }
                        delivery.setAttachmentState(nextState);
                        delivery.setTimeChanged(new Date());
                        mDatabase.saveDelivery(delivery);
                        mServer.getDeliveryAgent().requestDelivery(delivery.getSenderId(), false);
                    } else {
                        throw new RuntimeException("delivery not found, messageId="+message.getMessageId());
                    }
                }
            }
            return nextState;
        } catch (InterruptedException e) {
            throw new RuntimeException("processFileDownloadMessage ",e);
        } finally {
            lock.unlock();
        }
    }

    //------ sender attachment state indication methods
    // should be called by the sender of an transfer file after upload has been started
    @Override
    public String startedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("startedFileUpload(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOADING, null);
    }
    // should be called by the sender of an transfer file when the upload has been paused
    @Override
    public String pausedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("pausedFileUpload(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOAD_PAUSED, null);
    }
    // should be called by the sender of an transfer file after upload has been finished
    @Override
    public String finishedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("finishedFileUpload(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOADED, null);
    }
    // should be called by the sender of an transfer file when the upload is aborted by the user
    @Override
    public String abortedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("abortedFileUpload(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOAD_ABORTED, null);
    }
    // should be called by the sender of an transfer file when upload retry count has been exceeded
    @Override
    public String failedFileUpload(String fileId) {
        requireIdentification(true);
        logCall("failedFileUpload(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_UPLOAD_FAILED, null);
    }
    // should be called by the sender of an transfer file when a final attachment receiver set state has been seen
    @Override
    public String acknowledgeReceivedFile(String fileId, String receiverId) {
        requireIdentification(true);
        logCall("acknowledgeReceivedFile(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED, receiverId);
    }
    // should be called by the sender of an transfer file when a final attachment receiver set state has been seen
    @Override
    public String acknowledgeAbortedFileDownload(String fileId, String receiverId) {
        requireIdentification(true);
        logCall("acknowledgeReceivedFile(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_ABORTED_ACKNOWLEDGED, receiverId);
    }
    // should be called by the sender of an transfer file when a final attachment receiver set state has been seen
    @Override
    public String acknowledgeFailedFileDownload(String fileId, String receiverId) {
        requireIdentification(true);
        logCall("acknowledgeReceivedFile(fileId: '" + fileId + "')");
        return processFileUploadMessage(fileId, TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_FAILED_ACKNOWLEDGED, receiverId);
    }

    private String processFileUploadMessage(String fileId, String nextState, String receiverId) {
        if (fileId == null) {
            throw new RuntimeException("illegal parameter: fileId is null");
        }

        final String clientId = mConnection.getClientId();

        List<TalkMessage> messages = mDatabase.findMessagesWithAttachmentFileId(fileId);
        if (messages.isEmpty()) {
            throw new RuntimeException("No message found with file id "+fileId);
        }

        String result = nextState;
        TalkServer.NonReentrantLock lock = mServer.idLockNonReentrant("deliveryRequest-"+clientId);
        try {
            lock.lock("processFileUploadMessage");
            for (TalkMessage message : messages) {
                if (clientId.equals(message.getSenderId())) {
                    synchronized (mServer.idLock(message.getMessageId())) {
                        boolean messageChanged = false;
                        if (message.getAttachmentUploadStarted() == null &&
                                (TalkDelivery.ATTACHMENT_STATE_UPLOADING.equals(nextState) || TalkDelivery.ATTACHMENT_STATE_UPLOADED.equals(nextState) )) {
                            message.setAttachmentUploadStarted(new Date());
                            messageChanged = true;
                        }
                        if (message.getAttachmentUploadFinished() == null &&
                                (TalkDelivery.ATTACHMENT_STATE_UPLOADED.equals(nextState) )) {
                            message.setAttachmentUploadFinished(new Date());
                            messageChanged = true;
                        }
                        if (messageChanged) {
                            mDatabase.saveMessage(message);
                        }
                        List<TalkDelivery> deliveries = mDatabase.findDeliveriesForMessage(message.getMessageId());

                        // update all concerned deliveries
                        for (TalkDelivery delivery : deliveries) {
                            // for some calls we update only a specific delivery, while for other calls we update only the delivery with the proper receiverId
                            if (receiverId == null || delivery.getReceiverId().equals(receiverId)) {
                                LOG.debug("AttachmentState '" + delivery.getAttachmentState() + "' --> '" + nextState + "' (upload), messageId=" + message.getMessageId() + ", delivery=" + delivery.getId());

                                if ((TalkDelivery.ATTACHMENT_STATE_RECEIVED.equals(delivery.getAttachmentState()) ||
                                        TalkDelivery.ATTACHMENT_STATE_RECEIVED_ACKNOWLEDGED.equals(delivery.getAttachmentState()))
                                        && TalkDelivery.ATTACHMENT_STATE_UPLOADED.equals(nextState)) {
                                    LOG.warn("AttachmentState already '" + delivery.getAttachmentState() + "', ignoring next state 'uploaded' returning current state, messageId=" + message.getMessageId() + ", delivery=" + delivery.getId());
                                    result = delivery.getAttachmentState();
                                    continue;
                                }

                                if (!delivery.nextAttachmentStateAllowed(nextState)) {
                                    LOG.warn("next state '" + nextState + "'not allowed, delivery already in state '" + delivery.getAttachmentState() + "', messageId=" + message.getMessageId() + ", delivery=" + delivery.getId());
                                    result = delivery.getAttachmentState();
                                    continue;
                                }
                                delivery.setAttachmentState(nextState);
                                delivery.setTimeChanged(message.getAttachmentUploadStarted());
                                mDatabase.saveDelivery(delivery);
                                mServer.getDeliveryAgent().requestDelivery(delivery.getReceiverId(), false);
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("you are not the sender of this file with messageId="+message.getMessageId());
                }
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException("processFileDownloadMessage ",e);
        } finally {
            lock.unlock();
        }
    }

    private void createGroupWithEnvironment(TalkEnvironment environment) {
        LOG.debug("createGroupWithEnvironment: creating new group for client with id '" + environment.getClientId() + "'");
        String clientId = environment.getClientId();
        if (clientId == null) {
            throw new RuntimeException("can not create group from environment with client id null");
        }
        TalkGroupPresence groupPresence = new TalkGroupPresence();
        groupPresence.setGroupTag(UUID.randomUUID().toString());
        groupPresence.setGroupId(UUID.randomUUID().toString());
        groupPresence.setState(TalkGroupPresence.STATE_EXISTS);
        if (environment.getName() == null) {
            groupPresence.setGroupName(environment.getType() + "-" + groupPresence.getGroupId().substring(groupPresence.getGroupId().length() - 8));
        } else {
            groupPresence.setGroupName(environment.getName());
        }
        groupPresence.setGroupType(environment.getType());
        LOG.debug("createGroupWithEnvironment: creating new group for client with id '" + clientId + "' with type " + environment.getType());
        TalkGroupMembership membership = new TalkGroupMembership();
        membership.setClientId(clientId);
        membership.setGroupId(groupPresence.getGroupId());
        membership.setNotificationPreference(environment.getNotificationPreference());

        if (environment.isNearby()) {
            membership.setRole(TalkGroupMembership.ROLE_NEARBY_MEMBER);
            membership.setNotificationPreference(TalkGroupMembership.NOTIFICATIONS_DISABLED);
        } else if (environment.isWorldwide()) {
            membership.setRole(TalkGroupMembership.ROLE_WORLDWIDE_MEMBER);
            membership.setNotificationPreference(environment.getNotificationPreference());
        } else {
            throw new RuntimeException("createGroupWithEnvironment: illegal type "+environment.getType());
        }

        membership.setState(TalkGroupMembership.STATE_JOINED);
        changedGroupPresence(groupPresence, new Date());
        changedGroupMembership(membership, groupPresence.getLastChanged());

        environment.setGroupId(groupPresence.getGroupId());
        environment.setClientId(clientId);
        mDatabase.saveEnvironment(environment);

        String currentGroupId = environment.getGroupId();
        String potentiallyOtherGroupId = updateEnvironment(environment);
        if (!currentGroupId.equals(potentiallyOtherGroupId)) {
            LOG.debug("createGroupWithEnvironment Collision detected: determined there is actually another group we were merged with...");
            LOG.debug("  * original groupId: '" + currentGroupId + "' - new groupId: '" + potentiallyOtherGroupId + "'");
            environment.setGroupId(potentiallyOtherGroupId);

            // Now perform a hard-delete of old group - notifications have not yet been sent out, so this is ok
            mDatabase.deleteGroupPresence(groupPresence);
        }
    }

    private void joinGroupWithEnvironment(TalkGroupPresence groupPresence, TalkEnvironment environment) {
        LOG.debug("joinGroupWithEnvironment: type " + environment.getType() + " joining group " + groupPresence.getGroupId() + " with client id '" + environment.getClientId() + "'");

        TalkGroupMembership nearbyMembership = mDatabase.findGroupMembershipForClient(groupPresence.getGroupId(), environment.getClientId());
        if (nearbyMembership == null) {
            nearbyMembership = new TalkGroupMembership();
        }
        nearbyMembership.setClientId(environment.getClientId());
        nearbyMembership.setGroupId(groupPresence.getGroupId());

        if (environment.isNearby()) {
            nearbyMembership.setRole(TalkGroupMembership.ROLE_NEARBY_MEMBER);
            nearbyMembership.setNotificationPreference(TalkGroupMembership.NOTIFICATIONS_DISABLED);
        } else if (environment.isWorldwide()) {
            nearbyMembership.setRole(TalkGroupMembership.ROLE_WORLDWIDE_MEMBER);
            nearbyMembership.setNotificationPreference(environment.getNotificationPreference());
        } else {
            throw new RuntimeException("joinGroupWithEnvironment: illegal type "+environment.getType());
        }
        // TODO: Idea: if we would only invite here the client would only receive nearby group messages after joining, which
        // the clients could do on their discretion
        if (!environment.hasExpired()) {
            nearbyMembership.setState(TalkGroupMembership.STATE_JOINED);
        } else {
            nearbyMembership.setState(TalkGroupMembership.STATE_SUSPENDED);
        }
        if (!groupPresence.getState().equals(TalkGroupPresence.STATE_EXISTS)) {
            groupPresence.setState(TalkGroupPresence.STATE_EXISTS);
            mDatabase.saveGroupPresence(groupPresence);
        }
        changedGroupPresence(groupPresence, new Date());
        changedGroupMembership(nearbyMembership, groupPresence.getLastChanged());

        environment.setGroupId(groupPresence.getGroupId());
        environment.setClientId(environment.getClientId());
        mDatabase.saveEnvironment(environment);

        touchGroupMemberPresences(groupPresence.getGroupId());
        mServer.getUpdateAgent().requestGroupUpdate(groupPresence.getGroupId(), environment.getClientId());
        mServer.getUpdateAgent().requestGroupMembershipUpdatesForNewMember(groupPresence.getGroupId(), environment.getClientId());
        mServer.getUpdateAgent().requestPresenceUpdateForClientOfMembersOfGroup(environment.getClientId(), groupPresence.getGroupId());
        mServer.getUpdateAgent().requestPresenceUpdate(environment.getClientId(), null);
    }

    private ArrayList<Pair<String, Integer>> findGroupSortedBySize(List<TalkEnvironment> matchingEnvironments) {
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
            result.add(new ImmutablePair<String, Integer>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static long deliveryCountForEnvironment(ITalkServerDatabase database, TalkEnvironment environment) {
       return database.countDeliveriesForClientInGroupInState(environment.getClientId(), environment.getGroupId(), TalkDelivery.STATE_DELIVERING);
    }
    private long deliveryCountForEnvironment(TalkEnvironment environment) {
        return deliveryCountForEnvironment(mDatabase, environment);
    }

    private static boolean checkAndSuspendGroupMembershipIfNecessary(TalkServer server, TalkEnvironment environment, TalkGroupMembership membership) {
        if (!membership.isSuspended()) {
            if (environment.hasExpired()) {
                suspendGroupMember(server, membership.getGroupId(), membership.getClientId());
                return true;
            } else {
                LOG.debug("checkAndSuspendGroupMembershipIfNecessary:  membership not yet expired for group:" + membership.getGroupId() + " client:" + membership.getClientId());
            }
        } else {
            LOG.debug("checkAndSuspendGroupMembershipIfNecessary:  membership already suspended for group:" + membership.getGroupId() + " client:" + membership.getClientId());
        }
        return membership.isSuspended();
    }

    public static void cleanupNearbyEnvironments(TalkServer server, long olderThanMillis) {
        ITalkServerDatabase database = server.getDatabase();
        List<TalkEnvironment> environments = database.findEnvironmentsByType(TalkEnvironment.TYPE_NEARBY);
        LOG.info("cleanupNearbyEnvironments: checking " + environments.size() + " nearby environments");

        long destroyed = 0;
        Date limit = new Date(new Date().getTime() - olderThanMillis);
        for (TalkEnvironment environment : environments) {
            if (server.getClientConnection(environment.getClientId()) == null) {
                // client not connected
                if (environment.getTimeReceived() != null && environment.getTimeReceived().before(limit)) {
                    LOG.warn("cleanupNearbyEnvironments: destroying environment for client " + environment.getClientId() + " group " + environment.getGroupId());
                    destroyEnvironment(server, environment);
                }
            }
        }
        LOG.info("cleanupNearbyEnvironments: destroyed " + destroyed + " nearby environments");

        // Clean left over nearby groups without environment
        // This might not be required for normal operations and could be moved
        // to the cleaning agent if it causes performance problems
        List<TalkGroupMembership> myMemberships =
                database.findGroupMembershipsWithStatesAndRoles(
                        new String[]{TalkGroupMembership.STATE_JOINED},
                        new String[]{TalkGroupMembership.ROLE_NEARBY_MEMBER});

        LOG.info("cleanupNearbyEnvironments: found " + myMemberships.size() + " nearby group memberships");

        for (TalkGroupMembership membership : myMemberships) {
            if (membership.getClientId() == null || membership.getGroupId() == null) {
                LOG.warn("cleanupNearbyEnvironments: removing group membership with null group or client " + membership.getClientId() + ", group " + membership.getGroupId());
                database.deleteGroupMembership(membership);
            } else {
                TalkEnvironment myEnvironment = database.findEnvironmentByClientIdForGroup(membership.getClientId(), membership.getGroupId());
                if (myEnvironment == null) {
                    LOG.warn("cleanupNearbyEnvironments: removing group membership without environment for client " + membership.getClientId() + ", group " + membership.getGroupId());
                    removeGroupMembership(server, membership, new Date());
                }
            }
        }

        List<TalkGroupPresence> nearbyGroups = database.findGroupPresencesWithTypeAndState(TalkGroupPresence.GROUP_TYPE_NEARBY, TalkGroupPresence.STATE_EXISTS);
        LOG.info("cleanupNearbyEnvironments: checking " + nearbyGroups.size() + " existing nearby groups");
        for (TalkGroupPresence groupPresence : nearbyGroups) {
            if (groupPresence.getLastChanged().before(limit)) {  // ignore new groups

                List<TalkEnvironment> environments2 = database.findEnvironmentsForGroup(groupPresence.getGroupId());
                if (environments2.size() == 0) {
                    // nearby group without environment

                    Date now = new Date();
                    List<TalkGroupMembership> memberships = database.findGroupMembershipsByIdWithStates(groupPresence.getGroupId(),
                            new String[]{TalkGroupMembership.STATE_JOINED});
                    for (TalkGroupMembership membership:memberships) {
                        LOG.warn("cleanupNearbyEnvironments: removing group membership without environment, groupId = " +
                                groupPresence.getGroupId()+" clientId = "+ membership.getClientId());
                        removeGroupMembership(server, membership, now);
                    }
                    // group has no members any more, delete it too
                    LOG.warn("cleanupNearbyEnvironments: setting group state to deleted of nearby group without members and environment, groupId = " + groupPresence.getGroupId());
                    groupPresence.setState(TalkGroupPresence.STATE_DELETED);
                    changedGroupPresence(server, groupPresence, now);
                }
            }
        }
        LOG.info("cleanupNearbyEnvironments: done");
    }


    public static void expireEnvironments(TalkServer server, long removeDanglingGroupsOlderThanMillis) {

        ITalkServerDatabase database = server.getDatabase();
        List<TalkEnvironment> environments = database.findEnvironmentsByType(TalkEnvironment.TYPE_WORLDWIDE);
        LOG.debug("expireEnvironments: found " + environments.size() + " worldwide environments");

        for (TalkEnvironment environment : environments) {
            LOG.debug("expireEnvironments: checking worldwide environment for client " + environment.getClientId() + " group " + environment.getGroupId());
            if (environment.hasExpired()) {
                TalkGroupMembership membership = database.findGroupMembershipForClient(environment.getGroupId(), environment.getClientId());
                if (membership != null) {
                    long undelivered = deliveryCountForEnvironment(database, environment);
                    if (undelivered > 0) {
                        LOG.debug("expireEnvironments: " + undelivered + " undelivered messages for client " + environment.getClientId() + " group " + environment.getGroupId() + ",checking if suspend membership");
                        checkAndSuspendGroupMembershipIfNecessary(server, environment, membership);
                    } else {
                        LOG.debug("expireEnvironments: no undelivered messages for client " + environment.getClientId() + " group " + environment.getGroupId() + ", destroying environment");
                        destroyEnvironment(server, environment);
                    }
                } else {
                    LOG.warn("expireEnvironments: no membership for expired environment for client "+environment.getClientId()+" group "+environment.getGroupId());
                    destroyEnvironment(server, environment);
                }
            } else {
                LOG.debug("expireEnvironments: not expired environment " + environment.getClientId() + " group " + environment.getGroupId() + ", released=" + environment.getTimeReleased() + ",conn=" + server.getClientConnection(environment.getClientId()));
                if (environment.getTimeReleased() == null && server.getClientConnection(environment.getClientId()) == null) {
                    // client not connected, but environment not released
                    releaseEnvironmentForClient(server, environment.getClientId(), TalkEnvironment.TYPE_WORLDWIDE, null, null);
                }
            }
        }

        // Clean left over worldwide groups without environment
        // This might not be required for normal operations and could be moved
        // to the cleaning agent if it causes performance problems
        List<TalkGroupMembership> myMemberships =
                database.findGroupMembershipsWithStatesAndRoles(
                        new String[]{TalkGroupMembership.STATE_JOINED, TalkGroupMembership.STATE_SUSPENDED},
                        new String[]{TalkGroupMembership.ROLE_WORLDWIDE_MEMBER});

        LOG.debug("expireEnvironments: found " + myMemberships.size() + " worldwide group memberships");

        for (TalkGroupMembership membership : myMemberships) {
            if (membership.getClientId() == null || membership.getGroupId() == null) {
                LOG.warn("releaseEnvironment: removing group membership with null group or client " + membership.getClientId() + ", group " + membership.getGroupId());
                database.deleteGroupMembership(membership);
            } else {
                TalkEnvironment myEnvironment = database.findEnvironmentByClientIdForGroup(membership.getClientId(), membership.getGroupId());
                if (myEnvironment == null) {
                    LOG.warn("releaseEnvironment: removing group membership without environment for client " + membership.getClientId() + ", group " + membership.getGroupId());
                    removeGroupMembership(server, membership, new Date());
                }
            }
        }

        List<TalkGroupPresence> nearbyGroups = database.findGroupPresencesWithTypeAndState(TalkGroupPresence.GROUP_TYPE_WORLDWIDE, TalkGroupPresence.STATE_EXISTS);
        LOG.info("expireEnvironments: checking " + nearbyGroups.size() + " existing worldwide groups");
        Date limit = new Date(new Date().getTime() - removeDanglingGroupsOlderThanMillis);
        for (TalkGroupPresence groupPresence : nearbyGroups) {
            if (groupPresence.getLastChanged().before(limit)) {  // ignore new groups

                List<TalkEnvironment> environments2 = database.findEnvironmentsForGroup(groupPresence.getGroupId());
                if (environments2.size() == 0) {
                    // nearby group without environment

                    Date now = new Date();
                    List<TalkGroupMembership> memberships = database.findGroupMembershipsByIdWithStates(groupPresence.getGroupId(),
                            new String[]{TalkGroupMembership.STATE_JOINED});
                    for (TalkGroupMembership membership:memberships) {
                        LOG.warn("expireEnvironments: removing worldwide group membership without environment, groupId = " +
                                groupPresence.getGroupId()+" clientId = "+ membership.getClientId());
                        removeGroupMembership(server, membership, now);
                    }
                    // group has no members any more, delete it too
                    LOG.warn("expireEnvironments: setting group state to deleted of worldwide group without members and environment, groupId = " + groupPresence.getGroupId());
                    groupPresence.setState(TalkGroupPresence.STATE_DELETED);
                    changedGroupPresence(server, groupPresence, now);
                }
            }
        }
    }

    @Override
    public String updateEnvironment(TalkEnvironment environment) {
        String clientId =  mConnection.getClientId();
        logCall("updateEnvironment(clientId: '" + clientId + "')");
        LOG.info("updateEnvironment(clientId: " + clientId + ", type: " + environment.getType() + ", group: " + environment.getGroupId() + ", geo:" + Arrays.toString(environment.getGeoLocation()) + ", loctype:" + environment.getLocationType() + ", accuracy:" + environment.getAccuracy() + ", bssids:" + Arrays.toString(environment.getBssids()));

        requireIdentification(true);

        if (!environment.isValid()) {
            throw new RuntimeException("environment " + environment + " is not valid!");
        }

        if (environment.getType() == null) {
            LOG.warn("updateEnvironment: no environment type, defaulting to nearby. Please fix client");
            environment.setType(TalkEnvironment.TYPE_NEARBY);
        }
/*
        if (environment.isWorldwide()) {
            if (!mConnection.didWorldwideNagging) {
                mServer.getUpdateAgent().requestUserAlert(
                        mConnection.getClientId(),
                        StaticSystemMessage.Message.WORLD_WIDE_DEACTIVATED);
                mConnection.didWorldwideNagging = true;
            }
            return "00000000-0000-0000-0000-000000000000";
            //throw new RuntimeException("worldwide environment is no longer supported");
        }
*/
        if (environment.isWorldwide() && (environment.getTag() == null || environment.getTag().length() < 2)) {
            Date now = new Date();
            long day = now.getTime() / 1000 / 60 / 60 / 24;
            environment.setTag("*-"+ day);
        }

        environment.setTimeReceived(new Date());
        environment.setClientId(clientId);
        environment.setTimeReleased(null);

        String lockId = "env-"+environment.getType();
        if (TalkEnvironment.TYPE_NEARBY.equals(environment.getType())) {
            lockId = "env-nearby-"+clientId;
        }

        synchronized (mServer.idLock(lockId)) {

            List<TalkEnvironment> matching = mDatabase.findEnvironmentsMatching(environment);

            int minNumberOfGroups = 0;
            int maxNumberOfGroups = 0;
            int abandonedGroups = 0;

            if (!environment.isNearby()) {

                ArrayList<Pair<String, Integer>> preFilteredEnvironmentsPerGroup = findGroupSortedBySize(matching);

                HashMap<String, HashMap<String, TalkGroupMembership>> memberMap = new HashMap<String, HashMap<String, TalkGroupMembership>>();
                final String[] states = {TalkGroupMembership.STATE_JOINED};

                for (Pair<String, Integer> item : preFilteredEnvironmentsPerGroup) {
                    List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsByIdWithStates(item.getLeft(), states);

                    HashMap<String, TalkGroupMembership> membershipMap = new HashMap<String, TalkGroupMembership>();
                    for (TalkGroupMembership membership : memberships) {
                        if (membership.isMember() || membership.getClientId().equals(clientId)) {
                            membershipMap.put(membership.getClientId(), membership);
                        }
                    }
                    if (membershipMap.size() > 0) {
                        memberMap.put(item.getLeft(), membershipMap);
                    }
                }

                List<TalkEnvironment> matching_filtered = new ArrayList<TalkEnvironment>();
                for (TalkEnvironment te : matching) {
                    HashMap<String, TalkGroupMembership> membershipMap = memberMap.get(te.getGroupId());
                    if (membershipMap != null) {
                        TalkGroupMembership membership = membershipMap.get(te.getClientId());
                        if (membership != null) {
                            matching_filtered.add(te);
                        }
                    }
                }
                LOG.debug("updateEnvironment: found " + matching.size() + " worldwide environments, replacing with " + matching.size() + " filtered environments");
                matching = matching_filtered;

                minNumberOfGroups = matching.size() / MAX_WORLD_WIDE_GROUP_SIZE + 1;
                // TODO: it seems to me that in case MIN_WORLD_WIDE_GROUP_SIZE is 8 and we have a matching.size of 8 we would get 2 as answer, but in fact want it to be 1
                // maybe something like (int)(((double)matching.size() + 0.5) / MIN_WORLD_WIDE_GROUP_SIZE)
                // ... enter `plot Quotient[(x-0.5),8] + 1` at wolfram alpha to see what i mean
                maxNumberOfGroups = matching.size() / MIN_WORLD_WIDE_GROUP_SIZE + 1;

                // The Group distribution algorithm works as follows:
                // 1. The first member of a matching set will make a group to be created
                // 2. New arriving members will be added to the n-th smallest group
                //    depending on the condition explained in 3c.
                //    This might make one group grow over the limit if all groups are "full"
                // 3. When a group members updates his environment (and only then), there are three possible
                //    outcomes:
                //    a: If the average group size is between MIN_WORLD_WIDE_GROUP_SIZE and MAX_WORLD_WIDE_GROUP_SIZE,
                //       nothing special happens, the client will stay in the current group.
                //    b: If all groups are "full" and at least one group is over the limit (MAX_WORLD_WIDE_GROUP_SIZE),
                //       the "minNumberOfGroups" will be larger than the current number of groups,
                //       and a new group will be created, and the updating client will be moved to this new group.
                //    c: If there are so many groups that the average group size is below MIN_WORLD_WIDE_GROUP_SIZE,
                //       the smallest groups will be virtually "declared abandoned" by computing the number of
                //       superfluous groups.
                //       The updating client will be moved to the smallest non-abandoned group; this will happen to other
                //       environment-updaters too, until the group is empty. Note that a member will stay in a
                //       group and the group will remain in place unless the member updates it's environment.
                //       This means that there may be a number of abandoned groups with just one non-updating member
                //       until the environment expires. However, if the number of environments increases, these
                //       groups will be repopulated before new groups will be created.
                //   Yeah!
            }  else {
                LOG.info("updateEnvironment: nearby matching:"+matching.size()+", clientId: " + clientId + ", type: "+environment.getType()+", group: "+environment.getGroupId()+", geo:"+Arrays.toString(environment.getGeoLocation())+", loctype:"+environment.getLocationType()+", accuracy:"+environment.getAccuracy()+ ", bssids:"+Arrays.toString(environment.getBssids()));
            }

            // determine how many environments in the matching list belong to how many different groups
            ArrayList<Pair<String, Integer>> environmentsPerGroup = findGroupSortedBySize(matching);

            // determine how many groups are not needed
            abandonedGroups = environmentsPerGroup.size() - maxNumberOfGroups;
            if (abandonedGroups < 0) {
                abandonedGroups = 0;
            }
            TalkEnvironment myEnvironment = mDatabase.findEnvironmentByClientId(environment.getType(), clientId);

            // TODO: set to trace for production
            if (LOG.isTraceEnabled()) {
                // begin debug output code
                if (environment.isWorldwide()) {
                    int i = 0;
                    for (Pair<String, Integer> epg : environmentsPerGroup) {
                        LOG.debug("updateEnvironment: " + epg.getRight() + " members in group " + epg.getLeft() + ",(" + (i + 1) + "/" + environmentsPerGroup.size() + "),clientId=" + mConnection.getClientId() + ",my current groupId=" + environment.getGroupId());
                        ++i;
                    }
                    int ii = 0;

                    for (Pair<String, Integer> epg : environmentsPerGroup) {
                        LOG.debug("updateEnvironment(member listing): " + epg.getRight() + " members in group " + epg.getLeft() + ",(" + ii + "/" + environmentsPerGroup.size() + "),clientId=" + mConnection.getClientId() + ",my current groupId=" + environment.getGroupId());

                        List<TalkGroupMembership> members = mDatabase.findGroupMembershipsById(epg.getLeft());
                        int g = 0;
                        for (TalkGroupMembership member : members) {
                            if (member.isMember() || member.isSuspended()) {
                                TalkPresence presence = mDatabase.findPresenceForClient(member.getClientId());
                                TalkEnvironment hisEnvironment = mDatabase.findEnvironmentByClientId(environment.getType(), member.getClientId());
                                if (presence != null && hisEnvironment != null) {
                                    LOG.debug("updateEnvironment: member " + g + "/" + epg.getRight() + " members in group " + epg.getLeft() +
                                            ", nick '" + presence.getClientName() + "', membership '" + member.getState() + "'.status '" + presence.getConnectionStatus() +
                                            ", hasExpired=" + environment.hasExpired() + ", released:" + environment.getTimeReleased() + ", ttl=" + environment.getTimeToLive() + ", undelivered=" + deliveryCountForEnvironment(environment));
                                } else {
                                    LOG.error("updateEnvironment: missing presence or environment for member " + g + "/" + epg.getRight() +
                                            " members in group " + epg.getLeft() + ", clientId=" + member.getClientId() + ", presence:" + presence + ", environment:" + environment);
                                }
                                ++g;
                            }
                        }
                        ++ii;
                    }
                }
                // end debug output code
            }

            long undeliveredCount = deliveryCountForEnvironment(environment);

            for (TalkEnvironment te : matching) {
                if (te.getClientId().equals(clientId)) {
                    // there is already a matching environment for us
                    TalkGroupMembership myMembership = mDatabase.findGroupMembershipForClient(te.getGroupId(), te.getClientId());
                    TalkGroupPresence myGroupPresence = mDatabase.findGroupPresenceById(te.getGroupId());
                    if (myMembership != null && myGroupPresence != null) {
                        if ((myMembership.isNearby() || myMembership.isWorldwide()) &&
                                (myMembership.isJoined() || myMembership.isSuspended()) &&
                                myGroupPresence.getState().equals(TalkGroupPresence.STATE_EXISTS)) {
                            // everything seems fine, but are we in the right group?
                            if (myMembership.isNearby()) {
                                // for nearby, we want to be in the largest group
                                LOG.info("updateEnvironment: nearby matching:"+matching.size()+", clientId: " + clientId +", environmentsPerGroup:" + environmentsPerGroup.size());
                                if (environmentsPerGroup.size() > 1) {
                                    if (!environmentsPerGroup.get(0).getLeft().equals(te.getGroupId())) {
                                        // we are not in the largest group, lets move over
                                        destroyEnvironment(mServer, myEnvironment);
                                        // join the largest group
                                        String largestGroupId = environmentsPerGroup.get(0).getLeft();
                                        TalkGroupPresence largestGroup = mDatabase.findGroupPresenceById(largestGroupId);
                                        if (largestGroup != null) {
                                            LOG.info("updateEnvironment: nearby moving to largest group:" + largestGroup.getGroupId() + ", clientId: " + clientId);
                                            joinGroupWithEnvironment(largestGroup, environment);
                                            return largestGroup.getGroupId();
                                        } else {
                                            LOG.error("updateEnvironment: nearby largest group not found:"+ largestGroupId + ", clientId: " + clientId);
                                        }
                                    }
                                }
                            } else {
                                // we are in worldwide and already member of a worldwide group
                                // for worldwide, we want to make sure the group is neither too small nor too large

                                LOG.debug("updateEnvironment: worldwide: matching=" + matching.size() + ",groups=" + environmentsPerGroup.size() + ",minGroups=" + minNumberOfGroups + ",maxGroups=" + maxNumberOfGroups + ",abandoned=" + abandonedGroups + ",clientId=" + mConnection.getClientId() + ",groupId=" + myMembership.getGroupId());

                                if (minNumberOfGroups > environmentsPerGroup.size() && undeliveredCount == 0) {
                                    // we have not enough groups, lets create a new group and join it
                                    LOG.debug("updateEnvironment: worldwide: not enough groups, creating a new group and joining it,clientId=" + clientId + ",groupId=" + myMembership.getGroupId());
                                    destroyEnvironment(mServer, myEnvironment);
                                    createGroupWithEnvironment(environment);
                                    return environment.getGroupId();
                                }
                                if (environmentsPerGroup.size() > maxNumberOfGroups) {
                                    // we have too many groups, lets see if we have to consolidate
                                    // maxNumberOfGroups is always at least 1, so we get here only when there are at least 2 groups
                                    LOG.debug("updateEnvironment: worldwide: too many groups,clientId=" + clientId + ",groupId=" + myMembership.getGroupId());

                                    for (int n = 0; n < abandonedGroups; ++n) {
                                        // lets check if we are in an "abandoned group"
                                        if (environmentsPerGroup.get(environmentsPerGroup.size() - 1 - n).getLeft().equals(te.getGroupId())) {
                                            // we are in an an abandoned group, lets move the smallest non-abandoned group
                                            LOG.debug("updateEnvironment: worldwide: too many groups and we are in an abandoned group (" + n + "-smallest),joining " + abandonedGroups + "-smallest group,clientId=" + clientId + ",groupId=" + myMembership.getGroupId());

                                            // but before we can move, we need to check if there are undelivered messages

                                            if (undeliveredCount == 0) {
                                                destroyEnvironment(mServer, myEnvironment);
                                                String nThSmallestGroupId = environmentsPerGroup.get(environmentsPerGroup.size() - 1 - abandonedGroups).getLeft();
                                                TalkGroupPresence nThSmallestGroup = mDatabase.findGroupPresenceById(nThSmallestGroupId);
                                                if (nThSmallestGroup == null) {
                                                    LOG.error("updateEnvironment: worldwide: secondSmallestGroup presence not found, id=" + nThSmallestGroupId + ",clientId=" + clientId + ",groupId=" + myMembership.getGroupId());
                                                } else {
                                                    joinGroupWithEnvironment(nThSmallestGroup, environment);
                                                    return nThSmallestGroup.getGroupId();
                                                }
                                            } else {
                                                LOG.debug("updateEnvironment: worldwide: can not move client=" + clientId + " from groupId=" + myMembership.getGroupId() + ", there are " + undeliveredCount + " deliveries to be delivered");
                                            }
                                        }
                                    }
                                }
                                // we are fine and in the right group
                            }
                            // group membership has not changed, we are still fine
                            // just update environment
                            if (environment.getGroupId() == null) {
                                // first environment update without a group known to the client, but group exists on server, probably unclean connection shutdown
                                environment.setGroupId(myEnvironment.getGroupId());
                            } else if (!environment.getGroupId().equals(myEnvironment.getGroupId())) {
                                // matching environment found, but group id differs from old environment, which must not happen - client wants to gain membership to a group it is not entitled to
                                // lets destroy all environments
                                destroyEnvironment(mServer, te);
                                destroyEnvironment(mServer, myEnvironment);
                                LOG.warn("updateEnvironment: different group id for client=" + clientId + " myEnvironment.groupId=" + myEnvironment.getGroupId() + ", te.groupId=" + te.getGroupId() +" environment.groupId=" + environment.getGroupId());
                                throw new RuntimeException("illegal group id in environment");
                            }
                            myEnvironment.updateWith(environment);
                            mDatabase.saveEnvironment(myEnvironment);
                            // update worldwide notification preference if necessary
                            if (environment.isWorldwide() && environment.getNotificationPreference() != null) {
                                if (!environment.getNotificationPreference().equals(myMembership.getNotificationPreference())) {
                                    myMembership.setNotificationPreference(environment.getNotificationPreference());
                                    mDatabase.saveGroupMembership(myMembership);
                                    mServer.getUpdateAgent().requestGroupMembershipUpdate(myMembership);
                                }
                            }
                            return myGroupPresence.getGroupId();
                        } else {
                            // there is a group and a membership, but they seem to be tombstones, so lets ignore them, just get rid of the bad environment
                            mDatabase.deleteEnvironment(te);
                            break; // continue processing after the loop and join or create a new group
                        }
                    }
                }
            }
            // when we got here, there is no environment for us in the matching list
            if (myEnvironment != null) {
                // we have an environment for another location that does not match, lets get rid of it
                destroyEnvironment(mServer, myEnvironment);
            }
            if (!matching.isEmpty()) {
                if (environment.isNearby()) {
                    // join the largest group
                    String largestGroupId = environmentsPerGroup.get(0).getLeft();
                    TalkGroupPresence largestGroup = mDatabase.findGroupPresenceById(largestGroupId);
                    if (largestGroup.getState().equals(TalkGroupPresence.STATE_EXISTS)) {
                        LOG.info("updateEnvironment: nearby joining largest group:" + largestGroupId + ", clientId: " + clientId);
                        joinGroupWithEnvironment(largestGroup, environment);
                        LOG.info("updateEnvironment: nearby joined largest group:" + largestGroupId + ", clientId: " + clientId);
                        return largestGroup.getGroupId();
                    } else {
                        LOG.warn("the (largest) nearby group we were supposed to join is gone or does not exist, largestGroup=" + largestGroup);
                    }
                } else if (environment.isWorldwide()) {
                    if (minNumberOfGroups > environmentsPerGroup.size()) {
                        // we have not enough groups, lets create a new group and join it
                        LOG.debug("updateEnvironment: worldwide: not enough groups on first update, will create a new group and join it,clientId=" + clientId);
                    } else {
                        // join the n-th-smallest group in order to properly distribute the clients
                        String nThSmallestGroupId = environmentsPerGroup.get(environmentsPerGroup.size() - 1 - abandonedGroups).getLeft();
                        String kind = "" + abandonedGroups + "-smallestGroupId";
                        LOG.debug("updateEnvironment: worldwide: joining " + kind + ", id=" + nThSmallestGroupId + ",clientId=" + clientId);
                        TalkGroupPresence destinationGroup = mDatabase.findGroupPresenceById(nThSmallestGroupId);
                        if (destinationGroup.getState().equals(TalkGroupPresence.STATE_EXISTS)) {
                            joinGroupWithEnvironment(destinationGroup, environment);
                            return destinationGroup.getGroupId();
                        } else {
                            LOG.warn("the worldwide group (" + kind + ") we were supposed to join is gone, will create a new one, destinationGroupId=" + nThSmallestGroupId);
                        }
                    }
                } else {
                    throw new RuntimeException("unknown environment type:" + environment.getType());
                }
            }
            // we are alone or first at the location, lets create a new group
            LOG.info("updateEnvironment: creating new group type:" + environment.getType() + ", clientId: " + clientId);
            createGroupWithEnvironment(environment);
            LOG.info("updateEnvironment: created new group type:" + environment.getType() + ", clientId: " + clientId + ", groupId="+environment.getGroupId());
            return environment.getGroupId();
        }
    }

    private static void removeGroupMembership(TalkServer server, TalkGroupMembership membership, Date now) {
        // remove my membership
        // set membership state to NONE
        membership.setState(TalkGroupMembership.STATE_NONE);
        if (membership.isAdmin()) {
            // degrade removed users to member
            membership.setRole(TalkGroupMembership.ROLE_MEMBER);
        }
        membership.trashPrivate();
        changedGroupMembership(server, membership, now);
    }

    private static void destroyEnvironment(TalkServer server, TalkEnvironment environment) {
        LOG.debug("destroyEnvironment(" + environment + ")");
        ITalkServerDatabase database = server.getDatabase();
        TalkGroupPresence groupPresence = database.findGroupPresenceById(environment.getGroupId());
        TalkGroupMembership membership = database.findGroupMembershipForClient(environment.getGroupId(), environment.getClientId());
        if (membership != null && (membership.isJoined() || membership.isSuspended())) {
            Date now = new Date();
            removeGroupMembership(server, membership, now);
            String[] states = {TalkGroupMembership.STATE_JOINED, TalkGroupMembership.STATE_SUSPENDED};
            List<TalkGroupMembership> membershipsLeft = database.findGroupMembershipsByIdWithStates(environment.getGroupId(), states);
            LOG.debug("destroyEnvironment: membersLeft: " + membershipsLeft.size());

            // clean up other offline members that somehow might be stuck in the group
            // although this should never happen except on crash or server restart
            // The canonical place would be to check this on group join, but here
            // we already have a list of all remaining members, so it will be faster
            // and should cause less trouble than doing it on joining
            int removedCount = 0;
            if (environment.isNearby()) {
                // cleanup other offline members for nearby only
                for (int i = 0; i < membershipsLeft.size(); ++i) {

                    TalkGroupMembership otherMembership = membershipsLeft.get(i);
                    boolean isConnected = server.isClientConnected(otherMembership.getClientId());
                    if (!isConnected) {
                        // remove offline member from group
                        removeGroupMembership(server, otherMembership, now);
                        ++removedCount;
                    }
                }
            }
            LOG.debug("destroyEnvironment: offline members removed: " + removedCount);

            if (membershipsLeft.size() - removedCount <= 0) {
                LOG.debug("destroyEnvironment: last member left, removing group " + groupPresence.getGroupId());
                // last member removed, remove group
                groupPresence.setState(TalkGroupPresence.STATE_DELETED);
                changedGroupPresence(server, groupPresence, now);
                // explicitly request a group updated notification for the last removed client because
                // calling changedGroupPresence only will not send out "groupUpdated" notifications to members with state "none"
                server.getUpdateAgent().requestGroupUpdate(groupPresence.getGroupId(), environment.getClientId());
            }
        }
        database.deleteEnvironment(environment);
    }

    @Override
    public void destroyEnvironment(String type) {
        requirePastIdentification();
        String clientId =  mConnection.getClientId();
        logCall("destroyEnvironment(clientId: '" + clientId + "')");

        if (clientId == null) {
            throw new RuntimeException("null client id");
        }

        if (type == null) {
            LOG.warn("destroyEnvironment: no environment type, defaulting to nearby. Please fix client");
            type = TalkEnvironment.TYPE_NEARBY;
        }

        List<TalkEnvironment> myEnvironments = mDatabase.findEnvironmentsForClient(clientId, type);

        for (TalkEnvironment myEnvironment : myEnvironments) {
            destroyEnvironment(mServer, myEnvironment);
        }
    }

    private static void releaseEnvironmentForClient(TalkServer server, String clientId, String type, Long timeToLive, String notificationPreference) {

        ITalkServerDatabase database = server.getDatabase();
        List<TalkEnvironment> myEnvironments = database.findEnvironmentsForClient(clientId, type);

        TalkEnvironment mostRecentEnvironment = null;
        for (TalkEnvironment myEnvironment : myEnvironments) {
            if (mostRecentEnvironment == null || mostRecentEnvironment.getTimeReceived().getTime() < myEnvironment.getTimeReceived().getTime()) {
                mostRecentEnvironment = myEnvironment;
            }
        }

        if (mostRecentEnvironment != null) {
            if (timeToLive != null && notificationPreference != null) {
                LOG.debug("releaseEnvironmentForClient: setting most recent environment ttl with type " + mostRecentEnvironment.getType() +
                        ", ttl " + mostRecentEnvironment.getTimeToLive()+
                        ", notificationPreference " + mostRecentEnvironment.getNotificationPreference() +
                        " for client " + clientId);
                mostRecentEnvironment.setTimeToLive(timeToLive.longValue());
                if (mostRecentEnvironment.isWorldwide() && notificationPreference != null) {
                    if (!notificationPreference.equals(mostRecentEnvironment.getNotificationPreference())) {
                        mostRecentEnvironment.setNotificationPreference(notificationPreference);
                        TalkGroupMembership myMembership = database.findGroupMembershipForClient(mostRecentEnvironment.getGroupId(), mostRecentEnvironment.getClientId());
                        if (myMembership != null) {
                            myMembership.setNotificationPreference(mostRecentEnvironment.getNotificationPreference());
                            database.saveGroupMembership(myMembership);
                            server.getUpdateAgent().requestGroupMembershipUpdate(myMembership);
                            LOG.info("releaseEnvironmentForClient: changed notification preference for environment type " + type + " for client " + clientId + " group " + mostRecentEnvironment.getGroupId() + " to " + notificationPreference);
                        } else {
                            LOG.error("releaseEnvironmentForClient: could not find group membership for environment type " + type + " for client " + clientId+" group "+mostRecentEnvironment.getGroupId());
                        }
                    }
                }
                database.saveEnvironment(mostRecentEnvironment);
            }
        }

        for (TalkEnvironment myEnvironment : myEnvironments) {
            if (myEnvironment.getTimeReceived().getTime() != mostRecentEnvironment.getTimeReceived().getTime()) {
                // not the most recent environment; duplicate environments should not happen, but...
                if (mostRecentEnvironment.getGroupId().equals(myEnvironment.getGroupId())) {
                    // we have another environment with the same group id, so just trash the old one
                    LOG.warn("releaseEnvironmentForClient: deleting duplicate old environment with type " + myEnvironment.getType() + " for client " + clientId);
                    database.deleteEnvironment(myEnvironment);
                } else {
                    // we have another environment with the another group id, so destroy the old one
                    LOG.warn("releaseEnvironmentForClient: destroying duplicate old environment with type " + myEnvironment.getType() + " for client " + clientId);
                    destroyEnvironment(server, myEnvironment);
                }
            } else if (!myEnvironment.willLiveAfterRelease() || myEnvironment.hasExpired()) {
                LOG.debug("releaseEnvironmentForClient: destroying environment with type " + myEnvironment.getType() + ", ttl " + myEnvironment.getTimeToLive() + " for client " + clientId);
                long deliveryCount = deliveryCountForEnvironment(database, myEnvironment);
                if (deliveryCount == 0) {
                    LOG.debug("releaseEnvironmentForClient: destroying expired environment with type " + myEnvironment.getType() + ", ttl " + myEnvironment.getTimeToLive() + " for client " + clientId);
                    destroyEnvironment(server, myEnvironment);
                } else {
                    LOG.debug("releaseEnvironmentForClient: keeping expired environment with type " + myEnvironment.getType() + ", ttl " + myEnvironment.getTimeToLive() + " for client " + clientId + " because it has " + deliveryCount + " undelivered deliveries");
                    TalkGroupMembership membership = database.findGroupMembershipForClient(myEnvironment.getGroupId(),myEnvironment.getClientId());
                    if (membership != null) {
                        checkAndSuspendGroupMembershipIfNecessary(server, myEnvironment, membership);
                    } else {
                        LOG.debug("releaseEnvironmentForClient: no membership found for environment " + myEnvironment.getType() + ", ttl " + myEnvironment.getTimeToLive() + " for client " + clientId + " with " + deliveryCount + " undelivered deliveries");
                    }
                }
            } else {
                if (myEnvironment.getTimeReleased() == null) {
                    LOG.debug("releaseEnvironmentForClient: releasing environment with type " + myEnvironment.getType() + ", ttl " + myEnvironment.getTimeToLive() + " for client " + clientId);
                    myEnvironment.setTimeReleased(new Date());
                    database.saveEnvironment(myEnvironment);
                } else {
                    LOG.debug("releaseEnvironmentForClient: environment with ttl " + myEnvironment.getTimeToLive() +
                            " already released on " + myEnvironment.getTimeReleased() + ", ttl remaining " +
                            (myEnvironment.getTimeReleased().getTime() + myEnvironment.getTimeToLive() - new Date().getTime()));
                }
            }
        }
    }

    @Override
    public void releaseEnvironment(String type) {
        requirePastIdentification();
        String clientId = mConnection.getClientId();
        logCall("releaseEnvironment(clientId: '" + clientId + "')");

        if (type == null) {
            LOG.warn("releaseEnvironment: no environment type, defaulting to nearby. Please fix client");
            type = TalkEnvironment.TYPE_NEARBY;
        }
        releaseEnvironmentForClient(mServer,clientId,type, null, null);
    }

    @Override
    public void releaseEnvironmentUpdatingParameters(String type, long timeToLive, String notificationPreference) {
        requirePastIdentification();
        String clientId = mConnection.getClientId();
        logCall("releaseEnvironmentUpdatingParameters(clientId: '" + clientId + "'"+", ttl: "+timeToLive+", notificationPreference: "+notificationPreference+")");

        if (type == null || !type.equals(TalkEnvironment.TYPE_WORLDWIDE)) {
            throw new RuntimeException("releaseEnvironmentUpdatingParameters: illegal type "+type);
        }
        releaseEnvironmentForClient(mServer,clientId,type, new Long(timeToLive), notificationPreference);
    }

    /*
    @Override
    public Boolean[] isMemberInGroupsSlow(String[] groupIds) {
        requireIdentification(true);
        ArrayList<Boolean> result = new ArrayList<Boolean>();
        logCall("isMemberInGroups(groupIds: '" + Arrays.toString(groupIds) + "'");
        String clientId = mConnection.getClientId();

        for (String groupId : groupIds) {
            TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, clientId);
            // calling client is treated as member even if suspended
            if (membership != null && (membership.isInvited() || membership.isMember() || membership.isSuspended())) {
                result.add(true);
            } else {
                result.add(false);
            }
        }

        return result.toArray(new Boolean[result.size()]);
    }
    */

    @Override
    public Boolean[] isMemberInGroups(String[] groupIds) {
        requireIdentification(true);
        ArrayList<Boolean> result = new ArrayList<Boolean>();
        logCall("isMemberInGroups(groupIds: '" + Arrays.toString(groupIds) + "'");
        String clientId = mConnection.getClientId();

        List<TalkGroupMembership> memberships = mDatabase.findGroupMembershipsForClient(clientId);
        Set<String> groupSet = new HashSet<String>();
        for (TalkGroupMembership membership: memberships) {
            // calling client is treated as member even if suspended
            if (membership != null && (membership.isInvited() || membership.isMember() || membership.isSuspended())) {
                groupSet.add(membership.getGroupId());
            }
        }

        for (String groupId : groupIds) {
            if (groupSet.contains(groupId)) {
                result.add(true);
            } else {
                result.add(false);
            }
        }

        return result.toArray(new Boolean[result.size()]);
    }

    @Override
    public Boolean[] areMembersOfGroup(String groupId, String[] clientIds) {
        requireIdentification(true);
        ArrayList<Boolean> result = new ArrayList<Boolean>();
        logCall("areMembersOfGroup(groupId: '"+groupId+"clientIds '" + Arrays.toString(clientIds) + "'");

        String myClientId = mConnection.getClientId();
        TalkGroupMembership myMembership = mDatabase.findGroupMembershipForClient(groupId, myClientId);

        // allow retrieve membership information even if you are suspended
        if (!(myMembership != null && (myMembership.isInvited() || myMembership.isMember() || myMembership.isSuspended()))) {
            throw new RuntimeException("not allowed, you are not a member of this group");
        }

        for (String clientId : clientIds) {
            TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(groupId, clientId);
            // treat other suspended members as not being members
            if (membership != null && (membership.isInvited() || membership.isMember())) {
                result.add(true);
            } else {
                result.add(false);
            }
        }

        return result.toArray(new Boolean[result.size()]);
    }

    // return true if for each client the caller is related to by a relationship or by an active group membership or unfinished deliveries
    @Override
    public Boolean[] isContactOf(String[] clientIds) {
        requireIdentification(true);
        logCall("isContactOf(clientIds: '" + Arrays.toString(clientIds) + "'");
        final String clientId = mConnection.getClientId();

        final List<TalkRelationship> relationships =
                mDatabase.findRelationshipsForClientInStates(clientId, TalkRelationship.STATES_RELATED);

        final HashMap<String,TalkRelationship> relationshipHashMap = new HashMap<String, TalkRelationship>();

        Set<String> myContactIds = new HashSet<String>();
        for (TalkRelationship relationship : relationships) {
            myContactIds.add(relationship.getOtherClientId());
            relationshipHashMap.put(relationship.getOtherClientId(), relationship);
        }

        // we do this to check if the relationships are symmetrical
        final List<TalkRelationship> otherRelationships =
                mDatabase.findRelationshipsForOtherClientInStates(clientId, TalkRelationship.STATES_RELATED);

        Set<String> myOtherContactIds = new HashSet<String>();
        for (TalkRelationship relationship : otherRelationships) {
            if (!myContactIds.contains(relationship.getClientId())) {
                // we have only a reverse relationship pointing to us, but none pointing to the other client
                LOG.info("isContactOf: missing relationship from us (" + clientId + ") to contact " + relationship.getClientId() + " who has a relationship pointing to us with state '" + relationship.getState() + "'");
            }
            myOtherContactIds.add(relationship.getClientId());
        }
        for (String otherClientId : myContactIds) {
            if (!myOtherContactIds.contains(otherClientId)) {
                // we have a relationship pointing to otherClientId, but he has no contact point to us
                LOG.info("isContactOf: missing relationship from contact " + otherClientId + " to us (" + clientId + ") while we have a relationship pointing to him with state '" + relationshipHashMap.get(otherClientId).getState() + "'");
            }
        }

        final List<TalkGroupMembership> clientMemberships = mDatabase.findGroupMembershipsForClientWithStates(clientId, TalkGroupMembership.ACTIVE_STATES);

        for (TalkGroupMembership clientMembership : clientMemberships) {
            final List<TalkGroupMembership> myGroupContacts = mDatabase.findGroupMembershipsByIdWithStates(clientMembership.getGroupId(), TalkGroupMembership.ACTIVE_STATES);
            for (TalkGroupMembership membership : myGroupContacts) {
                myContactIds.add(membership.getClientId());
            }
        }

        // treat former senders with unfinished deliveries as contact
        final List<TalkDelivery> deliveries = mDatabase.findDeliveriesForClientInState(clientId, TalkDelivery.STATE_DELIVERING);
        for (TalkDelivery delivery : deliveries) {
            myContactIds.add(delivery.getSenderId());
        }
        final List<TalkDelivery> attachmentDeliveries =
                mDatabase.findDeliveriesForClientInDeliveryAndAttachmentStates(clientId,
                        TalkDelivery.IN_ATTACHMENT_DELIVERY_STATES, TalkDelivery.IN_ATTACHMENT_STATES);
        for (TalkDelivery delivery : attachmentDeliveries) {
            myContactIds.add(delivery.getSenderId());
        }

        ArrayList<Boolean> result = new ArrayList<Boolean>();

        for (String contactId : clientIds) {
            if (myContactIds.contains(contactId)) {
                 result.add(true);
            } else {
                result.add(false);
            }
        }

        return result.toArray(new Boolean[result.size()]);
    }

    @Override
    public long unfinishedDeliveriesFrom(String senderId) {
        requireIdentification(true);
        logCall("unfinishedDeliveriesFrom(senderId: '" + senderId + "'");
        final String myClientId = mConnection.getClientId();

        long undelivered = mDatabase.countDeliveriesBetweenClientsInState(senderId, myClientId, TalkDelivery.STATE_DELIVERING);
        long unfinished = mDatabase.countDeliveriesBetweenClientsInDeliveryAndAttachmentStates(senderId, myClientId, TalkDelivery.IN_ATTACHMENT_DELIVERY_STATES, TalkDelivery.IN_ATTACHMENT_STATES);
        long total = undelivered + unfinished;
        LOG.debug("unfinishedDeliveriesFrom:"+senderId+" -> "+myClientId+", undelivered="+undelivered+", unfinished="+unfinished+", returning total="+total);
        return total;
    }

}
