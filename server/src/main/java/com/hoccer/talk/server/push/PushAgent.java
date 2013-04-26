package com.hoccer.talk.server.push;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.android.gcm.server.Sender;
import com.hoccer.talk.logging.HoccerLoggers;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsServiceBuilder;

public class PushAgent {

    private static final Logger LOG = HoccerLoggers.getLogger(PushAgent.class);

	private ScheduledExecutorService mExecutor;

    TalkServer mServer;
    TalkServerConfiguration mConfig;
    ITalkServerDatabase mDatabase;

	private Sender mGcmSender;

    private ApnsService mApnsService;
	
	public PushAgent(TalkServer server) {
		mExecutor = Executors.newScheduledThreadPool(TalkServerConfiguration.THREADS_PUSH);
        mServer = server;
        mDatabase = mServer.getDatabase();
        mConfig = mServer.getConfiguration();
        if(mConfig.ismGcmEnabled()) {
            initializeGcm();
        }
        if(mConfig.ismApnsEnabled()) {
            initializeApns();
        }
    }

    public void submitRequest(TalkClient client) {
        final PushRequest request = new PushRequest(this, client);
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                request.perform();
            }
        }, 5, TimeUnit.SECONDS);
    }

    public TalkServerConfiguration getConfiguration() {
        return mConfig;
    }

    public ITalkServerDatabase getDatabase() {
        return mDatabase;
    }

    public Sender getGcmSender() {
        return mGcmSender;
    }

    public ApnsService getApnsService() {
        return mApnsService;
    }

    private void initializeGcm() {
        LOG.info("GCM support enabled");
        mGcmSender = new Sender(mConfig.getmGcmApiKey());
    }

    private void initializeApns() {
        LOG.info("APNS support enabled");
        ApnsServiceBuilder apnsServiceBuilder = APNS.newService()
                .withCert(mConfig.getmApnsCertPath(),
                          mConfig.getmApnsCertPassword());
        if(mConfig.ismApnsSandbox()) {
            apnsServiceBuilder = apnsServiceBuilder.withSandboxDestination();
        }
        mApnsService = apnsServiceBuilder.build();
    }

}
