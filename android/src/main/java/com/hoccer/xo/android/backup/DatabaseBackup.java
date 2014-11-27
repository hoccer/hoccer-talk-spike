package com.hoccer.xo.android.backup;

import java.io.File;

public class DatabaseBackup extends Backup {

    private BackupMetadata mMetadata;

    public DatabaseBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile);
        mMetadata = metadata;
    }

    public static DatabaseBackup create() {
        return null;
    }
}
