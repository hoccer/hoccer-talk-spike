package com.hoccer.xo.android.gesture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.hoccer.xo.android.gesture.Gestures.Transaction;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MotionInterpreter implements SensorEventListener {

    private static final Logger LOG = Logger.getLogger(MotionInterpreter.class);

    private static final long GESTURE_EXCLUSION_TIMESPAN = 1500;

    private final Context mContext;
    private final MotionGestureListener mListener;
    private List<Detector> mDetectors;

    private long mLastGestureTime;

    private final FeatureHistory mFeatureHistory;

    private boolean mActivated;

    public MotionInterpreter(Transaction mode, Context context, MotionGestureListener shakeListener) {
        LOG.debug("Creating GestureInterpreter");
        mContext = context;

        mFeatureHistory = new FeatureHistory();
        mLastGestureTime = 0;
        setMode(mode);

        mListener = shakeListener;
    }

    public FeatureHistory getFeatureHistory() {
        return mFeatureHistory;
    }

    public void addGestureDetector(Detector detector) {
        mDetectors.add(detector);
    }

    public void deactivate() {
        if (mContext == null || !mActivated) {
            return;
        }

        SensorManager sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);

        synchronized (mFeatureHistory) {
            mFeatureHistory.clear();
            mLastGestureTime = 0;
        }

        mActivated = false;
    }

    public void activate() {
        if (mContext == null || mActivated) {
            return;
        }

        SensorManager sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        mActivated = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        handleSensorChange(event.values, event.timestamp / 1000000);
    }

    public void handleSensorChange(float[] pValues, long pTimestamp) {
        handleSensorChange(new Vec3D(pValues[0], pValues[1], pValues[2]), pTimestamp);
    }

    public void handleSensorChange(Vec3D pMeasurement, long pTimestamp) {

        synchronized (mFeatureHistory) {

            mFeatureHistory.add(pMeasurement, pTimestamp);

            for (Detector detector : mDetectors) {
                handleGesture(pTimestamp, detector.detect(mFeatureHistory));
            }

        }

    }

    private void handleGesture(long pTimestamp, int pGesture) {
        if (pGesture != Gestures.NO_GESTURE
                && (pTimestamp - mLastGestureTime > GESTURE_EXCLUSION_TIMESPAN)) {
            LOG.debug("Gesture detected: " + Gestures.GESTURE_NAMES.get(pGesture));

            if (mListener != null) {
                mListener.onMotionGesture(pGesture);
            }

            mLastGestureTime = pTimestamp;
            mFeatureHistory.clear();
        }
    }

    private void setMode(Transaction pMode) {
        mDetectors = new ArrayList<Detector>();

        if (pMode == Transaction.SHARE || pMode == Transaction.SHARE_AND_RECEIVE) {
            addGestureDetector(new ThrowDetector());
        }
        if (pMode == Transaction.RECEIVE || pMode == Transaction.SHARE_AND_RECEIVE) {
            addGestureDetector(new CatchDetector());
        }
    }

    public boolean isActivated() {
        return mActivated;
    }

}
