package com.hoccer.xo.android.content;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.crypto.CryptoUtils;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Content objects
 *
 * This is a grabbag object for all the properties
 * of content objects that we need in the UI.
 *
 * They do not cary an identity and can be created
 * from uploads, downloads as well as by selecting
 * content from external sources.
 *
 */
public class SelectedContent implements IContentObject {

    private static final Logger LOG = Logger.getLogger(SelectedContent.class);

    String mFileName;
    String mContentUri;
    String mDataUri;
    String mContentType = null;
    String mMediaType = null;
    String mHmac = null;
    int    mLength = -1;
    double mAspectRatio = 1.0;

    /**
     * Literal data.
     *
     * Converted to a file when selected content becomes an upload.
     */
    byte[] mData = null;

    public SelectedContent(Intent intent, String dataUri) {
        if (intent != null && intent.getData() != null) {
            initWithContentUri(intent.getData().toString(), intent.getType());
        }
        mDataUri = dataUri;
        computeHmac();
    }

    public SelectedContent(String contentUri, String dataUri) {
        initWithContentUri(contentUri, null);
        mDataUri = dataUri;
        computeHmac();
    }

    public SelectedContent(byte[] data) {
        LOG.debug("new selected content with raw data");
        mData = data;
        mLength = data.length;
        computeHmac();
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    public void setContentType(String contentType) {
        this.mContentType = contentType;
    }

    public void setContentMediaType(String mediaType) {
        this.mMediaType = mediaType;
    }

    public void setContentLength(int length) {
        this.mLength = length;
    }

    public void setContentAspectRatio(double aspectRatio) {
        this.mAspectRatio = aspectRatio;
    }

    public byte[] getData() {
        return mData;
    }

    @Override
    public boolean isContentAvailable() {
        return true;
    }

    @Override
    public ContentState getContentState() {
        return ContentState.SELECTED;
    }

    @Override
    public ContentDisposition getContentDisposition() {
        return ContentDisposition.SELECTED;
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
    public String getContentMediaType() {
        return mMediaType;
    }

    @Override
    public double getContentAspectRatio() {
        return mAspectRatio;
    }

    @Override
    public String getContentHmac() {
        return mHmac;
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

    private void initWithContentUri(String uri, String contentType) {
        mContentUri = uri;
        mContentType = contentType;
        LOG.debug("create from content uri: " + mContentUri);
    }

    private void computeHmac() {
        try {
            if (mDataUri != null) {
                mHmac = CryptoUtils.computeHmac(mDataUri);
            } else if (mData != null) {
                mHmac = CryptoUtils.computeHmac(mData);
            }
        } catch (Exception e) {
            LOG.error("Error computing HMAC", e);
        }
    }

    private void toFile() {
        if(mData != null) {
            writeToFile();
        }
    }

    private void writeToFile() {
        try {
            File file = new File(XoApplication.getGeneratedDirectory(), UUID.randomUUID().toString());
            file.createNewFile();
            OutputStream os = new FileOutputStream(file);
            os.write(mData);
            os.flush();
            os.close();
            mDataUri = "file://" + file.toString();
            mData = null;
        } catch (IOException e) {
            LOG.error("error writing content to file", e);
        }
    }

    public static TalkClientUpload createAvatarUpload(IContentObject object) {
        if(object instanceof SelectedContent) {
            ((SelectedContent)object).toFile();
        }
        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAvatar(
                object.getContentUrl(),
                object.getContentDataUrl(),
                object.getContentType(),
                object.getContentLength());
        return upload;
    }

    /*
     * Warning: This method does not work with TalkClientDownloads as 'object' since there is a contentLength mismatch between
     * TalkClientUpload and TalkClientDownload although its the same file.
     * The contentLength seems to be conceptually different between both.
     */
    public static TalkClientUpload createAttachmentUpload(IContentObject object) {
        if(object instanceof SelectedContent) {
            ((SelectedContent)object).toFile();
        }
        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(
                object.getFileName(),
                object.getContentUrl(),
                object.getContentDataUrl(),
                object.getContentType(),
                object.getContentMediaType(),
                object.getContentAspectRatio(),
                object.getContentLength(),
                object.getContentHmac());
        return upload;
    }
}
