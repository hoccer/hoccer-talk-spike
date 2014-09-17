package com.hoccer.xo.android.view;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {
    private final Camera mCamera;
    private final Camera.PreviewCallback mPreviewCallback;
    private final boolean mAutoFocus;

    public CameraPreviewView(Context context, Camera camera, Camera.PreviewCallback previewCallback, boolean autoFocus) {
        super(context);
        mCamera = camera;
        mPreviewCallback = previewCallback;
        mAutoFocus = autoFocus;

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // do nothing
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
