package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.CameraPreviewView;
import net.sourceforge.zbar.*;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class QrCodeScannerFragment extends Fragment implements IPagerFragment, IXoPairingListener {

    private static final Logger LOG = Logger.getLogger(QrCodeScannerFragment.class);

    private Camera mCamera;
    private CameraPreviewView mCameraPreviewView;

    static {
        // required by zbar
        System.loadLibrary("iconv");
    }

    private final ImageScanner mQrCodeScanner = new ImageScanner();
    private final HashSet<String> mScannedCodes = new HashSet<String>();

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(final byte[] data, final Camera camera) {
            LOG.debug("onPreviewFrame");
            final Camera.Parameters parameters = camera.getParameters();
            final Camera.Size size = parameters.getPreviewSize();

            final Image image = new Image(size.width, size.height, "Y800");
            image.setData(data);

            final int result = mQrCodeScanner.scanImage(image);
            if (result != 0) {
                final SymbolSet symbols = mQrCodeScanner.getResults();

                for (final Symbol symbol : symbols) {
                    final String code = symbol.getData();

                    if (!mScannedCodes.contains(code)) {
                        final String pairingToken = UriUtils.getAbsoluteFileUri(code).getAuthority();
                        XoApplication.get().getXoClient().performTokenPairing(pairingToken, QrCodeScannerFragment.this);
                        mScannedCodes.add(code);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQrCodeScanner.setConfig(0, Config.X_DENSITY, 3);
        mQrCodeScanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_qr_code_scanner, null);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        mCameraPreviewView = (CameraPreviewView) view.findViewById(R.id.cpv_camera_preview);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_scan, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_enter_code:
                stopAndDisablePreview();
                showEnterCodeDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEnterCodeDialog() {
        XoDialogs.showInputTextDialog("CodeInputDialog", R.string.enter_code, -1, R.string.invite_code, getActivity(),
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String input) {
                        enableAndStartPreview();
                        performTokenPairing(input);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableAndStartPreview();
                    }
                }
        );
    }

    private void performTokenPairing(String token) {
        if (token != null && !token.isEmpty()) {
            XoApplication.get().getXoClient().performTokenPairing(token, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();
        startPreview();
    }

    private void openCamera() {
        try {
            mCamera = Camera.open();
            mCamera.setPreviewCallback(mPreviewCallback);
            mCameraPreviewView.setCamera(mCamera);
        } catch (final Exception e) {
            LOG.error("Error opening camera", e);
        }
    }

    private void startPreview() {
        if (mCameraPreviewView != null) {
            mCameraPreviewView.startPreview();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPreview();
        closeCamera();
    }

    private void stopPreview() {
        if (mCameraPreviewView != null) {
            mCameraPreviewView.stopPreview();
        }
    }

    private void closeCamera() {
        mCameraPreviewView.setCamera(null);
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    @Override
    public View getCustomTabView(final Context context) {
        return null;
    }

    @Override
    public String getTabName(final Resources resources) {
        return resources.getString(R.string.qr_code_tab_scan);
    }

    @Override
    public void onPageResume() {
        enableAndStartPreview();
    }

    private void enableAndStartPreview() {
        if (mCameraPreviewView != null) {
            mCameraPreviewView.setEnabled(true);
        }

        startPreview();
    }

    @Override
    public void onPageSelected() {
    }

    @Override
    public void onPageUnselected() {
    }

    @Override
    public void onPagePause() {
        stopAndDisablePreview();
    }

    private void stopAndDisablePreview() {
        stopPreview();

        if (mCameraPreviewView != null) {
            mCameraPreviewView.setEnabled(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            enableAndStartPreview();
        } else {
            stopAndDisablePreview();
        }
    }

    @Override
    public void onTokenPairingSucceeded(final String token) {
        final Activity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().finish();
                Toast.makeText(activity, getResources().getString(R.string.toast_pairing_successful), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onTokenPairingFailed(final String token) {
        final Activity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_pairing_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

}
