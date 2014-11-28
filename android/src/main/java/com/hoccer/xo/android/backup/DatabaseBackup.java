package com.hoccer.xo.android.backup;

import com.hoccer.xo.android.XoApplication;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public class DatabaseBackup extends Backup {

    private final BackupMetadata mMetadata;

    DatabaseBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile);
        mMetadata = metadata;
    }

    static DatabaseBackup create(String password) throws Exception {

        File database = new File("/data/data/" + XoApplication.getHoccerPackageName() + "/databases/hoccer-talk.db");

        String filename = BackupUtils.createUniqueBackupFilename();
        File backupFile = new File(XoApplication.getBackupDirectory(), filename + ".zip");
        String clientName = XoApplication.getXoClient().getSelfContact().getName();

        BackupMetadata metadata = new BackupMetadata(BackupType.DATABASE, clientName, new Date());
        BackupUtils.createBackupFile(backupFile, database, metadata, password);


        return new DatabaseBackup(backup, metadata);
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

    @Override
    public void restore(String password) {

    }
}
