package com.hoccer.xo.android.nearby;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;


public class NearbyEnvironmentUpdater {

    private static final Logger LOG = Logger.getLogger(NearbyEnvironmentUpdater.class);

    private static final long MIN_UPDATE_TIME = 1000;
    private static final long MIN_UPDATE_MOVED = 5;

    private final XoClient mClient;

    private final LocationManager mLocationManager;
    private final WifiManager mWifiManager;

    private final LocationChangedListener mGPSLocationListener = new LocationChangedListener();
    private final LocationChangedListener mNetworkLocationListener = new LocationChangedListener();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public NearbyEnvironmentUpdater(Context context, XoClient client) {
        mClient = client;

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void start() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isProviderEnabled(NETWORK_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(NETWORK_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_MOVED, mNetworkLocationListener);
                }
                if (isProviderEnabled(GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(GPS_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_MOVED, mGPSLocationListener);
                }
            }
        });

        sendEnvironmentUpdate();
    }

    public void pause() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLocationManager.removeUpdates(mGPSLocationListener);
                mLocationManager.removeUpdates(mNetworkLocationListener);
            }
        });
    }

    public void stop() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLocationManager.removeUpdates(mGPSLocationListener);
                mLocationManager.removeUpdates(mNetworkLocationListener);
                mClient.sendDestroyEnvironment(TalkEnvironment.TYPE_NEARBY);
            }
        });
    }

    public void sendEnvironmentUpdate() {
        XoApplication.get().getExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                TalkEnvironment environment = getEnvironment();
                if (environment.isValid()) {
                    mClient.sendEnvironmentUpdate(environment);
                }
            }
        }, 0, TimeUnit.SECONDS);
    }

    public boolean locationServicesEnabled() {
        return isProviderEnabled(NETWORK_PROVIDER) || isProviderEnabled(GPS_PROVIDER);
    }

    private boolean isProviderEnabled(String provider) {
        return mLocationManager.getAllProviders() != null
                && mLocationManager.getAllProviders().contains(provider)
                && mLocationManager.isProviderEnabled(provider);
    }

    private TalkEnvironment getEnvironment() {
        LOG.trace("getEnvironment()");

        TalkEnvironment environment = new TalkEnvironment();
        environment.setType(TalkEnvironment.TYPE_NEARBY);
        environment.setTimestamp(new Date());

        Location networkLocation = null;
        Location gpsLocation = null;
        if (isProviderEnabled(NETWORK_PROVIDER)) {
            networkLocation = mLocationManager.getLastKnownLocation(NETWORK_PROVIDER);
        }
        if (isProviderEnabled(GPS_PROVIDER)) {
            gpsLocation = mLocationManager.getLastKnownLocation(GPS_PROVIDER);
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

    private class LocationChangedListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            sendEnvironmentUpdate();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    }
}
