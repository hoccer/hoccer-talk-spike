package com.hoccer.xo.android.util;

import org.apache.commons.io.FileUtils;

import com.hoccer.xo.android.backup.CompleteBackupRestoreOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class CompleteBackupRestoreOperationTest {

    private static final File BACKUP_COMPLETE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_COMPLETE_PATH);

    private static final File ATTACHMENTS_TARGET_DIR = BackupTestResources.getAttachmentsTargetDirectory();
    private static final File ATTACHMENTS_TARGET_FILE = new File(BackupTestResources.getAttachmentsTargetDirectory(), "attachment.file");
    private static final File DB_TARGET_FILE = BackupTestResources.getDatabaseTargetFile();

    private static final String PASSWORD = "12345678";

    @Before
    public void setup() throws IOException {
        assertNotNull(BACKUP_COMPLETE_FILE);
        assertNotNull(ATTACHMENTS_TARGET_DIR);

        FileUtils.forceMkdir(ATTACHMENTS_TARGET_DIR);
        ATTACHMENTS_TARGET_FILE.createNewFile();
        FileUtils.forceMkdir(DB_TARGET_FILE.getParentFile());
        DB_TARGET_FILE.createNewFile();
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(ATTACHMENTS_TARGET_DIR.getParentFile());
        FileUtils.deleteDirectory(DB_TARGET_FILE.getParentFile());
    }

    @Test
    public void testRestore() throws Exception {
        new CompleteBackupRestoreOperation(BACKUP_COMPLETE_FILE, DB_TARGET_FILE, ATTACHMENTS_TARGET_DIR, PASSWORD).invoke();
        assertTrue(DB_TARGET_FILE.exists());
        assertTrue(DB_TARGET_FILE.length() > 0);
        assertTrue(ATTACHMENTS_TARGET_DIR.listFiles().length == 1);
        assertTrue(new File(ATTACHMENTS_TARGET_DIR, BackupTestResources.RESOURCE_ATTACHMENT_FILE_01).exists());
    }
}
