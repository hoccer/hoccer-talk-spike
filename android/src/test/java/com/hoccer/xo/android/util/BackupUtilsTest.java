package com.hoccer.xo.android.util;

import com.hoccer.xo.android.backup.*;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;


public class BackupUtilsTest {

    public static final String RESOURCE_DB_FILE = "/database.db";
    public static final String RESOURCE_ATTACHMENT_FILE_01 = "/IMG_20141120_130456_432.jpg";
    public static final String RESOURCE_BACKUP_FILE = "/hoccer_backup_20141126_162118.zip";

    public static final String PASSWORD = "12345678";

    private static File BACKUP_FILE = getResourceFile(RESOURCE_BACKUP_FILE);
    private static File DATABASE_FILE = getResourceFile(RESOURCE_DB_FILE);
    private static List<File> ATTACHMENT_FILES = getAttachmentFiles();

    private static File TARGET_DIR = createTargetDirectory();

    private BackupUtils mBackupUtils;

    @Before
    public void setup() {

        mBackupUtils = new BackupUtils();

        assertNotNull("Database file missing", DATABASE_FILE);

        assertNotNull(ATTACHMENT_FILES);
        assertFalse("Attachment files missing", ATTACHMENT_FILES.isEmpty());

        assertNotNull(BACKUP_FILE);

        assertNotNull(TARGET_DIR);
    }

    @Test
    public void testCreateDatabaseBackup() throws Exception {

        String filename = BackupUtils.createUniqueBackupFilename() + ".zip";
        File backup = new File(getClass().getResource("").getFile(), filename);
        assertNotNull(backup);

        mBackupUtils.createBackup(backup, DATABASE_FILE, PASSWORD);
        assertTrue("Creating backup failed", backup.length() > 0);

        boolean deleted = backup.delete();
        assertTrue(deleted);
    }

    @Test
    public void testCreateDatabaseBackupWithAttachments() throws Exception {

        String filename = BackupUtils.createUniqueBackupFilename() + ".zip";
        File backup = new File(getClass().getResource("").getFile(), filename);
        assertNotNull(backup);

        mBackupUtils.createBackup(backup, DATABASE_FILE, ATTACHMENT_FILES, PASSWORD);
        assertTrue("Creating backup failed", backup.length() > 0);

        boolean deleted = backup.delete();
        assertTrue(deleted);
    }

    @Test
    public void testExtractMetadata() throws Exception {

        BackupMetadata metadata = mBackupUtils.extractMetada(BACKUP_FILE);
        assertNotNull(metadata);
        assertEquals(BackupType.COMPLETE, metadata.getBackupType());
    }

    @Test
    public void testExtractAndDecryptDatabase() throws Exception {

        File database = new File(TARGET_DIR, "database.db");
        assertNotNull(database);

        mBackupUtils.extractAndDecryptDatabase(BACKUP_FILE, database, PASSWORD);
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
