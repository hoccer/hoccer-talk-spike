package com.hoccer.xo.android;


import com.hoccer.talk.client.IXoClientConfiguration;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.model.TalkEnvironment;
import org.apache.log4j.Logger;

import java.util.Date;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public enum WorldwideController {
    INSTANCE;

    private static final Logger LOG = Logger.getLogger(WorldwideController.class);

    private final IXoClientConfiguration mConfiguration;

    private TalkEnvironment mEnvironment;

    private boolean mEnvironmentReleased = false;

    private WorldwideController() {
        XoApplication.get().getClient().registerStateListener(new IXoStateListener() {
            @Override
            public void onClientStateChange(XoClient client) {
                if (client.isReady()) {
                    if (!mEnvironmentReleased) {
                        updateWorldwide();
                    }
                }
            }
        });

        mConfiguration = XoApplication.get().getClient().getConfiguration();
    }

    public void updateWorldwide() {
        mEnvironment = createWorldwideEnvironment();
        mEnvironmentReleased = false;
        sendEnvironmentUpdate();
    }

    private TalkEnvironment createWorldwideEnvironment() {
        TalkEnvironment environment = new TalkEnvironment();
        environment.setType(TYPE_WORLDWIDE);
        environment.setTimestamp(new Date());
        environment.setTimeToLive(mConfiguration.getTimeToLiveInWorldwide());
        environment.setNotificationPreference(mConfiguration.getNotificationPreferenceForWorldwide());
        return environment;
    }

    private void sendEnvironmentUpdate() {
        if (XoApplication.get().getClient().isReady()) {
            if (mEnvironment.isValid()) {
                LOG.info("Sending environment update: " + mEnvironment.toString());
                XoApplication.get().getClient().sendEnvironmentUpdate(mEnvironment);
            }
        }
    }

    public void releaseWorldWide() {
        XoApplication.get().getClient().sendDestroyEnvironment(TYPE_WORLDWIDE);
        mEnvironmentReleased = true;
    }

    public void stopWorldWideNow(){
        releaseWorldWide();
        releaseEnvironmentUpdatingParameters(0, "false");

    }

    public void updateWorldwideEnvironmentParameters() {
        if (!isWorldwideActive()) {
            return;
        }

        if (mEnvironmentReleased) {
            releaseEnvironmentUpdatingParameters(mConfiguration.getTimeToLiveInWorldwide(), mConfiguration.getNotificationPreferenceForWorldwide());
        } else {
            updateWorldwide();
        }
    }

    private void releaseEnvironmentUpdatingParameters(long timeToLive, String notificationPreference) {
        if (XoApplication.get().getClient().isReady()) {
            XoApplication.get().getClient().getServerRpc().releaseEnvironmentUpdatingParameters(TYPE_WORLDWIDE, timeToLive, notificationPreference);
        }
    }

    public boolean isWorldwideActive() {
        return XoApplication.get().getClient().getWorldwideGroupId() != null;
    }
}
