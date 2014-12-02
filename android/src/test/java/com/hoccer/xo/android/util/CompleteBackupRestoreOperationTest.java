package com.hoccer.xo.android.util;

import org.apache.commons.io.FileUtils;

import com.hoccer.xo.android.backup.CompleteBackupRestoreOperation;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class CompleteBackupRestoreOperationTest {

    private static final File BACKUP_COMPLETE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_COMPLETE_PATH);

    private static final File ATTACHMENTS_TARGET_DIR = BackupTestResources.createAttachmentsTargetDirectory();
    private static final File DB_TARGET_FILE = BackupTestResources.createDatabaseTargetFile();

    private static final String PASSWORD = "12345678";

    @Before
    public void setup() {
        assertNotNull(BACKUP_COMPLETE_FILE);
        assertNotNull(ATTACHMENTS_TARGET_DIR);
    }

    @Test
    public void testBackupRestoreOperation() throws Exception {

        new CompleteBackupRestoreOperation(BACKUP_COMPLETE_FILE, DB_TARGET_FILE, ATTACHMENTS_TARGET_DIR, PASSWORD).invoke();
        assertTrue(DB_TARGET_FILE.exists());
        assertTrue(DB_TARGET_FILE.length() > 0);
        assertTrue(ATTACHMENTS_TARGET_DIR.listFiles().length == 1);
        assertTrue(new File(ATTACHMENTS_TARGET_DIR, BackupTestResources.RESOURCE_ATTACHMENT_FILE_01).exists());

        FileUtils.deleteDirectory(ATTACHMENTS_TARGET_DIR);
        FileUtils.deleteDirectory(DB_TARGET_FILE.getParentFile());
    }
}
