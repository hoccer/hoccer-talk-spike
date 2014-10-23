package com.hoccer.talk.server.push;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;
import org.apache.log4j.Logger;

import java.io.IOException;

public class PushMessage {

    private static final Logger LOG = Logger.getLogger(PushMessage.class);
    private final String mMessage;
    private final TalkClient mClient;
    private final TalkClientHostInfo mClientHostInfo;

    PushAgent mAgent;
    TalkServerConfiguration mConfig;

    public PushMessage(PushAgent agent, TalkClient client, TalkClientHostInfo clientHostInfo, String message) {
        this.mClient = client;
        this.mMessage = message;
        this.mClientHostInfo = clientHostInfo;

        this.mAgent = agent;
        this.mConfig = mAgent.getConfiguration();
    }

    public void perform() {
        LOG.info("performing push to clientId: '" + mClient.getClientId() + "', message: '" + mMessage + "'");
        if(mConfig.isGcmEnabled() && mClient.isGcmCapable()) {
            performGcm();
        } else if(mConfig.isApnsEnabled() && mClient.isApnsCapable()) {
            performApns();
        } else {
            if(mClient.isPushCapable()) {
                LOG.warn("client " + mClient + " push not available");
            } else {
                LOG.info("client " + mClient.getClientId() + " has no registration");
            }
        }
    }

    private void performApns() {
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
            LOG.info("performApns: to clientId: '" + mClient.getClientId() + "', type: '" + type + "', message: '" + mMessage + "'");

            PayloadBuilder b = APNS.newPayload();
            b.alertBody(mMessage);
            b.sound("default");
            apnsService.push(mClient.getApnsToken(), b.build());
        } else {
            LOG.error("performApns: skipped, no service configured for clientName '" + clientName + "' and type '" + type + "'");
        }
    }

    private void performGcm() {
        LOG.info("GCM message '" + mMessage + "' for " + mClient.getClientId());

        Sender gcmSender = mAgent.getGcmSender();
        Message message = new Message.Builder()
                .restrictedPackageName(mClient.getGcmPackage())
                .addData("message", mMessage)
                .build();

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
            LOG.error("GCM push error", e);
        }
    }
}
