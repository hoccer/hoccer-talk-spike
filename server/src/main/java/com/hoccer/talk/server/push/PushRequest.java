package com.hoccer.talk.server.push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A single in-progress push request
 */
public class PushRequest {

    private static final Logger LOG = Logger.getLogger(PushRequest.class);

    private final PushAgent mAgent;

    private final String mClientId;
    private TalkClient mClient;
    private final TalkClientHostInfo mClientHostInfo;

    private final TalkServerConfiguration mConfig;

    private final Date mCreatedTime = new Date();

    public Date getCreatedTime() {
        return mCreatedTime;
    }

    public PushRequest(PushAgent agent, String clientId, TalkClientHostInfo clientHostInfo) {
        mAgent = agent;
        mConfig = mAgent.getConfiguration();
        mClientId = clientId;
        mClientHostInfo = clientHostInfo;
    }

    public String getClientId() {
        return mClientId;
    }

    public TalkClient getClient() {
        return mClient;
    }

    public TalkClientHostInfo getClientHostInfo() {
        return mClientHostInfo;
    }

    public boolean perform() {
        LOG.debug("try perform push for client " + mClientId);
        // get up-to-date client object
        ITalkServerDatabase database = mAgent.getDatabase();
        mClient = database.findClientById(mClientId);

        if (mClient == null) {
            LOG.warn("client " + mClientId + " does not exist");
            return false;
        }

        List<TalkDelivery> deliveries =
                database.findDeliveriesForClientInState(
                        mClient.getClientId(),
                        TalkDelivery.STATE_DELIVERING);

        int deliveringCount = (deliveries == null) ? 0 : deliveries.size();

        if (deliveringCount == 0) {
            LOG.debug("no messages to be delivered for " + mClientId);
            return false;
        }

        String messageInfo = "undelivered:"+deliveringCount;
        LOG.debug("push messageInfo='" + messageInfo + "', last info='"+mClient.getLastPushMessage()+"'");

        if (messageInfo.equals(mClient.getLastPushMessage())) {
            LOG.debug("info has already been pushed, nothing new to push for client " + mClientId);
            return false;
        }

        boolean didWakeupPush = false;

        // try to perform push
        if (mConfig.isGcmEnabled() && mClient.isGcmCapable()) {
            didWakeupPush = performGcm();
        } else if (mConfig.isApnsEnabled() && mClient.isApnsCapable()) {
            didWakeupPush = performApns(deliveringCount);
        } else {
            if (mClient.isPushCapable()) {
                LOG.warn("PushRequest.perform: client " + mClient + " push not available");
            } else {
                LOG.warn("PushRequest.perform: client " + mClientId + " has no registration");
            }
            return false;
        }
        mClient.setLastPushMessage(messageInfo);
        database.saveClient(mClient);
        return didWakeupPush;
    }

    private boolean performGcm() {
        LOG.debug("GCM push for " + mClientId);
        Message message = new Message.Builder()
                .collapseKey("com.hoccer.talk.wake")
                .timeToLive(TalkServerConfiguration.GCM_WAKE_TTL)
                .restrictedPackageName(mClient.getGcmPackage())
                .build();
        Sender gcmSender = mAgent.getGcmSender();
        try {
            Result res = gcmSender.send(message, mClient.getGcmRegistration(), 10);
            if (res.getMessageId() != null) {
                if (res.getCanonicalRegistrationId() != null) {
                    LOG.warn("GCM returned a canonical registration id - we should do something with it");
                }
                LOG.debug("GCM push successful, return message id " + res.getMessageId());
                return true;
            } else {
                LOG.error("GCM push returned error '" + res.getErrorCodeName() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // return true if push should wake up client
    private boolean performApns(int deliveringCount) {
        String clientName = mConfig.getApnsDefaultClientName();

        PushAgent.APNS_SERVICE_TYPE type = ApnsPushProvider.apnsServiceType(mClient, mClientHostInfo);
        boolean backgroundPush = false;
        if (mClientHostInfo != null) {
            clientName = mClientHostInfo.getClientName();
        }

        ApnsService apnsService = mAgent.getApnsService(clientName, type);

        if (apnsService != null) {
            LOG.debug("APNS push for " + mClientId + " using " + type + " type, token "+mClient.getApnsToken());

            PayloadBuilder b = APNS.newPayload();

            int messageCount = deliveringCount + mClient.getApnsUnreadMessages();

            if (TalkClient.APNS_MODE_BACKGROUND.equals(mClient.getApnsMode())) {
                // background message
                LOG.debug("Performing APNS background push configured for "+mClientId + " clientName '" + clientName + "' and type '" + type + "'");
                b.badge(messageCount);
                b.forNewsstand();
                backgroundPush = true;
            }  else {
                LOG.debug("Performing APNS foreground push for "+mClientId + " clientName '" + clientName + "' and type '" + type + "'");
                // default message
                if (messageCount > 1) {
                    b.localizedKey("apn_new_messages");
                    b.localizedArguments(String.valueOf(messageCount));
                } else {
                    b.localizedKey("apn_one_new_message");
                }

                b.badge(messageCount);
                b.sound("default");
            }
            apnsService.push(mClient.getApnsToken(), b.build());
            return backgroundPush;
        } else {
            LOG.error("APNS push skipped, no service configured for clientName '" + clientName + "' and type '" + type + "'");
        }
        return false;
    }
}
