package com.hoccer.xo.android.backup;

import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BackupFactory {

    public static Backup createBackup(BackupType type, String password) throws Exception {
        switch (type) {
            case DATABASE:
                return createDatabaseBackup(password);
            case COMPLETE:
                return createCompleteBackup(password);
            default:
                throw new IllegalArgumentException("Unknown BackupType " + type);
        }
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

    public static Backup readBackup(File backupFile) throws BackupTypeNotSupportedException, IOException, ZipException {
        if (isJson(backupFile)) {
            return new CredentialsBackup(backupFile);
        }

        if (isZip(backupFile)) {
            BackupMetadata metadata = BackupFileUtils.extractMetadata(backupFile);
            return readBackup(backupFile, metadata);
        }

        throw new IOException("Extension " + FilenameUtils.getExtension(backupFile.getName()) + " of " + backupFile.getName() + " is not supported.");
    }

    private static boolean isJson(File backupFile) {
        return FileFilterUtils.suffixFileFilter("json").accept(backupFile);
    }

    private static boolean isZip(File backupFile) {
        return FileFilterUtils.suffixFileFilter("zip").accept(backupFile);
    }

    private static Backup readBackup(File backupFile, BackupMetadata metadata) throws BackupTypeNotSupportedException, FileNotFoundException {
        Backup backup;
        if (metadata.getBackupType() == BackupType.DATABASE) {
            backup = new DatabaseBackup(backupFile, metadata);
        } else if (metadata.getBackupType() == BackupType.COMPLETE) {
            backup = new CompleteBackup(backupFile, metadata);
        } else {
            throw new BackupTypeNotSupportedException("Backup Type '" + metadata.getBackupType() + "' found in " + backupFile.getName() + " not supported");
        }
        return backup;
    }

    public static class BackupTypeNotSupportedException extends Exception {
        public BackupTypeNotSupportedException(String message) {
            super(message);
        }
    }
}
