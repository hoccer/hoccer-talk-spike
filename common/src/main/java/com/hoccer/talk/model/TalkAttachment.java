package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Attachments are files sent along with messages
 *
 * These objects are transferred only in encrypted form,
 * so the server never encounters them.
 */
@DatabaseTable(tableName="attachment")
public class TalkAttachment {

    /** URL of attached file */
    @DatabaseField
    String url;

    /** Name of file */
    @DatabaseField
    String filename;

    /** MIME type of attached file */
    @DatabaseField
    String mimeType;

    /** String-encoded size of attached file */
    @DatabaseField
    String contentSize;

    /** Media-type of attached file (sort of like the first half of MIME type, but consistent) */
    @DatabaseField
    String mediaType;

    /** Aspect-ratio for pretty client views */
    @DatabaseField
    double aspectRatio;

    public TalkAttachment() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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
