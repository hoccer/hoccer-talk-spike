package com.hoccer.talk.model;

public class TalkAttachment {

    /** URL of attached file */
    String url;

    /** MIME type of attached file */
    String mimeType;

    /** String-encoded size of attached file */
    String contentSize;

    /** Media-type of attached file (sort of like the first half of MIME type, but consistent) */
    String mediaType;

    /** Aspect-ratio for pretty client views */
    double aspectRatio;

    public TalkAttachment() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getContentSize() {
        return contentSize;
    }

    public void setContentSize(String contentSize) {
        this.contentSize = contentSize;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

}
