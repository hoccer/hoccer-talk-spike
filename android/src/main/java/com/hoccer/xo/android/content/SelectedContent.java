package com.hoccer.xo.android.content;

import android.content.Intent;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.crypto.CryptoUtils;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Content objects
 * <p/>
 * This is a grabbag object for all the properties
 * of content objects that we need in the UI.
 * <p/>
 * They do not cary an identity and can be created
 * from uploads, downloads as well as by selecting
 * content from external sources.
 */
public class SelectedContent implements IContentObject {

    private static final Logger LOG = Logger.getLogger(SelectedContent.class);

    String mFileName;
    String mContentUri;
    String mFilePath;
    String mContentType;
    String mMediaType;
    String mHmac;
    int mLength = -1;
    double mAspectRatio = 1.0;

    /**
     * Literal data.
     * <p/>
     * Converted to a file when selected content becomes an upload.
     */
    byte[] mData;

    public SelectedContent(Intent intent, String dataUri) {
        if (intent != null && intent.getData() != null) {
            initWithContentUri(intent.getData().toString(), intent.getType());
        }
        mFilePath = dataUri;
    }

    public SelectedContent(String contentUri, String dataUri) {
        initWithContentUri(contentUri, null);
        mFilePath = dataUri;
    }

    public SelectedContent(byte[] data) {
        LOG.debug("new selected content with raw data");
        mData = data;
        mLength = data.length;
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
    public String getFilePath() {
        return mFilePath;
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
        if (mHmac == null) {
            mHmac = computeHmac();
        }

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

    private void initWithContentUri(String uri, String contentType) {
        mContentUri = uri;
        mContentType = contentType;
        LOG.debug("create from content uri: " + mContentUri);
    }

    private String computeHmac() {
        try {
            if (mFilePath != null) {
                return CryptoUtils.computeHmac(mFilePath);
            } else if (mData != null) {
                return CryptoUtils.computeHmac(mData);
            }
        } catch (Exception e) {
            LOG.error("Error computing HMAC", e);
        }

        return null;
    }

    private void toFile() {
        if (mData != null) {
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
            mFilePath = UriUtils.FILE_URI_PREFIX + file;
            mData = null;
        } catch (IOException e) {
            LOG.error("error writing content to file", e);
        }
    }

    public static TalkClientUpload createAvatarUpload(IContentObject object) {
        if (object instanceof SelectedContent) {
            ((SelectedContent) object).toFile();
        }

        String filePath = null;
        if(object.getFilePath() != null) {
            filePath = UriUtils.getAbsoluteFileUri(object.getFilePath()).getPath();
        }

        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAvatar(
                object.getContentUrl(),
                filePath,
                object.getContentType(),
                object.getContentLength());
        return upload;
    }

    public static TalkClientUpload createAttachmentUpload(IContentObject object) {
        if (object instanceof SelectedContent) {
            ((SelectedContent) object).toFile();
        }

        int length = object.getContentLength();
        String contentUrl = object.getContentUrl();

        if (object instanceof XoTransfer) {
            XoTransfer transfer = (XoTransfer) object;
            File file = new File(UriUtils.getAbsoluteFileUri(transfer.getFilePath()).getPath());
            length = (int) file.length();

            // HACK: when re-sending an upload or download, the content url is cleared to exclude it from the music browser
            contentUrl = null;
        }

        String filePath = null;
        if(object.getFilePath() != null) {
            filePath = UriUtils.getAbsoluteFileUri(object.getFilePath()).getPath();
        }

        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(
                object.getFileName(),
                contentUrl,
                filePath,
                object.getContentType(),
                object.getContentMediaType(),
                object.getContentAspectRatio(),
                length,
                object.getContentHmac());

        return upload;
    }
}
