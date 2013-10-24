package com.hoccer.talk.content;

public enum ContentState {
    /* content selected by user */
    SELECTED,
    /* content being downloaded in various states */
    DOWNLOAD_NEW,
    DOWNLOAD_STARTED,
    DOWNLOAD_IN_PROGRESS,
    DOWNLOAD_PAUSED,
    DOWNLOAD_COMPLETE,
    DOWNLOAD_FAILED,
    /* content being uploaded in various states */
    UPLOAD_NEW,
    UPLOAD_IN_PROGRESS,
    UPLOAD_PAUSED,
    UPLOAD_COMPLETE,
    UPLOAD_FAILED,
}
