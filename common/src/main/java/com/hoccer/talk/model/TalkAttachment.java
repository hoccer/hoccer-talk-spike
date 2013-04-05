package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;

public class TalkAttachment {

    /** URL of attached file */
    String url;

    /** MIME type of attached file */
    String mimeType;

    /** String-encoded size of attached file */
    String size;

    /** Media-type of attached file (sort of like the first half of MIME type, but consistent) */
    String mediaType;

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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

}
