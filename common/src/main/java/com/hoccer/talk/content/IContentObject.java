package com.hoccer.talk.content;

/**
 * Interface for objects that represent some kind of content
 */
public interface IContentObject {

    /** True if the content is currently available locally */
    public boolean isContentAvailable();

    public ContentState getContentState();
    public ContentDisposition getContentDisposition();

    /** Length of active transfer */
    public int getTransferLength();
    /** Progress of active transfer */
    public int getTransferProgress();

    /** The media type, if known */
    public String getContentMediaType();

    /** The aspect ratio, 1.0 if not known */
    public double getContentAspectRatio();

    /** The MIME content type, if known */
    public String getContentType();

    public String getFileName();

    /** Content URL, if available */
    public String getContentUrl();

    /** Data URL, if available */
    public String getFilePath();

    /** Content length */
    public int getContentLength();

    /** hash over content */
    public String getContentHmac();
}
