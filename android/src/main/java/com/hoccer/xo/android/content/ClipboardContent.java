package com.hoccer.xo.android.content;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.crypto.CryptoUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.File;

public class ClipboardContent implements IContentObject, Parcelable {

    public static final String PREFERENCE_KEY_PREFIX = ClipboardContent.class.getSimpleName() + "_";
    public static final ClipboardContentCreator CREATOR = new ClipboardContentCreator();

    private static final Logger LOG = Logger.getLogger(ClipboardContent.class);
    private static final int DEFAULT_LENGTH = -1;
    private static final double DEFAULT_ASPECT_RATIO = 1.0;

    private static enum PREF_KEYS {
        FILE_NAME,
        CONTENT_URI,
        DATA_URI,
        CONTENT_TYPE,
        MEDIA_TYPE,
        HMAC,
        LENGTH,
        ASPECT_RATIO;

        String getKey() {
            return PREFERENCE_KEY_PREFIX + name();
        }
    }

    private final String mFileName;
    private final String mContentUri;
    private final String mDataUri;
    private final String mContentType;
    private final String mMediaType;
    private final String mHmac;
    private final double mAspectRatio;
    private final int mLength;

    public static ClipboardContent fromContentObject(IContentObject contentObject) {
        if ((contentObject instanceof ClipboardContent)) {
            return (ClipboardContent) contentObject;
        } else {
            return new ClipboardContent(contentObject);
        }
    }

    public static ClipboardContent fromPreferences(SharedPreferences preferences) {
        return new ClipboardContent(preferences);
    }

    private ClipboardContent(IContentObject fromContent) {
        LOG.debug("create from content object");
        mFileName = fromContent.getFileName();
        mContentUri = fromContent.getContentUrl();
        mDataUri = fromContent.getContentDataUrl();
        mContentType = fromContent.getContentType();
        mMediaType = fromContent.getContentMediaType();
        mHmac = getOrCreateHmac(fromContent.getContentHmac());
        mAspectRatio = getOrSetDefaulAspectRatio(fromContent.getContentAspectRatio());
        mLength = isFileAccessable() ? getLengthFromFile() : fromContent.getContentLength();
    }

    private ClipboardContent(Parcel source) {
        LOG.debug("create from parcel");
        mFileName = source.readString();
        mContentUri = source.readString();
        mDataUri = source.readString();
        mContentType = source.readString();
        mMediaType = source.readString();
        mHmac = getOrCreateHmac(source.readString());
        mAspectRatio = getOrSetDefaulAspectRatio(source.readDouble());
        mLength = isFileAccessable() ? getLengthFromFile() : source.readInt();

    }

    private ClipboardContent(SharedPreferences prefs) {
        mFileName = prefs.getString(PREF_KEYS.FILE_NAME.getKey(), null);
        mContentUri = prefs.getString(PREF_KEYS.CONTENT_URI.getKey(), null);
        mDataUri = prefs.getString(PREF_KEYS.DATA_URI.getKey(), null);
        mContentType = prefs.getString(PREF_KEYS.CONTENT_TYPE.getKey(), null);
        mMediaType = prefs.getString(PREF_KEYS.MEDIA_TYPE.getKey(), null);
        mHmac = prefs.getString(PREF_KEYS.HMAC.getKey(), null);
        mLength = isFileAccessable() ? getLengthFromFile() : prefs.getInt(PREF_KEYS.LENGTH.getKey(), DEFAULT_LENGTH);
        mAspectRatio = getAspectRatioFromPreferences(prefs);
    }

    public void saveToPreferences(SharedPreferences.Editor editor) {
        clearPreferences(editor);
        LOG.debug("Save ClipboardContent to preferences");
        editor.putString(PREF_KEYS.FILE_NAME.getKey(), mFileName);
        editor.putString(PREF_KEYS.CONTENT_URI.getKey(), mContentUri);
        editor.putString(PREF_KEYS.DATA_URI.getKey(), mDataUri);
        editor.putString(PREF_KEYS.CONTENT_TYPE.getKey(), mContentType);
        editor.putString(PREF_KEYS.MEDIA_TYPE.getKey(), mMediaType);
        editor.putString(PREF_KEYS.HMAC.getKey(), mHmac);
        editor.putInt(PREF_KEYS.LENGTH.getKey(), mLength);
        editor.putLong(PREF_KEYS.ASPECT_RATIO.getKey(), Double.doubleToRawLongBits(mAspectRatio));
        editor.apply();
    }

    public static void clearPreferences(SharedPreferences.Editor editor) {
        LOG.debug("Clear ClipboardContent from preferences");
        editor.remove(PREF_KEYS.FILE_NAME.getKey());
        editor.remove(PREF_KEYS.CONTENT_URI.getKey());
        editor.remove(PREF_KEYS.DATA_URI.getKey());
        editor.remove(PREF_KEYS.CONTENT_TYPE.getKey());
        editor.remove(PREF_KEYS.MEDIA_TYPE.getKey());
        editor.remove(PREF_KEYS.HMAC.getKey());
        editor.remove(PREF_KEYS.LENGTH.getKey());
        editor.remove(PREF_KEYS.ASPECT_RATIO.getKey());
        editor.apply();
    }

    @Override
    public boolean isContentAvailable() {
        return false;
    }

    @Override
    public ContentState getContentState() {
        return null;
    }

    @Override
    public ContentDisposition getContentDisposition() {
        return null;
    }

    @Override
    public int getTransferLength() {
        return 0;
    }
    @Override
    public int getTransferProgress() {
        return 0;
    }

    @Override
    public String getContentMediaType() {
        return mMediaType;
    }

    @Override
    public double getContentAspectRatio() {
        return mAspectRatio;
    }

    @Override
    public String getContentType() {
        return mContentType;
    }

    @Override
    public String getFileName() {
        return mFileName;
    }

    @Override
    public String getContentUrl() {
        return mContentUri;
    }

    @Override
    public String getContentDataUrl() {
        return mDataUri;
    }

    @Override
    public int getContentLength() {
        return mLength;
    }

    @Override
    public String getContentHmac() {
        return mHmac;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LOG.debug("write to parcel");
        dest.writeString(mFileName);
        dest.writeString(mContentUri);
        dest.writeString(mDataUri);
        dest.writeString(mContentType);
        dest.writeString(mMediaType);
        dest.writeString(mHmac);
        dest.writeDouble(mAspectRatio);
        dest.writeInt(mLength);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object) this).getClass() != o.getClass()) return false;

        ClipboardContent content = (ClipboardContent) o;

        if (Double.compare(content.mAspectRatio, mAspectRatio) != 0) return false;
        if (mContentType != null ? !mContentType.equals(content.mContentType) : content.mContentType != null)
            return false;
        if (mContentUri != null ? !mContentUri.equals(content.mContentUri) : content.mContentUri != null) return false;
        if (mDataUri != null ? !mDataUri.equals(content.mDataUri) : content.mDataUri != null) return false;
        if (mFileName != null ? !mFileName.equals(content.mFileName) : content.mFileName != null) return false;
        if (mHmac != null ? !mHmac.equals(content.mHmac) : content.mHmac != null) return false;
        if (mMediaType != null ? !mMediaType.equals(content.mMediaType) : content.mMediaType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mFileName != null ? mFileName.hashCode() : 0;
        result = 31 * result + (mContentUri != null ? mContentUri.hashCode() : 0);
        result = 31 * result + (mDataUri != null ? mDataUri.hashCode() : 0);
        result = 31 * result + (mContentType != null ? mContentType.hashCode() : 0);
        result = 31 * result + (mMediaType != null ? mMediaType.hashCode() : 0);
        result = 31 * result + (mHmac != null ? mHmac.hashCode() : 0);
        temp = Double.doubleToLongBits(mAspectRatio);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    private boolean isFileAccessable() {
        boolean b = false;
        if (mDataUri != null && !mDataUri.isEmpty()) {
            File f = new File(mDataUri);
            b = f.exists();
        }
        return b;
    }

    private int getLengthFromFile() {
        File f = new File(mDataUri);
        return safeLongToInt(f.length());
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private String getOrCreateHmac(String hmac) {
        if (hmac == null || hmac.isEmpty()) {
            hmac = createHmac();
        }
        return hmac;
    }

    private String createHmac() {
        String hmac = null;
        try {
            hmac = new String(Base64.encodeBase64(CryptoUtils.computeHmac(mDataUri)));
        } catch (Exception e) {
            LOG.error("Error creating HMAC", e);
        }
        return hmac;
    }

    private double getOrSetDefaulAspectRatio(double aspectRatio) {
        if (aspectRatio == 0) {
            aspectRatio = DEFAULT_ASPECT_RATIO;
        }
        return aspectRatio;
    }

    private double getAspectRatioFromPreferences(SharedPreferences preferences) {
        long defaultValue = Double.doubleToRawLongBits(DEFAULT_ASPECT_RATIO);
        return Double.longBitsToDouble(preferences.getLong(PREF_KEYS.ASPECT_RATIO.getKey(), defaultValue));
    }

    public static class ClipboardContentCreator implements Parcelable.Creator<ClipboardContent> {
        @Override
        public ClipboardContent createFromParcel(Parcel source) {
            return new ClipboardContent(source);
        }

        @Override
        public ClipboardContent[] newArray(int size) {
            return new ClipboardContent[size];
        }
    }
}
