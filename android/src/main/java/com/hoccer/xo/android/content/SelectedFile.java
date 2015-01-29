package com.hoccer.xo.android.content;

import com.hoccer.talk.content.SelectedContent;


public class SelectedFile extends SelectedContent {

    private final String mMimeType;
    private final String mContentMediaType;
    private final double mAspectRatio;

    public SelectedFile(String filePath, String mimeType, String contentMediaType) {
        this(filePath, mimeType, contentMediaType, 0);
    }

    public SelectedFile(String filePath, String mimeType, String contentMediaType, double aspectRatio) {
        super(filePath);

        mMimeType = mimeType;
        mContentMediaType = contentMediaType;
        mAspectRatio = aspectRatio;
    }

    public String getMediaType() {
        return mContentMediaType;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public double getAspectRatio() {
        return mAspectRatio;
    }

    public String getFilePath() {
        return mFilePath;
    }

    @Override
    protected String writeToFile() {
        return null;
    }
}
