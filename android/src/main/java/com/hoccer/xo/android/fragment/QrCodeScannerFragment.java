package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class QrCodeScannerFragment extends Fragment implements IPagerFragment, IXoPairingListener {

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

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(final byte[] data, final Camera camera) {
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
                        if (code.startsWith(XoApplication.getXoClient().getConfiguration().getUrlScheme())) {
                            final String pairingToken = code.replace(XoApplication.getXoClient().getConfiguration().getUrlScheme(), "");
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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQrCodeScanner.setConfig(0, Config.X_DENSITY, 3);
        mQrCodeScanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_code_scanner, null);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
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
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
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
        final String pairingToken = mPairingTokenEditText.getText().toString();

        if (pairingToken != null && !pairingToken.isEmpty()) {
            mConfirmCodeButton.setEnabled(false);
            mPairingTokenEditText.clearFocus();
            XoApplication.getXoClient().performTokenPairing(pairingToken, this);
        }
    }

    private void hideSoftKeyboard() {
        final InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
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
                Toast.makeText(activity, getResources().getString(R.string.toast_pairing_successful), Toast.LENGTH_LONG).show();

                mPairingTokenEditText.setText("");
                mConfirmCodeButton.setEnabled(true);
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

                mConfirmCodeButton.setEnabled(true);
            }
        });
    }

}
