package com.hoccer.xo.android.view;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 *
 * Taken from ANDROID_SDK/samples/android-19/legacy/ApiDemos/src/com/example/android/apis/graphics/CameraPreview.java
 */
public class CameraPreviewView extends ViewGroup implements SurfaceHolder.Callback {
    private static final Logger LOG = Logger.getLogger(CameraPreviewView.class);

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera.Size mPreviewSize;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera mCamera;
    private boolean mIsPortraitModeEnabled;
    private boolean mIsEnabled;
    private boolean mIsSurfaceCreated;

    private Handler mAutoFocusHandler = new Handler();
    private Runnable mAutoFocusRunnable;

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, final Camera camera) {
            mAutoFocusRunnable = new Runnable() {
                @Override
                public void run() {
                    camera.autoFocus(mAutoFocusCallback);
                }
            };

            mAutoFocusHandler.postDelayed(mAutoFocusRunnable, 1000);
        }
    };

    public CameraPreviewView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        mIsPortraitModeEnabled = attributeSet.getAttributeBooleanValue(null, "portrait", false);
        mIsEnabled = true;
        mIsSurfaceCreated = false;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            if (mIsPortraitModeEnabled) {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, height, width);
            } else {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            }
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        final int MIN_WIDTH = Math.max(w / 2, 640);
        final int MIN_HEIGHT = Math.max(h / 2, 480);

        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            if (size.width < MIN_WIDTH || size.height < MIN_HEIGHT) continue;

            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            if (mIsPortraitModeEnabled) {
                int temp = previewHeight;
                previewHeight = previewWidth;
                previewWidth = temp;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            } else {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mIsSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsSurfaceCreated = false;
        stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        requestLayout();
        startPreview();
    }

    public void startPreview() {
        if (mIsEnabled && mIsSurfaceCreated && mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                LOG.error("IOException caused by setPreviewDisplay()", e);
            }

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);

            if (mIsPortraitModeEnabled) {
                mCamera.setDisplayOrientation(90);
            }

            mCamera.startPreview();
            mCamera.autoFocus(mAutoFocusCallback);
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mAutoFocusHandler.removeCallbacks(mAutoFocusRunnable);
            mCamera.cancelAutoFocus();
            mCamera.stopPreview();
        }
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }
}
