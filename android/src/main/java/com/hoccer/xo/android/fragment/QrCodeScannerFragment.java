package com.hoccer.xo.android.fragment;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import com.hoccer.xo.android.view.CameraPreviewView;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class QrCodeScannerFragment extends Fragment {

    private static final Logger LOG = Logger.getLogger(QrCodeScannerFragment.class);

    private Camera mCamera;
    private FrameLayout mCameraPreviewLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_code_scanner, null);
        mCameraPreviewLayout = (FrameLayout)view.findViewById(R.id.fl_camera_preview);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCamera = openCamera();

        CameraPreviewView cameraPreview = new CameraPreviewView(getActivity(), mCamera, null, null);
        mCameraPreviewLayout.addView(cameraPreview);
    }

    private Camera openCamera() {
        try {
            return Camera.open();
        } catch (Exception e) {
            LOG.error("Error opening camera", e);
        }

        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraPreviewLayout.removeAllViews();

        closeCamera(mCamera);
        mCamera = null;
    }

    private void closeCamera(Camera camera) {
        camera.setPreviewCallback(null);
        camera.release();
    }
}
