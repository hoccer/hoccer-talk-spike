package com.hoccer.xo.android.backup;

import com.hoccer.xo.android.XoApplication;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CompleteBackup extends DatabaseBackup {

    protected CompleteBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile, metadata);
    }

    public static Backup create(String password) throws Exception {
        String filename = BackupUtils.createUniqueBackupFilename();
        File backupFile = new File(XoApplication.getBackupDirectory(), filename + "_with_attachments.zip");

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

        BackupUtils.createBackupFile(backupFile, database, attachments, metadata, password);

        return new CompleteBackup(backupFile, metadata);
    }

    @Override
    public void restore(String password) throws Exception {
        File databaseTarget = new File(DB_PATH_NAME);
        File attachmentsTargetDir = XoApplication.getAttachmentDirectory();

        new CompleteBackupRestoreOperation(mBackupFile, databaseTarget, attachmentsTargetDir, password).invoke();
    }
}
