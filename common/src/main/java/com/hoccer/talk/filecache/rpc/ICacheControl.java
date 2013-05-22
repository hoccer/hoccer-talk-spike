package com.hoccer.talk.filecache.rpc;

public interface ICacheControl {

    void deleteAccount(String accountId);

    FileHandles createFileForStorage(String accountId, String contentType, int contentLength);

    FileHandles createFileForTransfer(String accountId, String contentType, int contentLength);

    void deleteFile(String fileId);

    public static class FileHandles {
        public String fileId;
        public String uploadId;
        public String downloadId;
    }

}
