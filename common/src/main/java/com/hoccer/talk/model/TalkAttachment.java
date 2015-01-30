package com.hoccer.talk.model;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Attachments are files sent along with messages
 *
 * These objects are transferred only in encrypted form,
 * so the server never encounters them.
 */
@DatabaseTable(tableName = "attachment")
public class TalkAttachment {

    /** internal id, not transferred */
    @DatabaseField(generatedId = true)
    private int attachmentId;

    /** The download URL of attached file */
    @DatabaseField
    private String url;

    /**
     * An optional file name for the attachment; the receiver must make sure that it does not
     * collide with other file names
     */
    @DatabaseField
    private String fileName;

    /** MIME type of attached file */
    @DatabaseField
    private String mimeType;

    /** String of decimal digits denoting the size of attached file in bytes */
    @DatabaseField
    private String contentSize;

    /** Media-type of attached file, currently image, video, audio, contact, geolocation, data */
    @DatabaseField
    private String mediaType;

    /** Aspect-ratio for the preview of an attachment */
    @DatabaseField
    private double aspectRatio;

    @DatabaseField
    private String hmac;

    @DatabaseField
    private String fileId;

    public TalkAttachment() {
        this.aspectRatio = 1.0;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /** XXX Reserved for legacy json-mapping. !!Do not use!! */
    public void setFilename(String filename) {
        this.fileName = filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getContentLength(){
        try {
            return Long.parseLong(contentSize);
        } catch (NumberFormatException e) {
            return -1;
        }
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

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }
}
