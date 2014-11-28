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

import static junit.framework.Assert.*;
import static junit.framework.TestCase.assertFalse;


public class BackupUtilsTest {

    public static final String RESOURCE_DB_FILE = "/database.db";
    public static final String RESOURCE_ATTACHMENT_FILE_01 = "/IMG_20141120_130456_432.jpg";
    public static final String RESOURCE_BACKUP_FILE = "/hoccer_backup_20141127_144625.zip";

    public static final String PASSWORD = "12345678";
    public static final String CLIENT_NAME = "clientName";

    private static final File BACKUP_FILE = getResourceFile(RESOURCE_BACKUP_FILE);
    private static final File DATABASE_FILE = getResourceFile(RESOURCE_DB_FILE);
    private static final List<File> ATTACHMENT_FILES = getAttachmentFiles();

    private static final File TARGET_DIR = createTargetDirectory();

    @Before
    public void setup() {

        assertNotNull("Database file missing", DATABASE_FILE);

        assertNotNull(ATTACHMENT_FILES);
        assertFalse("Attachment files missing", ATTACHMENT_FILES.isEmpty());

        assertNotNull(BACKUP_FILE);

        assertNotNull(TARGET_DIR);
    }

    @Test
    public void testCreateDatabaseBackup() throws Exception {

        String filename = BackupUtils.createUniqueBackupFilename() + ".zip";
        File backupFile = new File(getClass().getResource("").getFile(), filename);

        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, CLIENT_NAME, new Date());

        BackupUtils.createBackup(backupFile, DATABASE_FILE, metadata, PASSWORD);
        assertTrue("Creating backup failed", backupFile.exists());
        assertTrue("Creating backup failed", backupFile.length() > 0);

        boolean deleted = backupFile.delete();
        assertTrue(deleted);
    }

    @Test
    public void testCreateDatabaseBackupWithAttachments() throws Exception {

        String filename = BackupUtils.createUniqueBackupFilename() + ".zip";
        File backupFile = new File(getClass().getResource("").getFile(), filename);

        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, CLIENT_NAME, new Date());

        BackupUtils.createBackup(backupFile, DATABASE_FILE, ATTACHMENT_FILES, metadata, PASSWORD);
        assertTrue("Creating backup failed", backupFile.exists());
        assertTrue("Creating backup failed", backupFile.length() > 0);

        boolean deleted = backupFile.delete();
        assertTrue(deleted);
    }

    @Test
    public void testReadMetadata() throws Exception {

        BackupMetadata metadata = BackupUtils.readMetadata(BACKUP_FILE);
        assertNotNull(metadata);
        assertEquals(BackupType.COMPLETE, metadata.getBackupType());
        assertEquals(CLIENT_NAME, metadata.getClientName());
        assertNotNull(metadata.getCreationDate());
    }

    @Test
    public void testReadBackup() throws BackupFactory.BackupTypeNotSupportedException, IOException {

        Backup backup = BackupFactory.readBackup(BACKUP_FILE);
        assertNotNull(backup);
        assertTrue(backup instanceof CompleteBackup);
    }

    @Test
    public void testExtractAndDecryptDatabase() throws Exception {

        File database = new File(TARGET_DIR, "database.db");
        assertNotNull(database);

        BackupUtils.extractAndDecryptDatabase(BACKUP_FILE, database, PASSWORD);
        assertTrue(database.length() > 0);

        boolean deleted = database.delete();
        assertTrue(deleted);
    }

    private static File getResourceFile(String path) {

        URL url = BackupUtilsTest.class.getResource(path);
        assertNotNull(url);

        return new File(url.getFile());
    }

    private static List<File> getAttachmentFiles() {

        URL attachmentFileUrl = BackupUtilsTest.class.getResource(RESOURCE_ATTACHMENT_FILE_01);
        assertNotNull(attachmentFileUrl);

        List<File> attachmentFiles = new ArrayList<File>();
        attachmentFiles.add(new File(attachmentFileUrl.getFile()));
        return attachmentFiles;
    }

    private static File createTargetDirectory() {

        File targetDir = new File(BackupUtilsTest.class.getResource("").getFile(), "target");
        targetDir.mkdir();
        return targetDir;
    }
}
