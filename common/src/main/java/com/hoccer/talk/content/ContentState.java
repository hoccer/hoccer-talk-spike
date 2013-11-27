package com.hoccer.talk.content;

public enum ContentState {
    /* content selected by user */
    SELECTED,
    /* content being downloaded in various states */
    DOWNLOAD_NEW,
    DOWNLOAD_DOWNLOADING,
    DOWNLOAD_DECRYPTING,
    DOWNLOAD_DETECTING,
    DOWNLOAD_COMPLETE,
    DOWNLOAD_PAUSED,
    DOWNLOAD_FAILED,
    /* content being uploaded in various states */
    UPLOAD_NEW,
    UPLOAD_REGISTERING,
    UPLOAD_ENCRYPTING,
    UPLOAD_UPLOADING,
    UPLOAD_COMPLETE,
    UPLOAD_PAUSED,
    UPLOAD_FAILED,
}
