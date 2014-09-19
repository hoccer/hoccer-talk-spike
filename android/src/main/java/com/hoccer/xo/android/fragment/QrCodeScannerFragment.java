package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.hoccer.talk.client.IXoPairingListener;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.CameraPreviewView;
import com.hoccer.xo.release.R;
import net.sourceforge.zbar.*;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class QrCodeScannerFragment extends PagerFragment implements IXoPairingListener {

    private static final Logger LOG = Logger.getLogger(QrCodeScannerFragment.class);

    private Camera mCamera;
    private CameraPreviewView mCameraPreviewView;
    private EditText mPairingTokenEditText;
    private Button mConfirmCodeButton;

    static {
        // required by zbar
        System.loadLibrary("iconv");
    }

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
                            XoApplication.getXoClient().performTokenPairing(pairingToken, QrCodeScannerFragment.this);
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
        mPairingTokenEditText = (EditText)view.findViewById(R.id.et_pairing_token);
        mConfirmCodeButton = (Button)view.findViewById(R.id.b_confirm_code);

        mPairingTokenEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideSoftKeyboard();
                }
            }
        });

        mPairingTokenEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mPairingTokenEditText.getText().toString().isEmpty()) {
                    mConfirmCodeButton.setEnabled(true);
                } else {
                    mConfirmCodeButton.setEnabled(false);
                }
            }
        });

        mPairingTokenEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    performTokenPairing();
                    return true;
                }

                return false;
            }
        });

        mConfirmCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTokenPairing();
            }
        });
    }

    private void performTokenPairing() {
        String pairingToken = mPairingTokenEditText.getText().toString();

        if (pairingToken != null && !pairingToken.isEmpty()) {
            mConfirmCodeButton.setEnabled(false);
            mPairingTokenEditText.clearFocus();
            XoApplication.getXoClient().performTokenPairing(pairingToken, this);
            hideSoftKeyboard();
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();
        startPreview();

        boolean useAutoFocus = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    private void openCamera() {
        try {
            mCamera = Camera.open();
            mCamera.setPreviewCallback(mPreviewCallback);
            mCameraPreviewView.setCamera(mCamera);
        } catch (Exception e) {
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
    public void onPageSelected() {
        enableAndStartPreview();
    }

    private void enableAndStartPreview() {
        if (mCameraPreviewView != null) {
            mCameraPreviewView.setEnabled(true);
        }

        startPreview();
    }

    @Override
    public void onPageUnselected() {
        stopAndDisablePreview();

        if (mPairingTokenEditText != null) {
            mPairingTokenEditText.clearFocus();
        }
    }

    private void stopAndDisablePreview() {
        stopPreview();

        if (mCameraPreviewView != null) {
            mCameraPreviewView.setEnabled(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            enableAndStartPreview();
        } else {
            stopAndDisablePreview();
        }
    }

    @Override
    public void onTokenPairingSucceeded(String token) {
        final Activity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, getResources().getString(R.string.toast_pairing_successful), Toast.LENGTH_LONG).show();

                mPairingTokenEditText.setText("");
                mConfirmCodeButton.setEnabled(true);
            }
        });
    }

    @Override
    public void onTokenPairingFailed(String token) {
        final Activity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_pairing_failed), Toast.LENGTH_LONG).show();

                mConfirmCodeButton.setEnabled(true);
            }
        });
    }

}
