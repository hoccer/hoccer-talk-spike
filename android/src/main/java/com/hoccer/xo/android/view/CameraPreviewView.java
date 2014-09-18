package com.hoccer.xo.android.view;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import org.apache.log4j.Logger;

public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {

    private static final Logger LOG = Logger.getLogger(CameraPreviewView.class);

    private final Camera mCamera;
    private final Camera.PreviewCallback mPreviewCallback;
    private final boolean mUseAutoFocus;
    private boolean mIsPreviewActive;

    public CameraPreviewView(Context context, Camera camera, Camera.PreviewCallback previewCallback, boolean useAutoFocus) {
        super(context);
        mCamera = camera;
        mPreviewCallback = previewCallback;
        mUseAutoFocus = useAutoFocus;
        mIsPreviewActive = false;

        setFocusableInTouchMode(true);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LOG.debug("surfaceCreated()");
        startPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LOG.debug("surfaceDestroyed()");
        stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LOG.debug("surfaceChanged()");

        if (mIsPreviewActive) {
            stopPreview();
            startPreview(holder);
        }
    }

    private void startPreview(SurfaceHolder holder) {
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.startPreview();

            if (mUseAutoFocus) {
                mCamera.autoFocus(null);
            }

            mIsPreviewActive = true;
        } catch (Exception e) {
            LOG.error("Error starting camera preview", e);
        }
    }

    private void stopPreview() {
        try {
            if (mUseAutoFocus) {
                mCamera.cancelAutoFocus();
            }

            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();

            mIsPreviewActive = false;
        } catch (Exception e) {
            LOG.error("Error stopping camera preview", e);
        }
    }
}
