package com.hoccer.talk.client;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.client.JsonRpcClientException;
import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.websocket.JettyWebSocket;
import better.jsonrpc.websocket.JsonRpcWsConnection;
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
import com.j256.ormlite.dao.ForeignCollection;
import de.undercouch.bson4jackson.BsonFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.eclipse.jetty.websocket.WebSocketClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class XoClient implements JsonRpcConnection.Listener, IXoTransferListenerOld {

    private static final Logger LOG = Logger.getLogger(XoClient.class);

    /** State in which the client does not attempt any communication */
    public static final int STATE_INACTIVE = 0;
    /** State in which the client is ready to connect if awakened */
    public static final int STATE_IDLE = 1;
    /** State while establishing connection */
    public static final int STATE_CONNECTING = 2;
    /** State of voluntary reconnect */
    public static final int STATE_RECONNECTING = 3;
    /** State where we register an account */
    public static final int STATE_REGISTERING = 4;
    /** State where we log in to our account */
    public static final int STATE_LOGIN = 5;
    /** State of synchronization after login */
    public static final int STATE_SYNCING = 6;
    /** State while there is an active connection */
    public static final int STATE_ACTIVE = 7;

    /** Digest instance used for SRP auth */
    private final Digest SRP_DIGEST = new SHA256Digest();
    /** RNG used for SRP auth */
    private static final SecureRandom SRP_RANDOM = new SecureRandom();
    /** Constant SRP parameters */
    private static final SRP6Parameters SRP_PARAMETERS = SRP6Parameters.CONSTANTS_1024;

    /** Names of our states for debugging */
    private static final String[] STATE_NAMES = {
            "inactive", "idle", "connecting", "reconnecting", "registering", "login", "syncing", "active"
    };

    private int mUploadLimit = -1;

    private int mDownloadLimit = -1;

    /** Return the name of the given state */
    public static final String stateToString(int state) {
        if(state >= 0 && state < STATE_NAMES.length) {
            return STATE_NAMES[state];
        } else {
            return "INVALID(" + state + ")";
        }
    }

    /** Host of this client */
    IXoClientHost mClientHost;

    IXoClientConfiguration mClientConfiguration;

    /* The database instance we use */
    XoClientDatabase mDatabase;

    /* Our own contact */
    TalkClientContact mSelfContact;

    /** Directory for avatar images */
    String mAvatarDirectory;
    /** Directory for received attachments */
    String mAttachmentDirectory;
    /** Directory for encrypted intermediate uploads */
    String mEncryptedUploadDirectory;
    /** Directory for encrypted intermediate downloads */
    String mEncryptedDownloadDirectory;

    XoTransferAgent mTransferAgent;

    private JettyWebSocket mWebSocket;
    protected JsonRpcWsConnection mConnection;
    TalkRpcClientImpl mHandler;
    ITalkRpcServer mServerRpc;

    /** Executor doing all the heavy network and database work */
    ScheduledExecutorService mExecutor;

    /* Futures keeping track of singleton background operations */
    ScheduledFuture<?> mLoginFuture;
    ScheduledFuture<?> mRegistrationFuture;
    ScheduledFuture<?> mConnectFuture;
    ScheduledFuture<?> mDisconnectFuture;
    ScheduledFuture<?> mAutoDisconnectFuture;
    ScheduledFuture<?> mKeepAliveFuture;

    List<IXoContactListener> mContactListeners = new ArrayList<IXoContactListener>();
    List<IXoMessageListener> mMessageListeners = new ArrayList<IXoMessageListener>();
    List<IXoStateListener> mStateListeners = new ArrayList<IXoStateListener>();
    List<IXoAlertListener> mAlertListeners = new ArrayList<IXoAlertListener>();

    Set<String> mGroupKeyUpdateInProgess = new HashSet<String>();

    /** The current state of this client */
    int mState = STATE_INACTIVE;

    /** Connection retry count for back-off */
    int mConnectionFailures = 0;

    /** Last client activity */
    long mLastActivity = 0;

    int mIdleTimeout = 0;

    ObjectMapper mJsonMapper;

    // temporary group for geolocation grouping
    String mEnvironmentGroupId;
    AtomicBoolean mEnvironmentUpdateCallPending = new AtomicBoolean(false);

    int mRSAKeysize = 1024;

    private long serverTimeDiff = 0;

    private boolean mBackgroundMode = false;

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoClient(IXoClientHost host, IXoClientConfiguration configuration) {
        mClientConfiguration = configuration;
        initialize(host);
    }

    public void initialize(IXoClientHost host) {
        // remember the host
        mClientHost = host;

        mIdleTimeout = mClientConfiguration.getIdleTimeout();

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
        if(mClientConfiguration.getUseBsonProtocol()) {
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

    private void createJsonRpcConnection(ObjectMapper rpcMapper) {
        mWebSocket = new JettyWebSocket();
        mWebSocket.setMaxIdleTime(mClientConfiguration.getConnectionIdleTimeout());
        mConnection = new JsonRpcWsConnection(mWebSocket, rpcMapper);
        mConnection.setSendKeepAlives(mClientConfiguration.getKeepAliveEnabled());

        if(mClientConfiguration.getUseBsonProtocol()) {
            mConnection.setSendBinaryMessages(true);
        }
    }


    private void ensureSelfContact() {
        try {
            mSelfContact = mDatabase.findSelfContact(true);
            if(mSelfContact.initializeSelf()) {
                mDatabase.saveCredentials(mSelfContact.getSelf());
                mDatabase.saveContact(mSelfContact);
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    public boolean isRegistered() {
        return mSelfContact.isSelfRegistered();
    }

    public boolean isBackgroundMode() {
        return mBackgroundMode;
    }

    public void setBackgroundMode(boolean backgroundMode) {
        this.mBackgroundMode = backgroundMode;
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

    public String getAttachmentDirectory() {
        return mAttachmentDirectory;
    }

    public void setAttachmentDirectory(String attachmentDirectory) {
        this.mAttachmentDirectory = attachmentDirectory;
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

    public int getIdleTimeout() {
        return mIdleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        mIdleTimeout = idleTimeout;
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

    public boolean isIdle() {
        if (mIdleTimeout > 0) {
            return (System.currentTimeMillis() - mLastActivity) > (mIdleTimeout * 1000);
        } else {
            return false;
        }
    }

    /**
     * Exports the client credentials.
     */
    public Credentials exportCredentials() {
        // return null if this client has never registered
        if(mSelfContact.getClientId() == null) {
            return null;
        }

        String clientId = mSelfContact.getClientId();
        String password = mSelfContact.getSelf().getSrpSecret();
        String salt = mSelfContact.getSelf().getSrpSalt();
        String clientName = mSelfContact.getName();

        return new Credentials(clientId, password, salt, clientName);
    }

    /**
     * Imports the client credentials.
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

        reconnect("Credentials imported.");
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
     * Returns true if the client has been activated
     *
     * This is only true after an explicit call to activate().
     *
     * @return
     */
    public boolean isActivated() {
        return mState > STATE_INACTIVE;
    }

    /**
     * Returns true if the client is fully active
     *
     * This means that the client is logged in and synced.
     *
     * @return
     */
    public boolean isActive() {
        return mState >= STATE_ACTIVE;
    }
    /**
     * Returns true if the client is logged in
     *     *
     * @return
     */
    public boolean isLoggedIn() {
        return mState >= STATE_SYNCING;
    }

    /**
     * Returns true if the client is awake
     *
     * This means that the client is trying to connect or connected.
     *
     * @return
     */
    public boolean isAwake() {
        return mState > STATE_IDLE;
    }

    /**
     * Activate the client, allowing it do operate
     */
    public void activate() {
        LOG.debug("client: activate()");
        if(mState == STATE_INACTIVE) {
            if(isIdle()) {
                switchState(STATE_IDLE, "client activated idle");
            } else {
                switchState(STATE_CONNECTING, "client activated");
            }
        }
    }

    /**
     * Deactivate the client, shutting it down completely
     */
    public void deactivate() {
        LOG.debug("client: deactivate()");
        if(mState != STATE_INACTIVE) {
            switchState(STATE_INACTIVE, "client deactivated");
        }
    }

    /**
     * Wake the client so it will connect and speak with the server
     */
    public void wake() {
        LOG.debug("client: wake()");
        resetIdle();
        if(mState < STATE_CONNECTING) {
            switchState(STATE_CONNECTING, "client woken");
        }
    }

    public void wakeInBackground() {
        LOG.debug("client: wakeInBackground()");
        mBackgroundMode = true;
        wake();
     }

    /**
     * Reconnect the client immediately
     */
    public void reconnect(final String reason) {
        if(mState > STATE_IDLE) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    switchState(STATE_RECONNECTING, "reconnect: " + reason);
                }
            });
        }
    }

    /**
     * Blocking version of the deactivation call
     */
    public void deactivateNow() {
        LOG.debug("client: deactivateNow()");
        mAutoDisconnectFuture.cancel(true);
        mLoginFuture.cancel(true);
        mConnectFuture.cancel(true);
        mDisconnectFuture.cancel(true);
        mState = STATE_INACTIVE;
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
                LOG.info("Hello: client time differs from server time by "+this.serverTimeDiff+" ms");
                LOG.debug("Hello: Current server time: " + talkServerInfo.getServerTime().toString());
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
     * @param packageName
     * @param registrationId
     */
    public void registerGcm(final String packageName, final String registrationId) {
        resetIdle();
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
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.unregisterGcm();
            }
        });
    }

    public void setClientString(String newName, String newStatus) {
        resetIdle();
        try {
            ensureSelfContact();
            TalkClientSelf self = mSelfContact.getSelf();
            if (newName != null) {
                self.setRegistrationName(newName);
                self.confirmRegistration();
            }
            mDatabase.saveCredentials(self);

            TalkPresence presence = mSelfContact.getClientPresence();
            if(presence != null) {
                if(newName != null) {
                    presence.setClientName(newName);
                }
                if(newStatus != null) {
                    presence.setClientStatus(newStatus);
                }
                mDatabase.savePresence(presence);

                for (int i = 0; i < mContactListeners.size(); i++) {
                    IXoContactListener listener = mContactListeners.get(i);
                    listener.onClientPresenceChanged(mSelfContact);
                }

                if (isLoggedIn())  {
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
                        LOG.debug("entering foreground -> idle timer deactivated");
                        //LOG.debug("stacktrace", new Exception());
                        mBackgroundMode = false;
                        shutdownIdle();
                    } else if (TalkPresence.CONN_STATUS_BACKGROUND.equals(newStatus)) {
                        mBackgroundMode = true;
                        LOG.debug("entering background -> idle timer activated");
                        scheduleIdle();
                    }

                    for (int i = 0; i < mContactListeners.size(); i++) {
                        IXoContactListener listener = mContactListeners.get(i);
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
        resetIdle();
        if(upload != null) {
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
                for (int i = 0; i < mContactListeners.size(); i++) {
                    IXoContactListener listener = mContactListeners.get(i);
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
               TalkGroup presence = group.getGroupPresence();
               if (presence == null) {
                   LOG.error("group has no presence");
                   return;
               }
               presence.setGroupName(groupName);
               if (group.isGroupRegistered()) {
                   try {
                       mDatabase.saveGroup(presence);
                       mDatabase.saveContact(group);
                       LOG.debug("sending new group presence");
                       mServerRpc.updateGroup(presence);
                   } catch (SQLException e) {
                       LOG.error("sql error", e);
                   } catch (JsonRpcClientException e) {
                       LOG.error("Error while sending new group presence: " , e);
                   }
               }
               for (int i = 0; i < mContactListeners.size(); i++) {
                   IXoContactListener listener = mContactListeners.get(i);
                   listener.onGroupPresenceChanged(group);
               }
           }
       });
    }

    /*
    * If upload is null no avatar is set.
    */
    public void setGroupAvatar(final TalkClientContact group, final TalkClientUpload upload) {
        resetIdle();
        if(upload != null) {
            LOG.debug("new group avatar as upload " + upload);
            mTransferAgent.startOrRestartUpload(upload);
        }
        sendGroupPresenceUpdateWithNewAvatar(group, upload);
    }

    private void sendGroupPresenceUpdateWithNewAvatar(final TalkClientContact group, final TalkClientUpload upload) {
        try {
            TalkGroup presence = group.getGroupPresence();
            if (presence != null) {
                if (upload != null) {
                    String downloadUrl = upload.getDownloadUrl();
                    presence.setGroupAvatarUrl(downloadUrl);
                } else {
                    presence.setGroupAvatarUrl(null);
                }

                group.setAvatarUpload(upload);
                if (group.isGroupRegistered()) {
                    mDatabase.saveGroup(presence);
                    mDatabase.saveContact(group);
                    mServerRpc.updateGroup(presence);
                    for (int i = 0; i < mContactListeners.size(); i++) {
                        IXoContactListener listener = mContactListeners.get(i);
                        listener.onGroupPresenceChanged(group);
                    }
                }
            }
        } catch(Exception e){
            LOG.error("error creating group avatar", e);
        }
    }

    public String generatePairingToken() {
        resetIdle();
        final int tokenLifetime = 7 * 24 * 3600;  // valid for one week
        final int maxTokenUseCount = 50;   // good to invite 50 people with same token

        try {
            String token = mServerRpc.generatePairingToken(maxTokenUseCount, tokenLifetime);
            LOG.debug("got pairing token " + token);
            return token;
        }  catch (JsonRpcClientException e) {
            LOG.error("Error while generating pairing token: ", e);
        }
        return null;
    }

    public void performTokenPairing(final String token) {
        performTokenPairing(token, null);
    }

    public void performTokenPairing(final String token, final IXoPairingListener listener) {
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = mServerRpc.pairByToken(token);

                    if(listener != null) {
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
        resetIdle();
        if(contact.isClient() || contact.isGroup()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        contact.markAsDeleted();

                        try {
                            mDatabase.saveContact(contact);
                        } catch (SQLException e) {
                            LOG.error("SQL error", e);
                        }

                        for (int i = 0; i < mContactListeners.size(); i++) {
                            IXoContactListener listener = mContactListeners.get(i);
                            listener.onContactRemoved(contact);
                        }

                        if (contact.isClient() && contact.isClientRelated()) {
                            mServerRpc.depairClient(contact.getClientId());
                        }

                        if (contact.isGroup()) {
                            if (contact.isGroupJoined() && !(contact.isGroupExisting() && contact.isGroupAdmin())) {
                                mServerRpc.leaveGroup(contact.getGroupId());
                            }
                            if (contact.isGroupExisting() && contact.isGroupAdmin()) {
                                mServerRpc.deleteGroup(contact.getGroupId());
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
        resetIdle();
        if(contact.isClient()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mServerRpc.blockClient(contact.getClientId());
                }
            });
        }
    }

    public void unblockContact(final TalkClientContact contact) {
        resetIdle();
        if(contact.isClient()) {
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

    public void createGroup(final TalkClientContact groupContact) {
        LOG.debug("createGroup()");

        TalkGroup groupPresence = groupContact.getGroupPresence();
        if (groupPresence == null) {
            LOG.error("Can not create group since groupPresence is null.");
            return;
        }

        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug("creating group");
                    TalkGroup groupPresence = groupContact.getGroupPresence();
                    TalkClientUpload avatarUpload = groupContact.getAvatarUpload();

                    groupContact.setAvatarUpload(null);

                    TalkGroupMember member = new TalkGroupMember();
                    member.setClientId(mSelfContact.getClientId());
                    member.setRole(TalkGroupMember.ROLE_ADMIN);
                    member.setState(TalkGroupMember.STATE_JOINED);
                    member.setMemberKeyId(mSelfContact.getPublicKey().getKeyId()); // TODO: make sure all members are properly updated when the public key changes
                    groupContact.updateGroupMember(member);

                    generateGroupKey(groupContact);
                    LOG.debug("creating group on server");
                    String groupId = mServerRpc.createGroup(groupPresence);

                    if(groupId == null) {
                        return;
                    }

                    groupPresence.setGroupId(groupId);            // was null
                    groupPresence.setState(TalkGroup.STATE_NONE); // was null
                    member.setGroupId(groupId);
                    groupContact.updateGroupId(groupId);
                    groupContact.updateGroupPresence(groupPresence);   // was missing
                    groupContact.setCreatedTimeStamp(new Date());

                    try {
                        mDatabase.saveGroupMember(member);
                        mDatabase.saveGroup(groupPresence);
                        mDatabase.saveContact(groupContact);
                        TalkClientMembership membership = mDatabase.findMembershipByContacts(
                                groupContact.getClientContactId(), mSelfContact.getClientContactId(), true);
                        membership.updateGroupMember(member);
                        mDatabase.saveClientMembership(membership);
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }

                    LOG.debug("new group contact " + groupContact.getClientContactId());

                    for (int i = 0; i < mContactListeners.size(); i++) {
                        IXoContactListener listener = mContactListeners.get(i);
                        listener.onContactAdded(groupContact);
                    }

                    if(avatarUpload != null) {
                        setGroupAvatar(groupContact, avatarUpload);
                    }

                    // start of error checking section, remove when all works
                    TalkClientMembership membership = null;
                    try {
                        LOG.debug("createGroup: looking for membership for group="+groupContact.getClientContactId()+" client="+mSelfContact.getClientContactId());
                        membership = mDatabase.findMembershipByContacts(
                                groupContact.getClientContactId(), mSelfContact.getClientContactId(), false);
                        if (membership == null) {
                            LOG.error("createGroup: not found: membership for group="+groupContact.getClientContactId()+" client="+mSelfContact.getClientContactId());
                        }
                        // just for error checking purposes, the following condition should never be true
                        if (membership != null && (membership.getGroupContact().getContactType() == null || membership.getClientContact().getContactType() == null)) {
                            LOG.error("createGroup: defective membership for group="+groupContact.getClientContactId()+" client="+mSelfContact.getClientContactId());
                        }
                    } catch (SQLException e) {
                        LOG.error("SQL error: ", e);
                    }
                    // end of error checking section

                } catch (JsonRpcClientException e) {
                    LOG.error("JSON RPC error while creating group: ", e);
                } catch (Exception e) {
                    LOG.error("Error while creating group: ", e);
                }
            }
        });
    }

    public void createGroupWithContacts(final TalkClientContact groupContact, final String[] members, final String[] roles) {
        LOG.debug("createGroupWithContacts()");
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.debug("creating group");
                    TalkGroup groupPresence = groupContact.getGroupPresence();
                    TalkClientUpload avatarUpload = groupContact.getAvatarUpload();
                    groupContact.setAvatarUpload(null);

                    TalkGroupMember member = new TalkGroupMember();
                    member.setClientId(mSelfContact.getClientId());
                    member.setRole(TalkGroupMember.ROLE_ADMIN);
                    member.setState(TalkGroupMember.STATE_JOINED);
                    member.setMemberKeyId(mSelfContact.getPublicKey().getKeyId()); // TODO: make sure all members are properly updated when the public key changes
                    groupContact.updateGroupMember(member);
                    generateGroupKey(groupContact);
                    LOG.debug("creating group on server");

                    String tag = groupContact.getGroupTag();
                    String name = groupContact.getName();
                    TalkGroup createdGroup = mServerRpc.createGroupWithMembers(TalkGroup.GROUP_TYPE_USER, tag,
                            name, members, roles);

                    if(createdGroup == null) {
                        return;
                    }

                    groupPresence.setGroupId(createdGroup.getGroupId());
                    groupPresence.setState(TalkGroup.STATE_NONE);
                    member.setGroupId(createdGroup.getGroupId());
                    groupContact.updateGroupId(createdGroup.getGroupId());
                    groupContact.updateGroupPresence(groupPresence);
                    groupContact.setCreatedTimeStamp(new Date());

                    try {
                        mDatabase.saveGroupMember(member);
                        mDatabase.saveGroup(groupPresence);
                        mDatabase.saveContact(groupContact);
                        TalkClientMembership membership = mDatabase.findMembershipByContacts(
                                groupContact.getClientContactId(), mSelfContact.getClientContactId(), true);
                        membership.updateGroupMember(member);
                        mDatabase.saveClientMembership(membership);
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }

                    LOG.debug("new group contact " + groupContact.getClientContactId());

                    for (int i = 0; i < mContactListeners.size(); i++) {
                        IXoContactListener listener = mContactListeners.get(i);
                        listener.onContactAdded(groupContact);
                    }

                    if(avatarUpload != null) {
                        setGroupAvatar(groupContact, avatarUpload);
                    }

                    // start of error checking section, remove when all works
                    TalkClientMembership membership = null;
                    try {
                        LOG.error("createGroup: looking for membership for group="+groupContact.getClientContactId()+" client="+mSelfContact.getClientContactId());
                        membership = mDatabase.findMembershipByContacts(
                                groupContact.getClientContactId(), mSelfContact.getClientContactId(), false);
                        if (membership == null) {
                            LOG.error("createGroup: not found: membership for group="+groupContact.getClientContactId()+" client="+mSelfContact.getClientContactId());
                        }
                        // just for error checking purposes, the following condition should never be true
                        if (membership != null && (membership.getGroupContact().getContactType() == null || membership.getClientContact().getContactType() == null)) {
                            LOG.error("createGroup: defective membership for group="+groupContact.getClientContactId()+" client="+mSelfContact.getClientContactId());
                        }
                    } catch (SQLException e) {
                        LOG.error("SQL error: ", e);
                    }
                    // end of error checking section

                } catch (JsonRpcClientException e) {
                    LOG.error("JSON RPC error while creating group: ", e);
                }  catch (Exception e) {
                    LOG.error("Error while creating group: ", e);
                }
            }
        });
    }

    public void inviteClientToGroup(final String groupId, final String clientId) {
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerRpc.inviteGroupMember(groupId, clientId);
                }  catch (JsonRpcClientException e) {
                    LOG.error("Error while sending group invitation: " , e);
                }
            }
        });
    }

    public void kickClientFromGroup(final String groupId, final String clientId) {
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.removeGroupMember(groupId, clientId);
            }
        });
    }

    public void joinGroup(final String groupId) {
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.joinGroup(groupId);
            }
        });
    }

    public void leaveGroup(final String groupId) {
        resetIdle();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mServerRpc.leaveGroup(groupId);
            }
        });
    }

    //TODO: messages which fail will be sent after the next sync
    public void sendMessage(String talkMessageTag) {
        try {
            TalkClientMessage message = mDatabase.findMessageByMessageTag(talkMessageTag, false);
            for(IXoMessageListener listener: mMessageListeners) {
                listener.onMessageCreated(message);
            }
            if(TalkDelivery.STATE_NEW.equals(message.getOutgoingDelivery().getState())) {
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
            TalkClientMessage message = getDatabase().findMessageById(messageId);
            deleteMessage(message);
        } catch (SQLException e) {
            LOG.error("SQL Error while deleting message with id: " + messageId, e);
        }
    }

    public void deleteMessage(TalkClientMessage message) {
        try {
            getDatabase().deleteMessageById(message.getClientMessageId());
            for(IXoMessageListener listener: mMessageListeners) {
                listener.onMessageDeleted(message);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while deleting message with id: " + message.getClientMessageId(), e);
        }
    }

    protected void requestDelivery(final TalkClientMessage message) {
        if (mState < STATE_ACTIVE) {
            LOG.info("requestSendAllPendingMessages() - cannot perform delivery in INACTIVE state.");
            return;
        }

        resetIdle();
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
        if (mState < STATE_ACTIVE) {
            LOG.info("requestSendAllPendingMessages() - cannot perform delivery in INACTIVE state.");
            return;
        }

        resetIdle();
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

    private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
        ObjectMapper result = new ObjectMapper(jsonFactory);
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return result;
    }

    private void switchState(int newState, String message) {
        // only switch of there really was a change
        if(mState == newState) {
            LOG.debug("state remains " + STATE_NAMES[mState] + " (" + message + ")");
        }

        // log about it
        LOG.info("[switchState: connection #" + mConnection.getConnectionId() + "] state " + STATE_NAMES[mState] + " -> " + STATE_NAMES[newState] + " (" + message + ")");

        // perform transition
        int previousState = mState;
        mState = newState;

        // maintain keep-alives timer
        if(mState >= STATE_SYNCING) {
            scheduleKeepAlive();
        } else {
            shutdownKeepAlive();
        }

        // make disconnects happen
        if(mState == STATE_IDLE || mState == STATE_INACTIVE) {
            scheduleDisconnect();
        } else {
            shutdownDisconnect();
        }

        // make connects happen
        if(mState == STATE_RECONNECTING) {
            LOG.info("[connection #" + mConnection.getConnectionId() + "] scheduling requested reconnect");
            mConnectionFailures = 0;
            scheduleConnect(true);
            resetIdle();
        } else if(mState == STATE_CONNECTING) {
            if(previousState <= STATE_IDLE) {
                LOG.info("[connection #" + mConnection.getConnectionId() + "] scheduling connect");
                // initial connect
                mConnectionFailures = 0;
                scheduleConnect(false);
            } else {
                LOG.info("[connection #" + mConnection.getConnectionId() + "] scheduling reconnect");
                // reconnect
                mConnectionFailures++;
                scheduleConnect(true);
            }
        } else {
            shutdownConnect();
        }

        if(mState == STATE_REGISTERING) {
            scheduleRegistration();
        } else {
            shutdownRegistration();
        }

        if(mState == STATE_LOGIN) {
            scheduleLogin();
        } else {
            shutdownLogin();
        }

        if(mState == STATE_SYNCING) {
            scheduleSync();
        }

        if(mState == STATE_ACTIVE) {
            mConnectionFailures = 0;
            // start talking
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

        // call listeners
        if(previousState != newState) {
            for(IXoStateListener listener: mStateListeners) {
                listener.onClientStateChange(this, newState);
            }
        }
    }

    private void handleDisconnect() {
        LOG.debug("handleDisconnect()");
        switch(mState) {
            case STATE_INACTIVE:
            case STATE_IDLE:
                LOG.debug("supposed to be disconnected, state="+stateToString(mState));
                // we are supposed to be disconnected, things are fine
                break;
            case STATE_CONNECTING:
            case STATE_RECONNECTING:
            case STATE_REGISTERING:
            case STATE_LOGIN:
            case STATE_SYNCING:
            case STATE_ACTIVE:
                LOG.debug("supposed to be connected - scheduling connect, state="+stateToString(mState));
                switchState(STATE_CONNECTING, "disconnected while active");
                break;
            default:
                LOG.error("illegal state=" + stateToString(mState));
                break;
        }
    }

    /**
     * Called when the connection is opened
     * @param connection
     */
    @Override
    public void onOpen(JsonRpcConnection connection) {
        LOG.debug("onOpen()");
        if(isRegistered()) {
            switchState(STATE_LOGIN, "connected");
        } else {
            switchState(STATE_REGISTERING, "connected and unregistered");
        }
    }

    /**
     * Called when the connection is closed
     * @param connection
     */
    @Override
    public void onClose(JsonRpcConnection connection) {
        LOG.debug("onClose()");
        shutdownIdle();
        handleDisconnect();
    }

    private void doConnect() {
        LOG.debug("performing connect on connection #" + mConnection.getConnectionId());
        try {
            WebSocketClient client = mClientHost.getWebSocketFactory().newWebSocketClient();
            URI uri = new URI(mClientConfiguration.getServerUri());
            String protocol = mClientConfiguration.getUseBsonProtocol()
                    ? mClientConfiguration.getBsonProtocolString()
                    : mClientConfiguration.getJsonProtocolString();

            mWebSocket.open(client, uri, protocol, mClientConfiguration.getConnectTimeout(), TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.warn("[connection #" + mConnection.getConnectionId() + "] exception while connecting: ", e);
        }
    }

    private void doDisconnect() {
        LOG.debug("performing disconnect");
        mConnection.disconnect();
    }

    private void shutdownIdle() {
        if (mAutoDisconnectFuture != null) {
            mAutoDisconnectFuture.cancel(false);
            mAutoDisconnectFuture = null;
        }
    }

    private void scheduleIdle() {
        shutdownIdle();
        if(mState > STATE_CONNECTING && mIdleTimeout > 0) {
            mAutoDisconnectFuture = mExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    switchState(STATE_IDLE, "activity timeout");
                    mAutoDisconnectFuture = null;
                }
            }, mIdleTimeout, TimeUnit.SECONDS);
        }
    }

    private void resetIdle() {
        LOG.debug("resetIdle()");
        if (mAutoDisconnectFuture != null) {
            mLastActivity = System.currentTimeMillis();
            scheduleIdle();
        }
    }

    private void shutdownKeepAlive() {
        if(mKeepAliveFuture != null) {
            mKeepAliveFuture.cancel(false);
            mKeepAliveFuture = null;
        }
    }

    private void scheduleKeepAlive() {
        shutdownKeepAlive();
        if(mClientConfiguration.getKeepAliveEnabled()) {
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

    private void shutdownConnect() {
        if(mConnectFuture != null) {
            mConnectFuture.cancel(false);
            mConnectFuture = null;
        }
    }

    private void scheduleConnect(boolean isReconnect) {
        LOG.debug("scheduleConnect()");
        shutdownConnect();

        int backoffDelay = 0;

        if(isReconnect) {
            // compute the backoff factor
            int variableFactor = 1 << mConnectionFailures;

            // compute variable backoff component
            double variableTime =
                    Math.random() * Math.min(
                            mClientConfiguration.getReconnectBackoffVariableMaximum(),
                            variableFactor * mClientConfiguration.getReconnectBackoffVariableFactor());

            // compute total backoff
            double totalTime = mClientConfiguration.getReconnectBackoffFixedDelay() + variableTime;

            // convert to msecs
            backoffDelay = (int) Math.round(1000.0 * totalTime);

            LOG.debug("connection attempt backed off by " + totalTime + " seconds");
        }

        // schedule the attempt
        mConnectFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                doConnect();
                mConnectFuture = null;
            }
        }, backoffDelay, TimeUnit.MILLISECONDS);
    }

    private void shutdownLogin() {
        if(mLoginFuture != null) {
            mLoginFuture.cancel(false);
            mLoginFuture = null;
        }
    }

    private void scheduleLogin() {
        LOG.debug("scheduleLogin()");
        shutdownLogin();
        mLoginFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                performLogin(mSelfContact);
                mLoginFuture = null;
                switchState(STATE_SYNCING, "login successful");
            }
        }, 0, TimeUnit.SECONDS);
    }

    private void shutdownRegistration() {
        if(mRegistrationFuture != null) {
            mRegistrationFuture.cancel(false);
            mRegistrationFuture = null;
        }
    }

    private void scheduleRegistration() {
        LOG.debug("scheduleRegistration()");
        shutdownRegistration();
        mRegistrationFuture = mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    performRegistration(mSelfContact);
                    mRegistrationFuture = null;
                    switchState(STATE_LOGIN, "registered");
                } catch (Exception e) {
                    LOG.error("registration error", e);
                }
            }
        }, 0, TimeUnit.SECONDS);
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
                    LOG.debug("sync: updating presence");
                    if (mBackgroundMode) {
                        setClientConnectionStatus(TalkPresence.CONN_STATUS_BACKGROUND);
                    } else {
                        setClientConnectionStatus(TalkPresence.CONN_STATUS_ONLINE);
                    }
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
                    TalkGroup[] groups = mServerRpc.getGroups(never);
                    for (TalkGroup group : groups) {
                        if (group.getState().equals(TalkGroup.STATE_EXISTS)) {
                            updateGroupPresence(group);
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
                    if (groupIds.size() > 0) {
                        Boolean[] groupMembershipFlags = mServerRpc.isMemberInGroups(groupIds.toArray(new String[groupIds.size()]));

                        for (int i = 0; i < groupContacts.size(); i++) {
                            TalkClientContact groupContact = groupContacts.get(i);
                            try {
                                LOG.debug("sync: membership in group (" + groupContact.getGroupId() + ") : '" + groupMembershipFlags[i] + "'");

                                if (groupMembershipFlags[i]) {
                                    TalkGroupMember[] members = mServerRpc.getGroupMembers(groupContact.getGroupId(), never);
                                    for (TalkGroupMember member : members) {
                                        updateGroupMember(member);
                                    }
                                } else {
                                    // TODO: properly handle group deletion, the following code just marks the group and members as deleted
                                    LOG.info("Removing members and group with name="+ groupContact.getNickname());
                                    TalkGroup groupPresence = groupContact.getGroupPresence();
                                    if (groupPresence != null) {
                                        groupPresence.setState(TalkGroup.STATE_NONE);
                                        mDatabase.saveGroup(groupPresence);
                                    }
                                    ForeignCollection<TalkClientMembership> memberships = groupContact.getGroupMemberships();
                                    for (TalkClientMembership tcm : memberships) {
                                        TalkGroupMember member = tcm.getMember();
                                        if (member != null) {
                                            member.setState(TalkGroupMember.STATE_GROUP_REMOVED);
                                            mDatabase.saveGroupMember(member);
                                        }
                                    }
                                    for (int j = 0; j < mContactListeners.size(); j++) {
                                        IXoContactListener listener = mContactListeners.get(j);
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
                    // ensure we are finished with generating pub/private keys before actually going active...
                    // TODO: have a proper statemachine
                    sendPresenceFuture.get();

                    switchState(STATE_ACTIVE, "Synchronization successfull");

                } catch (SQLException e) {
                    LOG.error("SQL Error while syncing: ", e);
                } catch (JsonRpcClientException e) {
                    LOG.error("Error while syncing: ", e);
                } catch (InterruptedException e) {
                    LOG.error("Error while asserting future", e);
                } catch (ExecutionException e) {
                    LOG.error("ExecutionException ", e);
                }

                if (!isActive()) {
                    LOG.warn("sync failed, scheduling new sync");
                    scheduleSync();
                }
            }
        });
    }

    private void shutdownDisconnect() {
        if(mDisconnectFuture != null) {
            mDisconnectFuture.cancel(false);
            mDisconnectFuture = null;
        }
    }

    private void scheduleDisconnect() {
        LOG.debug("scheduleDisconnect()");
        shutdownDisconnect();
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

    public TalkClientMessage composeClientMessage(TalkClientContact contact, String messageText) {
        return composeClientMessage(contact, messageText, null);
    }

    public List<TalkClientMessage> composeClientMessageWithMultipleAttachments(TalkClientContact contact, String messageText, List<TalkClientUpload> uploads) {

        ArrayList<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
        if (messageText != null && !messageText.equals("")) {
            messages.add(composeClientMessage(contact, messageText, null));
        }

        for (TalkClientUpload upload : uploads) {
            messages.add(composeClientMessage(contact, "", upload));
        }
        return messages;
    }

    public TalkClientMessage composeClientMessage(TalkClientContact contact, String messageText, TalkClientUpload upload) {
        XoClientDatabase db = getDatabase();
        // construct message and delivery objects
        final TalkClientMessage clientMessage = new TalkClientMessage();
        final TalkMessage message = new TalkMessage();
        final TalkDelivery delivery = new TalkDelivery(true);

        final String messageTag = message.generateMessageTag();
//        message.setBody(messageText);
        message.setSenderId(getSelfContact().getClientId());

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
        clientMessage.setSenderContact(getSelfContact());
        clientMessage.setMessage(message);
        clientMessage.setOutgoingDelivery(delivery);

        if (upload != null) {
            clientMessage.setAttachmentUpload(upload);
        }

        try {
            if (upload != null) {
                db.saveClientUpload(upload);
            }
            db.saveMessage(message);
            db.saveDelivery(delivery);
            db.saveClientMessage(clientMessage);
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }

        // log to help debugging
        LOG.debug("created message with id " + clientMessage.getClientMessageId() + " and tag " + message.getMessageTag());

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

            TalkClientContact groupContact = null;
            boolean isRenewGroupKey = false;

            try {
                groupContact = mDatabase.findContactByGroupId(groupId, false);
            } catch (SQLException e) {
                LOG.error("Error while retrieving group contact from id: " + groupId, e);
            }

            if (groupContact == null) {
                return new String[0];
            }

            if (sharedKeyId.equalsIgnoreCase("renew")) {
                generateGroupKey(groupContact);
                isRenewGroupKey = true;
            }

            // here we will have a valid group key

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
                return new String[0];
            }

            // encrypt group key with each member's public key
            // TODO: if there is no groupkey to encrypt, create one!
            // generateGroupKey();
            byte[] rawGroupKey = Base64.decodeBase64(groupContact.getGroupKey().getBytes(Charset.forName("UTF-8")));
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
                return new String[0];
            }

            if (isRenewGroupKey) {
                encryptedGroupKeys.add(groupContact.getGroupPresence().getSharedKeyId());
                encryptedGroupKeys.add(groupContact.getGroupPresence().getSharedKeyIdSalt());
            }

            String[] allKeys = encryptedGroupKeys.toArray(new String[encryptedGroupKeys.size()]);
            return allKeys;
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
            // XXX
            //for(ITalkClientListener listener: mListeners) {
            //    listener.onPushRegistrationRequested();
            //}
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
        public void groupUpdated(TalkGroup group) {
            LOG.debug("server: groupUpdated(" + group.getGroupId() + ")");
            updateGroupPresence(group);
        }

        @Override
        public void groupMemberUpdated(TalkGroupMember member) {
            LOG.debug("server: groupMemberUpdated(" + member.getGroupId() + "/" + member.getClientId() + ")");
            updateGroupMember(member);
        }

    }

    private void performRegistration(TalkClientContact selfContact) {
        LOG.debug("registration: attempting registration");

        Digest digest = SRP_DIGEST;
        byte[] salt = new byte[digest.getDigestSize()];
        byte[] secret = new byte[digest.getDigestSize()];
        SRP6VerifierGenerator vg = new SRP6VerifierGenerator();

        vg.init(SRP_PARAMETERS.N, SRP_PARAMETERS.g, digest);

        SRP_RANDOM.nextBytes(salt);
        SRP_RANDOM.nextBytes(secret);

//        String saltString = Hex.encodeHexString(salt);
//        String secretString = Hex.encodeHexString(secret);
        String saltString = new String(Hex.encodeHex(salt));
        String secretString = new String(Hex.encodeHex(secret));

        try {
            String clientId = mServerRpc.generateId();

            LOG.debug("registration: started with id " + clientId);

            BigInteger verifier = vg.generateVerifier(salt, clientId.getBytes(), secret);

//        mServerRpc.srpRegister(verifier.toString(16), Hex.encodeHexString(salt));
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
                LOG.error("SQL error on performRegistration", e);
            } catch (Exception e) { // TODO: specify exception in XoClientDatabase.savePresence!
                LOG.error("error on performRegistration", e);
            }

        } catch (JsonRpcClientException e) {
            LOG.error("Error while performing registration: ", e);
        }

    }

    private void performLogin(TalkClientContact selfContact) {
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

            String Bs = mServerRpc.srpPhase1(clientId,  A.toString(16));
            vc.calculateSecret(new BigInteger(Bs, 16));

            LOG.debug("login: performing phase 2");

//            String Vc = Hex.encodeHexString(vc.calculateVerifier());
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

    private void performDeliveries(final List<TalkClientMessage> clientMessages) {
        LOG.debug("performDeliveries()");
        LOG.debug(clientMessages.size() + " messages to deliver");

        for (int i = 0; i < clientMessages.size(); i++) {
            final TalkClientMessage clientMessage = clientMessages.get(i);
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
                if(upload != null) {
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

        int length = resultingDeliveries.length;
        for(int j = 0; j < length; j++) {
            updateOutgoingDelivery(resultingDeliveries[j]);
        }
    }

    private TalkPresence ensureSelfPresence(TalkClientContact contact) throws SQLException {
        try {
            TalkPresence presence = contact.getClientPresence();
            if(presence == null) {
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
        if(publicKey == null || privateKey == null) {
            Date now = new Date();
            try {
                mRSAKeysize = mClientConfiguration.getRSAKeysize();
                LOG.info("[connection #" + mConnection.getConnectionId() + "] generating new RSA keypair with size "+mRSAKeysize);
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
        if (isActive() && environment != null) {
            if (mEnvironmentUpdateCallPending.compareAndSet(false,true)) {

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
            LOG.debug("sendEnvironmentUpdate(): client not yet active or no environment");
        }
    }

    public TalkClientContact getCurrentNearbyGroup() {
        TalkClientContact currentNearbyGroup = null;
        try {
            if (mEnvironmentGroupId != null) {
                currentNearbyGroup = mDatabase.findContactByGroupId(mEnvironmentGroupId, false);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while retrieving current nearby group ", e);
        }
        return currentNearbyGroup;
    }


    public void sendDestroyEnvironment(final String type) {
        if (isActive()) {
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

        TalkClientContact clientContact = null;
        TalkClientContact groupContact = null;
        TalkClientMessage clientMessage = null;
        try {
            String receiverId = delivery.getReceiverId();
            if(receiverId != null) {
                clientContact = mDatabase.findContactByClientId(receiverId, false);
                if(clientContact == null) {
                    LOG.warn("outgoing message for unknown client " + receiverId);
                    return;
                }
            }

            String groupId = delivery.getGroupId();
            if(groupId != null) {
                groupContact = mDatabase.findContactByGroupId(groupId, false);
                if(groupContact == null) {
                    LOG.warn("outgoing message for unknown group " + groupId);
                    //TODO: return; ??
                }
            }

            String messageId = delivery.getMessageId();
            String messageTag = delivery.getMessageTag();
            if(messageTag != null) {
                clientMessage = mDatabase.findMessageByMessageTag(messageTag, false);
            }
            if(clientMessage == null) {
                clientMessage = mDatabase.findMessageByMessageId(messageId, false);
            }
            if(clientMessage == null) {
                LOG.warn("outgoing delivery notification for unknown message id " + messageId + " tag "+messageTag +
                        ", delivery state = "+delivery.getState() + ", group "+groupId+", receiver "+receiverId);
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

        for(IXoMessageListener listener: mMessageListeners) {
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
                    TalkDelivery result =  mServerRpc.outDeliveryAbort(delivery.getMessageId(), delivery.getReceiverId());
                     if (result != null) {
                         LOG.warn("aborted strange delivery for message id "+ delivery.getMessageId()+" receiver "+ delivery.getReceiverId());
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
        TalkClientMessage clientMessage = null;
        try {
            clientMessage = mDatabase.findMessageByMessageId(delivery.getMessageId(), false);
            if(clientMessage == null) {
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
            for(IXoMessageListener listener: mMessageListeners) {
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
        TalkClientContact senderContact = null;
        TalkClientMessage clientMessage = null;
        try {
            String groupId = delivery.getGroupId();
            if(groupId != null) {
                groupContact = mDatabase.findContactByGroupId(groupId, false);
                if(groupContact == null) {
                    LOG.warn("incoming message in unknown group " + groupId);
                    return;
                }
            }
            senderContact = mDatabase.findContactByClientId(delivery.getSenderId(), false);
            if(senderContact == null) {
                LOG.warn("incoming message from unknown client " + delivery.getSenderId());
                return;
            }
            clientMessage = mDatabase.findMessageByMessageId(delivery.getMessageId(), false);
            if(clientMessage == null) {
                newMessage = true;
                clientMessage = mDatabase.findMessageByMessageId(delivery.getMessageId(), true);
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
            return;
        }

        clientMessage.setSenderContact(senderContact);

        if(groupContact == null) {
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

            if(attachmentDownload != null) {
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

            for(IXoMessageListener listener: mMessageListeners) {
                if(newMessage) {
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
            if(delivery.getState().equals(TalkDelivery.STATE_DELIVERING)) {
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
            }  else {
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
        byte[] decryptedKey = null;
        if(contact.isClient()) {
            LOG.trace("decrypting using private key");
            // decrypt the provided key using our private key
            try {
                TalkPrivateKey talkPrivateKey = null;
                talkPrivateKey = mDatabase.findPrivateKeyByKeyId(keyId);
                if(talkPrivateKey == null) {
                    LOG.error("no private key for keyId " + keyId);
                    return;
                } else {
                    PrivateKey privateKey = talkPrivateKey.getAsNative();
                    if(privateKey == null) {
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
        } else if(contact.isGroup()) {
            LOG.trace("decrypting using group key");
            // get the group key for decryption
            String groupKey = contact.getGroupKey();
            if(groupKey == null) {
                LOG.warn("no group key");
                return;
            }
            decryptedKey = Base64.decodeBase64(contact.getGroupKey().getBytes(Charset.forName("UTF-8")));
        } else {
            LOG.error("don't know how to decrypt messages from contact of type " + contact.getContactType());
            throw new RuntimeException("don't know how to decrypt messages from contact of type " + contact.getContactType());
        }

        // check that we have a key
        if(decryptedKey == null) {
            LOG.error("could not determine decryption key");
            return;
        }

        // apply salt if present
        if(keySalt != null) {
            byte[] decodedSalt = Base64.decodeBase64(keySalt.getBytes(Charset.forName("UTF-8")));
            if(decodedSalt.length != decryptedKey.length) {
                LOG.error("message salt has wrong size");
                return;
            }
            for(int i = 0; i < decryptedKey.length; i++) {
                decryptedKey[i] = (byte)(decryptedKey[i] ^ decodedSalt[i]);
            }
        }

        // decrypt both body and attachment dtor
        byte[] decryptedBodyRaw;
        String decryptedBody = "";
        byte[] decryptedAttachmentRaw;
        TalkAttachment decryptedAttachment = null;
        try {
            // decrypt body
            if(rawBody != null) {
                decryptedBodyRaw = AESCryptor.decrypt(decryptedKey, AESCryptor.NULL_SALT, Base64.decodeBase64(rawBody.getBytes(Charset.forName("UTF-8"))));
                decryptedBody = new String(decryptedBodyRaw, "UTF-8");
                //LOG.debug("determined decrypted body to be '" + decryptedBody + "'");
            }
            // decrypt attachment
            if(rawAttachment != null) {
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
        if (decryptedBody != null) {
            clientMessage.setText(decryptedBody);
        }
        if(decryptedAttachment != null) {
            TalkClientDownload download = new TalkClientDownload();
            download.initializeAsAttachment(mTransferAgent, decryptedAttachment, message.getMessageId(), decryptedKey);
            clientMessage.setAttachmentDownload(download);
        }
    }

    private void encryptMessage(TalkClientMessage clientMessage, TalkDelivery delivery, TalkMessage message) {

        LOG.debug("encrypting message with id '" + clientMessage.getClientMessageId() + "'");

        if(message.getBody() != null) {
            LOG.error("message has already body");
            return;
        }

        TalkClientContact receiver = clientMessage.getConversationContact();
        if(receiver == null) {
            LOG.error("no receiver");
            return;
        }

        try {
            receiver = mDatabase.findClientContactById(receiver.getClientContactId());
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        byte[] plainKey = null;
        byte[] keySalt = null;
        if(receiver.isClient()) {
            LOG.trace("using client key for encryption");
            // generate message key
            plainKey = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            // get public key for encrypting the key
            TalkKey talkPublicKey = receiver.getPublicKey();

            if(talkPublicKey == null) {
                throw new RuntimeException("no pubkey for encryption");
            }
            // retrieve native version of the key
            PublicKey publicKey = talkPublicKey.getAsNative();
            if(publicKey == null) {
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
            if(groupKey == null) {
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
        if(keySalt != null) {
            if(keySalt.length != plainKey.length) {
                LOG.error("message salt has wrong size");
                return;
            }
            for(int i = 0; i < plainKey.length; i++) {
                plainKey[i] = (byte)(plainKey[i] ^ keySalt[i]);
            }
        }

        // initialize attachment upload
        TalkAttachment attachment = null;
        TalkClientUpload upload = clientMessage.getAttachmentUpload();
        if(upload != null) {
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
            attachment.setContentSize(Integer.toString(upload.getDataLength()));
            attachment.setMediaType(upload.getMediaType());
            attachment.setMimeType(upload.getContentType());
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
            if(attachment != null) {
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

        boolean isNewContact = false;
        TalkClientContact clientContact;
        try {
            clientContact = mDatabase.findContactByClientId(presence.getClientId(), false);
            if (clientContact == null) {
                clientContact = mDatabase.findContactByClientId(presence.getClientId(), true);
                isNewContact = true;
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        if(clientContact.isSelf()) {
            LOG.warn("server sent self-presence due to group presence bug, ignoring");
            return;
        }

        if(!clientContact.isClient()) {
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
            if (fields == null)  {
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
            if(avatarDownload != null) {
                mDatabase.saveClientDownload(avatarDownload);
            }
            mDatabase.savePresence(clientContact.getClientPresence());
            mDatabase.saveContact(clientContact);
        } catch (Exception e) {
            LOG.error("updateClientPresence", e);
        }
        if(avatarDownload != null && wantDownload) {
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

        if(isNewContact) {
            for (final IXoContactListener listener : mContactListeners) {
                listener.onContactAdded(clientContact);
            }
        }

        for (int i = 0; i < mContactListeners.size(); i++) {
            IXoContactListener listener = mContactListeners.get(i);
            listener.onClientPresenceChanged(clientContact);
        }
    }

    private boolean updateAvatarDownload(TalkClientContact contact, String avatarUrl, String avatarId, Date avatarTimestamp) throws MalformedURLException {
        boolean haveUrl = avatarUrl != null && !avatarUrl.isEmpty();
        if(!haveUrl) {
            LOG.warn("no avatar url for contact " + contact.getClientContactId());
            contact.setAvatarDownload(null);
            return false;
        }

        boolean wantDownload = false;
        TalkClientDownload avatarDownload = contact.getAvatarDownload();
        if(avatarDownload == null) {
            if(haveUrl) {
                LOG.debug("new avatar for contact " + contact.getClientContactId());
                avatarDownload = new TalkClientDownload();
                avatarDownload.initializeAsAvatar(mTransferAgent, avatarUrl, avatarId, avatarTimestamp);
                wantDownload = true;
            }
        } else {
            try {
                mDatabase.refreshClientDownload(avatarDownload);
            } catch (SQLException e) {
                LOG.error("sql error", e);
                return false;
            }
            String downloadUrl = avatarDownload.getDownloadUrl();
            if(haveUrl) {
                if(downloadUrl == null || !downloadUrl.equals(avatarUrl)) {
                    LOG.debug("new avatar for contact " + contact.getClientContactId());
                    avatarDownload = new TalkClientDownload();
                    avatarDownload.initializeAsAvatar(mTransferAgent, avatarUrl, avatarId, avatarTimestamp);
                    wantDownload = true;
                } else {
                    LOG.debug("avatar not changed for contact " + contact.getClientContactId());
                    TalkClientDownload.State state = avatarDownload.getState();
                    if(!state.equals(TalkClientDownload.State.COMPLETE) && !state.equals(TalkClientDownload.State.FAILED)) {
                        wantDownload = true;
                    }
                }
            }
        }

        if(avatarDownload != null) {
            contact.setAvatarDownload(avatarDownload);
        }

        return wantDownload;
    }

    private void updateClientKey(TalkClientContact client) {
        String clientId = client.getClientId();

        String currentKeyId = client.getClientPresence().getKeyId();
        if(currentKeyId == null || currentKeyId.isEmpty()) {
            LOG.warn("client " + clientId + " has no key id");
            return;
        }

        TalkKey clientKey = client.getPublicKey();
        if(clientKey != null) {
            if(clientKey.getKeyId().equals(currentKeyId)) {
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
        TalkClientContact clientContact = null;
        try {
            clientContact = mDatabase.findContactByClientId(relationship.getOtherClientId(), relationship.isRelated());
            if(clientContact == null) {
                clientContact = mDatabase.findDeletedContactByClientId(relationship.getOtherClientId());
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        if(clientContact == null) {
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

    private void updateGroupPresence(TalkGroup group) {
        LOG.info("updateGroupPresence(" + group.getGroupId() + ")");

        TalkClientContact groupContact;
        try {
            groupContact = mDatabase.findContactByGroupTag(group.getGroupTag());
            if(groupContact == null) {
                groupContact = mDatabase.findContactByGroupId(group.getGroupId(), true);
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }

        if(groupContact == null) {
            LOG.warn("gp update for unknown group " + group.getGroupId());
            return;
        }

        // ensure that a timestamp is set, this is a manual migration step for the newly introduced timestamp field
        if (groupContact.getCreatedTimeStamp() == null) {
            groupContact.setCreatedTimeStamp(new Date());
        }

        groupContact.updateGroupPresence(group);

        try {
            updateAvatarDownload(groupContact, group.getGroupAvatarUrl(), "g-" + group.getGroupId(), group.getLastChanged());
        } catch (MalformedURLException e) {
            LOG.warn("Malformed avatar URL", e);
        }

        updateAvatarsForGroupContact(groupContact);

        try {
            mDatabase.saveGroup(groupContact.getGroupPresence());
            mDatabase.saveContact(groupContact);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        // quietly destroy nearby group
        if (group.isTypeNearby() && !group.exists()) {
            destroyNearbyGroup(groupContact);
        }

        LOG.info("updateGroupPresence(" + group.getGroupId() + ") - saved");

        for (int i = 0; i < mContactListeners.size(); i++) {
            IXoContactListener listener = mContactListeners.get(i);
            listener.onGroupPresenceChanged(groupContact);
        }
    }

    private void destroyNearbyGroup(TalkClientContact groupContact) {
        LOG.debug("destroying nearby group with id " + groupContact.getGroupId());

        // reset group state
        TalkGroup groupPresence = groupContact.getGroupPresence();
        if (groupPresence == null) {
            LOG.error("Can not destroy nearby group since groupPresence is null");
            return;
        }

        groupPresence.setState(TalkGroup.STATE_KEPT);

        try {
            // remove all group members
            ForeignCollection<TalkClientMembership> groupMemberships = groupContact.getGroupMemberships();
            if (groupMemberships != null){
                for (TalkClientMembership membership : groupMemberships) {

                    // reset nearby status of group member contact
                    TalkClientContact groupMemberContact = membership.getClientContact();
                    groupMemberContact.setNearby(false);
                    mDatabase.saveContact(groupMemberContact);

                    // reset group membership state
                    TalkGroupMember member = membership.getMember();
                    if (member != null) {
                        member.setState(TalkGroupMember.STATE_NONE);
                        mDatabase.saveGroupMember(member);
                    }
                }

                mDatabase.saveContact(groupContact);
                mDatabase.saveGroup(groupPresence);
            }
        } catch (SQLException e) {
            LOG.error("Error while destroying nearby group " + groupContact.getGroupId());
        }
    }

    private void updateAvatarsForGroupContact(TalkClientContact contact) {
        TalkClientDownload avatarDownload = contact.getAvatarDownload();
        try {
            if(avatarDownload != null) {
                mDatabase.saveClientDownload(avatarDownload);
            }
        } catch (SQLException e) {
            LOG.error("SQL Error when saving avatar download", e);
        }
        if(avatarDownload != null) {
            mTransferAgent.startOrRestartDownload(avatarDownload, true);
        }
    }

    public void updateGroupMember(TalkGroupMember member) {
        LOG.info("updateGroupMember(groupId: '" + member.getGroupId() + "', clientId: '" + member.getClientId() + "', state: '" + member.getState() + "')");
        TalkClientContact groupContact;
        TalkClientContact clientContact;

        try {
            groupContact = mDatabase.findContactByGroupId(member.getGroupId(), false);
            if (groupContact == null) {
                boolean createGroup = member.isInvolved() && !member.isGroupRemoved();
                if (createGroup) {
                    LOG.info("creating group for member in state '" + member.getState() + "' groupId '" + member.getGroupId() + "'");
                    groupContact = mDatabase.findContactByGroupId(member.getGroupId(), true);
                } else {
                    LOG.warn("ignoring incoming member for unknown group for member in state '" + member.getState() + "' groupId '" + member.getGroupId() + "'");
                    return;
                }
            }

            clientContact = mDatabase.findContactByClientId(member.getClientId(), false);
            if (clientContact == null) {
                boolean createContact = member.isInvolved() && !member.isGroupRemoved();
                if (createContact) {
                    LOG.info("creating contact for member in state '" + member.getState() + "' clientId '" + member.getClientId() + "'");
                    clientContact = mDatabase.findContactByClientId(member.getClientId(), true);
                } else {
                    LOG.warn("ignoring incoming member for unknown contact for member in state '" + member.getState() + "' clientId '" + member.getGroupId() + "'");
                    return;
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            return;
        }
        if (clientContact == null) {
            LOG.error("groupMemberUpdate for unknown client: " + member.getClientId());
        }
        if (groupContact == null) {
            LOG.error("groupMemberUpdate for unknown group: " + member.getGroupId());
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

        // if this concerns our own membership
        if (clientContact.isSelf()) {
            LOG.info("groupMember is about us, decrypting group key");
            try {
                groupContact.updateGroupMember(member);
                TalkClientMembership membership = mDatabase.findMembershipByContacts(groupContact.getClientContactId(), clientContact.getClientContactId(), true);
                membership.updateGroupMember(member);

                decryptGroupKey(groupContact, member);

                mDatabase.saveGroupMember(membership.getMember());
                mDatabase.saveContact(groupContact);
                mDatabase.saveClientMembership(membership);

                // quietly destroy nearby group
                if (!member.isInvolved()) {
                    TalkGroup groupPresence = groupContact.getGroupPresence();
                    if (groupPresence != null && groupPresence.isTypeNearby()) {
                        destroyNearbyGroup(groupContact);
                    }
                }

            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
        // if this concerns the membership of someone else
        if (clientContact.isClient()) {
            try {
                TalkClientMembership membership = mDatabase.findMembershipByContacts(
                        groupContact.getClientContactId(), clientContact.getClientContactId(), true);
                TalkGroupMember oldMember = membership.getMember();
                LOG.info("old member " + ((oldMember == null) ? "null" : "there"));
                if (oldMember != null) {
                    LOG.info("old " + oldMember.getState() + " new " + member.getState());
                }

                /* Mark as nearby contact and save to database. */
                boolean isJoinedInNearbyGroup = groupContact.getGroupPresence() != null && groupContact.getGroupPresence().isTypeNearby() && member.isJoined();
                clientContact.setNearby(isJoinedInNearbyGroup);
                mDatabase.saveContact(clientContact);

                membership.updateGroupMember(member);
                mDatabase.saveGroupMember(membership.getMember());
                mDatabase.saveClientMembership(membership);
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        }

        for (int i = 0; i < mContactListeners.size(); i++) {
            IXoContactListener listener = mContactListeners.get(i);
            listener.onGroupMembershipChanged(groupContact);
        }
    }

    private void decryptGroupKey(TalkClientContact group, TalkGroupMember member) {
        LOG.debug("decrypting group key");
        String keyId = member.getMemberKeyId();
        String encryptedGroupKey = member.getEncryptedGroupKey();
        if(keyId == null || encryptedGroupKey == null) {
            LOG.info("can't decrypt group key because there isn't one yet");
            return;
        }
        try {
            TalkPrivateKey talkPrivateKey = mDatabase.findPrivateKeyByKeyId(keyId);
            if(talkPrivateKey == null) {
                LOG.error("no private key for keyId " + keyId);
            } else {
                PrivateKey privateKey = talkPrivateKey.getAsNative();
                if(privateKey == null) {
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

    private String[] updateableClients(TalkClientContact group, String[] onlyWithClientIds) {

        ArrayList<String> clientIds = new ArrayList<String>();
        HashSet<String> clientIdSet = new HashSet<String>(Arrays.asList(onlyWithClientIds));
        ForeignCollection<TalkClientMembership> memberships = group.getGroupMemberships();
        if (memberships != null) {

            // prepare ArrayList with keys first first
            for (TalkClientMembership membership : memberships) {
                TalkGroupMember member = membership.getMember();
                if (member != null && member.isJoinedOrInvited() && ((onlyWithClientIds == null) || clientIdSet.contains(member.getClientId()))) {
                    LOG.debug("joined member contact " + membership.getClientContact().getClientContactId());
                    try {
                        TalkClientContact client = mDatabase.findClientContactById(membership.getClientContact().getClientContactId());
                        TalkKey clientPubKey = client.getPublicKey();
                        if (clientPubKey == null) {
                            LOG.warn("no public key for client contact " + client.getClientContactId());
                        }  else {
                            clientIds.add(member.getClientId());
                        }
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }
                }
            }
        }
        return clientIds.toArray(new String[]{});
    }

    private void generateGroupKey(TalkClientContact group) {
        try {
            // generate the new key
            byte[] newGroupKey = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            byte [] sharedKeyIdSalt = AESCryptor.makeRandomBytes(AESCryptor.KEY_SIZE);
            String sharedKeyIdSaltString = new String(Base64.encodeBase64(sharedKeyIdSalt));
            byte [] sharedKeyId = new byte[0];
            sharedKeyId = AESCryptor.calcSymmetricKeyId(newGroupKey, sharedKeyIdSalt);
            String sharedKeyIdString = new String(Base64.encodeBase64(sharedKeyId));

            // remember the group key for ourselves
            group.setGroupKey(new String(Base64.encodeBase64(newGroupKey)));
            group.getGroupPresence().setSharedKeyIdSalt(sharedKeyIdSaltString);
            group.getGroupPresence().setSharedKeyId(sharedKeyIdString);

            mDatabase.saveContact(group);

        } catch (NoSuchAlgorithmException e) {
            LOG.error("failed to generate new group key, bad crypto provider or export restricted java security settings", e);
        }  catch (SQLException e) {
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
        resetIdle();
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

                for(IXoMessageListener listener : mMessageListeners) {
                    listener.onMessageUpdated(message);
                }
            }
        });
    }

    public void register() {
        if(!isRegistered()) {
            if(mState == STATE_REGISTERING) {
                scheduleRegistration();
            } else {
                wake();
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

        for(IXoMessageListener listener : mMessageListeners) {
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
