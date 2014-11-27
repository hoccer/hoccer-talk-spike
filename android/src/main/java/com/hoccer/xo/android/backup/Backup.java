package com.hoccer.xo.android.backup;

import com.hoccer.talk.client.exceptions.NoClientIdInPresenceException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
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

    public abstract void restore(String password) throws IOException, SQLException, NoClientIdInPresenceException;
}
