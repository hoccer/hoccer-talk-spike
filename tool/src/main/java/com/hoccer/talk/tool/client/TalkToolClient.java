package com.hoccer.talk.tool.client;

import better.cli.console.Console;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.tool.TalkToolContext;

import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.*;

public class TalkToolClient {

    TalkToolContext mContext;

    int mId;

    XoClient mClient;

    TalkToolClientHost mHost;

    TalkToolClientDatabase mDatabaseBackend;

    private Boolean mSupportModeEnabled = false;
    private String mSupportTag = "log";

    private ScheduledExecutorService mLocationUpdater;
    private ScheduledFuture mNearbyUpdater;

    // https://www.google.com/maps/place//@52.5017778,13.3427778,14z
    private final static double GEO_LONGITUDE = 52.501772;
    private final static double GEO_LATITUDE = 13.342769;

    private final static int NEARBY_UPDATE_RATE = 5; //in seconds
    private final static float NEARBY_ACCURACY = 0.0f;

    public TalkToolClient(TalkToolContext context) {
        mContext = context;
        mId = context.generateId();
    }

    public void initialize() throws SQLException {
        mDatabaseBackend = new TalkToolClientDatabase(this);
        mDatabaseBackend.initializeDb();
        mHost = new TalkToolClientHost(this);
        mClient = new XoClient(mHost);

        mLocationUpdater = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        mClient.activate();
    }

    public void stop() {
        mClient.deactivate();
    }

    public void wake() {
        mClient.wake();
    }

    public void setNearby(Boolean enabled) {
        if (enabled) {
            enableNearby();
        } else {
            disableNearby();
        }
    }

    public Double[] getGeoLocation() {
        return new Double[] {
            GEO_LONGITUDE,
            GEO_LATITUDE
        };
    }

    private void enableNearby() {
        if (mNearbyUpdater != null) {
            Console.warn("Environment updates are already running - IGNORING");
            return;
        }

        mNearbyUpdater = mLocationUpdater.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Console.info("running environment update...");
                TalkEnvironment environment = new TalkEnvironment();
                environment.setGeoLocation(getGeoLocation());
                environment.setAccuracy(NEARBY_ACCURACY);
                environment.setLocationType(TalkEnvironment.LOCATION_TYPE_GPS);
                environment.setTimestamp(new Date());

                mClient.setEnvironment(environment);
                mClient.sendEnvironmentUpdate();
            }
        }, 0, NEARBY_UPDATE_RATE, TimeUnit.SECONDS);
    }

    private void disableNearby() {
        if (mNearbyUpdater != null) {
            mNearbyUpdater.cancel(true);
            mNearbyUpdater = null;
            mClient.sendDestroyEnvironment();
            Console.info("environment updates disabled.");
        } else {
            Console.info("but it isn't even running yet!");
        }
    }

    public int getId() {
        return mId;
    }

    public TalkToolContext getContext() {
        return mContext;
    }

    public XoClient getClient() {
        return mClient;
    }

    public TalkToolClientDatabase getDatabaseBackend() {
        return mDatabaseBackend;
    }

    public XoClientDatabase getDatabase() {
        return mClient.getDatabase();
    }

    public String getClientId() {
        try {
            TalkClientContact self = mClient.getDatabase().findSelfContact(true);
            return self.getClientId();
        } catch (SQLException e) {
        }
        return null;
    }

    public Boolean getSupportMode() {
        return mSupportModeEnabled;
    }

    public String getSupportTag() {
        return mSupportTag;
    }

    public void updateSupportMode(Boolean modeEnabled, String tag) {
        if (modeEnabled != null) {
            mSupportModeEnabled = modeEnabled;
        }
        if (tag != null) {
            mSupportTag = tag;
        }
        if (mClient.isAwake()) {
            mClient.hello();
        }
    }
}
