package com.hoccer.talk.server.push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.filter.StringMatchFilter;

import java.io.IOException;
import java.util.ArrayList;
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
    private final List<TalkDelivery> mDeliveries;
    private List<TalkDelivery> mNewDeliveries;

    public Date getCreatedTime() {
        return mCreatedTime;
    }

    public PushRequest(PushAgent agent, String clientId, TalkClientHostInfo clientHostInfo, List<TalkDelivery> deliveries) {
        mAgent = agent;
        mConfig = mAgent.getConfiguration();
        mClientId = clientId;
        mClientHostInfo = clientHostInfo;
        mDeliveries = deliveries;
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

    public List<TalkDelivery> getDeliveries() {
        return mDeliveries;
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

        mNewDeliveries = new ArrayList<TalkDelivery>();
        Date latest = new Date(0);
        Date lastLatest = new Date(0);
        if (mClient.getLatestPushMessageTime() != null) {
            lastLatest = mClient.getLatestPushMessageTime();
        }
        for (TalkDelivery delivery : deliveries) {
            if (delivery.getTimeAccepted().after(lastLatest)) {
                mNewDeliveries.add(delivery);
                if (delivery.getTimeAccepted().after(latest)) {
                    latest = delivery.getTimeAccepted();
                }
            }
        }

        String messageInfo = "undelivered:"+deliveringCount+":"+ String.valueOf(latest.getTime());
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
        mClient.setLatestPushMessageTime(latest);
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

            boolean done = false;
            LOG.debug("APNS push for " + mClientId + " using " + type + " type, token "+mClient.getApnsToken());

            PayloadBuilder b = APNS.newPayload();

            int messageCount = deliveringCount + mClient.getApnsUnreadMessages();

            if (TalkClient.APNS_MODE_BACKGROUND.equals(mClient.getApnsMode())) {
                // background message
                LOG.debug("Performing APNS background push configured for "+mClientId + " clientName '" + clientName + "' and type '" + type + "'");
                b.badge(messageCount);
                b.forNewsstand();
                backgroundPush = true;
            } else if (TalkClient.APNS_MODE_DIRECT.equals(mClient.getApnsMode()) && mDeliveries != null) {
                ITalkServerDatabase database = mAgent.getDatabase();
                for (TalkDelivery delivery : mNewDeliveries) {
                    TalkMessage message = database.findMessageById(delivery.getMessageId());
                    if (message == null) {
                        LOG.warn("APNS push: message not found: " + delivery.getMessageId());
                    } else {
                        String cipherText = delivery.getKeyCiphertext();
                        String keyId = delivery.getKeyId();
                        String body = message.getBody();
                        String salt = message.getSalt();
                        // TODO: send group and sender as hashes
                        String sender = message.getSenderId();
                        String group = delivery.getGroupId();
                        String saltAttr = "";
                        if (salt != null) {
                            saltAttr = ", \"salt\":\"" + salt +"\"";
                        }
                        String groupAttr = "";
                        if (group != null) {
                            groupAttr = ", \"group\":\"" + group +"\"";
                        }
                        String apnMessage = "{\"aps\":{\"badge\":" + String.valueOf(messageCount) +
                                ",\"alert\":{\"title\":\"Message\",\"body\":\"has arrived\"},\"sound\":\"default\",\"mutable-content\":1},\"keyCiphertext\":\"" +
                                cipherText + "\", \"keyId\":\"" + keyId + "\", \"body\":\"" + body + "\", \"sender\":\""+sender+"\""+groupAttr+saltAttr+"}";
                        if (apnMessage.length() < 4096) {
                            LOG.debug("APNS direct push for " + mClientId + ", message:" + apnMessage);
                            apnsService.push(mClient.getApnsToken(), apnMessage);
                            done = true;
                        } else {
                            LOG.debug("APNS message too long for direct push for client " + mClientId + ", message:" + apnMessage);
                        }
                    }
                }
            }
            if (!done && !backgroundPush) {
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
            if (!done) {
                String message = b.build();
                LOG.debug("APNS push for " + mClientId + "message:" + message);
                apnsService.push(mClient.getApnsToken(), message);
            }

            return backgroundPush;
        } else {
            LOG.error("APNS push skipped, no service configured for clientName '" + clientName + "' and type '" + type + "'");
        }
        return false;
    }
}
