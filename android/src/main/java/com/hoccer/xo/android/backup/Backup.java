package com.hoccer.xo.android.backup;

import java.io.File;

public abstract class Backup {

    private File mBackupFile;

    public Backup(File backupFile) {
        mBackupFile = backupFile;
    }
}
