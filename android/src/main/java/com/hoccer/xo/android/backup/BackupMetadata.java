package com.hoccer.xo.android.backup;

import java.util.Date;

public class BackupMetadata {

    private final BackupType mBackupType;
    private final String mClientName;
    private final Date mCreationDate;

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
