package com.hoccer.xo.android.util;

import com.hoccer.xo.android.backup.DatabaseRestoreOperation;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class DatabaseRestoreOperationTest {

    private static final File BACKUP_COMPLETE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_COMPLETE_PATH);

    private static final File DB_TARGET_FILE = BackupTestResources.getDatabaseTargetFile();

    private static final String PASSWORD = "12345678";

    @Before
    public void setup() throws IOException {
        assertNotNull(BACKUP_COMPLETE_FILE);

        FileUtils.forceMkdir(DB_TARGET_FILE.getParentFile());
        DB_TARGET_FILE.createNewFile();
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(DB_TARGET_FILE.getParentFile());
    }

    @Test
    public void testRestore() throws Exception {
        new DatabaseRestoreOperation(BACKUP_COMPLETE_FILE, DB_TARGET_FILE, PASSWORD).invoke();
        assertTrue(DB_TARGET_FILE.exists());
        assertTrue(DB_TARGET_FILE.length() > 0);
    }
}
