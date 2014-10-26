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

    public PushRequest(PushAgent agent, String clientId, TalkClientHostInfo clientHostInfo) {
        mAgent = agent;
        mConfig = mAgent.getConfiguration();
        mClientId = clientId;
        mClientHostInfo = clientHostInfo;
    }

    public void perform() {
        LOG.debug("try perform push for client " + mClientId);
        // get up-to-date client object
        ITalkServerDatabase database = mAgent.getDatabase();
        mClient = database.findClientById(mClientId);

        if (mClient == null) {
            LOG.warn("client " + mClientId + " does not exist");
            return;
        }

        List<TalkDelivery> deliveries =
                database.findDeliveriesForClientInState(
                        mClient.getClientId(),
                        TalkDelivery.STATE_DELIVERING);

        int deliveringCount = (deliveries == null) ? 0 : deliveries.size();

        if (deliveringCount == 0) {
            LOG.debug("no messages to be delivered for " + mClientId);
            return;
        }

        String messageInfo = "undelivered:"+deliveringCount;
        LOG.debug("push messageInfo='" + messageInfo + "', last info='"+mClient.getLastPushMessage()+"'");

        if (messageInfo.equals(mClient.getLastPushMessage())) {
            LOG.debug("info has already been pushed, nothing new to push for client " + mClientId);
            return;
        }

        // try to perform push
        if (mConfig.isGcmEnabled() && mClient.isGcmCapable()) {
            performGcm();
        } else if (mConfig.isApnsEnabled() && mClient.isApnsCapable()) {
            performApns(deliveringCount);
        } else {
            if (mClient.isPushCapable()) {
                LOG.warn("client " + mClient + " push not available");
            } else {
                LOG.info("client " + mClientId + " has no registration");
            }
            return;
        }
        mClient.setLastPushMessage(messageInfo);
        database.saveClient(mClient);
    }

    private void performGcm() {
        LOG.info("GCM push for " + mClientId);
        Message message = new Message.Builder()
                .collapseKey("com.hoccer.talk.wake")
                .timeToLive(TalkServerConfiguration.GCM_WAKE_TTL)
                .restrictedPackageName(mClient.getGcmPackage())
                .dryRun(false) //TODO: maybe make a setting out of it
                .build();
        Sender gcmSender = mAgent.getGcmSender();
        try {
            Result res = gcmSender.send(message, mClient.getGcmRegistration(), 10);
            if (res.getMessageId() != null) {
                if (res.getCanonicalRegistrationId() != null) {
                    LOG.warn("GCM returned a canonical registration id - we should do something with it");
                }
                LOG.debug("GCM push successful, return message id " + res.getMessageId());
            } else {
                LOG.error("GCM push returned error '" + res.getErrorCodeName() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performApns(int deliveringCount) {
        String clientName = mConfig.getApnsDefaultClientName();
        PushAgent.APNS_SERVICE_TYPE type = PushAgent.APNS_SERVICE_TYPE.PRODUCTION;

        if (mClientHostInfo != null) {
            clientName = mClientHostInfo.getClientName();

            if ("debug".equals(mClientHostInfo.getClientBuildVariant())) {
                type = PushAgent.APNS_SERVICE_TYPE.SANDBOX;
            }
        }

        ApnsService apnsService = mAgent.getApnsService(clientName, type);

        if (apnsService != null) {
            LOG.info("APNS push for " + mClientId + " using " + type + " type");

            PayloadBuilder b = APNS.newPayload();
            int messageCount = deliveringCount + mClient.getApnsUnreadMessages();
            if (messageCount > 1) {
                b.localizedKey("apn_new_messages");
                b.localizedArguments(String.valueOf(messageCount));
            } else {
                b.localizedKey("apn_one_new_message");
            }

            b.badge(messageCount);
            b.sound("default");
            apnsService.push(mClient.getApnsToken(), b.build());
        } else {
            LOG.error("APNS push skipped, no service configured for clientName '" + clientName + "' and type '" + type + "'");
        }
    }
}
