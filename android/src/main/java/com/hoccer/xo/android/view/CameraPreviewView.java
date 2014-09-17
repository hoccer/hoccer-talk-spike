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
    private final boolean mAutoFocus;

    public CameraPreviewView(Context context, Camera camera, Camera.PreviewCallback previewCallback, boolean autoFocus) {
        super(context);
        mCamera = camera;
        mPreviewCallback = previewCallback;
        mAutoFocus = autoFocus;

        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LOG.debug("surfaceCreated()");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LOG.debug("surfaceDestroyed()");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LOG.debug("surfaceChanged()");

        if (holder.getSurface() == null) {
            return;
        }

        try {
            if (mAutoFocus) {
                mCamera.cancelAutoFocus();
            }

            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        } catch (Exception e) {
            LOG.error("Error stopping camera preview", e);
        }

        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.startPreview();

            if (mAutoFocus) {
                mCamera.autoFocus(null);
            }
        } catch (Exception e) {
            LOG.error("Error starting camera preview", e);
        }
    }
}
