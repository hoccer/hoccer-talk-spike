package com.hoccer.xo.android.backup;

import java.io.File;

public class CompleteBackup extends DatabaseBackup {

    public CompleteBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile, metadata);
    }

    public static CompleteBackup create() {
        return null;
    }
}
