package com.hoccer.xo.android.backup;

import android.os.Parcelable;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public abstract class Backup implements Parcelable {

    protected File mBackupFile;

    Backup(File backupFile) {
        mBackupFile = backupFile;
    }

    protected Backup() {}

    @Nullable
    public abstract String getClientName();

    public abstract Date getCreationDate();

    public long getSize() {
        return mBackupFile.length();
    }

    public abstract void restore(String password) throws Exception;

    public File getFile() {
        return mBackupFile;
    }
}
