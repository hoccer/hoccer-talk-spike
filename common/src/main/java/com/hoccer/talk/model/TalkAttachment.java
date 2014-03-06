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

    /** internal id, not transfered */
    @DatabaseField(generatedId = true)
    private int attachmentId;

    /** The download URL of attached file */
    @DatabaseField
    String url;

    /** An optional file name for the attachment; the receiver must make sure that it does not collide with other file names */
    @DatabaseField
    String filename;

    /** MIME type of attached file */
    @DatabaseField
    String mimeType;

    /** String of decimal digits denoting the size of attached file in bytes */
    @DatabaseField
    String contentSize;

    /** Media-type of attached file, currently image, video, audio, contact, geolocation, data */
    @DatabaseField
    String mediaType;

    /** Aspect-ratio for the preview of an attachment */
    @DatabaseField
    double aspectRatio;

    public TalkAttachment() {
        this.aspectRatio = 1.0;
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
