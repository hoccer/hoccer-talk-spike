package com.hoccer.xo.android.fragment;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.CameraPreviewView;
import com.hoccer.xo.release.R;
import net.sourceforge.zbar.*;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class QrCodeScannerFragment extends Fragment {

    private static final Logger LOG = Logger.getLogger(QrCodeScannerFragment.class);

    private Camera mCamera;
    private FrameLayout mCameraPreviewLayout;

    private final ImageScanner mQrCodeScanner = new ImageScanner();
    private final HashSet<String> mScannedCodes = new HashSet<String>();

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image image = new Image(size.width, size.height, "Y800");
            image.setData(data);

            int result = mQrCodeScanner.scanImage(image);
            if (result != 0) {
                SymbolSet symbols = mQrCodeScanner.getResults();

                for (Symbol symbol : symbols) {
                    String code = symbol.getData();

                    if (!mScannedCodes.contains(code)) {
                        if (code.startsWith(XoApplication.getXoClient().getConfiguration().getUrlScheme())) {
                            String pairingToken = code.replace(XoApplication.getXoClient().getConfiguration().getUrlScheme(), "");
                            XoApplication.getXoClient().performTokenPairing(pairingToken);
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_pairing_failed), Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        mScannedCodes.add(code);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQrCodeScanner.setConfig(0, Config.X_DENSITY, 3);
        mQrCodeScanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_code_scanner, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mCameraPreviewLayout = (FrameLayout)view.findViewById(R.id.fl_camera_preview);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCamera = openCamera();

        boolean autoFocus = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        CameraPreviewView cameraPreview = new CameraPreviewView(getActivity(), mCamera, mPreviewCallback, autoFocus);
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
