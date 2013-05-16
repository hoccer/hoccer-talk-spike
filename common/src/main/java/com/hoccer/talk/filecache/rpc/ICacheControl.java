package com.hoccer.talk.filecache.rpc;

public interface ICacheControl {

    void deleteAccount(String accountId);

    FileHandles createFileForStorage(String accountId, int fileSize);

    FileHandles createFileForTransfer(String accountId, int fileSize);

    void deleteFile(String fileId);

    public static class FileHandles {
        public String fileId;
        public String uploadId;
        public String downloadId;
    }

}
