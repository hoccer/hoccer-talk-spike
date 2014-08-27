package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;
import net.sourceforge.zbar.*;

public class QrScannerActivity extends Activity implements IXoContactListener {
    private ImageScanner scanner;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private int mPairedContact;
    private boolean hasAutoFocus;

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);
            int result = scanner.scanImage(barcode);
            if (result != 0) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                SymbolSet symbols = scanner.getResults();
                for (Symbol sym : symbols) {
                    String code = sym.getData();
                    if (code.startsWith(XoApplication.getXoClient().getConfiguration().getUrlScheme())) {
                        code = code.replace(XoApplication.getXoClient().getConfiguration().getUrlScheme(), "");
                        XoApplication.getXoClient().performTokenPairing(code);
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), getResources().getString(R.string.pairing_incorrect_code), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }
    };

    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            try {
                if (mCamera != null && hasAutoFocus) {
                    mCamera.autoFocus(autoFocusCB);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hasAutoFocus = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        setContentView(R.layout.activity_qr_scanner);
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
        if(hasAutoFocus) {
            autoFocusHandler = new Handler();
        }
        XoApplication.getXoClient().registerContactListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            if (preview.getChildCount() != 0) {
                preview.removeAllViews();
            }
            preview.addView(mPreview);
        }
    }

    private Context getContext() {
        return this;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        if (!isFinishing() && mPairedContact == contact.getClientContactId()) {
            final TalkClientContact newContact = contact;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), getResources().getString(R.string.paired_with) + " " + newContact.getName(), Toast.LENGTH_LONG).show();
                    mPreview.restartPreview();
                }
            });
        }
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        mPairedContact = contact.getClientContactId();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        // do nothing
    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private PreviewCallback previewCallback;
        private AutoFocusCallback autoFocusCallback;

        public CameraPreview(Context context, Camera camera, PreviewCallback previewCb, AutoFocusCallback autoFocusCb) {
            super(context);
            mCamera = camera;
            previewCallback = previewCb;
            autoFocusCallback = autoFocusCb;
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
            if (mHolder.getSurface() == null) {
                return;
            }
            try {
                if(hasAutoFocus) {
                    mCamera.cancelAutoFocus();
                }
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setPreviewCallback(previewCallback);
                mCamera.startPreview();
                if(hasAutoFocus) {
                    mCamera.autoFocus(autoFocusCallback);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void restartPreview() {
            try {
                mCamera.setPreviewCallback(previewCallback);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
