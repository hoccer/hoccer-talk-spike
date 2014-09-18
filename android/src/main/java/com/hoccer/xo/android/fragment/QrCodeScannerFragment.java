package com.hoccer.xo.android.fragment;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.CameraPreviewView;
import com.hoccer.xo.release.R;
import net.sourceforge.zbar.*;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class QrCodeScannerFragment extends PagerFragment {

    private static final Logger LOG = Logger.getLogger(QrCodeScannerFragment.class);

    private Camera mCamera;
    private CameraPreviewView mCameraPreviewView;

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
        mCameraPreviewView = (CameraPreviewView)view.findViewById(R.id.cpv_camera_preview);
    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();

        boolean useAutoFocus = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    private void openCamera() {
        try {
            mCamera = Camera.open();
            mCameraPreviewView.setCamera(mCamera);
        } catch (Exception e) {
            LOG.error("Error opening camera", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        closeCamera();
    }

    private void closeCamera() {
        mCameraPreviewView.setCamera(null);
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }
}
