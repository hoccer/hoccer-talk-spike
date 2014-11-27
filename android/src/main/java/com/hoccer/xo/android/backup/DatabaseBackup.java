package com.hoccer.xo.android.backup;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public class DatabaseBackup extends Backup {

    private final BackupMetadata mMetadata;

    DatabaseBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile);
        mMetadata = metadata;
    }

    static DatabaseBackup create() {
        return null;
    }

    @Nullable
    @Override
    public String getClientName() {
        return null;
    }

    @Override
    public Date getCreationDate() {
        return mMetadata.getCreationDate();
    }

    @Override
    public void restore(String password) {

    }
}
