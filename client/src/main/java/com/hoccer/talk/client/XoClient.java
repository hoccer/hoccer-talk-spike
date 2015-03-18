package com.hoccer.talk.client;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.client.JsonRpcClientException;
import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.websocket.JsonRpcWsConnection;
import better.jsonrpc.websocket.java.JavaWebSocket;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.exceptions.NoClientIdInPresenceException;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.crypto.RSACryptor;
import com.hoccer.talk.model.*;
import com.hoccer.talk.rpc.ITalkRpcClient;
import com.hoccer.talk.rpc.ITalkRpcServer;
import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingClient;
import com.hoccer.talk.util.Credentials;
import de.undercouch.bson4jackson.BsonFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class XoClient implements JsonRpcConnection.Listener, IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(XoClient.class);

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_REGISTERING = 2;
    public static final int STATE_LOGIN = 3;
    public static final int STATE_SYNCING = 4;
    public static final int STATE_READY = 5;

    // Digest instance used for SRP auth
    private static final Digest SRP_DIGEST = new SHA256Digest();

    // RNG used for SRP auth
    private static final SecureRandom SRP_RANDOM = new SecureRandom();

    // Constant SRP parameters
    private static final SRP6Parameters SRP_PARAMETERS = SRP6Parameters.CONSTANTS_1024;

    // Names of our states for debugging
    private static final String[] STATE_NAMES = {"disconnected", "connecting", "registering", "login", "syncing", "ready"};

    private int mUploadLimit = -1;
    private int mDownloadLimit = -1;

    private final Object mGroupCreationLock = new Object();

    // Return the name of the given state
    public static String stateToString(int state) {
        if (state >= 0 && state < STATE_NAMES.length) {
            return STATE_NAMES[state];
        } else {
            return "INVALID(" + state + ")";
        }
    }

    IXoClientHost mClientHost;

    IXoClientConfiguration mClientConfiguration;

    XoClientDatabase mDatabase;

    TalkClientContact mSelfContact;

    String mAvatarDirectory;
    String mRelativeAvatarDirectory;
    String mAttachmentDirectory;
    String mRelativeAttachmentDirectory;
    String mEncryptedUploadDirectory;
    String mEncryptedDownloadDirectory;
    String mExternalStorageDirectory;

    XoTransferAgent mTransferAgent;

    private JavaWebSocket mWebSocket;
    protected JsonRpcWsConnection mConnection;
    TalkRpcClientImpl mHandler;
    ITalkRpcServer mServerRpc;

    // Executor doing all the heavy network and database work
    ScheduledExecutorService mExecutor;

    // Futures keeping track of singleton background operations
    ScheduledFuture<?> mLoginFuture;
    ScheduledFuture<?> mRegistrationFuture;
    ScheduledFuture<?> mConnectFuture;
    ScheduledFuture<?> mDisconnectFuture;
    ScheduledFuture<?> mDisconnectTimeoutFuture;
    ScheduledFuture<?> mKeepAliveFuture;

    List<IXoContactListener> mContactListeners = new CopyOnWriteArrayList<IXoContactListener>();
    List<IXoMessageListener> mMessageListeners = new ArrayList<IXoMessageListener>();
    List<IXoStateListener> mStateListeners = new ArrayList<IXoStateListener>();
    List<IXoAlertListener> mAlertListeners = new ArrayList<IXoAlertListener>();

    // The current state of this client
    int mState;

    // Count connections attempts for back-off
    int mNumConnectionAttempts;

    // temporary group for geolocation grouping
    String mEnvironmentGroupId;
    AtomicBoolean mEnvironmentUpdateCallPending = new AtomicBoolean(false);

    ObjectMapper mJsonMapper;

    int mRSAKeysize = 1024;

    private long serverTimeDiff;

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoClient(IXoClientHost host, IXoClientConfiguration configuration) {
        mClientHost = host;
        mClientConfiguration = configuration;

        // fetch executor and db immediately
        mExecutor = host.getBackgroundExecutor();

        // create and initialize the database
        mDatabase = new XoClientDatabase(mClientHost.getDatabaseBackend());
        try {
            mDatabase.initialize();
        } catch (SQLException e) {
            LOG.error("sql error in database initialization", e);
        }

        // create JSON object mapper
        JsonFactory jsonFactory = new JsonFactory();
        mJsonMapper = createObjectMapper(jsonFactory);

        // create RPC object mapper (BSON or JSON)
        JsonFactory rpcFactory;
        if (mClientConfiguration.getUseBsonProtocol()) {
            rpcFactory = new BsonFactory();
        } else {
            rpcFactory = jsonFactory;
        }
        ObjectMapper rpcMapper = createObjectMapper(rpcFactory);

        createJsonRpcConnection(rpcMapper);

        // create client-side RPC handler object
        mHandler = new TalkRpcClientImpl();

        // configure JSON-RPC client
        JsonRpcClient clt = new JsonRpcClient();
        mConnection.bindClient(clt);

        // configure JSON-RPC server object
        JsonRpcServer srv = new JsonRpcServer(ITalkRpcClient.class);
        mConnection.bindServer(srv, getHandler());

        // listen for connection state changes
        mConnection.addListener(this);

        // create RPC proxy
        mServerRpc = mConnection.makeProxy(ITalkRpcServer.class);

        // create transfer agent
        mTransferAgent = new XoTransferAgent(this);
        mTransferAgent.registerListener(this);

        // ensure we have a self contact
        ensureSelfContact();
    }

    private static ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
        ObjectMapper result = new ObjectMapper(jsonFactory);
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return result;
    }

    private void createJsonRpcConnection(ObjectMapper rpcMapper) {
        mWebSocket = new JavaWebSocket(mClientHost.getKeyStore(), XoClientSslConfiguration.TLS_CIPHERS);
        mConnection = new JsonRpcWsConnection(mWebSocket, rpcMapper);
        mConnection.setSendKeepAlives(mClientConfiguration.getKeepAliveEnabled());

        if (mClientConfiguration.getUseBsonProtocol()) {
            mConnection.setSendBinaryMessages(true);
        }
    }

    private void ensureSelfContact() {
        try {
            mSelfContact = mDatabase.findSelfContact(true);
            if (mSelfContact.initializeSelf()) {
                mDatabase.saveCredentials(mSelfContact.getSelf());
                mDatabase.saveContact(mSelfContact);
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    /**
     * Returns true if the client has been activated
     * This is only true after an explicit call to connect().
     *
     * @return
     */
    public boolean isActivated() {
        return mState > STATE_DISCONNECTED;
    }

    /**
     * Returns true if the client is ready
     * This means that the client is logged in and synced.
     */
    public boolean isReady() {
        return mState >= STATE_READY;
    }

    /**
     * Returns true if the client is logged in
     */
    public boolean isLoggedIn() {
        return mState >= STATE_SYNCING;
    }

    /**
     * Returns true if the client is awake
     * This means that the client is trying to connect or connected.
     */
    public boolean isAwake() {
        return mState > STATE_DISCONNECTED;
    }

    public boolean isRegistered() {
        return mSelfContact.isSelfRegistered();
    }

    public TalkClientContact getSelfContact() {
        return mSelfContact;
    }

    public String getAvatarDirectory() {
        return mAvatarDirectory;
    }

    public void setAvatarDirectory(String avatarDirectory) {
        this.mAvatarDirectory = avatarDirectory;
    }

    public String getRelativeAvatarDirectory() {
        return mRelativeAvatarDirectory;
    }

    public void setRelativeAvatarDirectory(String avatarDirectory) {
        this.mRelativeAvatarDirectory = avatarDirectory;
    }

    public String getAttachmentDirectory() {
        return mAttachmentDirectory;
    }

    public void setAttachmentDirectory(String attachmentDirectory) {
        this.mAttachmentDirectory = attachmentDirectory;
    }

    public String getRelativeAttachmentDirectory() {
        return mRelativeAttachmentDirectory;
    }

    public void setRelativeAttachmentDirectory(String attachmentDirectory) {
        this.mRelativeAttachmentDirectory = attachmentDirectory;
    }

    public String getEncryptedUploadDirectory() {
        return mEncryptedUploadDirectory;
    }

    public void setEncryptedUploadDirectory(String encryptedUploadDirectory) {
        this.mEncryptedUploadDirectory = encryptedUploadDirectory;
    }

    public String getEncryptedDownloadDirectory() {
        return mEncryptedDownloadDirectory;
    }

    public void setEncryptedDownloadDirectory(String encryptedDownloadDirectory) {
        this.mEncryptedDownloadDirectory = encryptedDownloadDirectory;
    }

    public String getExternalStorageDirectory() {
        return mExternalStorageDirectory;
    }

    public void setExternalStorageDirectory(String externalStorageDirectory) {
        this.mExternalStorageDirectory = externalStorageDirectory;
    }

    public IXoClientHost getHost() {
        return mClientHost;
    }

    public IXoClientConfiguration getConfiguration() { return mClientConfiguration; }

    public XoClientDatabase getDatabase() {
        return mDatabase;
    }

    public XoTransferAgent getTransferAgent() {
        return mTransferAgent;
    }

    /**
     * @return the RPC proxy towards the server
     */
    public ITalkRpcServer getServerRpc() {
        return mServerRpc;
    }

    /**
     * @return the RPC handler for notifications
     */
    public ITalkRpcClient getHandler() {
        return mHandler;
    }

    /**
     * @return the current state of this client (numerical)
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * @return the current state of this client (textual)
     */
    public synchronized String getStateString() {
        return stateToString(mState);
    }

    public synchronized void registerStateListener(IXoStateListener listener) {
        if (!mStateListeners.contains(listener)) {
            mStateListeners.add(listener);
        }
    }

    public synchronized void unregisterStateListener(IXoStateListener listener) {
        mStateListeners.remove(listener);
    }

    public synchronized void registerContactListener(IXoContactListener listener) {
        if (!mContactListeners.contains(listener)) {
            mContactListeners.add(listener);
        }
    }

    public synchronized void unregisterContactListener(IXoContactListener listener) {
        mContactListeners.remove(listener);
    }

    public synchronized void registerMessageListener(IXoMessageListener listener) {
        if (!mMessageListeners.contains(listener)) {
            mMessageListeners.add(listener);
        }
    }

    public synchronized void unregisterMessageListener(IXoMessageListener listener) {
        mMessageListeners.remove(listener);
    }

    public synchronized void registerTransferListener(IXoTransferListenerOld listener) {
        mTransferAgent.registerListener(listener);
    }

    public synchronized void unregisterTransferListener(IXoTransferListenerOld listener) {
        mTransferAgent.unregisterListener(listener);
    }

    public synchronized void registerAlertListener(IXoAlertListener listener) {
        if (!mAlertListeners.contains(listener)) {
            mAlertListeners.add(listener);
        }
    }

    public synchronized void unregisterAlertListener(IXoAlertListener listener) {
        mAlertListeners.remove(listener);
    }

    /**
     * Exports the client credentials.
     */
    public Credentials exportCredentials() {
        // return null if this client has never registered
        if (mSelfContact.getClientId() == null) {
            return null;
        }

        String clientId = mSelfContact.getClientId();
        String password = mSelfContact.getSelf().getSrpSecret();
        String salt = mSelfContact.getSelf().getSrpSalt();
        String clientName = mSelfContact.getName();

        return new Credentials(clientId, password, salt, clientName, new Date().getTime());
    }

    /**
     * Imports the client credentials.
     *
     * @note After import the client deletes all contacts and relationsships and reconnects for full sync.
     */
    public void importCredentials(final Credentials newCredentials) throws SQLException, NoClientIdInPresenceException {
        final TalkClientSelf self = mSelfContact.getSelf();
        self.provideCredentials(newCredentials.getSalt(), newCredentials.getPassword());
        self.confirmRegistration();

        // update client id
        mSelfContact.updateSelfRegistered(newCredentials.getClientId());
        mSelfContact.getClientPresence().setClientId(newCredentials.getClientId());

        // update client name
        String newClientName = newCredentials.getClientName();
        if (newClientName != null) {
            mSelfContact.getClientPresence().setClientName(newClientName);
        }

        // save credentials and contact
        mDatabase.saveCredentials(self);
        mDatabase.savePresence(mSelfContact.getClientPresence());
        mDatabase.saveContact(mSelfContact);

        // remove contacts + groups from DB
        mDatabase.eraseAllRelationships();
        mDatabase.eraseAllClientContacts();
        mDatabase.eraseAllGroupMemberships();
        mDatabase.eraseAllGroupContacts();

        switchState(STATE_DISCONNECTED, "disconnect because of imported credentials");
    }

    /**
     * Requests a new srp secret from the server which should be used from now on.
     */
    public boolean changeSrpSecret() {
        final Digest digest = SRP_DIGEST;
        final byte[] salt = new byte[digest.getDigestSize()];
        final byte[] secret = new byte[digest.getDigestSize()];
        final SRP6VerifierGenerator vg = new SRP6VerifierGenerator();

        vg.init(SRP_PARAMETERS.N, SRP_PARAMETERS.g, digest);

        SRP_RANDOM.nextBytes(salt);
        SRP_RANDOM.nextBytes(secret);

        final String saltString = new String(Hex.encodeHex(salt));
        final String secretString = new String(Hex.encodeHex(secret));

        try {
            final String clientId = mSelfContact.getClientId();

            LOG.debug("Changing srp secret");

            final BigInteger verifier = vg.generateVerifier(salt, clientId.getBytes(), secret);

            mServerRpc.srpChangeVerifier(verifier.toString(16), new String(Hex.encodeHex(salt)));

            mSelfContact.getSelf().provideCredentials(saltString, secretString);

            try {
                mDatabase.saveCredentials(mSelfContact.getSelf());
            } catch (SQLException e) {
                LOG.error("SQL error on saving new srp secret", e);
            }

            LOG.debug("Srp verifier changed");
            return true;
        } catch (JsonRpcClientException e) {
            LOG.error("Error while performing srp secret change request: ", e);
            return false;
        }
    }

    /**
     * Connect the client, allowing it do operate
     */
    public void connect() {
        LOG.debug("client: connect()");
        if (mState == STATE_DISCONNECTED) {
            switchState(STATE_CONNECTING, "connecting client");
        }
    }

    /**
     * Deactivate the client, shutting it down completely
     */
    public void disconnect() {
        LOG.debug("client: disconnect()");
        switchState(STATE_DISCONNECTED, "disconnecting client");
    }

    /**
     * delete old key pair and create a new one
     */
    public void regenerateKeyPair() throws SQLException {
        LOG.debug("regenerateKeyPair()");
        mSelfContact.setPublicKey(null);
        mSelfContact.setPrivateKey(null);
        ensureSelfKey(mSelfContact);
    }

    public Date estimatedServerTime() {
        return new Date(new Date().getTime() + this.serverTimeDiff);
    }

    public void scheduleHello() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                hello();
            }
        });
    }

    public void hello() {
        try {
            TalkClientInfo clientInfo = new TalkClientInfo();
            clientInfo.setClientName(mClientHost.getClientName());
            clientInfo.setClientTime(mClientHost.getClientTime());
            clientInfo.setClientLanguage(mClientHost.getClientLanguage());
            clientInfo.setClientVersion(mClientHost.getClientVersionName());
            clientInfo.setClientBuildNumber(mClientHost.getClientVersionCode());
            clientInfo.setDeviceModel(mClientHost.getDeviceModel());
            clientInfo.setSystemName(mClientHost.getSystemName());
            clientInfo.setSystemLanguage(mClientHost.getSystemLanguage());
            clientInfo.setSystemVersion(mClientHost.getSystemVersion());
            clientInfo.setClientBuildVariant(mClientHost.getClientBuildVariant());
            if (mClientConfiguration.isSupportModeEnabled()) {
                clientInfo.setSupportTag(mClientConfiguration.getSupportTag());
            }

            LOG.debug("Hello: Saying hello to the server.");
            TalkServerInfo talkServerInfo = mServerRpc.hello(clientInfo);
            if (talkServerInfo != null) {
                // serverTimeDiff is positive if server time is ahead of client time
                this.serverTimeDiff = talkServerInfo.getServerTime().getTime() - new Date().getTime();
                LOG.info("Hello: client time differs from server time by " + this.serverTimeDiff + " ms");
                LOG.debug("Hello: Current server time: " + talkServerInfo.getServerTime());
                LOG.debug("Hello: Server switched to supportMode: " + talkServerInfo.isSupportMode());
                LOG.debug("Hello: Server version is '" + talkServerInfo.getVersion() + "'");
                LOG.debug("Hello: supported protocol versions: '" + talkServerInfo.getProtocolVersions() + "'");
                LOG.debug("Hello: git commit is '" + talkServerInfo.getCommitId() + "'");
            }
        } catch (JsonRpcClientException e) {
            LOG.error("Error while sending Hello: ", e);
        }
    }

    /**
     * Register the given GCM push information with the server
     *
     * @param packageName
     * @param registrationId
     */
    public void registerGcm(final String packageName, final String registrationId) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.registerGcm(packageName, registrationId);
            }
        });
    }

    /**
     * Unregister any GCM push information on the server
     */
    public void unregisterGcm() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.unregisterGcm();
            }
        });
    }

    public void setClientString(String newName, String newStatus) {
        try {
            ensureSelfContact();
            TalkClientSelf self = mSelfContact.getSelf();
            if (newName != null) {
                self.setRegistrationName(newName);
                self.confirmRegistration();
            }
            mDatabase.saveCredentials(self);

            TalkPresence presence = mSelfContact.getClientPresence();
            if (presence != null) {
                if (newName != null) {
                    presence.setClientName(newName);
                }
                if (newStatus != null) {
                    presence.setClientStatus(newStatus);
                }
                mDatabase.savePresence(presence);

                for (IXoContactListener listener : mContactListeners) {
                    listener.onClientPresenceChanged(mSelfContact);
                }

                if (isLoggedIn()) {
                    sendPresence();
                }
            }
        } catch (Exception e) {
            LOG.error("setClientString", e);
        }
    }

    public void setClientConnectionStatus(String newStatus) {
        try {
            TalkPresence presence = mSelfContact.getClientPresence();
            if (presence != null && presence.getClientId() != null) {
                if (newStatus != null && !newStatus.equals(presence.getClientStatus())) {
                    presence.setConnectionStatus(newStatus);
                    mSelfContact.updatePresence(presence);
                    mDatabase.savePresence(presence);

                    if (TalkPresence.CONN_STATUS_ONLINE.equals(newStatus)) {
                        LOG.debug("entering foreground");
                        cancelDisconnectTimeout();
                        connect();
                    } else if (TalkPresence.CONN_STATUS_BACKGROUND.equals(newStatus)) {
                        int timeout = mClientConfiguration.getBackgroundDisconnectTimeoutSeconds();
                        scheduleDisconnectTimeout(timeout);
                        LOG.debug("entering background");
                    }

                    for (IXoContactListener listener : mContactListeners) {
                        listener.onClientPresenceChanged(mSelfContact);
                    }

                    if (isLoggedIn()) {
                        sendPresence();
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
        } catch (Exception e) { // TODO: specify exception in XoClientDatabase.savePresence!
            LOG.error("error in setClientConnectionStatus", e);
        }
    }

    /*
     * If upload is null no avatar is set.
     */
    public void setClientAvatar(final TalkClientUpload upload) {
        if (upload != null) {
            LOG.debug("new client avatar as upload " + upload);
            mTransferAgent.startOrRestartUpload(upload);
        }
        sendPresenceUpdateWithNewAvatar(upload);
    }

    private void sendPresenceUpdateWithNewAvatar(final TalkClientUpload upload) {
        try {
            TalkPresence presence = mSelfContact.getClientPresence();
            if (presence != null) {
                if (upload != null) {
                    String downloadUrl = upload.getDownloadUrl();
                    presence.setAvatarUrl(downloadUrl);
                } else {
                    presence.setAvatarUrl(null);
                }

                mSelfContact.setAvatarUpload(upload);
                mDatabase.savePresence(presence);
                mDatabase.saveContact(mSelfContact);
                for (IXoContactListener listener : mContactListeners) {
                    listener.onClientPresenceChanged(mSelfContact);
                }
                sendPresence();
            }
        } catch (Exception e) {
            LOG.error("setClientAvatar", e);
        }
    }

    public void setGroupName(final TalkClientContact group, final String groupName) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                LOG.debug("changing group name");
                TalkGroupPresence groupPresence = group.getGroupPresence();
                if (groupPresence == null) {
                    LOG.error("group has no presence");
                    return;
                }
                groupPresence.setGroupName(groupName);

                try {
                    mDatabase.saveGroupPresence(groupPresence);
                    mDatabase.saveContact(group);
                    LOG.debug("sending new group presence");
                    mServerRpc.updateGroup(groupPresence);
                } catch (SQLException e) {
                    LOG.error("sql error", e);
                } catch (JsonRpcClientException e) {
                    LOG.error("Error while sending new group presence: ", e);
                }

                for (IXoContactListener listener : mContactListeners) {
                    listener.onGroupPresenceChanged(group);
                }
            }
        });
    }

    /*
    * If upload is null no avatar is set.
    */
    public void setGroupAvatar(final TalkClientContact group, final TalkClientUpload upload) {
        if (upload != null) {
            LOG.debug("new group avatar as upload " + upload);
            mTransferAgent.startOrRestartUpload(upload);
        }
        sendGroupPresenceUpdateWithNewAvatar(group, upload);
    }

    private void sendGroupPresenceUpdateWithNewAvatar(final TalkClientContact group, final TalkClientUpload upload) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    TalkGroupPresence groupPresence = group.getGroupPresence();
                    if (groupPresence != null) {
                        if (upload != null) {
                            String downloadUrl = upload.getDownloadUrl();
                            groupPresence.setGroupAvatarUrl(downloadUrl);
                        } else {
                            groupPresence.setGroupAvatarUrl(null);
                        }

                        group.setAvatarUpload(upload);
                        mDatabase.saveGroupPresence(groupPresence);
                        mDatabase.saveContact(group);
                        mServerRpc.updateGroup(groupPresence);

                        for (IXoContactListener listener : mContactListeners) {
                            listener.onGroupPresenceChanged(group);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("error creating group avatar", e);
                }
            }
        });
    }

    public String generatePairingToken() {
        final int tokenLifetime = 7 * 24 * 3600;  // valid for one week
        final int maxTokenUseCount = 50;   // good to invite 50 people with same token

        try {
            String token = mServerRpc.generatePairingToken(maxTokenUseCount, tokenLifetime);
            LOG.debug("got pairing token " + token);
            return token;
        } catch (JsonRpcClientException e) {
            LOG.error("Error while generating pairing token: ", e);
        }
        return null;
    }

    public void performTokenPairing(final String token) {
        performTokenPairing(token, null);
    }

    public void performTokenPairing(final String token, final IXoPairingListener listener) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = mServerRpc.pairByToken(token);

                    if (listener != null) {
                        if (success) {
                            listener.onTokenPairingSucceeded(token);
                        } else {
                            listener.onTokenPairingFailed(token);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error while pairing using token: " + token, e);
                }
            }
        });
    }

    public void deleteContact(final TalkClientContact contact) {
        if (contact.isClient() || contact.isGroup()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (contact.isClient() && contact.isClientRelated()) {
                            mServerRpc.depairClient(contact.getClientId());
                        }

                        if (contact.isGroup()) {
                            if (contact.isGroupExisting() && contact.isGroupAdmin()) {
                                mServerRpc.deleteGroup(contact.getGroupId());
                            } else if (contact.isGroupJoined()) {
                                mServerRpc.leaveGroup(contact.getGroupId());
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Exception while deleting contact ", e);
                    }
                }
            });
        }
    }

    public void blockContact(final TalkClientContact contact) {
        if (contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.blockClient(contact.getClientId());
                }
            });
        }
    }

    public void unblockContact(final TalkClientContact contact) {
        if (contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.unblockClient(contact.getClientId());
                }
            });
        }
    }

    public void inviteFriend(final TalkClientContact contact) {
        if (contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.inviteFriend(contact.getClientId());
                }
            });
        }
    }

    public void disinviteFriend(final TalkClientContact contact) {
        if (contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.disinviteFriend(contact.getClientId());
                }
            });
        }
    }

    public void acceptFriend(final TalkClientContact contact) {
        if (contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.acceptFriend(contact.getClientId());
                }
            });
        }
    }

    public void declineFriend(final TalkClientContact contact) {
        if (contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.refuseFriend(contact.getClientId());
                }
            });
        }
    }

    public String createGroup(final String name) {
        return createGroupWithContacts(name, Collections.<String>emptyList());
    }

    public String createGroupWithContacts(final String name, final List<String> clientIds) {
        final String tag = UUID.randomUUID().toString();

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug("creating group on server");
                    TalkGroupPresence groupPresence = mServerRpc.createGroupWithMembers(
                            TalkGroupPresence.GROUP_TYPE_USER,
                            tag,
                            name,
                            clientIds.toArray(new String[clientIds.size()]),
                            createMemberRoles(clientIds.size()));

                    if (groupPresence != null) {
                        updateGroupPresence(groupPresence);
                    }
                } catch (JsonRpcClientException e) {
                    LOG.error("JSON RPC error while creating group: ", e);
                } catch (Exception e) {
                    LOG.error("Error while creating group: ", e);
                }
            }
        });

        return tag;
    }

    private static String[] createMemberRoles(int count) {
        String[] roles = new String[count];

        for (int i = 0; i < count; i++) {
            roles[i] = TalkGroupMembership.ROLE_MEMBER;
        }

        return roles;
    }

    public void inviteClientToGroup(final String groupId, final String clientId) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerRpc.inviteGroupMember(groupId, clientId);
                } catch (JsonRpcClientException e) {
                    LOG.error("Error while sending group invitation: ", e);
                }
            }
        });
    }

    public void kickClientFromGroup(final String groupId, final String clientId) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.removeGroupMember(groupId, clientId);
            }
        });
    }

    public void joinGroup(final String groupId) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.joinGroup(groupId);
            }
        });
    }

    public void leaveGroup(final String groupId) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.leaveGroup(groupId);
            }
        });
    }

    public void sendMessage(String talkMessageTag) {
        try {
            TalkClientMessage message = mDatabase.findMessageByMessageTag(talkMessageTag, false);
            for (IXoMessageListener listener : mMessageListeners) {
                listener.onMessageCreated(message);
            }
            if (TalkDelivery.STATE_NEW.equals(message.getOutgoingDelivery().getState())) {
                requestDelivery(message);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while retrieving message by message tag ", e);
        }
    }

    public void sendMessages(List<String> messageTags) {
        int delayMultiplier = 1;
        for (final String messageTag : messageTags) {
            mExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    sendMessage(messageTag);
                }
            }, 1000 * delayMultiplier, TimeUnit.MILLISECONDS);
            delayMultiplier++;
        }
    }

    public void deleteMessage(int messageId) {
        try {
            TalkClientMessage message = mDatabase.findMessageById(messageId);
            deleteMessage(message);
        } catch (SQLException e) {
            LOG.error("SQL Error while deleting message with id: " + messageId, e);
        }
    }

    public void deleteMessage(TalkClientMessage message) {
        try {
            mDatabase.deleteMessageById(message.getClientMessageId());
            for (IXoMessageListener listener : mMessageListeners) {
                listener.onMessageDeleted(message);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while deleting message with id: " + message.getClientMessageId(), e);
        }
    }

    protected void requestDelivery(final TalkClientMessage message) {
        if (mState < STATE_READY) {
            LOG.info("requestSendAllPendingMessages() - cannot perform delivery because the client is not in ready.");
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
                messages.add(message);
                performDeliveries(messages);
            }
        });
    }

    private void requestSendAllPendingMessages() {
        if (mState < STATE_READY) {
            LOG.info("requestSendAllPendingMessages() - cannot perform delivery because the client is not ready.");
            return;
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    performDeliveries(mDatabase.findMessagesForDelivery());
                } catch (SQLException e) {
                    LOG.error("error while retrieving pending messages", e);
                }
            }
        });
    }

    private void switchState(int newState, String message) {
        if (mState != newState) {
            LOG.info("[switchState: connection #" + mConnection.getConnectionId() + "] state " + STATE_NAMES[mState] + " -> " + STATE_NAMES[newState] + " (" + message + ")");

            int previousState = mState;
            mState = newState;

            if (mState == STATE_DISCONNECTED) {
                mNumConnectionAttempts = 0;
                scheduleDisconnect();
            } else {
                cancelDisconnect();
            }

            if (mState == STATE_CONNECTING) {
                scheduleConnect();
            } else {
                cancelConnect();
            }

            if (mState == STATE_REGISTERING) {
                scheduleRegistration();
            } else {
                cancelRegistration();
            }

            if (mState == STATE_LOGIN) {
                scheduleLogin();
            } else {
                cancelLogin();
            }

            if (mState == STATE_SYNCING) {
                scheduleSync();
            }

            if (mState == STATE_READY) {
                mNumConnectionAttempts = 0;
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mServerRpc.ready();
                        LOG.info("[connection #" + mConnection.getConnectionId() + "] connected and ready");
                        LOG.info("Delivering potentially unsent messages.");
                        requestSendAllPendingMessages();
                    }
                });
            }

            if (mState >= STATE_SYNCING) {
                scheduleKeepAlive();
            } else {
                cancelKeepAlive();
            }

            // call listeners
            if (previousState != newState) {
                for (IXoStateListener listener : mStateListeners) {
                    listener.onClientStateChange(this, newState);
                }
            }
        } else {
            LOG.debug("state remains " + getStateString() + " (" + message + ")");
        }
    }

    /**
     * Called when the connection is opened
     *
     * @param connection
     */
    @Override
    public void onOpen(JsonRpcConnection connection) {
        LOG.debug("onOpen()");
        if (isRegistered()) {
            switchState(STATE_LOGIN, "connected");
        } else {
            switchState(STATE_REGISTERING, "connected and unregistered");
        }
    }

    /**
     * Called when the connection is closed
     *
     * @param connection
     */
    @Override
    public void onClose(JsonRpcConnection connection) {
        LOG.debug("onClose()");
        if (mState != STATE_DISCONNECTED) {
            switchState(STATE_CONNECTING, "connection had closed");
        }
    }

    private void scheduleConnect() {
        LOG.debug("scheduleConnect()");
        cancelConnect();

        int backoffDelay;
        if (mNumConnectionAttempts > 0) {
            // compute the backoff factor
            int variableFactor = 1 << mNumConnectionAttempts;

            // compute variable backoff component
            double variableTime = Math.random() * Math.min(mClientConfiguration.getReconnectBackoffVariableMaximum(), variableFactor * mClientConfiguration.getReconnectBackoffVariableFactor());

            // compute total backoff
            double totalTime = mClientConfiguration.getReconnectBackoffFixedDelay() + variableTime;
            LOG.debug("connection attempt backed off by " + totalTime + " seconds");

            backoffDelay = (int) Math.round(1000.0 * totalTime);
        } else {
            backoffDelay = 0;
        }

        mNumConnectionAttempts++;
        mConnectFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                doConnect();
                mConnectFuture = null;
            }
        }, backoffDelay, TimeUnit.MILLISECONDS);
    }

    private void cancelConnect() {
        if (mConnectFuture != null) {
            mConnectFuture.cancel(false);
            mConnectFuture = null;
        }
    }

    private void doConnect() {
        LOG.debug("do connect on connection #" + mConnection.getConnectionId());
        try {
            URI uri = new URI(mClientConfiguration.getServerUri());
            String protocol = mClientConfiguration.getUseBsonProtocol()
                    ? mClientConfiguration.getBsonProtocolString()
                    : mClientConfiguration.getJsonProtocolString();

            mWebSocket.open(uri, protocol, mClientConfiguration.getConnectTimeout() * 1000);
        } catch (Exception e) {
            LOG.warn("[connection #" + mConnection.getConnectionId() + "] exception while connecting: ", e);
            scheduleConnect();
        }
    }

    private void scheduleDisconnectTimeout(int timeoutInSeconds) {
        LOG.debug("scheduleDisconnectTimeout()");
        cancelDisconnectTimeout();
        mDisconnectTimeoutFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                switchState(STATE_DISCONNECTED, "disconnect timout");
                mDisconnectTimeoutFuture = null;
            }
        }, timeoutInSeconds, TimeUnit.SECONDS);
    }

    private void cancelDisconnectTimeout() {
        if (mDisconnectTimeoutFuture != null) {
            mDisconnectTimeoutFuture.cancel(false);
            mDisconnectTimeoutFuture = null;
        }
    }

    private void scheduleDisconnect() {
        LOG.debug("scheduleDisconnect()");
        cancelDisconnect();
        mDisconnectFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    doDisconnect();
                } catch (Throwable t) {
                    LOG.error("error disconnecting", t);
                }
                mDisconnectFuture = null;
            }
        }, 0, TimeUnit.SECONDS);
    }

    private void cancelDisconnect() {
        if (mDisconnectFuture != null) {
            mDisconnectFuture.cancel(false);
            mDisconnectFuture = null;
        }
    }

    private void doDisconnect() {
        LOG.debug("performing disconnect");
        mConnection.disconnect();
    }

    private void scheduleRegistration() {
        LOG.debug("scheduleRegistration()");
        cancelRegistration();
        mRegistrationFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    doRegistration(mSelfContact);
                    mRegistrationFuture = null;
                    switchState(STATE_LOGIN, "registered");
                } catch (Exception e) {
                    LOG.error("registration error", e);
                }
            }
        }, 0, TimeUnit.SECONDS);
    }

    private void cancelRegistration() {
        if (mRegistrationFuture != null) {
            mRegistrationFuture.cancel(false);
            mRegistrationFuture = null;
        }
    }

    private void doRegistration(TalkClientContact selfContact) {
        LOG.debug("registration: attempting registration");

        Digest digest = SRP_DIGEST;
        byte[] salt = new byte[digest.getDigestSize()];
        byte[] secret = new byte[digest.getDigestSize()];
        SRP6VerifierGenerator vg = new SRP6VerifierGenerator();

        vg.init(SRP_PARAMETERS.N, SRP_PARAMETERS.g, digest);

        SRP_RANDOM.nextBytes(salt);
        SRP_RANDOM.nextBytes(secret);

        String saltString = new String(Hex.encodeHex(salt));
        String secretString = new String(Hex.encodeHex(secret));

        try {
            String clientId = mServerRpc.generateId();

            LOG.debug("registration: started with id " + clientId);

            BigInteger verifier = vg.generateVerifier(salt, clientId.getBytes(), secret);
            mServerRpc.srpRegister(verifier.toString(16), new String(Hex.encodeHex(salt)));

            LOG.debug("registration: finished");

            TalkClientSelf self = mSelfContact.getSelf();
            self.provideCredentials(saltString, secretString);
            selfContact.updateSelfRegistered(clientId);

            try {
                TalkPresence presence = ensureSelfPresence(mSelfContact);
                presence.setClientId(clientId);
                presence.setClientName(self.getRegistrationName());
                mDatabase.saveCredentials(self);
                mDatabase.savePresence(presence);
                mDatabase.saveContact(selfContact);
            } catch (SQLException e) {
                LOG.error("SQL error on doRegistration", e);
            } catch (Exception e) { // TODO: specify exception in XoClientDatabase.savePresence!
                LOG.error("error on doRegistration", e);
            }

        } catch (JsonRpcClientException e) {
            LOG.error("Error while registering: ", e);
        }

    }

    private void scheduleLogin() {
        LOG.debug("scheduleLogin()");
        cancelLogin();
        mLoginFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                doLogin(mSelfContact);
                mLoginFuture = null;
                switchState(STATE_SYNCING, "login successful");
            }
        }, 0, TimeUnit.SECONDS);
    }

    private void cancelLogin() {
        if (mLoginFuture != null) {
            mLoginFuture.cancel(false);
            mLoginFuture = null;
        }
    }

    private void doLogin(TalkClientContact selfContact) {
        String clientId = selfContact.getClientId();
        LOG.debug("login: attempting login as " + clientId);
        Digest digest = SRP_DIGEST;

        TalkClientSelf self = selfContact.getSelf();

        SRP6VerifyingClient vc = new SRP6VerifyingClient();
        vc.init(SRP_PARAMETERS.N, SRP_PARAMETERS.g, digest, SRP_RANDOM);

        LOG.debug("login: performing phase 1");

        try {
            byte[] loginId = clientId.getBytes();
            byte[] loginSalt = Hex.decodeHex(self.getSrpSalt().toCharArray());
            byte[] loginSecret = Hex.decodeHex(self.getSrpSecret().toCharArray());
            BigInteger A = vc.generateClientCredentials(loginSalt, loginId, loginSecret);

            String Bs = mServerRpc.srpPhase1(clientId, A.toString(16));
            vc.calculateSecret(new BigInteger(Bs, 16));

            LOG.debug("login: performing phase 2");

            String Vc = new String(Hex.encodeHex(vc.calculateVerifier()));
            String Vs = mServerRpc.srpPhase2(Vc);
            vc.verifyServer(Hex.decodeHex(Vs.toCharArray()));
        } catch (JsonRpcClientException e) {
            LOG.error("Error while performing registration: ", e);
        } catch (Exception e) {
            LOG.error("decoder exception in login", e);
            throw new RuntimeException("exception during login", e);
        }
        LOG.debug("login: successful");
    }

    private void cancelKeepAlive() {
        if (mKeepAliveFuture != null) {
            mKeepAliveFuture.cancel(false);
            mKeepAliveFuture = null;
        }
    }

    private void scheduleKeepAlive() {
        cancelKeepAlive();
        if (mClientConfiguration.getKeepAliveEnabled()) {
            mKeepAliveFuture = mExecutor.scheduleAtFixedRate(new Runnable() {
                                                                 @Override
                                                                 public void run() {
                                                                     LOG.debug("performing keep-alive");
                                                                     try {
                                                                         mConnection.sendKeepAlive();
                                                                     } catch (IOException e) {
                                                                         LOG.error("error sending keepalive", e);
                                                                     }
                                                                 }
                                                             },
                    mClientConfiguration.getKeepAliveInterval(),
                    mClientConfiguration.getKeepAliveInterval(),
                    TimeUnit.SECONDS);
        }
    }

    private void scheduleSync() {
        LOG.debug("scheduleSync()");
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Date never = new Date(0);
                try {
                    LOG.debug("sync: HELLO");
                    hello();

                    LOG.debug("sync: updating self presence");
                    ScheduledFuture sendPresenceFuture = sendPresence();

                    LOG.debug("sync: syncing presences");
                    TalkPresence[] presences = mServerRpc.getPresences(never);
                    for (TalkPresence presence : presences) {
                        updateClientPresence(presence, null);
                    }
                    LOG.debug("sync: syncing relationships");
                    TalkRelationship[] relationships = mServerRpc.getRelationships(never);
                    for (TalkRelationship relationship : relationships) {
                        updateClientRelationship(relationship);
                    }
                    LOG.debug("sync: syncing groups");
                    TalkGroupPresence[] groupPresences = mServerRpc.getGroups(never);
                    for (TalkGroupPresence groupPresence : groupPresences) {
                        if (groupPresence.getState().equals(TalkGroupPresence.STATE_EXISTS)) {
                            updateGroupPresence(groupPresence);
                        }
                    }

                    LOG.debug("sync: syncing group memberships");
                    List<TalkClientContact> contacts = mDatabase.findAllGroupContacts();
                    List<TalkClientContact> groupContacts = new ArrayList<TalkClientContact>();
                    List<String> groupIds = new ArrayList<String>();
                    for (TalkClientContact contact : contacts) {
                        if (contact.isGroup() && contact.isGroupExisting()) {
                            groupContacts.add(contact);
                            groupIds.add(contact.getGroupId());
                        }
                    }
                    if (!groupIds.isEmpty()) {
                        Boolean[] groupMembershipFlags = mServerRpc.isMemberInGroups(groupIds.toArray(new String[groupIds.size()]));

                        for (int i = 0; i < groupContacts.size(); i++) {
                            TalkClientContact groupContact = groupContacts.get(i);
                            try {
                                LOG.debug("sync: membership in group (" + groupContact.getGroupId() + ") : '" + groupMembershipFlags[i] + "'");

                                if (groupMembershipFlags[i]) {
                                    TalkGroupMembership[] memberships = mServerRpc.getGroupMembers(groupContact.getGroupId(), never);
                                    for (TalkGroupMembership membership : memberships) {
                                        updateGroupMembership(membership);
                                    }
                                } else {
                                    TalkGroupPresence groupPresence = groupContact.getGroupPresence();
                                    if (groupPresence != null && groupPresence.isTypeNearby()) {
                                        destroyNearbyGroup(groupContact);
                                    } else {
                                        if (groupPresence != null) {
                                            groupPresence.setState(TalkGroupPresence.STATE_DELETED);
                                            mDatabase.saveGroupPresence(groupPresence);
                                        }

                                        // update group member state
                                        List<TalkGroupMembership> memberships = mDatabase.findMembershipsInGroup(groupContact.getGroupId());
                                        for (TalkGroupMembership membership : memberships) {
                                            membership.setState(TalkGroupMembership.STATE_GROUP_REMOVED);
                                            mDatabase.saveGroupMembership(membership);
                                        }
                                    }

                                    for (IXoContactListener listener : mContactListeners) {
                                        listener.onGroupMembershipChanged(groupContact);
                                        listener.onGroupPresenceChanged(groupContact);
                                    }
                                }
                            } catch (JsonRpcClientException e) {
                                LOG.error("Error while updating group member: ", e);
                            } catch (RuntimeException e) {
                                LOG.error("Error while updating group members: ", e);
                            }
                        }
                    }
                    // ensure we are finished with generating pub/private keys before actually becoming ready...
                    // TODO: have a proper statemachine
                    sendPresenceFuture.get();

                    switchState(STATE_READY, "Synchronization successfull");

                } catch (SQLException e) {
                    LOG.error("SQL Error while syncing: ", e);
                } catch (JsonRpcClientException e) {
                    LOG.error("Error while syncing: ", e);
                } catch (InterruptedException e) {
                    LOG.error("Error while asserting future", e);
                } catch (ExecutionException e) {
                    LOG.error("ExecutionException ", e);
                }

                if (!isReady()) {
                    LOG.warn("sync failed, scheduling new sync");
                    scheduleSync();
                }
            }
        });
    }

    public TalkClientMessage composeClientMessage(TalkClientContact contact, String messageText) {
        return createClientMessage(contact, messageText, null);
    }

    public TalkClientMessage composeClientMessage(TalkClientContact contact, String messageText, TalkClientUpload upload) {
        return createClientMessage(contact, messageText, upload);
    }

    public List<TalkClientMessage> composeClientMessage(TalkClientContact contact, String messageText, List<TalkClientUpload> uploads) {
        ArrayList<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
        if (messageText != null && !"".equals(messageText)) {
            messages.add(createClientMessage(contact, messageText, null));
        }

        for (TalkClientUpload upload : uploads) {
            messages.add(createClientMessage(contact, "", upload));
        }

        return messages;
    }

    private TalkClientMessage createClientMessage(TalkClientContact contact, String messageText, TalkClientUpload upload) {
        final TalkClientMessage clientMessage = new TalkClientMessage();
        final TalkMessage message = new TalkMessage();
        final TalkDelivery delivery = new TalkDelivery(true);

        final String messageTag = message.generateMessageTag();
        message.setSenderId(mSelfContact.getClientId());
        delivery.setMessageTag(messageTag);

        if (contact.isGroup()) {
            delivery.setGroupId(contact.getGroupId());
        }
        if (contact.isClient()) {
            delivery.setReceiverId(contact.getClientId());
        }

        clientMessage.markAsSeen();
        clientMessage.setText(messageText);
        clientMessage.setMessageTag(messageTag);
        clientMessage.setConversationContact(contact);
        clientMessage.setSenderContact(mSelfContact);
        clientMessage.setMessage(message);
        clientMessage.setOutgoingDelivery(delivery);
        clientMessage.setAttachmentUpload(upload);

        try {
            if (upload != null) {
                mDatabase.saveClientUpload(upload);
            }
            mDatabase.saveMessage(message);
            mDatabase.saveDelivery(delivery);
            mDatabase.saveClientMessage(clientMessage);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }

        LOG.debug("Created message with id " + clientMessage.getClientMessageId() + " and tag " + message.getMessageTag());

        return clientMessage;
    }

    public int getDownloadLimit() {
        return mDownloadLimit;
    }

    public void setDownloadLimit(int downloadLimit) {
        mDownloadLimit = downloadLimit;
    }

    public int getUploadLimit() {
        return mUploadLimit;
    }

    public void setUploadLimit(int uploadLimit) {
        mUploadLimit = uploadLimit;
    }

    /**
     * Client-side RPC implementation
     */
    public class TalkRpcClientImpl implements ITalkRpcClient {

        @Override
        public void ping() {
            LOG.debug("server: ping()");
        }

        @Override
        public String[] getEncryptedGroupKeys(String groupId, String sharedKeyId, String sharedKeyIdSalt, String[] clientIds, String[] publicKeyIds) {
            LOG.debug("server: getEncryptedGroupKeys()");
            String[] failed = new String[0];

            TalkClientContact contact = null;
            boolean isRenewGroupKey = false;

            try {
                contact = mDatabase.findGroupContactByGroupId(groupId, false);
            } catch (SQLException e) {
                LOG.error("Error while retrieving group contact from id: " + groupId, e);
            }

            if (contact == null) {
                return failed;
            }

            TalkGroupPresence presence = contact.getGroupPresence();

            if (presence == null) {
                return failed;
            }

            if ("renew".equalsIgnoreCase(sharedKeyId)) {
                generateGroupKey(contact);
                isRenewGroupKey = true;
            } else {
                if (!sharedKeyId.equals(presence.getSharedKeyId()) || !sharedKeyIdSalt.equals(presence.getSharedKeyIdSalt())) {
                    return failed;
                }
            }

            if (contact.getGroupKey() == null) {
                return failed;
            }

            // do we have a public key for each group member?
            List<TalkClientContact> clientsInGroup = new ArrayList<TalkClientContact>();
            for (String clientId : clientIds) {
                try {
                    TalkClientContact clientContact = mDatabase.findContactByClientId(clientId, false);
                    if (clientContact == null || clientContact.getPublicKey() == null) {
                        break;
                    }
                    clientsInGroup.add(clientContact);

                } catch (SQLException e) {
                    LOG.error("Error while retrieving client contact with id: " + clientId, e);
                    break;
                }
            }

            if (clientsInGroup.size() != clientIds.length) {
                return failed;
            }

            // encrypt group key with each member's public key
            byte[] rawGroupKey = Base64.decodeBase64(contact.getGroupKey().getBytes(Charset.forName("UTF-8")));
            List<String> encryptedGroupKeys = new ArrayList<String>();
            for (TalkClientContact clientContact : clientsInGroup) {
                PublicKey publicKey = clientContact.getPublicKey().getAsNative();
                try {
                    byte[] encryptedGroupKey = RSACryptor.encryptRSA(publicKey, rawGroupKey);
                    String encryptedGroupKeyString = new String(Base64.encodeBase64(encryptedGroupKey));
                    encryptedGroupKeys.add(encryptedGroupKeyString);

                } catch (GeneralSecurityException e) {
                    LOG.error("Error while encrypting group key with client's public key", e);
                    break;
                }
            }

            if (encryptedGroupKeys.size() != clientsInGroup.size()) {
                return failed;
            }

            if (isRenewGroupKey) {
                encryptedGroupKeys.add(presence.getSharedKeyId());
                encryptedGroupKeys.add(presence.getSharedKeyIdSalt());
            }

            return encryptedGroupKeys.toArray(new String[encryptedGroupKeys.size()]);
        }

        @Override
        public void alertUser(String message) {
            LOG.debug("server: alertUser()");
            LOG.info("ALERTING USER: \"" + message + "\"");

            for (IXoAlertListener listener : mAlertListeners) {
                listener.onAlertMessageReceived(message);
            }
        }

        @Override
        public void settingsChanged(String setting, String value, String message) {
            LOG.debug("server: settingsChanged()");
        }


        @Override
        public void pushNotRegistered() {
            LOG.debug("server: pushNotRegistered()");
        }

        @Override
        public void incomingDelivery(TalkDelivery d, TalkMessage m) {
            LOG.debug("server: incomingDelivery()");
            updateIncomingDelivery(d, m);
        }

        @Override
        public void incomingDeliveryUpdated(TalkDelivery d) {
            LOG.debug("server: incomingDeliveryUpdate()");
            updateIncomingDelivery(d);
        }

        @Override
        public void outgoingDeliveryUpdated(TalkDelivery delivery) {
            LOG.debug("server: outgoingDelivery()");
            updateOutgoingDelivery(delivery);
        }

        @Override
        public void presenceUpdated(TalkPresence presence) {
            LOG.debug("server: presenceUpdated(" + presence.getClientId() + ")");
            updateClientPresence(presence, null);
        }

        @Override
        public void presenceModified(TalkPresence presence) {
            LOG.debug("server: presenceModified(" + presence.getClientId() + ")");
            Set<String> fields = presence.nonNullFields();
            updateClientPresence(presence, fields);
        }

        @Override
        public void relationshipUpdated(TalkRelationship relationship) {
            LOG.debug("server: relationshipUpdated(" + relationship.getOtherClientId() + ")");
            updateClientRelationship(relationship);
        }

        @Override
        public void groupUpdated(TalkGroupPresence groupPresence) {
            LOG.debug("server: groupUpdated(" + groupPresence.getGroupId() + ")");
            updateGroupPresence(groupPresence);
        }

        @Override
        public void groupMemberUpdated(TalkGroupMembership membership) {
            LOG.debug("server: groupMemberUpdated(" + membership.getGroupId() + "/" + membership.getClientId() + ")");
            updateGroupMembership(membership);
        }

    }

    private void performDeliveries(final List<TalkClientMessage> clientMessages) {
        LOG.debug("performDeliveries()");
        LOG.debug(clientMessages.size() + " messages to deliver");

        for (final TalkClientMessage clientMessage : clientMessages) {
            final TalkClientUpload upload = clientMessage.getAttachmentUpload();
            if (upload != null) {
                upload.register(mTransferAgent);
            }
            performDelivery(clientMessage);
        }
    }

    private void performDelivery(TalkClientMessage clientMessage) {
        final TalkMessage message = clientMessage.getMessage();
        final TalkDelivery delivery = clientMessage.getOutgoingDelivery();
        LOG.debug("preparing delivery of message " + clientMessage.getClientMessageId());
        try {
            encryptMessage(clientMessage, delivery, message);
        } catch (Exception e) {
            LOG.error("error while encrypting message " + clientMessage.getClientMessageId(), e);
            return;
        }

        TalkDelivery[] deliveries = new TalkDelivery[1];
        deliveries[0] = clientMessage.getOutgoingDelivery();

        LOG.debug(" delivering message " + clientMessage.getClientMessageId());

        TalkDelivery[] resultingDeliveries = new TalkDelivery[0];

        try {

            try {
                clientMessage.setProgressState(true);
                mDatabase.saveClientMessage(clientMessage);
                resultingDeliveries = mServerRpc.outDeliveryRequest(message, deliveries);
                TalkClientUpload upload = clientMessage.getAttachmentUpload();
                if (upload != null) {
                    mTransferAgent.startOrRestartUpload(upload);
                }
            } catch (Exception e) {
                LOG.error("error while performing delivery request for message " + clientMessage.getClientMessageId(), e);

                clientMessage.setProgressState(false);
                mDatabase.saveClientMessage(clientMessage);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while saving delivery", e);
        }

        for (TalkDelivery resultingDelivery : resultingDeliveries) {
            updateOutgoingDelivery(resultingDelivery);
        }
    }

    private TalkPresence ensureSelfPresence(TalkClientContact contact) throws SQLException {
        try {
            TalkPresence presence = contact.getClientPresence();
            if (presence == null) {
                presence = new TalkPresence();
                presence.setClientId(contact.getClientId());
                presence.setClientName("Client");
                presence.setTimestamp(new Date());
                contact.updatePresence(presence);
                mDatabase.savePresence(presence);
                mDatabase.saveContact(contact);
            }
            return presence;
        } catch (Exception e) {
            LOG.error("ensureSelfPresence", e);
            return null;
        }
    }

    private synchronized void ensureSelfKey(TalkClientContact contact) throws SQLException {
        LOG.debug("ensureSelfKey()");
        TalkKey publicKey = contact.getPublicKey();
        TalkPrivateKey privateKey = contact.getPrivateKey();
        if (publicKey == null || privateKey == null) {
            Date now = new Date();
            try {
                mRSAKeysize = mClientConfiguration.getRSAKeysize();
                LOG.info("[connection #" + mConnection.getConnectionId() + "] generating new RSA keypair with size " + mRSAKeysize);
                KeyPair keyPair = RSACryptor.generateRSAKeyPair(mRSAKeysize);

                LOG.trace("unwrapping public key");
                PublicKey pubKey = keyPair.getPublic();
                byte[] pubEnc = RSACryptor.unwrapRSA_X509(pubKey.getEncoded());
//                String pubStr = Base64.encodeBase64String(pubEnc);
                String pubStr = new String(Base64.encodeBase64(pubEnc));

                LOG.trace("unwrapping private key");
                PrivateKey privKey = keyPair.getPrivate();
                byte[] privEnc = RSACryptor.unwrapRSA_PKCS8(privKey.getEncoded());
//                String privStr = Base64.encodeBase64String(privEnc);
                String privStr = new String(Base64.encodeBase64(privEnc));

                LOG.debug("calculating key id");
                String kid = RSACryptor.calcKeyId(pubEnc);

                LOG.debug("creating database objects");

                publicKey = new TalkKey();
                publicKey.setClientId(contact.getClientId());
                publicKey.setTimestamp(now);
                publicKey.setKeyId(kid);
                publicKey.setKey(pubStr);

                privateKey = new TalkPrivateKey();
                privateKey.setClientId(contact.getClientId());
                privateKey.setTimestamp(now);
                privateKey.setKeyId(kid);
                privateKey.setKey(privStr);

                contact.setPublicKey(publicKey);
                contact.setPrivateKey(privateKey);

                LOG.info("[connection #" + mConnection.getConnectionId() + "] generated new key with key id " + kid);

                mDatabase.savePublicKey(publicKey);
                mDatabase.savePrivateKey(privateKey);
                mDatabase.saveContact(contact);
            } catch (Exception e) {
                LOG.error("exception generating key", e);
            }
        } else {
            LOG.info("using key with key id " + publicKey.getKeyId());
        }
    }

    private ScheduledFuture sendPresence() {
        LOG.info("sendPresence()");
        return mExecutor.schedule(new Runnable() {
            @Override
            public void run() {

                try {
                    TalkClientContact contact = mSelfContact;
                    ensureSelfPresence(contact);
                    ensureSelfKey(contact);
                    TalkPresence presence = contact.getClientPresence();
                    presence.setKeyId(contact.getPublicKey().getKeyId());
                    mDatabase.savePresence(presence);
                    mDatabase.saveContact(contact);
                    mServerRpc.updateKey(contact.getPublicKey());
                    mServerRpc.updatePresence(presence);
                } catch (SQLException e) {
                    LOG.error("SQL error", e);
                } catch (JsonRpcClientException e) {
                    LOG.error("Error while sending presence: ", e);
                } catch (Exception e) { // TODO: specify own exception in XoClientDatabase.savePresence!
                    LOG.error("error in sendPresence", e);
                }
            }

        }, 0, TimeUnit.SECONDS);
    }

    public void sendEnvironmentUpdate(TalkEnvironment environment) {
        LOG.debug("sendEnvironmentUpdate()");
        if (isReady() && environment != null) {
            if (mEnvironmentUpdateCallPending.compareAndSet(false, true)) {

                final TalkEnvironment environmentToSend = environment;
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            environmentToSend.setClientId(mSelfContact.getClientId());
                            environmentToSend.setGroupId(mEnvironmentGroupId);
                            mEnvironmentGroupId = mServerRpc.updateEnvironment(environmentToSend);
                        } catch (Throwable t) {
                            LOG.error("sendEnvironmentUpdate: other error", t);
                        }
                        mEnvironmentUpdateCallPending.set(false);
                    }
                });
            } else {
                LOG.debug("sendEnvironmentUpdate(): another update is still pending");
            }
        } else {
            LOG.debug("sendEnvironmentUpdate(): client not yet ready or no environment");
        }
    }

    public TalkClientContact getCurrentNearbyGroup() {
        TalkClientContact currentNearbyGroup = null;
        try {
            if (mEnvironmentGroupId != null) {
                currentNearbyGroup = mDatabase.findGroupContactByGroupId(mEnvironmentGroupId, false);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while retrieving current nearby group ", e);
        }
        return currentNearbyGroup;
    }


    public void sendDestroyEnvironment(final String type) {
        if (isReady()) {
            mEnvironmentGroupId = null;
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mServerRpc.destroyEnvironment(type);
                    } catch (Throwable t) {
                        LOG.error("sendDestroyEnvironment: other error", t);
                    }
                }
            });
        }
    }


    private void updateOutgoingDelivery(final TalkDelivery delivery) {
        LOG.debug("updateOutgoingDelivery(" + delivery.getMessageId() + ")");

        TalkClientContact clientContact;
        TalkClientContact groupContact;
        TalkClientMessage clientMessage = null;
        try {
            String receiverId = delivery.getReceiverId();
            if (receiverId != null) {
                clientContact = mDatabase.findContactByClientId(receiverId, false);
                if (clientContact == null) {
                    LOG.warn("outgoing message for unknown client " + receiverId);
                    return;
                }
            }

            String groupId = delivery.getGroupId();
            if (groupId != null) {
                groupContact = mDatabase.findGroupContactByGroupId(groupId, false);
                if (groupContact == null) {
                    LOG.warn("outgoing message for unknown group " + groupId);
                    //TODO: return; ??
                }
            }

            String messageId = delivery.getMessageId();
            String messageTag = delivery.getMessageTag();
            if (messageTag != null) {
                clientMessage = mDatabase.findMessageByMessageTag(messageTag, false);
            }
            if (clientMessage == null) {
                clientMessage = mDatabase.findMessageByMessageId(messageId, false);
            }
            if (clientMessage == null) {
                LOG.warn("outgoing delivery notification for unknown message id " + messageId + " tag " + messageTag +
                        ", delivery state = " + delivery.getState() + ", group " + groupId + ", receiver " + receiverId);
                if (TalkDelivery.SENDER_SHOULD_ACKNOWLEDGE_STATES_SET.contains(delivery.getState())) {
                    LOG.warn("acknowledging outgoing delivery for unknown message id " + messageId + " tag " + messageTag +
                            ", delivery state = " + delivery.getState() + ", group " + groupId + ", receiver " + receiverId);
                    sendDeliveryConfirmation(delivery, false);
                } else {
                    // weird delivery, abort it
                }
                return;
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        clientMessage.updateOutgoing(delivery);

        try {
            mDatabase.saveDelivery(clientMessage.getOutgoingDelivery());
            mDatabase.saveClientMessage(clientMessage);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        sendDeliveryConfirmation(delivery, true);
        sendUploadDeliveryConfirmation(delivery);

        for (IXoMessageListener listener : mMessageListeners) {
            listener.onMessageUpdated(clientMessage);
        }
    }

    private void sendUploadDeliveryConfirmation(final TalkDelivery delivery) {
        TalkClientUpload clientUpload = null;
        try {
            clientUpload = mDatabase.getClientUploadForDelivery(delivery);
        } catch (SQLException e) {
            LOG.error("Error while retrieving client upload for delivery", e);
        }

        if (clientUpload == null) {
            return;
        }

        final TalkClientUpload upload = clientUpload;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String nextState = null;
                    if (delivery.getAttachmentState().equals(TalkDelivery.ATTACHMENT_STATE_RECEIVED)) {
                        nextState = mServerRpc.acknowledgeReceivedFile(upload.getFileId(), delivery.getReceiverId());
                        delivery.setAttachmentState(nextState);
                    }
                    if (delivery.getAttachmentState().equals(TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_ABORTED)) {
                        nextState = mServerRpc.acknowledgeAbortedFileDownload(upload.getFileId(), delivery.getReceiverId());
                        delivery.setAttachmentState(nextState);
                    }
                    if (delivery.getAttachmentState().equals(TalkDelivery.ATTACHMENT_STATE_DOWNLOAD_FAILED)) {
                        nextState = mServerRpc.acknowledgeFailedFileDownload(upload.getFileId(), delivery.getReceiverId());
                        delivery.setAttachmentState(nextState);
                    }
                    if (nextState != null) {
                        mDatabase.saveDelivery(delivery);
                    }
                } catch (Exception e) {
                    LOG.error("Error while sending delivery confirmation: ", e);
                }
            }
        });
    }

    private void sendDownloadAttachmentDeliveryConfirmation(final TalkDelivery delivery) {
        TalkClientDownload clientDownload = null;
        try {
            clientDownload = mDatabase.getClientDownloadForDelivery(delivery);
        } catch (SQLException e) {
            LOG.error("Error while retrieving client download for delivery", e);
        }

        if (clientDownload == null) {
            return;
        }

        final TalkClientDownload download = clientDownload;
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String nextState = null;
                    if (delivery.getAttachmentState().equals(TalkDelivery.ATTACHMENT_STATE_UPLOAD_ABORTED)) {
                        nextState = mServerRpc.acknowledgeAbortedFileUpload(download.getFileId());
                        delivery.setAttachmentState(nextState);
                    }
                    if (delivery.getAttachmentState().equals(TalkDelivery.ATTACHMENT_STATE_UPLOAD_FAILED)) {
                        nextState = mServerRpc.acknowledgeFailedFileUpload(download.getFileId());
                        delivery.setAttachmentState(nextState);
                    }

                    if (nextState != null) {
                        mDatabase.saveDelivery(delivery);
                    }
                } catch (Exception e) {
                    LOG.error("Error while sending attachment delivery confirmation: ", e);
                }
            }
        });
    }

    // caution: this is only for unknown deliveries not stored in the database
    private void sendDeliveryAbort(final TalkDelivery delivery) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    TalkDelivery result = mServerRpc.outDeliveryAbort(delivery.getMessageId(), delivery.getReceiverId());
                    if (result != null) {
                        LOG.warn("aborted strange delivery for message id " + delivery.getMessageId() + " receiver " + delivery.getReceiverId());
                    }
                } catch (Exception e) {
                    LOG.error("Error while sending delivery abort: ", e);
                }
            }
        });
    }


    private void sendDeliveryConfirmation(final TalkDelivery delivery, final boolean saveResult) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    TalkDelivery result = null;
                    if (delivery.getState().equals(TalkDelivery.STATE_DELIVERED_SEEN)) {
                        result = mServerRpc.outDeliveryAcknowledgeSeen(delivery.getMessageId(), delivery.getReceiverId());
                    } else if (delivery.getState().equals(TalkDelivery.STATE_DELIVERED_UNSEEN)) {
                        result = mServerRpc.outDeliveryAcknowledgeUnseen(delivery.getMessageId(), delivery.getReceiverId());
                    } else if (delivery.getState().equals(TalkDelivery.STATE_DELIVERED_PRIVATE)) {
                        result = mServerRpc.outDeliveryAcknowledgePrivate(delivery.getMessageId(), delivery.getReceiverId());
                    } else if (delivery.getState().equals(TalkDelivery.STATE_FAILED)) {
                        result = mServerRpc.outDeliveryAcknowledgeFailed(delivery.getMessageId(), delivery.getReceiverId());
                    } else if (delivery.getState().equals(TalkDelivery.STATE_REJECTED)) {
                        result = mServerRpc.outDeliveryAcknowledgeRejected(delivery.getMessageId(), delivery.getReceiverId());
                    }
                    if (result != null && saveResult) {
                        mDatabase.saveDelivery(result);
                    }
                } catch (Exception e) {
                    LOG.error("Error while sending delivery confirmation: ", e);
                }
            }
        });
    }

    private void updateIncomingDelivery(final TalkDelivery delivery) {
        LOG.debug("updateIncomingDelivery (Only)(" + delivery.getMessageId() + ")");
        TalkClientMessage clientMessage;
        try {
            clientMessage = mDatabase.findMessageByMessageId(delivery.getMessageId(), false);
            if (clientMessage == null) {
                throw new RuntimeException("updateIncomingDelivery: message not found, id=" + delivery.getMessageId());
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
            return;
        }

        try {
            clientMessage.updateIncoming(delivery);
            TalkClientDownload attachmentDownload = clientMessage.getAttachmentDownload();
            mDatabase.updateDelivery(clientMessage.getIncomingDelivery());

            if (attachmentDownload != null && !mTransferAgent.isDownloadActive(attachmentDownload) && attachmentDownload.getState() != TalkClientDownload.State.PAUSED) {
                mTransferAgent.onDownloadRegistered(attachmentDownload);
            }
            for (IXoMessageListener listener : mMessageListeners) {
                listener.onMessageUpdated(clientMessage);
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    private void updateIncomingDelivery(final TalkDelivery delivery, final TalkMessage message) {
        LOG.debug("updateIncomingDelivery(" + delivery.getMessageId() + ")");
        boolean newMessage = false;
        TalkClientContact groupContact = null;
        TalkClientContact senderContact;
        TalkClientMessage clientMessage;
        try {
            String groupId = delivery.getGroupId();
            if (groupId != null) {
                groupContact = mDatabase.findGroupContactByGroupId(groupId, false);
                if (groupContact == null) {
                    LOG.warn("incoming message in unknown group " + groupId);
                    return;
                }
            }
            senderContact = mDatabase.findContactByClientId(delivery.getSenderId(), false);
            if (senderContact == null) {
                LOG.warn("incoming message from unknown client " + delivery.getSenderId());
                return;
            }
            clientMessage = mDatabase.findMessageByMessageId(delivery.getMessageId(), false);
            if (clientMessage == null) {
                newMessage = true;
                clientMessage = mDatabase.findMessageByMessageId(delivery.getMessageId(), true);
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
            return;
        }

        clientMessage.setSenderContact(senderContact);

        if (groupContact == null) {
            clientMessage.setConversationContact(senderContact);
        } else {
            clientMessage.setConversationContact(groupContact);
        }

        boolean messageFailed = true;
        String reason = "unknown";
        try {
            decryptMessage(clientMessage, delivery, message);

            clientMessage.updateIncoming(delivery, message);

            TalkClientDownload attachmentDownload = clientMessage.getAttachmentDownload();

            if (attachmentDownload != null) {
                String attachmentFileId = clientMessage.getMessage().getAttachmentFileId();
                attachmentDownload.setFileId(attachmentFileId);
                mDatabase.saveClientDownload(clientMessage.getAttachmentDownload());
            }
            mDatabase.saveMessage(clientMessage.getMessage());
            mDatabase.saveDelivery(clientMessage.getIncomingDelivery());
            mDatabase.saveClientMessage(clientMessage);

//            if(attachmentDownload != null) {
//                mTransferAgent.startOrRestartDownload(attachmentDownload);
//            }

            for (IXoMessageListener listener : mMessageListeners) {
                if (newMessage) {
                    listener.onMessageCreated(clientMessage);
                } else {
                    listener.onMessageUpdated(clientMessage);
                }
            }

            messageFailed = false;
        } catch (GeneralSecurityException e) {
            reason = "decryption problem" + e;
            LOG.error("decryption problem", e);
        } catch (IOException e) {
            reason = "io error" + e;
            LOG.error("io error", e);
        } catch (SQLException e) {
            reason = "sql error" + e;
            LOG.error("sql error", e);
        }
        if (!messageFailed) {
            if (delivery.getState().equals(TalkDelivery.STATE_DELIVERING)) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        LOG.debug("confirming " + delivery.getMessageId());

                        try {
                            TalkDelivery result;
                            boolean sendDeliveryConfirmation = mClientConfiguration.isSendDeliveryConfirmationEnabled();
                            if (sendDeliveryConfirmation) {
                                result = mServerRpc.inDeliveryConfirmUnseen(delivery.getMessageId());
                            } else {
                                result = mServerRpc.inDeliveryConfirmPrivate(delivery.getMessageId());
                            }
                            updateIncomingDelivery(result);

                        } catch (Exception e) {
                            LOG.error("Error while sending delivery confirmation: ", e);
                        }
                    }
                });
            }
        } else {
            final XoClient that = this;
            final String finalReason = reason;
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("rejecting " + delivery.getMessageId());
                    TalkDelivery result = mServerRpc.inDeliveryReject(delivery.getMessageId(), finalReason);
                    that.updateIncomingDelivery(result);
                }
            });
        }


        sendDownloadAttachmentDeliveryConfirmation(delivery);
    }

    private void decryptMessage(TalkClientMessage clientMessage, TalkDelivery delivery, TalkMessage message) throws GeneralSecurityException, IOException, SQLException {
        LOG.debug("decryptMessage()");

        // contact (provides decryption context)
        TalkClientContact contact = clientMessage.getConversationContact();

        if (!message.getMessageTag().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            byte[] hmac = message.computeHMAC();
            String hmacString = new String(Base64.encodeBase64(hmac));
            if (hmacString.equals(message.getMessageTag())) {
                LOG.info("HMAC ok");
            } else {
                LOG.error("HMAC mismatch");
            }
        }

        // default message text
        // LOG.debug("Setting message's default body to empty string");
        clientMessage.setText("");

        // get various fields
        String keyId = delivery.getKeyId();
        String keyCiphertext = delivery.getKeyCiphertext();
        String keySalt = message.getSalt();
        String rawBody = message.getBody();
        String rawAttachment = message.getAttachment();

        // get decryption key
        byte[] decryptedKey;
        if (contact.isClient()) {
            LOG.trace("decrypting using private key");
            // decrypt the provided key using our private key
            try {
                TalkPrivateKey talkPrivateKey;
                talkPrivateKey = mDatabase.findPrivateKeyByKeyId(keyId);
                if (talkPrivateKey == null) {
                    LOG.error("no private key for keyId " + keyId);
                    return;
                } else {
                    PrivateKey privateKey = talkPrivateKey.getAsNative();
                    if (privateKey == null) {
                        LOG.error("could not decode private key");
                        return;
                    } else {
                        decryptedKey = RSACryptor.decryptRSA(privateKey, Base64.decodeBase64(keyCiphertext.getBytes(Charset.forName("UTF-8"))));
                    }
                }
            } catch (SQLException e) {
                LOG.error("sql error", e);
                throw e;
            } catch (GeneralSecurityException e) {
                LOG.error("decryption error", e);
                throw e;
            }
        } else if (contact.isGroup()) {
            LOG.trace("decrypting using group key");
            // get the group key for decryption
            String groupKey = contact.getGroupKey();
            if (groupKey == null) {
                LOG.warn("no group key");
                return;
            }
            decryptedKey = Base64.decodeBase64(contact.getGroupKey().getBytes(Charset.forName("UTF-8")));
        } else {
            LOG.error("don't know how to decrypt messages from contact of type " + contact.getContactType());
            throw new RuntimeException("don't know how to decrypt messages from contact of type " + contact.getContactType());
        }

        // check that we have a key
        if (decryptedKey == null) {
            LOG.error("could not determine decryption key");
            return;
        }

        // apply salt if present
        if (keySalt != null) {
            byte[] decodedSalt = Base64.decodeBase64(keySalt.getBytes(Charset.forName("UTF-8")));
            if (decodedSalt.length != decryptedKey.length) {
                LOG.error("message salt has wrong size");
                return;
            }
            for (int i = 0; i < decryptedKey.length; i++) {
                decryptedKey[i] = (byte) (decryptedKey[i] ^ decodedSalt[i]);
            }
        }

        // decrypt both body and attachment dtor
        byte[] decryptedBodyRaw;
        String decryptedBody = "";
        byte[] decryptedAttachmentRaw;
        TalkAttachment decryptedAttachment = null;
        try {
            // decrypt body
            if (rawBody != null) {
                decryptedBodyRaw = AESCryptor.decrypt(decryptedKey, AESCryptor.NULL_SALT, Base64.decodeBase64(rawBody.getBytes(Charset.forName("UTF-8"))));
                decryptedBody = new String(decryptedBodyRaw, "UTF-8");
                //LOG.debug("determined decrypted body to be '" + decryptedBody + "'");
            }
            // decrypt attachment
            if (rawAttachment != null) {
                decryptedAttachmentRaw = AESCryptor.decrypt(decryptedKey, AESCryptor.NULL_SALT, Base64.decodeBase64(rawAttachment.getBytes(Charset.forName("UTF-8"))));
                decryptedAttachment = mJsonMapper.readValue(decryptedAttachmentRaw, TalkAttachment.class);
            }
        } catch (GeneralSecurityException e) {
            LOG.error("error decrypting", e);
            throw e;
        } catch (IOException e) {
            LOG.error("error decrypting", e);
            throw e;
        }

        // add decrypted information to message
        clientMessage.setText(decryptedBody);

        if (decryptedAttachment != null) {
            TalkClientDownload download = new TalkClientDownload();
            download.initializeAsAttachment(mTransferAgent, decryptedAttachment, message.getMessageId(), decryptedKey);
            clientMessage.setAttachmentDownload(download);
        }
    }

    private void encryptMessage(TalkClientMessage clientMessage, TalkDelivery delivery, TalkMessage message) {

        LOG.debug("encrypting message with id '" + clientMessage.getClientMessageId() + "'");

        if (message.getBody() != null) {
            LOG.error("message has already body");
            return;
        }

        TalkClientContact receiver = clientMessage.getConversationContact();
        if (receiver == null) {
            LOG.error("no receiver");
            return;
        }

        try {
            receiver = mDatabase.findContactById(receiver.getClientContactId());
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        byte[] plainKey;
        byte[] keySalt = null;
        if (receiver.isClient()) {
            LOG.trace("using client key for encryption");
            // generate message key
            plainKey = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            // get public key for encrypting the key
            TalkKey talkPublicKey = receiver.getPublicKey();

            if (talkPublicKey == null) {
                throw new RuntimeException("no pubkey for encryption");
            }
            // retrieve native version of the key
            PublicKey publicKey = talkPublicKey.getAsNative();
            if (publicKey == null) {
                throw new RuntimeException("could not get public key for encryption");
            }
            // encrypt the message key
            try {
                byte[] encryptedKey = RSACryptor.encryptRSA(publicKey, plainKey);
                delivery.setKeyId(talkPublicKey.getKeyId());
                delivery.setKeyCiphertext(new String(Base64.encodeBase64(encryptedKey)));
            } catch (GeneralSecurityException e) {
                LOG.error("error encrypting", e);
                return;
            }
        } else if (receiver.isGroup()) {
            LOG.trace("using group key for encryption");
            // get and decode the group key
            String groupKey = receiver.getGroupKey();
            if (groupKey == null) {
                LOG.warn("no group key");
                return;
            }
            plainKey = Base64.decodeBase64(groupKey.getBytes(Charset.forName("UTF-8")));
            // generate message-specific salt
            keySalt = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            // encode the salt for transmission
            String encodedSalt = new String(Base64.encodeBase64(keySalt));
            message.setSalt(encodedSalt);
            message.setSharedKeyId(receiver.getGroupPresence().getSharedKeyId());
            message.setSharedKeyIdSalt(receiver.getGroupPresence().getSharedKeyIdSalt());
        } else {
            throw new RuntimeException("bad receiver type, is neither group nor client");
        }

        // apply salt if present
        if (keySalt != null) {
            if (keySalt.length != plainKey.length) {
                LOG.error("message salt has wrong size");
                return;
            }
            for (int i = 0; i < plainKey.length; i++) {
                plainKey[i] = (byte) (plainKey[i] ^ keySalt[i]);
            }
        }

        // initialize attachment upload
        TalkAttachment attachment = null;
        TalkClientUpload upload = clientMessage.getAttachmentUpload();
        if (upload != null) {
            LOG.debug("generating attachment");

            upload.provideEncryptionKey(new String(Hex.encodeHex(plainKey)));

            try {
                mDatabase.saveClientUpload(upload);
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }

            LOG.debug("attachment download url is '" + upload.getDownloadUrl() + "'");
            attachment = new TalkAttachment();
            attachment.setFileName(upload.getFileName());
            attachment.setUrl(upload.getDownloadUrl());
            attachment.setContentSize(Long.toString(upload.getContentLength()));
            attachment.setMediaType(upload.getMediaType());
            attachment.setMimeType(upload.getMimeType());
            attachment.setAspectRatio(upload.getAspectRatio());
            attachment.setHmac(upload.getContentHmac());
            attachment.setFileId(upload.getFileId());
            message.setAttachmentFileId(attachment.getFileId());
        }

        // encrypt body and attachment dtor
        try {
            // encrypt body
            LOG.trace("encrypting body");
            byte[] encryptedBody = AESCryptor.encrypt(plainKey, AESCryptor.NULL_SALT, clientMessage.getText().getBytes("UTF-8"));
            message.setBody(new String(Base64.encodeBase64(encryptedBody)));
            // encrypt attachment dtor
            if (attachment != null) {
                LOG.trace("encrypting attachment");
                byte[] encodedAttachment = mJsonMapper.writeValueAsBytes(attachment);
                byte[] encryptedAttachment = AESCryptor.encrypt(plainKey, AESCryptor.NULL_SALT, encodedAttachment);
                message.setAttachment(new String(Base64.encodeBase64(encryptedAttachment)));
            }
        } catch (GeneralSecurityException e) {
            LOG.error("error encrypting", e);
        } catch (UnsupportedEncodingException e) {
            LOG.error("error encrypting", e);
        } catch (JsonProcessingException e) {
            LOG.error("error encrypting", e);
        }

        message.setTimeSent(new Date());
        byte[] hmac = message.computeHMAC();
        message.setMessageTag(new String(Base64.encodeBase64(hmac)));
    }

    private void updateClientPresence(TalkPresence presence, Set<String> fields) {
        LOG.debug("updateClientPresence(" + presence.getClientId() + ")");

        TalkClientContact clientContact;
        try {
            clientContact = mDatabase.findContactByClientId(presence.getClientId(), false);
            if (clientContact == null) {
                clientContact = mDatabase.findContactByClientId(presence.getClientId(), true);
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        if (clientContact.isSelf()) {
            LOG.warn("server sent self-presence due to group presence bug, ignoring");
            return;
        }

        if (!clientContact.isClient()) {
            LOG.warn("contact is not a client contact!? " + clientContact.getContactType());
            return;
        }
        if (fields == null) {
            clientContact.updatePresence(presence);
        } else {
            clientContact.modifyPresence(presence, fields);
        }

        // ensure that a timestamp is set, this is a manual migration step for the newly introduced timestamp field
        if (clientContact.getCreatedTimeStamp() == null) {
            clientContact.setCreatedTimeStamp(new Date());
        }

        boolean wantDownload = false;
        try {
            if (fields == null) {
                wantDownload = updateAvatarDownload(clientContact, presence.getAvatarUrl(), "c-" + presence.getClientId(), presence.getTimestamp());
            } else {
                if (fields.contains(TalkPresence.FIELD_AVATAR_URL)) {
                    // TODO: check this date collision-avoidance stuff if is ok
                    wantDownload = updateAvatarDownload(clientContact, presence.getAvatarUrl(), "c-" + presence.getClientId(), new Date());
                }
            }
        } catch (MalformedURLException e) {
            LOG.warn("malformed avatar url", e);
        }

        TalkClientDownload avatarDownload = clientContact.getAvatarDownload();
        try {
            if (avatarDownload != null) {
                mDatabase.saveClientDownload(avatarDownload);
            }
            mDatabase.savePresence(clientContact.getClientPresence());
            mDatabase.saveContact(clientContact);
        } catch (Exception e) {
            LOG.error("updateClientPresence", e);
        }
        if (avatarDownload != null && wantDownload) {
            mTransferAgent.startOrRestartDownload(avatarDownload, true);
        }

        final TalkClientContact fContact = clientContact;
        if (fields == null || fields.contains(TalkPresence.FIELD_KEY_ID)) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    updateClientKey(fContact);
                }
            });
        }

        for (IXoContactListener listener : mContactListeners) {
            listener.onClientPresenceChanged(clientContact);
        }
    }

    private boolean updateAvatarDownload(TalkClientContact contact, String avatarUrl, String avatarId, Date avatarTimestamp) throws MalformedURLException {
        boolean urlIsValid = avatarUrl != null && !avatarUrl.isEmpty();
        if (!urlIsValid) {
            LOG.debug("no valid avatar url for contact " + contact.getClientContactId());
            contact.setAvatarDownload(null);
            return false;
        }

        boolean wantDownload = false;
        TalkClientDownload avatarDownload = contact.getAvatarDownload();
        if (avatarDownload == null) {
            LOG.debug("new avatar for contact " + contact.getClientContactId());
            avatarDownload = new TalkClientDownload();
            avatarDownload.initializeAsAvatar(mTransferAgent, avatarUrl, avatarId, avatarTimestamp);
            wantDownload = true;
        } else {
            try {
                mDatabase.refreshClientDownload(avatarDownload);
            } catch (SQLException e) {
                LOG.error("sql error", e);
                return false;
            }
            String downloadUrl = avatarDownload.getDownloadUrl();
            if (downloadUrl == null || !downloadUrl.equals(avatarUrl)) {
                LOG.debug("new avatar for contact " + contact.getClientContactId());
                avatarDownload = new TalkClientDownload();
                avatarDownload.initializeAsAvatar(mTransferAgent, avatarUrl, avatarId, avatarTimestamp);
                wantDownload = true;
            } else {
                LOG.debug("avatar not changed for contact " + contact.getClientContactId());
                TalkClientDownload.State state = avatarDownload.getState();
                if (state != TalkClientDownload.State.COMPLETE && state != TalkClientDownload.State.FAILED) {
                    wantDownload = true;
                }
            }
        }

        contact.setAvatarDownload(avatarDownload);
        return wantDownload;
    }

    private void updateClientKey(TalkClientContact client) {
        String clientId = client.getClientId();

        String currentKeyId = client.getClientPresence().getKeyId();
        if (currentKeyId == null || currentKeyId.isEmpty()) {
            LOG.warn("client " + clientId + " has no key id");
            return;
        }

        TalkKey clientKey = client.getPublicKey();
        if (clientKey != null) {
            if (clientKey.getKeyId().equals(currentKeyId)) {
                LOG.debug("client " + clientId + " has current key");
                return;
            }
        }

        try {
            LOG.debug("retrieving key " + currentKeyId + " for client " + clientId);
            TalkKey key = mServerRpc.getKey(client.getClientId(), currentKeyId);
            if (key != null) {
                try {
                    mDatabase.savePublicKey(key);
                    client.setPublicKey(key);
                    mDatabase.saveContact(client);
                } catch (SQLException e) {
                    LOG.error("SQL error", e);
                }
            }
        } catch (JsonRpcClientException e) {
            LOG.error("Error while retrieving key: ", e);
        }
    }

    private void updateClientRelationship(TalkRelationship relationship) {
        LOG.debug("updateClientRelationship(" + relationship.getOtherClientId() + ")");
        TalkClientContact clientContact;
        try {
            clientContact = mDatabase.findContactByClientId(relationship.getOtherClientId(), relationship.isRelated());
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        if (clientContact == null) {
            return;
        }

        clientContact.updateRelationship(relationship);

        try {
            mDatabase.saveRelationship(clientContact.getClientRelationship());
            mDatabase.saveContact(clientContact);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        for (final IXoContactListener listener : mContactListeners) {
            listener.onClientRelationshipChanged(clientContact);
        }
    }

    private void updateGroupPresence(TalkGroupPresence groupPresence) {
        LOG.info("updateGroupPresence(" + groupPresence.getGroupId() + ")");

        TalkClientContact groupContact;

        try {
            synchronized (mGroupCreationLock) {
                groupContact = mDatabase.findGroupContactByGroupId(groupPresence.getGroupId(), false);
                if (groupContact == null) {
                    groupContact = mDatabase.findGroupContactByGroupId(groupPresence.getGroupId(), true);
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        if (groupContact == null) {
            LOG.warn("gp update for unknown group " + groupPresence.getGroupId());
            return;
        }

        // ensure that a timestamp is set, this is a manual migration step for the newly introduced timestamp field
        if (groupContact.getCreatedTimeStamp() == null) {
            groupContact.setCreatedTimeStamp(new Date());
        }

        groupContact.updateGroupPresence(groupPresence);

        try {
            updateAvatarDownload(groupContact, groupPresence.getGroupAvatarUrl(), "g-" + groupPresence.getGroupId(), groupPresence.getLastChanged());
        } catch (MalformedURLException e) {
            LOG.warn("Malformed avatar URL", e);
        }

        updateAvatarsForGroupContact(groupContact);

        try {
            mDatabase.saveGroupPresence(groupContact.getGroupPresence());
            mDatabase.saveContact(groupContact);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        // quietly destroy nearby group
        if (groupPresence.isTypeNearby() && !groupPresence.exists()) {
            destroyNearbyGroup(groupContact);
        }

        LOG.info("updateGroupPresence(" + groupPresence.getGroupId() + ") - saved");

        for (IXoContactListener listener : mContactListeners) {
            listener.onGroupPresenceChanged(groupContact);
        }
    }

    private void destroyNearbyGroup(TalkClientContact groupContact) {
        LOG.debug("destroying nearby group with id " + groupContact.getGroupId());

        try {
            // reset group state
            TalkGroupPresence groupPresence = groupContact.getGroupPresence();
            if (groupPresence == null) {
                LOG.error("Can not destroy nearby group since groupPresence is null");
                return;
            }
            groupPresence.setState(TalkGroupPresence.STATE_KEPT);

            // save group
            mDatabase.saveContact(groupContact);
            mDatabase.saveGroupPresence(groupPresence);

            // set member states to none
            List<TalkGroupMembership> memberships = mDatabase.findMembershipsInGroup(groupContact.getGroupId());
            for (TalkGroupMembership membership : memberships) {
                membership.setState(TalkGroupMembership.STATE_NONE);
                mDatabase.saveGroupMembership(membership);
            }

            // set contact nearby state to false
            List<TalkClientContact> contacts = mDatabase.findContactsInGroup(groupContact.getGroupId());
            for (TalkClientContact contact : contacts) {
                contact.setNearby(false);
                mDatabase.saveContact(contact);
            }
        } catch (SQLException e) {
            LOG.error("Error while destroying nearby group " + groupContact.getGroupId());
        }
    }

    private void updateAvatarsForGroupContact(TalkClientContact contact) {
        TalkClientDownload avatarDownload = contact.getAvatarDownload();
        try {
            if (avatarDownload != null) {
                mDatabase.saveClientDownload(avatarDownload);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error when saving avatar download", e);
        }
        if (avatarDownload != null) {
            mTransferAgent.startOrRestartDownload(avatarDownload, true);
        }
    }

    private void updateGroupMembership(TalkGroupMembership membership) {
        LOG.info("updateGroupMembership(groupId: '" + membership.getGroupId() + "', clientId: '" + membership.getClientId() + "', state: '" + membership.getState() + "')");
        TalkClientContact groupContact;
        TalkClientContact clientContact;

        try {
            synchronized (mGroupCreationLock) {
                groupContact = mDatabase.findGroupContactByGroupId(membership.getGroupId(), false);
                if (groupContact == null) {
                    boolean createGroup = membership.isInvolved() && !membership.isGroupRemoved();
                    if (createGroup) {
                        LOG.info("creating group for member in state '" + membership.getState() + "' groupId '" + membership.getGroupId() + "'");
                        groupContact = mDatabase.findGroupContactByGroupId(membership.getGroupId(), true);
                    } else {
                        LOG.warn("ignoring incoming member for unknown group for member in state '" + membership.getState() + "' groupId '" + membership.getGroupId() + "'");
                        return;
                    }
                }
            }

            clientContact = mDatabase.findContactByClientId(membership.getClientId(), false);
            if (clientContact == null) {
                boolean createContact = membership.isInvolved() && !membership.isGroupRemoved();
                if (createContact) {
                    LOG.info("creating contact for member in state '" + membership.getState() + "' clientId '" + membership.getClientId() + "'");
                    clientContact = mDatabase.findContactByClientId(membership.getClientId(), true);
                } else {
                    LOG.warn("ignoring incoming member for unknown contact for member in state '" + membership.getState() + "' clientId '" + membership.getGroupId() + "'");
                    return;
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }
        if (clientContact == null) {
            LOG.error("groupMemberUpdate for unknown client: " + membership.getClientId());
        }
        if (groupContact == null) {
            LOG.error("groupMemberUpdate for unknown group: " + membership.getGroupId());
        }
        if (clientContact == null || groupContact == null) {
            return;
        }

        // ensure that a timestamp is set, this is a manual migration step for the newly introduced timestamp field
        if (groupContact.getCreatedTimeStamp() == null) {
            groupContact.setCreatedTimeStamp(new Date());
        }
        if (clientContact.getCreatedTimeStamp() == null) {
            clientContact.setCreatedTimeStamp(new Date());
        }

        try {
            // update membership in database
            TalkGroupMembership dbMembership = mDatabase.findMembershipInGroupByClientId(membership.getGroupId(), membership.getClientId());

            if (dbMembership != null) {
                dbMembership.updateWith(membership);
            } else {
                dbMembership = membership;
            }

            mDatabase.saveGroupMembership(dbMembership);

            // if this concerns our own membership
            if (clientContact.isSelf()) {
                LOG.info("groupMember is about us, decrypting group key");
                groupContact.updateGroupMembership(dbMembership);
                decryptGroupKey(groupContact, membership);

                mDatabase.saveContact(groupContact);

                // quietly destroy nearby group
                if (!membership.isInvolved()) {
                    TalkGroupPresence groupPresence = groupContact.getGroupPresence();
                    if (groupPresence != null && groupPresence.isTypeNearby()) {
                        destroyNearbyGroup(groupContact);
                    }
                }
            }
            // if this concerns the membership of someone else
            if (clientContact.isClient()) {
                /* Mark as nearby contact and save to database. */
                boolean isJoinedInNearbyGroup = groupContact.getGroupPresence() != null && groupContact.getGroupPresence().isTypeNearby() && membership.isJoined();
                clientContact.setNearby(isJoinedInNearbyGroup);

                mDatabase.saveContact(clientContact);
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }

        for (IXoContactListener listener : mContactListeners) {
            listener.onGroupMembershipChanged(groupContact);
        }
    }

    private void decryptGroupKey(TalkClientContact group, TalkGroupMembership membership) {
        LOG.debug("decrypting group key");
        String keyId = membership.getMemberKeyId();
        String encryptedGroupKey = membership.getEncryptedGroupKey();
        if (keyId == null || encryptedGroupKey == null) {
            LOG.info("can't decrypt group key because there isn't one yet");
            return;
        }
        try {
            TalkPrivateKey talkPrivateKey = mDatabase.findPrivateKeyByKeyId(keyId);
            if (talkPrivateKey == null) {
                LOG.error("no private key for keyId " + keyId);
            } else {
                PrivateKey privateKey = talkPrivateKey.getAsNative();
                if (privateKey == null) {
                    LOG.error("could not decode private key");
                } else {
                    byte[] rawEncryptedGroupKey = Base64.decodeBase64(encryptedGroupKey.getBytes(Charset.forName("UTF-8")));
                    byte[] rawGroupKey = RSACryptor.decryptRSA(privateKey, rawEncryptedGroupKey);
                    LOG.debug("successfully decrypted group key");
                    String groupKey = new String(Base64.encodeBase64(rawGroupKey));
                    group.setGroupKey(groupKey);
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        } catch (GeneralSecurityException e) {
            LOG.error("error decrypting group key", e);
        }
    }

    private void generateGroupKey(TalkClientContact group) {
        try {
            // generate the new key
            byte[] newGroupKey = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            byte[] sharedKeyIdSalt = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            String sharedKeyIdSaltString = new String(Base64.encodeBase64(sharedKeyIdSalt));
            byte[] sharedKeyId = AESCryptor.calcSymmetricKeyId(newGroupKey, sharedKeyIdSalt);
            String sharedKeyIdString = new String(Base64.encodeBase64(sharedKeyId));

            // remember the group key for ourselves
            group.setGroupKey(new String(Base64.encodeBase64(newGroupKey)));
            group.getGroupPresence().setSharedKeyIdSalt(sharedKeyIdSaltString);
            group.getGroupPresence().setSharedKeyId(sharedKeyIdString);

            mDatabase.saveContact(group);

        } catch (NoSuchAlgorithmException e) {
            LOG.error("failed to generate new group key, bad crypto provider or export restricted java security settings", e);
        } catch (SQLException e) {
            LOG.error("sql error saving group contact after key generation", e);
        }
    }

    public void requestDownload(TalkClientDownload download, boolean forcedDownload) {
        mTransferAgent.startOrRestartDownload(download, forcedDownload);
    }

    public void pauseDownload(TalkClientDownload download) {
        mTransferAgent.pauseDownload(download);
    }

    public void markAsSeen(final TalkClientMessage message) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                message.markAsSeen();

                if (mClientConfiguration.isSendDeliveryConfirmationEnabled()) {
                    try {
                        mServerRpc.inDeliveryConfirmSeen(message.getMessageId());
                    } catch (Exception e) {
                        LOG.error("Error while sending delivery confirmation: ", e);
                    }
                }

                try {
                    mDatabase.saveClientMessage(message);
                } catch (SQLException e) {
                    LOG.error("SQL error", e);
                }

                for (IXoMessageListener listener : mMessageListeners) {
                    listener.onMessageUpdated(message);
                }
            }
        });
    }

    public void register() {
        if (!isRegistered()) {
            if (mState == STATE_REGISTERING) {
                scheduleRegistration();
            } else {
                connect();
            }
        }
    }

    public void markMessageAsAborted(TalkClientMessage message) {
        message.getOutgoingDelivery().setState(TalkDelivery.STATE_ABORTED); // TODO: ABORTED OR ABORTED_ACKNOWLEDGED?
        try {
            mDatabase.saveDelivery(message.getOutgoingDelivery());
        } catch (SQLException e) {
            LOG.error("error while saving a message which will never be sent since the receiver is blocked or the group is empty", e);
        }

        for (IXoMessageListener listener : mMessageListeners) {
            listener.onMessageUpdated(message);
        }
    }

    private void updateUploadDelivery(TalkClientUpload upload, String state) {
        try {
            TalkDelivery delivery = mDatabase.deliveryForUpload(upload);
            if (delivery != null) {
                delivery.setAttachmentState(state);
                mDatabase.saveDelivery(delivery);
            }
        } catch (SQLException e) {
            LOG.error("Error while processing delivery", e);
        }
    }

    private void updateDownloadDelivery(TalkClientDownload download, String state) {
        try {
            TalkDelivery delivery = mDatabase.deliveryForDownload(download);
            if (delivery != null) {
                delivery.setAttachmentState(state);
                mDatabase.saveDelivery(delivery);
            }
        } catch (SQLException e) {
            LOG.error("Error while processing delivery", e);
        }
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {
    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {
    }

    @Override
    public void onDownloadFinished(final TalkClientDownload download) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {


                    if (download.getTransferType() == XoTransfer.Type.ATTACHMENT) {
                        String nextState = mServerRpc.receivedFile(download.getFileId());
                        updateDownloadDelivery(download, nextState);
                    }
                } catch (Exception e) {
                    LOG.error("Error while updating upload state ", e);
                }
            }
        });
    }

    @Override
    public void onDownloadFailed(final TalkClientDownload download) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    if (download.getTransferType() == XoTransfer.Type.ATTACHMENT) {
                        String nextState = mServerRpc.failedFileDownload(download.getFileId());
                        updateDownloadDelivery(download, nextState);
                    }
                } catch (Exception e) {
                    LOG.error("Error while updating upload state ", e);
                }
            }
        });
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
    }

    @Override
    public void onUploadStarted(final TalkClientUpload upload) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (upload.getTransferType() == XoTransfer.Type.ATTACHMENT) {
                        String nextState = mServerRpc.startedFileUpload(upload.getFileId());
                        updateUploadDelivery(upload, nextState);
                    }
                } catch (Exception e) {
                    LOG.error("Error while updating upload state ", e);
                }
            }
        });
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
    }

    @Override
    public void onUploadFinished(final TalkClientUpload upload) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (upload.getTransferType() == XoTransfer.Type.ATTACHMENT) {
                        String nextState = mServerRpc.finishedFileUpload(upload.getFileId());
                        updateUploadDelivery(upload, nextState);
                    }
                } catch (Exception e) {
                    LOG.error("Error while updating upload state ", e);
                }
            }
        });
    }

    @Override
    public void onUploadFailed(final TalkClientUpload upload) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (upload.getTransferType() == XoTransfer.Type.ATTACHMENT) {
                        String nextState = mServerRpc.failedFileUpload(upload.getFileId());
                        updateUploadDelivery(upload, nextState);
                    }
                } catch (Exception e) {
                    LOG.error("Error while updating upload state ", e);
                }
            }
        });
    }

    @Override
    public void onUploadStateChanged(final TalkClientUpload upload) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (upload.getTransferType() == XoTransfer.Type.ATTACHMENT &&
                            upload.getState() == TalkClientUpload.State.PAUSED &&
                            upload.isEncryptionKeySet()) {
                        // if encryption key is not set we have not sent the delivery so the server does not know about the delivery yet.
                        String nextState = mServerRpc.pausedFileUpload(upload.getFileId());
                        updateUploadDelivery(upload, nextState);
                    }
                } catch (Exception e) {
                    LOG.error("Error while updating upload state ", e);
                }
            }
        });
    }

}
