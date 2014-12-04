package com.hoccer.xo.android.backup;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BackupFactory {

    public static Backup readBackup(File backupFile) throws BackupTypeNotSupportedException, IOException {
        if (isJson(backupFile)) {
            return new CredentialsBackup(backupFile);
        }

        if (isZip(backupFile)) {
            BackupMetadata metadata = BackupFileUtils.readMetadata(backupFile);
            return readBackup(backupFile, metadata);
        }

        throw new IllegalArgumentException("Extension " + FilenameUtils.getExtension(backupFile.getName()) + " of " + backupFile.getName() + "is not supported.");
    }

    private static Backup readBackup(File backupFile, BackupMetadata metadata) throws BackupTypeNotSupportedException, FileNotFoundException {
        Backup backup;
        if (metadata != null) {
            if (metadata.getBackupType() == BackupType.DATABASE) {
                backup = new DatabaseBackup(backupFile, metadata);
            } else if (metadata.getBackupType() == BackupType.COMPLETE) {
                backup = new CompleteBackup(backupFile, metadata);
            } else {
                throw new BackupTypeNotSupportedException("Backup Type '" + metadata.getBackupType() + "' found in " + backupFile.getName() + " not supported");
            }
        } else {
            throw new FileNotFoundException(BackupFileUtils.METADATA_FILENAME + " not found in " + backupFile.getName());
        }
        return backup;
    }

    private static boolean isZip(File backupFile) {
        return FileFilterUtils.suffixFileFilter("zip").accept(backupFile);
    }

    private static boolean isJson(File backupFile) {
        return FileFilterUtils.suffixFileFilter("json").accept(backupFile);
    }

    public static Backup createCredentialsBackup(String password) throws IOException {
        return CredentialsBackup.create(password);
    }

    public static Backup createDatabaseBackup(String password) throws Exception {
        return DatabaseBackup.create(password);
    }

    public static Backup createCompleteBackup(String password) throws Exception {
        return CompleteBackup.create(password);
    }

    public static class BackupTypeNotSupportedException extends Exception {
        public BackupTypeNotSupportedException(String message) {
            super(message);
        }
    }
}
