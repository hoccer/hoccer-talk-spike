package com.hoccer.xo.android.backup;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class BackupMetadata {

    @JsonProperty("backupType")
    private BackupType mBackupType;

    @JsonProperty("clientName")
    private String mClientName;

    @JsonProperty("creationDate")
    private Date mCreationDate;

    public BackupMetadata() {}

    public BackupMetadata(BackupType type, String clientName, Date creationDate) {
        mBackupType = type;
        mClientName = clientName;
        mCreationDate = creationDate;
    }

    public BackupType getBackupType() {
        return mBackupType;
    }

    public String getClientName() {
        return mClientName;
    }

    public Date getCreationDate() {
        return mCreationDate;
    }
}
