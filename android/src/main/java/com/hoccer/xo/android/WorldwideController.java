package com.hoccer.xo.android;


import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.model.TalkEnvironment;

import java.util.Date;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class WorldwideController {

    private static WorldwideController sInstance;

    private IXoStateListener mStateListener;

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
                    sendEnvironmentUpdate(createWorldwideEnvironment());
                }
            }
        };
    }

    public void enableWorldwide() {
        XoApplication.get().getXoClient().registerStateListener(mStateListener);
        sendEnvironmentUpdate(createWorldwideEnvironment());
    }

    private TalkEnvironment createWorldwideEnvironment() {
        TalkEnvironment environment = new TalkEnvironment();
        environment.setType(TYPE_WORLDWIDE);
        environment.setTimeToLive(0);
        environment.setTimestamp(new Date());
//        environment.setNotificationPreference(TalkRelationship.NOTIFICATIONS_ENABLED); //TODO #826
//        environment.setTag("*"); TODO: send tag?
        return environment;
    }

    private void sendEnvironmentUpdate(TalkEnvironment environment) {
        if (XoApplication.get().getXoClient().getState() == XoClient.State.READY) {
            if (environment.isValid()) {
                XoApplication.get().getXoClient().sendEnvironmentUpdate(environment);
            }
        }
    }

    public void disableWorldWide() {
        XoApplication.get().getXoClient().unregisterStateListener(mStateListener);
        XoApplication.get().getXoClient().sendDestroyEnvironment(TalkEnvironment.TYPE_WORLDWIDE);
    }
}
