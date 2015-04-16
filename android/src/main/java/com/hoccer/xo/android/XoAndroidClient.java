package com.hoccer.xo.android;

import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;
import com.hoccer.xo.android.database.AndroidTalkDatabase;

import org.apache.log4j.Logger;

import android.content.Context;

public class XoAndroidClient extends XoClient {

    static final Logger LOG = Logger.getLogger(XoAndroidClient.class);

    static final int DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT = -1;
    static final int DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY = 100;

    private int mImageUploadMaxPixelCount = DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT;

    private int mImageUploadEncodingQuality = DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY;

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoAndroidClient(IXoClientHost client_host, XoAndroidClientConfiguration configuration) {
        super(client_host, configuration);

    }

    public boolean isEncodingNecessary() {
        return mImageUploadMaxPixelCount != DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT
                || mImageUploadEncodingQuality != DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY;
    }

    public int getImageUploadEncodingQuality() {
        return mImageUploadEncodingQuality;
    }

    public int getImageUploadMaxPixelCount() {
        return mImageUploadMaxPixelCount;
    }

    public void setImageUploadEncodingQuality(int imageUploadEncodingQuality) {
        mImageUploadEncodingQuality = imageUploadEncodingQuality;
        LOG.info("Image max pixel count set to " + mImageUploadMaxPixelCount);
    }

    public void setImageUploadMaxPixelCount(int imageUploadMaxPixelCount) {
        mImageUploadMaxPixelCount = imageUploadMaxPixelCount;
        LOG.info("Image encoding quality set to " + mImageUploadEncodingQuality);
    }

    public void deleteAccount(Context context) {
        super.deleteAccount();
        context.deleteDatabase(AndroidTalkDatabase.DATABASE_NAME_DEFAULT);
    }
}
