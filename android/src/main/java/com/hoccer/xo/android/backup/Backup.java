package com.hoccer.xo.android.backup;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public abstract class Backup {

    protected final File mBackupFile;

    Backup(File backupFile) {
        mBackupFile = backupFile;
    }

    @Nullable
    public abstract String getClientName();

    public abstract Date getCreationDate();

    public long getSize() {
        return mBackupFile.length();
    }

    public abstract void restore(String password) throws Exception;

    public File getFile() {
        return mBackupFile;
    };
}
