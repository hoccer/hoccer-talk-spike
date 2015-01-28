package com.hoccer.xo.android.content;

import com.hoccer.talk.content.SelectedAttachment;


public class SelectedFile extends SelectedAttachment {

    private final String mContentType;
    private final String mContentMediaType;
    private final double mAspectRatio;

    public SelectedFile(String filePath, String contentType, String contentMediaType) {
        this(filePath, contentType, contentMediaType, 0);
    }

    public SelectedFile(String filePath, String contentType, String contentMediaType, double aspectRatio) {
        super(filePath);

        mContentType = contentType;
        mContentMediaType = contentMediaType;
        mAspectRatio = aspectRatio;
    }

    public String getContentMediaType() {
        return mContentMediaType;
    }

    public String getContentType() {
        return mContentType;
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
