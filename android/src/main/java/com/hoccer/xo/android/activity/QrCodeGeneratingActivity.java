package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;

import java.util.EnumMap;
import java.util.Map;

public class QrCodeGeneratingActivity extends Activity implements IXoContactListener {
    private ImageView targetImageView;
    private String mQrString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);
        targetImageView = (ImageView) findViewById(R.id.iv_qr_code);
        Intent intent = getIntent();
        if (intent != null) {
            mQrString = intent.getExtras().getString("QR");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bitmap barcode_bitmap;
        try {
            barcode_bitmap = encodeAsBitmap(mQrString, BarcodeFormat.QR_CODE, 400, 400);
            targetImageView.setImageBitmap(barcode_bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        XoApplication.getXoClient().registerContactListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        XoApplication.getXoClient().unregisterContactListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height)
            throws WriterException {
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contents, format, img_width, img_height, hints);
        } catch (IllegalArgumentException e) {
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
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
        // do nothing
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        // we assume that this relationship update is a new friendship via QR code scan
        if(contact.isClientFriend()) {
            final Context context = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, getResources().getString(R.string.toast_pairing_successful), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        // do nothing
    }
}
