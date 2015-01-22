package com.hoccer.xo.android.util;

import com.hoccer.xo.android.backup.Backup;
import com.hoccer.xo.android.backup.BackupFileUtils;
import com.hoccer.xo.android.backup.BackupMetadata;
import com.hoccer.xo.android.backup.BackupType;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.to;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.*;
import static org.hamcrest.Matchers.is;


public class BackupFileUtilsTest {

    private static final File BACKUP_CREDENTIALS_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_CREDENTIALS_PATH);
    private static final File BACKUP_DB_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_DB_PATH);
    private static final File BACKUP_COMPLETE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_COMPLETE_PATH);

    private static final File DATABASE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_DB_FILE);
    private static final List<File> ATTACHMENT_FILES = BackupTestResources.getAttachmentFiles();

    private static final File ATTACHMENTS_TARGET_DIR = BackupTestResources.getAttachmentsTargetDirectory();
    private static final File DB_TARGET_FILE = BackupTestResources.getDatabaseTargetFile();

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

        assertNotNull(ATTACHMENTS_TARGET_DIR);
    }

    @Test
    public void testCreateDatabaseBackupFile() throws Exception {
        String filename = BackupFileUtils.createUniqueBackupFilename() + ".zip";
        File backupFile = new File(getClass().getResource("").getFile(), filename);

        BackupMetadata metadata = new BackupMetadata(BackupType.DATABASE, CLIENT_NAME, new Date());

        BackupFileUtils.createBackupFile(backupFile, metadata, DATABASE_FILE, PASSWORD);
        assertTrue("Creating backup failed", backupFile.exists());
        assertTrue("Creating backup failed", backupFile.length() > 0);

        boolean deleted = backupFile.delete();
        assertTrue(deleted);
    }

    @Test
    public void testCreateDatabaseBackupFileWithAttachments() throws Exception {
        String filename = BackupFileUtils.createUniqueBackupFilename() + ".zip";
        File backupFile = new File(getClass().getResource("").getFile(), filename);

        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, CLIENT_NAME, new Date());

        BackupFileUtils.createBackupFile(backupFile, metadata, DATABASE_FILE, PASSWORD, ATTACHMENT_FILES);
        assertTrue("Creating backup failed", backupFile.exists());
        assertTrue("Creating backup failed", backupFile.length() > 0);

        boolean deleted = backupFile.delete();
        assertTrue(deleted);
    }

    @Test
    public void testCancelCreateDatabaseBackupFileWithAttachments() throws Exception {
        String filename = BackupFileUtils.createUniqueBackupFilename() + ".zip";
        final File backupFile = new File(getClass().getResource("").getFile(), filename);

        final BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, CLIENT_NAME, new Date());

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    BackupFileUtils.createBackupFile(backupFile, metadata, DATABASE_FILE, PASSWORD, ATTACHMENT_FILES);
                } catch (InterruptedException e) {
                    assertTrue(!backupFile.exists());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();

        await().atMost(10, SECONDS).untilCall(to(backupFile).exists(), is(true));
    }

    @Test
    public void testReadMetadata() throws Exception {
        BackupMetadata metadata = BackupFileUtils.extractMetadata(BACKUP_COMPLETE_FILE);
        assertNotNull(metadata);
        assertEquals(BackupType.COMPLETE, metadata.getBackupType());
        assertEquals(CLIENT_NAME, metadata.getClientName());
        assertNotNull(metadata.getCreationDate());
    }

    @Test
    public void testGetBackupFilesMetadata() {
        List<Backup> backupFiles = BackupFileUtils.getBackups(BackupTestResources.getResourceDir());
        assertEquals(3, backupFiles.size());
    }

    @Test
    public void testExtractAndDecryptDatabase() throws Exception {
        BackupFileUtils.extractAndDecryptDatabase(BACKUP_DB_FILE, DB_TARGET_FILE, PASSWORD);
        assertTrue(DB_TARGET_FILE.exists());
        assertTrue(DB_TARGET_FILE.length() > 0);

        FileUtils.deleteDirectory(DB_TARGET_FILE.getParentFile());
    }

    @Test
    public void testGetUncompressedSizeOfZipFile() throws IOException {
        long uncompressedSize = BackupFileUtils.getUncompressedSize(BACKUP_DB_FILE);
        assertTrue(uncompressedSize > 0);
    }
}
