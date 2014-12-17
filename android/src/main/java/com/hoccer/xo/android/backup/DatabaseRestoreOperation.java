package com.hoccer.xo.android.backup;

import com.hoccer.talk.crypto.CryptoJSON;
import com.hoccer.xo.android.task.DeleteAvatarsTask;
import com.hoccer.xo.android.task.DeleteMissingTransfersTask;
import com.hoccer.xo.android.task.StartupTasks;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DatabaseRestoreOperation {

    public static final String TMP_EXTENSION = ".tmp";

    private final File mBackupFile;
    private final File mDatabaseTarget;
    private final String mPassword;

    private File mDatabaseTemp;

    public DatabaseRestoreOperation(File backupFile, File databaseTarget, String password) {
        mBackupFile = backupFile;
        mDatabaseTarget = databaseTarget;
        mPassword = password;


        mDatabaseTemp = new File(mDatabaseTarget.getParent(), mDatabaseTarget.getName() + TMP_EXTENSION);
    }

    public void invoke() throws IOException, CryptoJSON.DecryptionException {
        deleteTempFile();

        try {
            restore();
        } finally {
            cleanup();
        }

        registerStartupTasks();
    }

    private void deleteTempFile() throws IOException {
        if (mDatabaseTemp.exists()) {
            FileUtils.forceDelete(mDatabaseTemp);
        }
    }

    private void restore() throws IOException, CryptoJSON.DecryptionException {
        BackupFileUtils.extractAndDecryptDatabase(mBackupFile, mDatabaseTemp, mPassword);
        FileUtils.copyFile(mDatabaseTemp, mDatabaseTarget);
    }

    private void cleanup() throws IOException {
        deleteTempFile();
    }

    private static void registerStartupTasks() {
        StartupTasks.registerForNextStart(DeleteAvatarsTask.class);
        StartupTasks.registerForNextStart(DeleteMissingTransfersTask.class);
    }
}
