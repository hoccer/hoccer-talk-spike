package com.hoccer.xo.android;


import com.hoccer.talk.client.IXoClientConfiguration;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkEnvironment;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class WorldwideController {

    private static final Logger LOG = Logger.getLogger(WorldwideController.class);

    private static WorldwideController sInstance;
    private final IXoClientConfiguration mConfiguration;

    private IXoStateListener mStateListener;
    private TalkEnvironment mEnvironment;

    private boolean mShouldActivateWorldwide;

    public static WorldwideController get() {
        if (sInstance == null) {
            sInstance = new WorldwideController();
        }
        return sInstance;
    }

    private WorldwideController() {
        mStateListener = new IXoStateListener() {
            @Override
            public void onClientStateChange(XoClient client) {
                if (client.isReady()) {
                    if (mShouldActivateWorldwide) {
                        activateWorldwide();
                    }
                }
            }
        };

        XoApplication.get().getXoClient().registerStateListener(mStateListener);

        mConfiguration = XoApplication.get().getXoClient().getConfiguration();
    }

    public void activateWorldwide() {
        mShouldActivateWorldwide = true;
        mEnvironment = createWorldwideEnvironment();
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
        if (XoApplication.get().getXoClient().getState() == XoClient.State.READY) {
            if (mEnvironment.isValid()) {
                LOG.info("Sending environment update: " + mEnvironment.toString());
                XoApplication.get().getXoClient().sendEnvironmentUpdate(mEnvironment);
            }
        }
    }

    public void deactivateWorldWide() {
        mShouldActivateWorldwide = false;
        XoApplication.get().getXoClient().sendDestroyEnvironment(TalkEnvironment.TYPE_WORLDWIDE);
        mEnvironment = null;
    }

    public void updateTimeToLive(long timeToLive) {
        if (mEnvironment != null) {
            mEnvironment.setTimeToLive(timeToLive);
            sendEnvironmentUpdate();
        }
    }

    public void updateNotificationPreference(String notificationPreference) {
        if (mEnvironment != null) {
            mEnvironment.setNotificationPreference(notificationPreference);
            sendEnvironmentUpdate();
        }
    }
}
