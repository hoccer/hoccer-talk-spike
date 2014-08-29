package com.hoccer.xo.android.service;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import com.google.android.gcm.GCMRegistrar;
import com.hoccer.talk.android.push.TalkPushService;
import com.hoccer.talk.client.*;
import com.hoccer.talk.client.model.*;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ContactsActivity;
import com.hoccer.xo.android.activity.MessagingActivity;
import com.hoccer.xo.android.sms.SmsReceiver;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.net.URI;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Android service for Hoccer Talk
 * <p/>
 * This service wraps a Talk client instance for use by Android applications.
 * <p/>
 * It should be started with startService() and kept alive using keepAlive() RPC calls
 * for as long as it is needed. If not called regularly the service will stop itself.
 */


public class XoClientService extends Service {

    /**
     * Delay after which new activities send their first keepalive (seconds)
     */
    public static final int SERVICE_KEEPALIVE_PING_DELAY = 60;
    /**
     * Interval at which activities send keepalives to the client service (seconds)
     */
    public static final int SERVICE_KEEPALIVE_PING_INTERVAL = 600;
    /**
     * Timeout after which the client service terminates automatically (seconds)
     */
    public static final int SERVICE_KEEPALIVE_TIMEOUT = 1800;

    private static final Logger LOG = Logger.getLogger(XoClientService.class);

    private static final AtomicInteger ID_COUNTER = new AtomicInteger();

    private static final long NOTIFICATION_ALARM_BACKOFF = 5000;
    private static final long NOTIFICATION_CANCEL_BACKOFF = 2000;
    private static final int NOTIFICATION_UNCONFIRMED_INVITATIONS = 1;
    private static final int NOTIFICATION_UNSEEN_MESSAGES = 0;

    private static final String sPreferenceUploadLimitMobileKey = "preference_upload_limit_mobile";
    private static final String sPreferenceUploadLimitWifiKey = "preference_upload_limit_wifi";
    private static final String sPreferenceDownloadLimitMobileKey = "preference_download_limit_mobile";
    private static final String sPreferenceDownloadLimitWifiKey = "preference_download_limit_wifi";

//    private int mUploadLimit = -1;
//    private int mDownloadLimit = -1;

    /**
     * Executor for ourselves and the client
     */
    ScheduledExecutorService mExecutor;

    /**
     * Hoccer client that we serve
     */
    XoClient mClient;

    /**
     * Reference to latest auto-shutdown future
     */
    ScheduledFuture<?> mShutdownFuture;

    /**
     * All service connections
     */
    ArrayList<Connection> mConnections;

    /**
     * Preferences containing service configuration
     */
    SharedPreferences mPreferences;

    /**
     * Listener for configuration changes
     */
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;

    /**
     * Connectivity manager for monitoring
     */
    ConnectivityManager mConnectivityManager;

    /**
     * Our connectivity change broadcast receiver
     */
    ConnectivityReceiver mConnectivityReceiver;

    /**
     * Previous state of connectivity
     */
    boolean mCurrentConnectionState = false;

    /**
     * Type of previous connection
     */
    int mCurrentConnectionType = -1;

    /**
     * Notification manager
     */
    NotificationManager mNotificationManager;

    /**
     * Time of last notification (for cancellation backoff)
     */
    long mNotificationTimestamp;

    ClientListener mClientListener;

    boolean mGcmSupported;

    private ClientIdReceiver m_clientIdReceiver;

    @Override
    public void onCreate() {
        LOG.debug("onCreate()");
        super.onCreate();

        mExecutor = XoApplication.getExecutor();

        mConnections = new ArrayList<Connection>();

        mClient = XoApplication.getXoClient();

        if (mClientListener == null) {
            mClientListener = new ClientListener();
            mClient.registerTokenListener(mClientListener);
            mClient.registerStateListener(mClientListener);
            mClient.registerMessageListener(mClientListener);
            mClient.registerTransferListener(mClientListener);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("preference_service_uri")) {
                    configureServiceUri();
                }
                if (key.equals(sPreferenceDownloadLimitMobileKey)
                        || key.equals(sPreferenceDownloadLimitWifiKey)
                        || key.equals(sPreferenceUploadLimitMobileKey)
                        || key.equals(sPreferenceUploadLimitWifiKey)) {
                    configureAutoTransfers();
                }
            }
        };
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesListener);

        configureAutoTransfers();

        doVerifyGcm();

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        registerConnectivityReceiver();
        handleConnectivityChange(mConnectivityManager.getActiveNetworkInfo());

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        IntentFilter filter = new IntentFilter(ClientIdReceiver.class.getName());
        filter.addAction(IntentHelper.ACTION_CONTACT_ID_IN_CONVERSATION);
        m_clientIdReceiver = new ClientIdReceiver();
        registerReceiver(m_clientIdReceiver, filter);
    }

    @Override
    public void onDestroy() {
        LOG.debug("onDestroy()");
        super.onDestroy();
        unregisterConnectivityReceiver();
        if (mClientListener != null) {
            mClient.unregisterTokenListener(mClientListener);
            mClient.unregisterStateListener(mClientListener);
            mClient.unregisterMessageListener(mClientListener);
            mClient.unregisterTransferListener(mClientListener);
            mClientListener = null;
        }
        // XXX unregister client listeners
        if (mPreferencesListener != null) {
            mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferencesListener);
            mPreferencesListener = null;
        }
        unregisterReceiver(m_clientIdReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.debug("onStartCommand(" + ((intent == null) ? "null" : intent.toString()) + ")");
        if (intent != null) {
            if (intent.hasExtra(TalkPushService.EXTRA_WAKE_CLIENT)) {
                wakeClientInBackground();
            }
            if (intent.hasExtra(TalkPushService.EXTRA_GCM_REGISTERED)) {
                doUpdateGcm(true);
            }
            if (intent.hasExtra(TalkPushService.EXTRA_GCM_UNREGISTERED)) {
                doUpdateGcm(true);
            }
            if (intent.hasExtra(SmsReceiver.EXTRA_SMS_URL_RECEIVED)) {
                String sender = intent.getStringExtra(SmsReceiver.EXTRA_SMS_SENDER);
                String body = intent.getStringExtra(SmsReceiver.EXTRA_SMS_BODY);
                String url = intent.getStringExtra(SmsReceiver.EXTRA_SMS_URL_RECEIVED);
                mClient.handleSmsUrl(sender, body, url);
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        LOG.debug("onBind(" + intent.toString() + ")");

        if (!mClient.isActivated()) {
            mClient.activate();
        }

        Connection newConnection = new Connection(intent);

        mConnections.add(newConnection);

        return newConnection;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LOG.debug("onUnbind(" + intent.toString() + ")");
        return super.onUnbind(intent);
    }

    private void configureServiceUri() {
        String uriString = mPreferences.getString("preference_service_uri", "");
        if (uriString.isEmpty()) {
            uriString = XoApplication.getXoClient().getConfiguration().getServerUri();
        }
        URI uri = URI.create(uriString);
        mClient.setServiceUri(uri);
    }

    private void configureAutoTransfers() {
        switch (mCurrentConnectionType) {
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_BLUETOOTH:
            case ConnectivityManager.TYPE_WIMAX:
                loadPreferences(mPreferences, sPreferenceUploadLimitMobileKey);
                loadPreferences(mPreferences, sPreferenceDownloadLimitMobileKey);
                break;
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
                loadPreferences(mPreferences, sPreferenceUploadLimitWifiKey);
                loadPreferences(mPreferences, sPreferenceDownloadLimitWifiKey);
                break;
        }
    }

    private void loadPreferences(SharedPreferences preferences, String key) {
        if (key != null && key.equals(sPreferenceUploadLimitMobileKey)) {
            String uploadLimitString = preferences.getString(key, "-1");
            mClient.setUploadLimit(Integer.parseInt(uploadLimitString));
        }
        if (key != null && key.equals(sPreferenceDownloadLimitMobileKey)) {
            String downloadLimitString = preferences.getString(key, "-1");
            mClient.setDownloadLimit(Integer.parseInt(downloadLimitString));
        }
        if (key != null && key.equals(sPreferenceUploadLimitWifiKey)) {
            String uploadLimitString = preferences.getString(key, "-1");
            mClient.setUploadLimit(Integer.parseInt(uploadLimitString));
        }
        if (key != null && key.equals(sPreferenceDownloadLimitWifiKey)) {
            String downloadLimitString = preferences.getString(key, "-1");
            mClient.setDownloadLimit(Integer.parseInt(downloadLimitString));
        }
    }

    private void wakeClient() {
        if (mCurrentConnectionState) {
            mClient.wake();
        }
    }

    private void wakeClientInBackground() {
        if (mCurrentConnectionState) {
            mClient.wakeInBackground();
        }
    }

    private void doVerifyGcm() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("doVerifyGcm()");
        }

        // check our manifest for GCM compatibility
        boolean manifestAllowsGcm = false;
        try {
            GCMRegistrar.checkManifest(this);
            manifestAllowsGcm = true;
        } catch (IllegalStateException ex) {
            LOG.warn("GCM unavailable due to manifest problems", ex);
        }

        // check GCM device support
        boolean deviceSupportsGcm = false;
        if (manifestAllowsGcm) {
            try {
                GCMRegistrar.checkDevice(this);
                deviceSupportsGcm = true;
            } catch (UnsupportedOperationException ex) {
                LOG.warn("GCM not supported by device", ex);
            }
        }

        // make the final decision
        mGcmSupported = deviceSupportsGcm && manifestAllowsGcm;
        if (mGcmSupported) {
            LOG.info("GCM is supported");
        } else {
            LOG.warn("GCM not supported");
        }
    }

    private void doRegisterGcm(boolean forced) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("doRegisterGcm(" + (forced ? "forced" : "") + ")");
        }
        if (mGcmSupported) {
            if (forced || !GCMRegistrar.isRegistered(this)) {
                LOG.debug("requesting GCM registration");
                GCMRegistrar.register(this, TalkPushService.GCM_SENDER_ID);
            } else {
                LOG.debug("no need to request GCM registration");
            }
        }
    }

    private void doUpdateGcm(boolean forced) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("doUpdateGcm(" + (forced ? "forced" : "") + ")");
        }
        if (mGcmSupported && GCMRegistrar.isRegistered(this)) {
            // check if we got here already
            if (forced || !GCMRegistrar.isRegisteredOnServer(this)) {
                LOG.debug("updating GCM registration");
                // perform the registration call
                mClient.registerGcm(this.getPackageName(), GCMRegistrar.getRegistrationId(this));
                // set the registration timeout (XXX move elsewhere)
                GCMRegistrar.setRegisterOnServerLifespan(
                        this, TalkPushService.GCM_REGISTRATION_EXPIRATION * 1000);
                // tell the registrar that we did this successfully
                GCMRegistrar.setRegisteredOnServer(this, true);
            } else {
                LOG.debug("no need to update GCM registration");
            }
        } else {
            if (forced || GCMRegistrar.isRegisteredOnServer(this)) {
                LOG.debug("retracting GCM registration");
                mClient.unregisterGcm();
                GCMRegistrar.setRegisteredOnServer(this, false);
            }
        }
    }

    private void doShutdown() {
        LOG.info("shutting down");
        // command the client to deactivate
        if (mClient.isActivated()) {
            mClient.deactivateNow();
        }
        // stop ourselves
        stopSelf();
    }

    private void scheduleShutdown() {
        shutdownShutdown();
        mShutdownFuture = mExecutor.schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        LOG.debug("keep-alive timeout");
                        doShutdown();
                    }
                },
                SERVICE_KEEPALIVE_TIMEOUT, TimeUnit.SECONDS
        );
    }

    private void shutdownShutdown() {
        if (mShutdownFuture != null) {
            mShutdownFuture.cancel(false);
            mShutdownFuture = null;
        }
    }

    private void registerConnectivityReceiver() {
        LOG.debug("registerConnectivityReceiver()");
        if (mConnectivityReceiver == null) {
            mConnectivityReceiver = new ConnectivityReceiver();
            registerReceiver(mConnectivityReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unregisterConnectivityReceiver() {
        LOG.debug("unregisterConnectivityReceiver()");
        if (mConnectivityReceiver != null) {
            unregisterReceiver(mConnectivityReceiver);
            mConnectivityReceiver = null;
        }
    }

    private void handleConnectivityChange(NetworkInfo activeNetwork) {
        if (activeNetwork == null) {
            LOG.debug("connectivity change: no connectivity");
            mClient.deactivate();
            mCurrentConnectionState = false;
            mCurrentConnectionType = -1;
        } else {
            LOG.debug("connectivity change:"
                    + " type " + activeNetwork.getTypeName()
                    + " state " + activeNetwork.getState().name());

            int previousState = mClient.getState();
            if (activeNetwork.isConnected()) {
                if (previousState <= XoClient.STATE_INACTIVE) {
                    mClient.activate();
                }
            } else if (activeNetwork.isConnectedOrConnecting()) {
                if (previousState <= XoClient.STATE_INACTIVE) {
                    mClient.activate();
                }
            } else {
                if (previousState > XoClient.STATE_INACTIVE) {
                    mClient.deactivate();
                }
            }

            // TODO: is this check too early ? Last if-statement above deactivates client when network dead.
            mCurrentConnectionState = activeNetwork.isConnected();
            mCurrentConnectionType = activeNetwork.getType();

            // reset transfer limits on network type change.
            configureAutoTransfers();
        }
    }

    private void updateInvitateNotification(List<TalkClientSmsToken> unconfirmedTokens,
                                            boolean notify) {
        LOG.debug("updateInvitateNotification()");
        XoClientDatabase db = mClient.getDatabase();

        // cancel present notification if everything has been seen
        // we back off here to prevent interruption of any in-progress alarms
        if (unconfirmedTokens == null || unconfirmedTokens.isEmpty()) {
            LOG.debug("no unconfirmed tokens");
            mNotificationManager.cancel(NOTIFICATION_UNCONFIRMED_INVITATIONS);
            return;
        }

        int numUnconfirmed = unconfirmedTokens.size();

        // log about what we got
        LOG.debug("notifying " + numUnconfirmed + " invitations ");

        // build the notification
        Notification.Builder builder = new Notification.Builder(this);
        // always set the small icon (should be different depending on if we have a large one)
        builder.setSmallIcon(R.drawable.ic_notification);
        // large icon XXX
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        builder.setLargeIcon(largeIcon);
        // determine if alarms should be sounded
        if (notify) {
            builder.setDefaults(Notification.DEFAULT_ALL);
        }
        // set total number of messages of more than one
        if (numUnconfirmed > 1) {
            builder.setNumber(numUnconfirmed);
        }
        // create pending intent
        Intent contactsIntent = new Intent(this, ContactsActivity.class);
        PendingIntent pendingIntent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pendingIntent = TaskStackBuilder.create(this).addNextIntent(contactsIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent
                    .getActivity(this, 0, contactsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.setContentIntent(pendingIntent);
        // set fields
        if (numUnconfirmed > 1) {
            builder.setContentTitle(numUnconfirmed + " unconfirmed invitations");
        } else {
            builder.setContentTitle(numUnconfirmed + " unconfirmed invitation");
        }

        // finish up
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        // log about it
        LOG.debug("invite notification " + notification.toString());
        // update the notification
        mNotificationManager.notify(NOTIFICATION_UNCONFIRMED_INVITATIONS, notification);
    }

    private void updateMessageNotification() {
        LOG.debug("updateMessageNotification()");

        XoClientDatabase database = mClient.getDatabase();
        List<TalkClientMessage> unseenMessages;
        try {
            unseenMessages = database.findUnseenMessages();
        } catch (SQLException e) {
            LOG.error("SQL Exception while retrieving lit of unseen messages", e);
            return;
        }

        if(unseenMessages.size() == 0) {
            cancelMessageNotification();
            return;
        }

        // determine where we are in time
        // do not sound alarms overly often (sound, vibrate)
        long now = System.currentTimeMillis();
        long timeSinceLastNotification = Math.max(0, now - mNotificationTimestamp);
        boolean doAlarm = true;
        if (timeSinceLastNotification < NOTIFICATION_ALARM_BACKOFF) {
            doAlarm = false;
        }
        mNotificationTimestamp = now;

        // collect unseen messages by contact and remove messages from deleted clients
        int unseenMessagesCount = 0;
        Map<Integer, ContactUnseenMessageHolder> contactsMap = new HashMap<Integer, ContactUnseenMessageHolder>();
        for (TalkClientMessage message : unseenMessages) {
            TalkClientContact contact = message.getConversationContact();
            if (contact != null) {
                try {
                    database.refreshClientContact(contact);
                } catch (SQLException e) {
                    LOG.error("SQL Exception while retrieving contact", e);
                    continue;
                }

                if(contact.isDeleted()) {
                    continue;
                }

                if(!contactsMap.containsKey(contact.getClientContactId())) {
                    ContactUnseenMessageHolder holder = new ContactUnseenMessageHolder(contact);
                    contactsMap.put(contact.getClientContactId(), holder);
                }

                ContactUnseenMessageHolder holder = contactsMap.get(contact.getClientContactId());
                holder.getUnseenMessages().add(message);
                unseenMessagesCount++;
            } else {
                LOG.error("Message without contact in unseen messages found");
            }
        }

        // if we have no messages after culling then cancel notification
        if(unseenMessagesCount == 0) {
            cancelMessageNotification();
            return;
        }

        // log all unseen messages found
        StringBuilder logMessage = new StringBuilder("Notifying about unseen messages: ");
        for(ContactUnseenMessageHolder holder : contactsMap.values()) {
            logMessage.append(holder.getContact().getName()).append("(").append(holder.getUnseenMessages().size()).append(") ");
        }
        LOG.debug(logMessage);

        // build the notification
        Notification.Builder builder = new Notification.Builder(this);

        // always set the small icon (should be different depending on if we have a large one)
        builder.setSmallIcon(R.drawable.ic_notification);

        // large icon
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        builder.setLargeIcon(largeIcon);

        // determine if alarms should be sounded
        if (doAlarm) {
            builder.setDefaults(Notification.DEFAULT_ALL);
        }

        // set total number of messages of more than one
        if (unseenMessagesCount > 1) {
            builder.setNumber(unseenMessagesCount);
        }

        // fill in content
        if (contactsMap.size() == 1) {
            // create intent to start the messaging activity for the right contact
            ContactUnseenMessageHolder holder = contactsMap.values().iterator().next();
            TalkClientContact contact = holder.getContact();

            Intent messagingIntent = new Intent(this, ContactsActivity.class);
            messagingIntent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());

            // make a pending intent with correct back-stack
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                pendingIntent =
                        TaskStackBuilder.create(this)
                                .addParentStack(ContactsActivity.class)
                                .addNextIntentWithParentStack(messagingIntent)
                                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent
                        .getActivity(this, 0, messagingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            // add the intent to the notification
            builder.setContentIntent(pendingIntent);

            // title is always the contact name
            builder.setContentTitle(contact.getNickname());

            // text depends on number of messages
            if (holder.getUnseenMessages().size() == 1) {
                TalkClientMessage singleMessage = holder.getUnseenMessages().get(0);
                builder.setContentText(singleMessage.getText());
            } else {
                builder.setContentText(holder.getUnseenMessages().size() + " new messages");
            }
        } else {
            // create pending intent
            Intent contactsIntent = new Intent(this, ContactsActivity.class);
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                pendingIntent =
                        TaskStackBuilder.create(this)
                                .addNextIntent(contactsIntent)
                                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent
                        .getActivity(this, 0, contactsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builder.setContentIntent(pendingIntent);

            // concatenate contact names
            StringBuilder sb = new StringBuilder();
            for(ContactUnseenMessageHolder holder : contactsMap.values()) {
                sb.append(holder.getContact().getNickname()).append(", ");
            }

            // set fields
            builder.setContentTitle(sb.substring(0, sb.length() - 2));
            builder.setContentText(unseenMessagesCount + " new messages");
        }

        // finish up
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        // log about it
        LOG.debug("message notification " + notification.toString());

        // update the notification
        mNotificationManager.notify(NOTIFICATION_UNSEEN_MESSAGES, notification);
    }

    private void cancelMessageNotification() {
        long now = System.currentTimeMillis();
        long cancelTime = mNotificationTimestamp + NOTIFICATION_CANCEL_BACKOFF;
        long delay = Math.max(0, cancelTime - now);
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.cancel(NOTIFICATION_UNSEEN_MESSAGES);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private class ConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOG.debug("onConnectivityChange()");
            handleConnectivityChange(mConnectivityManager.getActiveNetworkInfo());
        }
    }

    private class ClientListener implements
            IXoStateListener,
            IXoMessageListener,
            IXoTokenListener,
            IXoTransferListenerOld,
            MediaScannerConnection.OnScanCompletedListener {

        Hashtable<String, TalkClientDownload> mScanningDownloads
                = new Hashtable<String, TalkClientDownload>();

        @Override
        public void onClientStateChange(XoClient client, int state) {
            LOG.debug("onClientStateChange(" + XoClient.stateToString(state) + ")");
            if (state == XoClient.STATE_ACTIVE) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        doRegisterGcm(TalkPushService.GCM_ALWAYS_REGISTER);
                        doUpdateGcm(TalkPushService.GCM_ALWAYS_UPDATE);
                    }
                });
            }
        }

        @Override
        public void onTokensChanged(List<TalkClientSmsToken> tokens, boolean newTokens) {
            LOG.debug("onTokensChanged(" + tokens.size() + "," + newTokens + ")");
            try {
                updateInvitateNotification(tokens, newTokens);
            } catch (Throwable t) {
                LOG.error("exception updating invite notification", t);
            }
        }

        @Override
        public void onDownloadRegistered(TalkClientDownload download) {
            LOG.debug("onDownloadRegistered(" + download.getClientDownloadId() + ")");
            if (download.isAttachment()) {
                mClient.requestDownload(download, false);
            }
        }

        @Override
        public void onDownloadStateChanged(TalkClientDownload download) {
            if (download.isAttachment() && download.isContentAvailable()
                    && download.getContentUrl() == null) {
                String[] path = new String[]{download.getDataFile()};
                String[] ctype = new String[]{download.getContentType()};
                LOG.debug("requesting media scan of " + ctype[0] + " at " + path[0]);
                mScanningDownloads.put(path[0], download);
                MediaScannerConnection.scanFile(
                        XoClientService.this,
                        path, ctype, this
                );
            }
        }

        @Override
        public void onDownloadStarted(TalkClientDownload download) {
        }

        @Override
        public void onDownloadProgress(TalkClientDownload download) {
        }

        @Override
        public void onDownloadFinished(TalkClientDownload download) {
        }

        @Override
        public void onDownloadFailed(TalkClientDownload downlad) {

        }

        @Override
        public void onUploadStarted(TalkClientUpload upload) {
        }

        @Override
        public void onUploadProgress(TalkClientUpload upload) {
        }

        @Override
        public void onUploadFinished(TalkClientUpload upload) {
        }

        @Override
        public void onUploadFailed(TalkClientUpload upload) {
        }

        @Override
        public void onUploadStateChanged(TalkClientUpload upload) {
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            LOG.debug("media scan of " + path + " completed - uri " + uri.toString());
            TalkClientDownload download = mScanningDownloads.get(path);
            if (download != null) {
                download.provideContentUrl(mClient.getTransferAgent(), uri.toString());
            }
            mScanningDownloads.remove(path);
        }

        @Override
        public void onMessageCreated(TalkClientMessage message) {
            if(!isMessagingActivityOnTopForContact()) {
                updateMessageNotification();
            }
        }

        @Override
        public void onMessageUpdated(TalkClientMessage message) {
            if(!isMessagingActivityOnTopForContact()) {
                updateMessageNotification();
            }
        }

        @Override
        public void onMessageDeleted(TalkClientMessage message) {
            if(!isMessagingActivityOnTopForContact()) {
                updateMessageNotification();
            }
        }
    }

    private boolean isMessagingActivityOnTopForContact() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.RunningTaskInfo taskInfo = activityManager.getRunningTasks(1).get(0);
        if(taskInfo == null) {
            return false;
        } else {
            return taskInfo.topActivity.getShortClassName().equalsIgnoreCase(MessagingActivity.class.getName());
        }
    }

    public class Connection extends IXoClientService.Stub {

        int mId;

        Intent mBindIntent;

        Connection(Intent bindIntent) {
            mId = ID_COUNTER.incrementAndGet();
            mBindIntent = bindIntent;
            LOG.debug("[" + mId + "] connected");
        }

        @Override
        public void keepAlive() throws RemoteException {
            LOG.debug("[" + mId + "] keepAlive()");
            scheduleShutdown();
        }

        @Override
        public void wake() throws RemoteException {
            LOG.debug("[" + mId + "] wake()");
            wakeClient();
        }

        @Override
        public void reconnect() throws RemoteException {
            LOG.debug("[" + mId + "] reconnect()");
            mClient.reconnect("client request");
        }
    }

    private class ContactUnseenMessageHolder {
        private TalkClientContact mContact;
        private List<TalkClientMessage> mUnseenMessages;

        public ContactUnseenMessageHolder(TalkClientContact contact) {
            mContact = contact;
            mUnseenMessages = new ArrayList<TalkClientMessage>();
        }

        public TalkClientContact getContact() {
            return mContact;
        }

        public List<TalkClientMessage> getUnseenMessages() {
            return mUnseenMessages;
        }
    }
}
