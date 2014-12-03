package com.hoccer.xo.android.backup;

import android.os.Environment;
import android.os.StatFs;
import com.hoccer.xo.android.XoApplication;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CompleteBackup extends DatabaseBackup {

    protected CompleteBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile, metadata);
    }

    public static Backup create(String password) throws Exception {
        String filename = BackupFileUtils.createUniqueBackupFilename();
        File backupFile = new File(XoApplication.getBackupDirectory(), filename + "_with_attachments.zip");
        backupFile.createNewFile();

        File database = new File(DB_PATH_NAME);

        String clientName = XoApplication.getXoClient().getSelfContact().getName();
        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, clientName, new Date());

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        };

        File[] files = XoApplication.getAttachmentDirectory().listFiles(filter);
        List<File> attachments = new ArrayList<File>();
        if (files != null) {
            attachments = Arrays.asList(files);
        }

        BackupFileUtils.createBackupFile(backupFile, database, attachments, metadata, password);

        return new CompleteBackup(backupFile, metadata);
    }

    @Override
    public void restore(String password) throws IOException, NotEnoughDiskSpaceAvailable {
        ensureEnoughDiskSpaceAvailable();

        File databaseTarget = new File(DB_PATH_NAME);
        File attachmentsTargetDir = XoApplication.getAttachmentDirectory();
        new CompleteBackupRestoreOperation(mBackupFile, databaseTarget, attachmentsTargetDir, password).invoke();
    }

    private void ensureEnoughDiskSpaceAvailable() throws IOException, NotEnoughDiskSpaceAvailable {
        long requiredDiskSpace = BackupFileUtils.getUncompressedSize(mBackupFile);
        long availableDiskSpace = getAvailableDiskStorage();
        if (requiredDiskSpace < availableDiskSpace) {
            throw new NotEnoughDiskSpaceAvailable(requiredDiskSpace, availableDiskSpace);
        }
    }

    private long getAvailableDiskStorage() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return (long)stat.getAvailableBlocks() * (long)stat.getBlockSize();
    }

    public class NotEnoughDiskSpaceAvailable extends Exception {
        private NotEnoughDiskSpaceAvailable(long requiredDiskSpace, long availableDiskStorage) {
            super("Not enough free disk space available. Required: " + requiredDiskSpace + "Available: " + availableDiskStorage);
        }
    }
}
