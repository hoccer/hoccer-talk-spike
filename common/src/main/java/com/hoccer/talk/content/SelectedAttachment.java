package com.hoccer.talk.content;

/**
 * Base for user selected attachments.
 */
public abstract class SelectedAttachment {

    protected String mFilePath;

    protected SelectedAttachment() {}

    protected SelectedAttachment(String filePath) {
        mFilePath = filePath;
    }

    // Returns the file path of the attachment.
    // @note If the attachment is not represented as file it will be created first.
    public String getFilePath() {
        if (mFilePath == null) {
            mFilePath = writeToFile();
        }
        return mFilePath;
    }

    protected abstract String writeToFile();

    public abstract String getContentMediaType();

    public abstract String getContentType();

    public abstract double getAspectRatio();
}
