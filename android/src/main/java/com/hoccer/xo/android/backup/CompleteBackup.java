package com.hoccer.xo.android.backup;

import java.io.File;

public class CompleteBackup extends DatabaseBackup {

    CompleteBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile, metadata);
    }

    static CompleteBackup create() {
        return null;
    }

    @Override
    public void restore(String password) {

    }
}
