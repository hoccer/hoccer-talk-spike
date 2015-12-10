package com.hoccer.talk.android.push;

import android.content.Context;
import android.content.Intent;
import com.google.android.gcm.GCMBaseIntentService;
import com.hoccer.xo.android.service.XoClientService;
import net.hockeyapp.android.ExceptionHandler;
import org.apache.log4j.Logger;

/**
 * GCM push notification service
 *
 * This service processes all our GCM events.
 * Its lifecycle is intent-driven.
 *
 * For most events this service receives it will generate an Intent
 * to call the client service to perform appropriate actions.
 *
 * NOTE this class can not be renamed because of upgrade
 *      issues with respect to GCM registration state
 *      on target devices
 */
public class TalkPushService extends GCMBaseIntentService {

    public static final boolean GCM_ALWAYS_REGISTER = false;
    public static final boolean GCM_ALWAYS_UPDATE = true;
    public static final String GCM_SENDER_ID = "1894273085";
    public static final long GCM_REGISTRATION_EXPIRATION = 24 * 3600;

    public static final String EXTRA_GCM_REGISTERED = "com.hoccer.xo.GCM_REGISTERED";
    public static final String EXTRA_GCM_UNREGISTERED = "com.hoccer.xo.GCM_UNREGISTERED";
    public static final String EXTRA_WAKE_CLIENT = "com.hoccer.xo.WAKE_CLIENT";
    public static final String EXTRA_SHOW_GENERIC_PUSH_MESSAGE = "com.hoccer.xo.SHOW_MESSAGE";

    private static final Logger LOG = Logger.getLogger(TalkPushService.class);

    public TalkPushService() {
        super(GCM_SENDER_ID);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        LOG.debug("onMessage(" + intent + ") message:" + message);

        if (message != null) {
            startServiceWithIntent(EXTRA_SHOW_GENERIC_PUSH_MESSAGE, message);
        } else {
            wakeClient();
        }
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        LOG.info("onDeletedMessages(" + total + ")");
        // wake the client, no matter what the message might have been
        wakeClient();
    }

    @Override
    protected void onError(Context context, String errorId) {
        LOG.error("onError(" + errorId + ")");
        ExceptionHandler.saveException(new Exception("TalkPushService.onError("+errorId+")"), null);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        LOG.info("onRecoverableError(" + errorId + ")");
        ExceptionHandler.saveException(new Exception("TalkPushService.onRecoverableError("+errorId+")"), null);
        return super.onRecoverableError(context, errorId);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        LOG.info("onRegistered(" + registrationId + ")");
        startServiceWithIntent(EXTRA_GCM_REGISTERED, registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        LOG.info("onUnregistered(" + registrationId + ")");
        startServiceWithIntent(EXTRA_GCM_UNREGISTERED, registrationId);
    }

    private void wakeClient() {
        startServiceWithIntent(EXTRA_WAKE_CLIENT, "dummy");
    }

    private void startServiceWithIntent(String extra, String extraValue) {
        Intent serviceIntent = new Intent(this, XoClientService.class);
        serviceIntent.putExtra(extra, extraValue);
        startService(serviceIntent);
    }
}
