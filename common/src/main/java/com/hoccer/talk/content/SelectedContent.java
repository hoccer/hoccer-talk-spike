package com.hoccer.talk.content;

import org.jetbrains.annotations.Nullable;

/**
 * Base for user selected attachments.
 */
public abstract class SelectedContent {

    @Nullable
    protected String mFilePath;
    private final String mMimeType;
    private final String mMediaType;

    protected SelectedContent(@Nullable String filePath, String mimeType, String mediaType) {
        mFilePath = filePath;
        mMimeType = mimeType;
        mMediaType = mediaType;
    }

    @Nullable
    public String getFilePath() {
        return mFilePath;
    }

    public void createContentFile() {
        if(mFilePath == null) {
            mFilePath = writeContentToFile();
        }
    }

    protected String writeContentToFile() {
        return mFilePath;
    }

    public String getMediaType() {
        return mMediaType;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public double getAspectRatio() {
        return 0;
    }
}
