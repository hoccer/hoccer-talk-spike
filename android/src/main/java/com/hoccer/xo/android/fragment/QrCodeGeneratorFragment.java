package com.hoccer.xo.android.fragment;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;

import java.util.EnumMap;
import java.util.Map;

public class QrCodeGeneratorFragment extends Fragment {

    private ImageView mQrCodeView;
    private TextView mPairingTokenView;
    private ProgressBar mProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_code_generator, null);
        mQrCodeView = (ImageView)view.findViewById(R.id.iv_qr_code);
        mPairingTokenView = (TextView)view.findViewById(R.id.tv_pairing_token);
        mProgressBar = (ProgressBar)view.findViewById(R.id.pb_generating_pairing_token);

        generateToken();
        return view;
    }

    private void generateToken() {
        updateViews(null);

        XoApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final String pairingToken = XoApplication.getXoClient().generatePairingToken();

                if (isResumed()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateViews(pairingToken);
                        }
                    });
                }
            }
        });
    }

    private void updateViews(String pairingToken) {
        if (pairingToken != null) {
            mPairingTokenView.setText(pairingToken);
            mProgressBar.setVisibility(View.INVISIBLE);

            String invitationUrl = XoApplication.getXoClient().getConfiguration().getUrlScheme() + pairingToken;
            Bitmap qrCode = createQrCode(invitationUrl, 400, 400);

            if (qrCode != null) {
                mQrCodeView.setVisibility(View.VISIBLE);
                mQrCodeView.setImageBitmap(qrCode);
            }
        } else {
            mPairingTokenView.setText("");
            mProgressBar.setVisibility(View.VISIBLE);
            mQrCodeView.setVisibility(View.INVISIBLE);
        }
    }

    private static Bitmap createQrCode(String contents, int img_width, int img_height) {
        if (contents == null) {
            return null;
        }

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        MultiFormatWriter writer = new MultiFormatWriter();

        BitMatrix bitMatrix;

        try {
            bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, img_width, img_height, hints);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (WriterException e) {
            return null;
        }

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
