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
    private Future mNearbyUpdater;

    // https://www.google.com/maps/place//@52.5017778,13.3427778,14z
    private final static double DEFAULT_GEO_LONGITUDE = 52.501772;
    private final static double DEFAULT_GEO_LATITUDE = 13.342769;
    private Double[] mGeoLocation;

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
        mClient = new XoClient(mHost, new TalkToolClientConfiguration(this));

        mLocationUpdater = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        mClient.activate();
    }

    public void stop() {
        if (mNearbyUpdater != null) {
            resetNearbyUpdater();
        }
        mClient.disconnect();
    }

    public void wake() {
        mClient.wake();
    }

    public void setNearby(boolean enabled, boolean schedule) {
        if (enabled) {
            enableNearby(schedule);
        } else {
            disableNearby();
        }
    }

    public Double[] getGeoLocation() {
        if (mGeoLocation != null) {
            return mGeoLocation;
        } else {
            Console.debug("No explicit location set yet, using default.");
            return new Double[]{DEFAULT_GEO_LONGITUDE, DEFAULT_GEO_LATITUDE};
        }
    }

    public void setGeoLocation(Double longitude, Double latitude) {
        Console.info("Setting geo-location to (" + longitude + ", " + latitude + ")");
        mGeoLocation = new Double[]{longitude, latitude};
    }

    private void enableNearby(boolean schedule) {
        if (mNearbyUpdater != null) {
            Console.warn("Environment updates are already running - IGNORING");
            return;
        }
        if (schedule) {
            Console.info("Starting environment update scheduler...");
            mNearbyUpdater = mLocationUpdater.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    updateEnvironment();
                }
            }, 0, NEARBY_UPDATE_RATE, TimeUnit.SECONDS);
        } else {
            Console.info("Sending environment update just once");
            mNearbyUpdater = mLocationUpdater.schedule(new Runnable() {
                @Override
                public void run() {
                    updateEnvironment();
                }
            }, 0, TimeUnit.SECONDS);
        }

    }

    private void updateEnvironment() {
        Console.debug("updating environment...");
        TalkEnvironment environment = new TalkEnvironment();
        environment.setGeoLocation(getGeoLocation());
        environment.setAccuracy(NEARBY_ACCURACY);
        environment.setLocationType(TalkEnvironment.LOCATION_TYPE_GPS);
        environment.setTimestamp(new Date());
        environment.setType(TalkEnvironment.TYPE_NEARBY);

        mClient.sendEnvironmentUpdate(environment);
    }

    private void disableNearby() {
        if (mNearbyUpdater != null) {
            resetNearbyUpdater();
            mClient.sendDestroyEnvironment(TalkEnvironment.TYPE_NEARBY);
            Console.info("environment updates disabled.");
        } else {
            Console.info("Nothing to disable - nearby was not running. Doing nothing.");
        }
    }

    private void resetNearbyUpdater() {
        mNearbyUpdater.cancel(true);
        mNearbyUpdater = null;
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
