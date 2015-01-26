package com.hoccer.talk.content;

/**
 * Interface for objects that represent some kind of content
 */
public interface IContentObject {

    public boolean isContentAvailable();

    public ContentState getContentState();

    public long getTransferLength();

    public long getTransferProgress();

    public String getContentMediaType();

    public double getContentAspectRatio();

    public String getContentType();

    public String getFileName();

    public String getContentUrl();

    public String getFilePath();

    public long getContentLength();

    public String getContentHmac();
}
