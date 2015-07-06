package com.hoccer.xo.android.service;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
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
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.xo.android.BackgroundManager;
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

/**
 * Android service for Hoccer Talk
 * <p/>
 * This service wraps a Talk client instance for use by Android applications.
 * <p/>
 * It should be started with startService() and kept alive using keepAlive() RPC calls
 * for as long as it is needed. If not called regularly the service will stop itself.
 */


public class XoClientService extends Service {

    public static final String CONTACT_DELIMETER = ", ";

    private static final Logger LOG = Logger.getLogger(XoClientService.class);

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

    // Executor for ourselves and the client
    ScheduledExecutorService mExecutor;

    // Hoccer client that we serve
    XoAndroidClient mClient;

    XoClientServiceBinder mBinder = new XoClientServiceBinder();

    // Preferences containing service configuration
    SharedPreferences mPreferences;

    // Listener for configuration changes
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;

    // Connectivity manager for monitoring
    ConnectivityManager mConnectivityManager;

    // Our connectivity change broadcast receiver
    ConnectivityReceiver mConnectivityReceiver;

    // Previous state of connectivity
    boolean mNetworkConnected;

    // Type of previous connection
    int mNetworkConnectionType = -1;

    // Notification manager
    NotificationManager mNotificationManager;

    // Time of last notification
    long mTimeOfLastAlarm;

    ClientListener mClientListener;

    boolean mGcmSupported;

    int mCurrentConversationContactId = -1;

    private ClientIdReceiver m_clientIdReceiver;

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        LOG.debug("onCreate()");
        super.onCreate();

        mExecutor = XoApplication.get().getExecutor();
        mClient = XoApplication.get().getXoClient();

        if (mClientListener == null) {
            mClientListener = new ClientListener();
            mClient.registerStateListener(mClientListener);
            mClient.registerMessageListener(mClientListener);
            mClient.registerContactListener(mClientListener);
            mClient.getDownloadAgent().registerListener(mClientListener);
            mClient.getUploadAgent().registerListener(mClientListener);

            BackgroundManager.get().registerListener(mClientListener);
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
            mClient.getDownloadAgent().unregisterListener(mClientListener);
            mClient.getUploadAgent().unregisterListener(mClientListener);
            mClientListener = null;

            BackgroundManager.get().unregisterListener(mClientListener);
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
                mClient.connect();
            }
            if (intent.hasExtra(TalkPushService.EXTRA_GCM_REGISTERED)) {
                doUpdateGcm(true);
            }
            if (intent.hasExtra(TalkPushService.EXTRA_GCM_UNREGISTERED)) {
                doUpdateGcm(true);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void configureAutoTransfers() {
        switch (mNetworkConnectionType) {
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
                mClient.setUploadLimit(Integer.parseInt(preferences.getString(key, Integer.toString(TransferAgent.UNLIMITED))));
            } else if (key.equals(sPreferenceDownloadLimitMobileKey)) {
                mClient.setDownloadLimit(Integer.parseInt(preferences.getString(key, Integer.toString(TransferAgent.UNLIMITED))));
            } else if (key.equals(sPreferenceUploadLimitWifiKey)) {
                mClient.setUploadLimit(Integer.parseInt(preferences.getString(key, Integer.toString(TransferAgent.UNLIMITED))));
            } else if (key.equals(sPreferenceDownloadLimitWifiKey)) {
                mClient.setDownloadLimit(Integer.parseInt(preferences.getString(key, Integer.toString(TransferAgent.UNLIMITED))));
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
            mClient.disconnect();
            mNetworkConnected = false;
            mNetworkConnectionType = -1;
        } else {
            LOG.debug("connectivity change:"
                    + " type " + activeNetwork.getTypeName()
                    + " state " + activeNetwork.getState().name());

            if (activeNetwork.isConnected()) {
                if (!mClient.isTimedOut()) {
                    mClient.connect();
                }
            } else {
                mClient.disconnect();
            }

            mNetworkConnected = activeNetwork.isConnected();
            mNetworkConnectionType = activeNetwork.getType();

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
        Map<Integer, ContactUnseenMessageHolder> relevantContactsMap = new HashMap<Integer, ContactUnseenMessageHolder>();
        for (Map.Entry<Integer, ContactUnseenMessageHolder> entry : contactsMap.entrySet()) {
            if (!entry.getValue().getContact().isNotificationsDisabled()) {
                unseenMessagesCount += entry.getValue().getUnseenMessages().size();
                relevantContactsMap.put(entry.getKey(), entry.getValue());
            }
        }
        if (relevantContactsMap.isEmpty()) {
            return;
        }

        // build the notification
        Notification.Builder builder = new Notification.Builder(this);

        // always set the small icon (should be different depending on if we have a large one)
        builder.setSmallIcon(R.drawable.ic_notification_message);

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
        if (relevantContactsMap.size() == 1) {
            // create intent to start the messaging activity for the right contact
            ContactUnseenMessageHolder holder = relevantContactsMap.values().iterator().next();
            TalkClientContact contact = holder.getContact();

            Intent intent = new Intent(this, ChatsActivity.class);
            intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());

            // make a pending intent with correct back-stack
            PendingIntent pendingIntent = PendingIntent.getActivity(this, NotificationId.UNSEEN_MESSAGES, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // add the intent to the notification
            builder.setContentIntent(pendingIntent);

            // title is always the contact name
            builder.setContentTitle(getContactName(contact));

            // text depends on number of messages
            if (holder.getUnseenMessages().size() == 1) {
                TalkClientMessage singleMessage = holder.getUnseenMessages().get(0);
                builder.setContentText(singleMessage.getText());
            } else {
                builder.setContentText(holder.getUnseenMessages().size() + getResources().getString(R.string.unseen_messages_notification_text));
            }
        } else {
            // create pending intent
            Intent intent = new Intent(this, ChatsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, NotificationId.UNSEEN_MESSAGES, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            // concatenate contact names
            StringBuilder sb = new StringBuilder();
            for (ContactUnseenMessageHolder holder : relevantContactsMap.values()) {
                sb.append(getContactName(holder.getContact())).append(CONTACT_DELIMETER);
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
        for (ContactUnseenMessageHolder holder : relevantContactsMap.values()) {
            logMessage.append(getContactName(holder.getContact())).append("(").append(holder.getUnseenMessages().size()).append(") ");
        }
        LOG.debug(logMessage);
    }

    private String getContactName(TalkClientContact contact) {
        if (contact.isNearbyGroup()) {
            return getString(R.string.all_nearby);
        } else if (contact.isWorldwideGroup()) {
            return getString(R.string.all_worldwide);
        } else {
            return contact.getNickname();
        }
    }

    private void createPushMessageNotification(String message) {
        Intent intent = new Intent(this, ChatsActivity.class);
        intent.putExtra(IntentHelper.EXTRA_PUSH_MESSAGE, message);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NotificationId.PUSH_MESSAGE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
            TransferListener,
            BackgroundManager.Listener,
            MediaScannerConnection.OnScanCompletedListener {

        @Override
        public void onClientStateChange(XoClient client) {
            LOG.debug("onClientStateChange(" + client.getState() + ")");
            if (client.isReady()) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        doRegisterGcm(TalkPushService.GCM_ALWAYS_REGISTER);
                        doUpdateGcm(TalkPushService.GCM_ALWAYS_UPDATE);
                    }
                });
            }
            if (client.isDisconnected()) {
                mWakeLock.release();
            }
        }

        @Override
        public void onDownloadRegistered(TalkClientDownload download) {
        }

        @Override
        public void onDownloadStateChanged(TalkClientDownload download) {
        }

        @Override
        public void onDownloadStarted(TalkClientDownload download) {
        }

        @Override
        public void onDownloadProgress(TalkClientDownload download) {
        }

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
        public void onDownloadFailed(TalkClientDownload download) {
        }

        @Override
        public void onUploadStarted(TalkClientUpload upload) {
        }

        @Override
        public void onUploadProgress(TalkClientUpload upload) {
        }

        @Override
        public void onUploadFinished(TalkClientUpload upload) {
            // delete tempCompressed files now
            String temporaryFilePath = upload.getTempCompressedFilePath();
            if (temporaryFilePath != null) {
                try {
                    upload.setTempCompressedFilePath(null);
                    XoApplication.get().getXoClient().getDatabase().saveClientUpload(upload);
                    FileUtils.deleteQuietly(new File(temporaryFilePath));
                } catch (SQLException e) {
                    LOG.error("Error updating upload with original file path.");
                }
            }
        }

        @Override
        public void onUploadFailed(TalkClientUpload upload) {
        }

        @Override
        public void onUploadStateChanged(TalkClientUpload upload) {
        }

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
        public void onClientPresenceChanged(TalkClientContact contact) {
        }

        @Override
        public void onClientRelationshipChanged(TalkClientContact contact) {
            updateUnseenMessageNotification(false);
        }

        @Override
        public void onGroupPresenceChanged(TalkClientContact contact) {
        }

        @Override
        public void onGroupMembershipChanged(TalkClientContact contact) {
            updateUnseenMessageNotification(false);
        }

        @Override
        public void onBecameForeground(Activity activity) {}

        @Override
        public void onBecameBackground(Activity activity) {
            LOG.debug("onBecameBackground()");
            acquireWakeLockToComleteDisconnect();

            mClient.setPresenceStatus(TalkPresence.STATUS_BACKGROUND);
        }

        private void acquireWakeLockToComleteDisconnect() {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Background disconnect");
            mWakeLock.acquire();
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

    public class XoClientServiceBinder extends Binder {
        public XoClientService getService() {
            return XoClientService.this;
        }
    }
}
