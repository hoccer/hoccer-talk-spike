package com.hoccer.xo.android.backup;

import com.hoccer.xo.android.XoApplication;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public class DatabaseBackup extends Backup {

    public static final String DB_PATH_NAME = "/data/data/" + XoApplication.getAppPackageName() + "/databases/hoccer-talk.db";

    protected final BackupMetadata mMetadata;

    protected DatabaseBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile);
        mMetadata = metadata;
    }

    static Backup create(String password) throws Exception {
        File database = new File(DB_PATH_NAME);

        String filename = BackupFileUtils.createUniqueBackupFilename();
        File backupFile = new File(XoApplication.getBackupDirectory(), filename + "." + BackupFileUtils.FILE_EXTENSION_ZIP);
        String clientName = XoApplication.getXoClient().getSelfContact().getName();

        BackupMetadata metadata = new BackupMetadata(BackupType.DATABASE, clientName, new Date());
        BackupFileUtils.createBackupFile(backupFile, database, metadata, password);

        return new DatabaseBackup(backupFile, metadata);
    }

    @Override
    public void restore(String password) throws Exception {
        File databaseTarget = new File(DB_PATH_NAME);
        BackupFileUtils.restoreBackup(mBackupFile, databaseTarget, password);
    }

    @Nullable
    @Override
    public String getClientName() {
        return mMetadata.getClientName();
    }

    @Override
    public Date getCreationDate() {
        return mMetadata.getCreationDate();
    }
}
