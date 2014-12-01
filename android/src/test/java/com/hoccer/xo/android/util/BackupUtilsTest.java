package com.hoccer.xo.android.util;

import com.hoccer.xo.android.backup.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static junit.framework.TestCase.*;


public class BackupUtilsTest {

    private static final File BACKUP_CREDENTIALS_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_CREDENTIALS_PATH);
    private static final File BACKUP_DB_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_DB_PATH);
    private static final File BACKUP_COMPLETE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_COMPLETE_PATH);

    private static final File DATABASE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_DB_FILE);
    private static final List<File> ATTACHMENT_FILES = BackupTestResources.getAttachmentFiles();

    private static final File TARGET_DIR = BackupTestResources.createTargetDirectory();

    public static final String CLIENT_NAME = "clientName";
    public static final String PASSWORD = "12345678";

    @Before
    public void setup() {

        assertNotNull("Database file missing", DATABASE_FILE);

        assertNotNull(ATTACHMENT_FILES);
        assertFalse("Attachment files missing", ATTACHMENT_FILES.isEmpty());

        assertNotNull(BACKUP_COMPLETE_FILE);

        assertNotNull(BACKUP_CREDENTIALS_FILE);

        assertNotNull(BACKUP_DB_FILE);

        assertNotNull(TARGET_DIR);
    }

    @Test
    public void testCreateDatabaseBackupFile() throws Exception {

        String filename = BackupUtils.createUniqueBackupFilename() + ".zip";
        File backupFile = new File(getClass().getResource("").getFile(), filename);

        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, CLIENT_NAME, new Date());

        BackupUtils.createBackupFile(backupFile, DATABASE_FILE, metadata, PASSWORD);
        assertTrue("Creating backup failed", backupFile.exists());
        assertTrue("Creating backup failed", backupFile.length() > 0);

        boolean deleted = backupFile.delete();
        assertTrue(deleted);
    }

    @Test
    public void testCreateDatabaseBackupFileWithAttachments() throws Exception {

        String filename = BackupUtils.createUniqueBackupFilename() + ".zip";
        File backupFile = new File(getClass().getResource("").getFile(), filename);

        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, CLIENT_NAME, new Date());

        BackupUtils.createBackupFile(backupFile, DATABASE_FILE, ATTACHMENT_FILES, metadata, PASSWORD);
        assertTrue("Creating backup failed", backupFile.exists());
        assertTrue("Creating backup failed", backupFile.length() > 0);

        boolean deleted = backupFile.delete();
        assertTrue(deleted);
    }

    @Test
    public void testReadMetadata() throws Exception {

        BackupMetadata metadata = BackupUtils.readMetadata(BACKUP_COMPLETE_FILE);
        assertNotNull(metadata);
        assertEquals(BackupType.COMPLETE, metadata.getBackupType());
        assertEquals(CLIENT_NAME, metadata.getClientName());
        assertNotNull(metadata.getCreationDate());
    }

    @Test
    public void testGetBackupFiles() {

        List<File> backupFiles = BackupUtils.getBackupFiles(BACKUP_COMPLETE_FILE.getParentFile());
        assertEquals(3, backupFiles.size());
        assertTrue(backupFiles.contains(BACKUP_CREDENTIALS_FILE));
        assertTrue(backupFiles.contains(BACKUP_DB_FILE));
        assertTrue(backupFiles.contains(BACKUP_COMPLETE_FILE));
    }

    @Test
    public void testReadBackup() throws BackupFactory.BackupTypeNotSupportedException, IOException {

        Backup backup = BackupFactory.readBackup(BACKUP_COMPLETE_FILE);
        assertNotNull(backup);
        assertTrue(backup instanceof CompleteBackup);
    }

    @Test
    public void testImportDatabaseBackup() throws Exception {

        File databaseTarget = new File(TARGET_DIR, "database.db");
        assertNotNull(databaseTarget);

        BackupUtils.importBackup(BACKUP_COMPLETE_FILE, databaseTarget, PASSWORD);
        assertTrue(databaseTarget.length() > 0);

        boolean deleted = databaseTarget.delete();
        assertTrue(deleted);
    }

    @Test
    public void testImportCompleteBackup() throws Exception {

        File databaseTarget = new File(TARGET_DIR, "database.db");
        assertNotNull(databaseTarget);

        BackupUtils.importBackup(BACKUP_COMPLETE_FILE, databaseTarget, TARGET_DIR, PASSWORD);
        assertTrue(databaseTarget.length() > 0);
        assertTrue(TARGET_DIR.listFiles().length > 1);

        org.apache.commons.io.FileUtils.cleanDirectory(TARGET_DIR);
    }
}
