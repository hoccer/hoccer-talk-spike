package com.hoccer.xo.android.backup;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DatabaseRestoreOperation {

    public static final String TMP_EXTENSION = ".tmp";

    private final File mBackupFile;
    private final File mDatabaseTarget;
    private final String mPassword;

    private final File mDatabaseTemp;

    public DatabaseRestoreOperation(File backupFile, File databaseTarget, String password) {
        mBackupFile = backupFile;
        mDatabaseTarget = databaseTarget;
        mPassword = password;


        mDatabaseTemp = new File(mDatabaseTarget.getParent(), mDatabaseTarget.getName() + TMP_EXTENSION);
    }

    public void invoke() throws Exception {
        deleteTempFile();

        try {
            restore();
        } finally {
            cleanup();
        }
    }

    private void deleteTempFile() throws IOException {
        if (mDatabaseTemp.exists()) {
            FileUtils.forceDelete(mDatabaseTemp);
        }
    }

    private void restore() throws Exception {
        BackupFileUtils.extractAndDecryptDatabase(mBackupFile, mDatabaseTemp, mPassword);
        FileUtils.copyFile(mDatabaseTemp, mDatabaseTarget);
    }

    private void cleanup() throws IOException {
        deleteTempFile();
    }
}
