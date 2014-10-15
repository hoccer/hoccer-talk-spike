package com.hoccer.xo.android.content;

import android.os.Parcel;
import android.os.Parcelable;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import org.apache.log4j.Logger;

import java.io.File;

public class ClipboardContent implements IContentObject, Parcelable {

    public static final ClipboardContentCreator CREATOR = new ClipboardContentCreator();

    private static final Logger LOG = Logger.getLogger(ClipboardContent.class);
    private static final double DEFAULT_ASPECT_RATIO = 1.0;

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

    private ClipboardContent(IContentObject fromContent) {
        LOG.debug("create from content object");
        mFileName = fromContent.getFileName();
        mContentUri = fromContent.getContentUrl();
        mDataUri = fromContent.getContentDataUrl();
        mContentType = fromContent.getContentType();
        mMediaType = fromContent.getContentMediaType();
        mHmac = fromContent.getContentHmac();
        mAspectRatio = getOrSetDefaultAspectRatio(fromContent.getContentAspectRatio());
        mLength = isFileAccessible() ? getLengthFromFile() : fromContent.getContentLength();
    }

    private ClipboardContent(Parcel source) {
        LOG.debug("create from parcel");
        mFileName = source.readString();
        mContentUri = source.readString();
        mDataUri = source.readString();
        mContentType = source.readString();
        mMediaType = source.readString();
        mHmac = source.readString();
        mAspectRatio = getOrSetDefaultAspectRatio(source.readDouble());
        mLength = isFileAccessible() ? getLengthFromFile() : source.readInt();
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
        if (o == null || !IContentObject.class.isAssignableFrom(o.getClass())) return false;

        IContentObject content = (IContentObject) o;
        return mHmac != null && mHmac.equals(content.getContentHmac());
    }

    @Override
    public int hashCode() {
        return mHmac != null ? mHmac.hashCode() : 0;
    }

    private boolean isFileAccessible() {
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

    private double getOrSetDefaultAspectRatio(double aspectRatio) {
        if (aspectRatio == 0) {
            aspectRatio = DEFAULT_ASPECT_RATIO;
        }
        return aspectRatio;
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
