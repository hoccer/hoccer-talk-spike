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
import android.support.v4.app.NotificationCompat;
import com.artcom.hoccer.R;
import com.google.android.gcm.GCMRegistrar;
import com.hoccer.talk.android.push.TalkPushService;
import com.hoccer.talk.client.*;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.XoAndroidClient;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ChatsActivity;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static final String CONTACT_DELIMETER = ", ";

    private static final Logger LOG = Logger.getLogger(XoClientService.class);

    private static final AtomicInteger ID_COUNTER = new AtomicInteger();

    private static final long NOTIFICATION_ALARM_BACKOFF = 10000;

    private static final String DEFAULT_TRANSFER_LIMIT = "-1";

    private static final int DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT = -1;
    private static final int DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY = 100;

    private static final String sPreferenceUploadLimitMobileKey = "preference_upload_limit_mobile";
    private static final String sPreferenceUploadLimitWifiKey = "preference_upload_limit_wifi";
    private static final String sPreferenceDownloadLimitMobileKey = "preference_download_limit_mobile";
    private static final String sPreferenceDownloadLimitWifiKey = "preference_download_limit_wifi";
    private static final String sPreferenceImageUploadPixelCountKey = "preference_image_encoding_size";
    private static final String sPreferenceImageUploadQualityKey = "preference_image_encoding_quality";

    /**
     * Executor for ourselves and the client
     */
    ScheduledExecutorService mExecutor;

    /**
     * Hoccer client that we serve
     */
    XoAndroidClient mClient;

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
     * Time of last notification
     */
    long mTimeOfLastAlarm;

    ClientListener mClientListener;

    boolean mGcmSupported;

    int mCurrentConversationContactId = -1;

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
            mClient.registerStateListener(mClientListener);
            mClient.registerMessageListener(mClientListener);
            mClient.registerContactListener(mClientListener);
            mClient.registerTransferListener(mClientListener);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(sPreferenceDownloadLimitMobileKey)
                        || key.equals(sPreferenceDownloadLimitWifiKey)
                        || key.equals(sPreferenceUploadLimitMobileKey)
                        || key.equals(sPreferenceUploadLimitWifiKey)) {
                    configureAutoTransfers();
                } else if (key.equals(sPreferenceImageUploadPixelCountKey)) {
                    loadPreference(mPreferences, sPreferenceImageUploadPixelCountKey);
                } else if (key.equals(sPreferenceImageUploadQualityKey)) {
                    loadPreference(mPreferences, sPreferenceImageUploadQualityKey);
                }
            }
        };
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesListener);

        loadPreference(mPreferences, sPreferenceImageUploadPixelCountKey);
        loadPreference(mPreferences, sPreferenceImageUploadQualityKey);
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
            mClient.unregisterStateListener(mClientListener);
            mClient.unregisterMessageListener(mClientListener);
            mClient.unregisterTransferListener(mClientListener);
            mClientListener = null;
        }

        // unregister client listeners
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
            if (intent.hasExtra(TalkPushService.EXTRA_SHOW_MESSAGE)) {
                String message = intent.getStringExtra(TalkPushService.EXTRA_SHOW_MESSAGE);
                createPushMessageNotification(message);
            }
            if (intent.hasExtra(TalkPushService.EXTRA_WAKE_CLIENT)) {
                wakeClientInBackground();
            }
            if (intent.hasExtra(TalkPushService.EXTRA_GCM_REGISTERED)) {
                doUpdateGcm(true);
            }
            if (intent.hasExtra(TalkPushService.EXTRA_GCM_UNREGISTERED)) {
                doUpdateGcm(true);
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

    private void configureAutoTransfers() {
        switch (mCurrentConnectionType) {
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_BLUETOOTH:
            case ConnectivityManager.TYPE_WIMAX:
                loadPreference(mPreferences, sPreferenceUploadLimitMobileKey);
                loadPreference(mPreferences, sPreferenceDownloadLimitMobileKey);
                break;
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_WIFI:
                loadPreference(mPreferences, sPreferenceUploadLimitWifiKey);
                loadPreference(mPreferences, sPreferenceDownloadLimitWifiKey);
                break;
        }
    }

    private void loadPreference(SharedPreferences preferences, String key) {
        if (key != null) {
            if (key.equals(sPreferenceUploadLimitMobileKey)) {
                String uploadLimitString = preferences.getString(key, DEFAULT_TRANSFER_LIMIT);
                mClient.setUploadLimit(Integer.parseInt(uploadLimitString));
            } else if (key.equals(sPreferenceDownloadLimitMobileKey)) {
                String downloadLimitString = preferences.getString(key, DEFAULT_TRANSFER_LIMIT);
                mClient.setDownloadLimit(Integer.parseInt(downloadLimitString));
            } else if (key.equals(sPreferenceUploadLimitWifiKey)) {
                String uploadLimitString = preferences.getString(key, DEFAULT_TRANSFER_LIMIT);
                mClient.setUploadLimit(Integer.parseInt(uploadLimitString));
            } else if (key.equals(sPreferenceDownloadLimitWifiKey)) {
                String downloadLimitString = preferences.getString(key, DEFAULT_TRANSFER_LIMIT);
                mClient.setDownloadLimit(Integer.parseInt(downloadLimitString));
            } else if (key.equals(sPreferenceImageUploadPixelCountKey)) {
                String maxPixelCount = mPreferences.getString(sPreferenceImageUploadPixelCountKey,
                        Integer.toString(DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT));
                mClient.setImageUploadMaxPixelCount(Integer.parseInt(maxPixelCount));
            } else if (key.equals(sPreferenceImageUploadQualityKey)) {
                String imageQuality = mPreferences.getString(sPreferenceImageUploadQualityKey,
                        Integer.toString(DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY));
                mClient.setImageUploadEncodingQuality(Integer.parseInt(imageQuality));
            }
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

        if (mClient.isActivated()) {
            mClient.deactivateNow();
        }

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

    private void updateUnseenMessageNotification(boolean doAlarm) {
        XoClientDatabase database = mClient.getDatabase();
        List<TalkClientMessage> unseenMessages;
        try {
            unseenMessages = database.findUnseenMessages();
        } catch (SQLException e) {
            LOG.error("SQL Exception while retrieving lit of unseen messages", e);
            return;
        }

        // if we have no messages cancel notification
        if (unseenMessages.isEmpty()) {
            mNotificationManager.cancel(NotificationId.UNSEEN_MESSAGES);
            return;
        }

        // determine where we are in time
        // do not sound alarms overly often (sound, vibrate)
        long now = System.currentTimeMillis();
        long timeSinceLastNotification = Math.max(0, now - mTimeOfLastAlarm);
        if (timeSinceLastNotification < NOTIFICATION_ALARM_BACKOFF) {
            doAlarm = false;
        }

        // collect unseen messages by contact
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

                // ignore unseen messages from contacts we are currently conversing with
                if (mCurrentConversationContactId == contact.getClientContactId()) {
                    continue;
                }

                // ignore clients with whom we are not befriended
                if (contact.isClient() && !contact.isClientFriend()) {
                    continue;
                }

                // ignore groups which we are not joined with yet
                if (contact.isGroup() && !contact.isGroupJoined()) {
                    continue;
                }

                if (!contactsMap.containsKey(contact.getClientContactId())) {
                    ContactUnseenMessageHolder holder = new ContactUnseenMessageHolder(contact);
                    contactsMap.put(contact.getClientContactId(), holder);
                }

                ContactUnseenMessageHolder holder = contactsMap.get(contact.getClientContactId());
                holder.getUnseenMessages().add(message);
            } else {
                LOG.error("Message without contact in unseen messages found");
            }
        }

        // if we have no messages after culling cancel notification
        if (contactsMap.isEmpty()) {
            mNotificationManager.cancel(NotificationId.UNSEEN_MESSAGES);
            return;
        }

        createUnseenMessageNotification(contactsMap, doAlarm);
    }

    private void createUnseenMessageNotification(Map<Integer, ContactUnseenMessageHolder> contactsMap, boolean doAlarm) {
        // sum up all unseen messages
        int unseenMessagesCount = 0;
        for (ContactUnseenMessageHolder holder : contactsMap.values()) {
            unseenMessagesCount += holder.getUnseenMessages().size();
        }

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
            long now = System.currentTimeMillis();
            mTimeOfLastAlarm = now;
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

            Intent messagingIntent = new Intent(this, ChatsActivity.class);
            messagingIntent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());

            // make a pending intent with correct back-stack
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                pendingIntent =
                        TaskStackBuilder.create(this)
                                .addParentStack(ChatsActivity.class)
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
                builder.setContentText(holder.getUnseenMessages().size() + getResources().getString(R.string.unseen_messages_notification_text));
            }
        } else {
            // create pending intent
            Intent contactsIntent = new Intent(this, ChatsActivity.class);
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
            for (ContactUnseenMessageHolder holder : contactsMap.values()) {
                sb.append(holder.getContact().getNickname()).append(CONTACT_DELIMETER);
            }

            // set fields
            builder.setContentTitle(sb.substring(0, sb.length() - 2));
            builder.setContentText(unseenMessagesCount + getResources().getString(R.string.unseen_messages_notification_text));
        }

        // finish up
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }

        // update the notification
        mNotificationManager.notify(NotificationId.UNSEEN_MESSAGES, notification);

        // log all unseen messages found
        StringBuilder logMessage = new StringBuilder("Notifying about unseen messages: ");
        for (ContactUnseenMessageHolder holder : contactsMap.values()) {
            logMessage.append(holder.getContact().getNickname()).append("(").append(holder.getUnseenMessages().size()).append(") ");
        }
        LOG.debug(logMessage);
    }

    private void createPushMessageNotification(String message) {
        // create intent
        Intent pushMessageIntent = new Intent(this, ChatsActivity.class);
        pushMessageIntent.putExtra(IntentHelper.EXTRA_PUSH_MESSAGE, message);

        // make a pending intent with correct back-stack
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pendingIntent =
                    TaskStackBuilder.create(this)
                            .addParentStack(ChatsActivity.class)
                            .addNextIntentWithParentStack(pushMessageIntent)
                            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent
                    .getActivity(this, 0, pushMessageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();


        mNotificationManager.notify(NotificationId.PUSH_MESSAGE, notification);
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
            IXoContactListener,
            IXoTransferListenerOld,
            MediaScannerConnection.OnScanCompletedListener {

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
        public void onDownloadRegistered(TalkClientDownload download) {
            LOG.debug("onDownloadRegistered(" + download.getClientDownloadId() + ")");
            if (download.isAttachment()) {
                mClient.requestDownload(download, false);
            }
        }

        @Override
        public void onDownloadStateChanged(TalkClientDownload download) {}

        @Override
        public void onDownloadStarted(TalkClientDownload download) {}

        @Override
        public void onDownloadProgress(TalkClientDownload download) {}

        @Override
        public void onDownloadFinished(TalkClientDownload download) {
            if (download.isAttachment()) {
                Uri downloadUri = UriUtils.getAbsoluteFileUri(download.getFilePath());
                String[] filePathes = new String[]{downloadUri.getPath()};
                String[] mimeTypes = new String[]{download.getMimeType()};

                LOG.debug("requesting media scan of " + mimeTypes[0] + " at " + filePathes[0]);
                MediaScannerConnection.scanFile(
                        XoClientService.this,
                        filePathes, mimeTypes, this
                );
            }
        }

        @Override
        public void onDownloadFailed(TalkClientDownload download) {}

        @Override
        public void onUploadStarted(TalkClientUpload upload) {}

        @Override
        public void onUploadProgress(TalkClientUpload upload) {}

        @Override
        public void onUploadFinished(TalkClientUpload upload) {
            // delete tempCompressed files now
            String temporaryFilePath = upload.getTempCompressedFilePath();
            if (temporaryFilePath != null) {
                try {
                    upload.setTempCompressedFilePath(null);
                    XoApplication.getXoClient().getDatabase().saveClientUpload(upload);
                    FileUtils.deleteQuietly(new File(temporaryFilePath));
                } catch (SQLException e) {
                    LOG.error("Error updating upload with original file path.");
                }
            }
        }

        @Override
        public void onUploadFailed(TalkClientUpload upload) {}

        @Override
        public void onUploadStateChanged(TalkClientUpload upload) {}

        @Override
        public void onScanCompleted(String filePath, Uri contentUri) {
            Intent intent = new Intent();
            intent.setAction(IntentHelper.ACTION_DOWNLOAD_SCANNED);
            intent.putExtra(IntentHelper.EXTRA_ATTACHMENT_FILE_URI, UriUtils.getAbsoluteFileUri(filePath));
            sendBroadcast(intent);
        }

        @Override
        public void onMessageCreated(TalkClientMessage message) {
            if (message.isIncoming()) {
                updateUnseenMessageNotification(true);
            }
        }

        @Override
        public void onMessageUpdated(TalkClientMessage message) {
            if (message.isIncoming()) {
                updateUnseenMessageNotification(false);
            }
        }

        @Override
        public void onMessageDeleted(TalkClientMessage message) {
            if (message.isIncoming()) {
                updateUnseenMessageNotification(false);
            }
        }

        @Override
        public void onClientPresenceChanged(TalkClientContact contact) {}

        @Override
        public void onClientRelationshipChanged(TalkClientContact contact) {
            updateUnseenMessageNotification(false);
        }

        @Override
        public void onGroupPresenceChanged(TalkClientContact contact) {}

        @Override
        public void onGroupMembershipChanged(TalkClientContact contact) {
            updateUnseenMessageNotification(false);
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
        private final TalkClientContact mContact;
        private final List<TalkClientMessage> mUnseenMessages;

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

    private class ClientIdReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mCurrentConversationContactId = intent.getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
        }
    }
}
