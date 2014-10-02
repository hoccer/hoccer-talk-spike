package com.hoccer.xo.android.nearby;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.xo.android.error.EnvironmentUpdaterException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class EnvironmentUpdater implements LocationListener {

    private static final Logger LOG = Logger.getLogger(EnvironmentUpdater.class);

    private static final long MIN_UPDATE_TIME = 1000;
    private static final long MIN_UPDATE_MOVED = 5;

    private final XoClient mClient;

    private final LocationManager mLocationManager;
    private final WifiManager mWifiManager;

    private boolean mIsEnabled = false;

    public EnvironmentUpdater(Context context, XoClient client) {
        mClient = client;

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void startEnvironmentTracking() throws EnvironmentUpdaterException {
        // TODO: handle failed startups
        mIsEnabled = true;

        if (!isGpsProviderEnabled() && !isNetworkProviderEnabled()) {
            throw new EnvironmentUpdaterException("no source for environment information available");
        }

        if (isGpsProviderEnabled()) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_MOVED, this);
        }

        if (isNetworkProviderEnabled()) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_MOVED, this);
        }

        sendEnvironmentUpdate();
    }

    public void stopEnvironmentTracking() {
        mLocationManager.removeUpdates(this);
        mClient.sendDestroyEnvironment(TalkEnvironment.TYPE_NEARBY);
        mIsEnabled = false;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    private boolean isGpsProviderEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean isNetworkProviderEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void sendEnvironmentUpdate() {
        TalkEnvironment environment = getEnvironment();
        if (environment.isValid()) {
            mClient.sendEnvironmentUpdate(environment);
        }
    }

    private TalkEnvironment getEnvironment() {
        LOG.trace("getEnvironment()");

        TalkEnvironment environment = new TalkEnvironment();
        environment.setType(TalkEnvironment.TYPE_NEARBY);
        environment.setTimestamp(new Date());

        Location networkLocation = null;
        Location gpsLocation = null;
        if (isNetworkProviderEnabled()) {
            networkLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (isGpsProviderEnabled()) {
            gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        Location location;
        if (gpsLocation != null && networkLocation != null) {
            // both available, select most precise
            if (gpsLocation.getAccuracy() < networkLocation.getAccuracy()) {
                location = gpsLocation;
            } else {
                location = networkLocation;
            }
        } else {
            location = gpsLocation != null ? gpsLocation : networkLocation;
        }

        if (location != null) {
            Double[] geoLocation = {location.getLongitude(), location.getLatitude()};
            environment.setGeoLocation(geoLocation);
            environment.setLocationType(location.getProvider());
            if (location.hasAccuracy()) {
                environment.setAccuracy(location.getAccuracy());
            } else {
                environment.setAccuracy(0.0f);
            }
        }

        // wifi scan result
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        if (scanResults != null) {
            List<String> bssids = new ArrayList<String>();
            for (ScanResult scan : scanResults) {
                bssids.add(scan.BSSID);
            }
            environment.setBssids(bssids.toArray(new String[bssids.size()]));
        }

        return environment;
    }

    @Override
    public void onLocationChanged(Location location) {
        LOG.debug("onLocationChanged:" + location.toString());
        if (mIsEnabled) {
            sendEnvironmentUpdate();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        LOG.debug("ignoring onProviderDisabled: " + provider);
        // we're only interested in onLocationChanged()
    }

    @Override
    public void onProviderEnabled(String provider) {
        LOG.debug("ignoring onProviderEnabled: " + provider);
        // we're only interested in onLocationChanged()
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LOG.debug("ignoring onStatusChanged: " + provider);
        // we're only interested in onLocationChanged()
    }
}
