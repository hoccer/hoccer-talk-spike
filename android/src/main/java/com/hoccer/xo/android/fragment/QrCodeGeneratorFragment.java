package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.artcom.hoccer.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoPagerFragment;

import java.util.EnumMap;
import java.util.Map;

public class QrCodeGeneratorFragment extends XoPagerFragment implements IXoContactListener, IXoStateListener {

    private ImageView mQrCodeView;
    private TextView mPairingTokenView;
    private ProgressBar mProgressBar;
    private LinearLayout mErrorLayout;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_code_generator, null);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQrCodeView = (ImageView) view.findViewById(R.id.iv_qr_code);
        mPairingTokenView = (TextView) view.findViewById(R.id.tv_pairing_token);
        mProgressBar = (ProgressBar) view.findViewById(R.id.pb_generating_pairing_token);
        mErrorLayout = (LinearLayout) view.findViewById(R.id.ll_error);

        final Button retryButton = (Button) view.findViewById(R.id.btn_retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateToken();
            }
        });
    }

    @Override
    public View getCustomTabView(final Context context) {
        return null;
    }

    @Override
    public String getTabName(final Resources resources) {
        return resources.getString(R.string.qr_code_tab_generate);
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
    }

    @Override
    public void onPageSelected() {
        if (!isTokenGenerated()) {
            generateToken();
        }

        XoApplication.get().getXoClient().registerContactListener(this);
        XoApplication.get().getXoClient().registerStateListener(this);
    }

    @Override
    public void onPageUnselected() {
        XoApplication.get().getXoClient().unregisterContactListener(this);
        XoApplication.get().getXoClient().unregisterStateListener(this);
    }

    private boolean isTokenGenerated() {
        return mQrCodeView.getVisibility() == View.VISIBLE;
    }

    private void generateToken() {
        resetViews();

        XoApplication.get().getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final String pairingToken = XoApplication.get().getXoClient().generatePairingToken();

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

    private void updateViews(final String pairingToken) {
        mProgressBar.setVisibility(View.INVISIBLE);

        if (pairingToken != null) {
            mPairingTokenView.setText(pairingToken);

            final String invitationUrl = XoApplication.get().getXoClient().getConfiguration().getUrlScheme() + pairingToken;
            final Bitmap qrCode = createQrCode(invitationUrl, 400, 400);

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

    private static Bitmap createQrCode(final String contents, final int preferredWidth, final int preferredHeight) {
        if (contents == null) {
            return null;
        }

        final MultiFormatWriter writer = new MultiFormatWriter();
        final BitMatrix bitMatrix;

        final Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, preferredWidth, preferredHeight, hints);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (WriterException e) {
            return null;
        }

        final int width = bitMatrix.getWidth();
        final int height = bitMatrix.getHeight();
        final int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            final int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    @Override
    public void onClientPresenceChanged(final TalkClientContact contact) {}

    @Override
    public void onClientRelationshipChanged(final TalkClientContact contact) {
        // we assume that this relationship update is a new friendship via QR code scan
        if (contact.isClientFriend()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_pairing_successful), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onGroupPresenceChanged(final TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(final TalkClientContact contact) {}

    @Override
    public void onClientStateChange(XoClient client) {
        if (!isTokenGenerated() && client.isReady()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    generateToken();
                }
            });
        }
    }
}
