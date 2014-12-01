package com.hoccer.xo.android.util;

import com.hoccer.xo.android.backup.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class BackupFactoryTest {

    private static final File BACKUP_CREDENTIALS_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_CREDENTIALS_PATH);
    private static final File BACKUP_DB_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_DB_PATH);
    private static final File BACKUP_COMPLETE_FILE = BackupTestResources.getResourceFile(BackupTestResources.RESOURCE_BACKUP_COMPLETE_PATH);

    @Before
    public void setup() {
        assertNotNull(BACKUP_CREDENTIALS_FILE);
        assertNotNull(BACKUP_DB_FILE);
        assertNotNull(BACKUP_COMPLETE_FILE);
    }

    @Test
    public void testReadCredentialsBackup() throws BackupFactory.BackupTypeNotSupportedException, IOException {
        Backup backup = BackupFactory.readBackup(BACKUP_CREDENTIALS_FILE);
        assertTrue(backup instanceof CredentialsBackup);
    }

    @Test
    public void testReadDatabaseBackup() throws BackupFactory.BackupTypeNotSupportedException, IOException {
        Backup backup = BackupFactory.readBackup(BACKUP_DB_FILE);
        assertTrue(backup instanceof DatabaseBackup);
    }

    @Test
    public void testReadCompleteBackup() throws BackupFactory.BackupTypeNotSupportedException, IOException {
        Backup backup = BackupFactory.readBackup(BACKUP_COMPLETE_FILE);
        assertTrue(backup instanceof CompleteBackup);
    }
}
