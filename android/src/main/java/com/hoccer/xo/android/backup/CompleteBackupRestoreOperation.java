package com.hoccer.xo.android.backup;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CompleteBackupRestoreOperation {

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


    private static final FileFilter IS_NOT_DIRECTORY_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return !file.isDirectory();
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

    public void invoke() throws Exception {
        try {
            restore();
        } catch (Exception e) {
            restoreOld();
        } finally {
            cleanup();
        }
    }

    private void restore() throws IOException, CryptoJSON.DecryptionException {
        extractAttachmentsToTempDir();
        extractAndDecryptDatabaseToTempDir();
        keepCurrentAttachments();
        keepCurrentDatabase();
        moveAttachmentsToTargetDir();
        moveDatabaseToTarget();
    }

    private void extractAttachmentsToTempDir() throws IOException {
        if (mTempAttachmentsDir.exists()) {
            FileUtils.forceDelete(mTempAttachmentsDir);
        }
        FileUtils.forceMkdir(mTempAttachmentsDir);
        BackupFileUtils.extractAttachmentFiles(mBackupFile, mTempAttachmentsDir);
    }

    private void extractAndDecryptDatabaseToTempDir() throws IOException, CryptoJSON.DecryptionException {
        File tempDatabaseDirectory = mTempDatabaseFile.getParentFile();
        if (tempDatabaseDirectory.exists()) {
            FileUtils.forceDelete(tempDatabaseDirectory);
        }
        FileUtils.forceMkdir(tempDatabaseDirectory);
        BackupFileUtils.extractAndDecryptDatabase(mBackupFile, mTempDatabaseFile, mPassword);
    }

    private void keepCurrentAttachments() throws IOException {
        mOldAttachmentsDir = new File(mAttachmentsTargetDir.getPath() + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        for (File attachment : mAttachmentsTargetDir.listFiles(IS_NOT_DIRECTORY_FILTER)) {
            FileUtils.moveFileToDirectory(attachment, mOldAttachmentsDir, true);
        }
    }

    private void keepCurrentDatabase() throws IOException {
        mOldDatabaseFile = new File(mDatabaseTarget.getPath() + "." + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        FileUtils.moveFile(mDatabaseTarget, mOldDatabaseFile);
    }

    private void moveAttachmentsToTargetDir() throws IOException {
        for (File attachment : mTempAttachmentsDir.listFiles(IS_NOT_DIRECTORY_FILTER)) {
            FileUtils.moveFileToDirectory(attachment, mAttachmentsTargetDir, true);
        }
    }

    private void moveDatabaseToTarget() throws IOException {
        FileUtils.moveFile(mTempDatabaseFile, mDatabaseTarget);
    }

    private void cleanup() throws IOException {
        deleteTemp();
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