package com.hoccer.xo.android.backup;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BackupFactory {

    public static Backup readBackup(File backupFile) throws BackupTypeNotSupportedException, IOException {

        String extension = FilenameUtils.getExtension(backupFile.getName());

        Backup backup;

        if ("json".equals(extension)) {
            backup = new CredentialsBackup(backupFile);
        } else if ("zip".equals(extension)) {

            BackupUtils backupUtils = new BackupUtils();
            BackupMetadata metadata = backupUtils.readMetadata(backupFile);

            if (metadata != null) {
                if (metadata.getBackupType() == BackupType.DATABASE) {
                    backup = new DatabaseBackup(backupFile, metadata);
                } else if (metadata.getBackupType() == BackupType.COMPLETE) {
                    backup = new CompleteBackup(backupFile, metadata);
                } else {
                    throw new BackupTypeNotSupportedException("Backup Type '" + metadata.getBackupType() + "' found in " + backupFile.getName() + " not supported");
                }
            } else {
                throw new FileNotFoundException(BackupUtils.METADATA_FILENAME + " not found in " + backupFile.getName());
            }
        } else {
            throw new IllegalArgumentException("Extension " + extension + " of " + backupFile.getName() + "is not supported.");
        }

        return backup;
    }

    public static Backup createCredentialsBackup() {
        return CredentialsBackup.create();
    }

    public static Backup createDatabaseBackup() {
        return DatabaseBackup.create();
    }

    public static Backup createCompleteBackup() {
        return CompleteBackup.create();
    }

    public static class BackupTypeNotSupportedException extends Throwable {
        public BackupTypeNotSupportedException(String message) {
            super(message);
        }
    }
}
