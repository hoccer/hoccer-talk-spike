package com.hoccer.xo.android.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;

import java.util.EnumMap;
import java.util.Map;

public class QrCodeGeneratorFragment extends PagerFragment {

    private ImageView mQrCodeView;
    private TextView mPairingTokenView;
    private ProgressBar mProgressBar;
    private LinearLayout mErrorLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_code_generator, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQrCodeView = (ImageView)view.findViewById(R.id.iv_qr_code);
        mPairingTokenView = (TextView)view.findViewById(R.id.tv_pairing_token);
        mProgressBar = (ProgressBar)view.findViewById(R.id.pb_generating_pairing_token);
        mErrorLayout = (LinearLayout)view.findViewById(R.id.ll_error);

        Button retryButton = (Button)view.findViewById(R.id.btn_retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateToken();
            }
        });
    }

    @Override
    public void onPageSelected() {
        if (!isTokenGenerated()) {
            generateToken();
        }
    }

    private boolean isTokenGenerated() {
        return mQrCodeView.getVisibility() == View.VISIBLE;
    }

    private void generateToken() {
        resetViews();

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

    private void resetViews() {
        mPairingTokenView.setText("");
        mQrCodeView.setVisibility(View.INVISIBLE);
        mErrorLayout.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void updateViews(String pairingToken) {
        mProgressBar.setVisibility(View.INVISIBLE);

        if (pairingToken != null) {
            mPairingTokenView.setText(pairingToken);

            String invitationUrl = XoApplication.getXoClient().getConfiguration().getUrlScheme() + pairingToken;
            Bitmap qrCode = createQrCode(invitationUrl, 400, 400);

            if (qrCode != null) {
                mQrCodeView.setVisibility(View.VISIBLE);
                mQrCodeView.setImageBitmap(qrCode);
            } else {
                mErrorLayout.setVisibility(View.VISIBLE);
            }
        } else {
            mErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    private static Bitmap createQrCode(String contents, int preferredWidth, int preferredHeight) {
        if (contents == null) {
            return null;
        }

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix;

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, preferredWidth, preferredHeight, hints);
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
