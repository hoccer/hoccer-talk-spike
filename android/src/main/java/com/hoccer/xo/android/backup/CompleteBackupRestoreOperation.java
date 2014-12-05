package com.hoccer.xo.android.backup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CompleteBackupRestoreOperation {

    private static final Logger LOG = Logger.getLogger(CompleteBackupRestoreOperation.class.getName());
    private static final String TEMP_ATTACHMENTS_DIR_NAME = "tmp_attachments";
    private static final String TEMP_DB_DIR_NAME = "tmp_db";

    private final File mBackupFile;
    private final File mDatabaseTarget;
    private final File mAttachmentsTargetDir;
    private final String mPassword;

    private final File mTempAttachmentsDir;
    private final File mTempDatabaseFile;
    private File mOldAttachmentsDir;
    private File mOldDatabaseFile;


    FileFilter mFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.isDirectory() && "Backups".equals(file.getName())) {
                return false;
            }
            return true;
        }
    };

    public CompleteBackupRestoreOperation(File backupFile, File databaseTarget, File attachmentsTargetDir, String password) {
        mBackupFile = backupFile;
        mDatabaseTarget = databaseTarget;
        mAttachmentsTargetDir = attachmentsTargetDir;
        mPassword = password;

        mTempAttachmentsDir = new File(attachmentsTargetDir.getParent(), TEMP_ATTACHMENTS_DIR_NAME);
        mTempDatabaseFile = new File(attachmentsTargetDir.getParent(), TEMP_DB_DIR_NAME + File.separator + databaseTarget.getName());
    }

    public void invoke() throws IOException {
        try {
            restore();
        } catch (Exception e) {
            restoreOld();
        } finally {
            cleanup();
        }
    }

    private void restore() throws Exception {
        extractAttachmentsToTempDir();
        extractAndDecryptDatabaseToTempDir();
        keepCurrentAttachments();
        keepCurrentDatabase();
        moveAttachmentsToTargetDir();
        moveDatabaseToTarget();
    }

    private void extractAttachmentsToTempDir() throws IOException {
        FileUtils.forceMkdir(mTempAttachmentsDir);
        BackupFileUtils.extractAttachmentFiles(mBackupFile, mTempAttachmentsDir);
    }

    private void extractAndDecryptDatabaseToTempDir() throws Exception {
        BackupFileUtils.extractAndDecryptDatabase(mBackupFile, mTempDatabaseFile, mPassword);
    }

    private void keepCurrentAttachments() throws IOException {
        if (mAttachmentsTargetDir.exists()) {
            mOldAttachmentsDir = new File(mAttachmentsTargetDir.getPath() + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
            for (File attachment : mAttachmentsTargetDir.listFiles(mFileFilter)) {
                FileUtils.moveFileToDirectory(attachment, mOldAttachmentsDir, true);
            }
        }
    }

    private void keepCurrentDatabase() throws IOException {
        if (mDatabaseTarget.exists()) {
            mOldDatabaseFile = new File(mDatabaseTarget.getPath() + "." + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
            FileUtils.moveFile(mDatabaseTarget, mOldDatabaseFile);
        }
    }

    private void moveAttachmentsToTargetDir() throws IOException {
        for (File attachment : mTempAttachmentsDir.listFiles(mFileFilter)) {
            FileUtils.moveFileToDirectory(attachment, mAttachmentsTargetDir, true);
        }
    }

    private void moveDatabaseToTarget() throws IOException {
        FileUtils.moveFile(mTempDatabaseFile, mDatabaseTarget);
    }

    private void cleanup() throws IOException {
        deleteTemp();
//        deleteOld();      //TODO: discuss if old attachments and db should be kept
    }

    private void deleteTemp() throws IOException {
        FileUtils.deleteDirectory(mTempDatabaseFile.getParentFile());
        FileUtils.deleteDirectory(mTempAttachmentsDir);
    }

    private void deleteOld() throws IOException {
        deleteOldDatabase();
        deleteOldAttachments();
    }

    private void deleteOldDatabase() throws IOException {
        if (mOldDatabaseFile.exists()) {
            FileUtils.forceDelete(mOldDatabaseFile);
        }
    }

    private void deleteOldAttachments() throws IOException {
        if (mOldAttachmentsDir.exists()) {
            FileUtils.forceDelete(mOldAttachmentsDir);
        }
    }

    private void restoreOld() throws IOException {
        restoreOldDatabase();
        restoreOldAttachments();
        deleteOld();
    }

    private void restoreOldAttachments() throws IOException {
        if (mOldAttachmentsDir.exists()) {
            FileUtils.moveDirectory(mOldAttachmentsDir, mAttachmentsTargetDir);
        }
    }

    private void restoreOldDatabase() throws IOException {
        if (mOldDatabaseFile.exists()) {
            FileUtils.moveFile(mOldDatabaseFile, mDatabaseTarget);
        }
    }
}