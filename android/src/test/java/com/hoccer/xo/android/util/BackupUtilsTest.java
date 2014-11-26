package com.hoccer.xo.android.util;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.*;


public class BackupUtilsTest {

    public static final String RESOURCE_DB_FILE = "/database.db";
    public static final String RESOURCE_ATTACHMENT_FILE_01 = "/attachment_01.txt";
    public static final String PASSWORD = "12345678";

    private File mDatabase;
    private List<File> mAttachments = new ArrayList<File>();

    private BackupUtils mBackupUtils;

    @Before
    public void setup() {

        mBackupUtils = new BackupUtils();

        mDatabase = getDatabaseFile();
        assertNotNull("Database file missing", mDatabase);

        mAttachments = getAttachmentFiles();
        TestCase.assertNotNull(mAttachments);
        assertFalse("Database file missing", mAttachments.isEmpty());
    }

    @Test
    public void testCreateDatabaseBackupWithAttachments() throws Exception {

        File backup = mBackupUtils.createEmptyBackupFile(getClass().getResource("").getFile());
        assertNotNull(backup);

        mBackupUtils.createBackup(backup, mDatabase, mAttachments, PASSWORD);
        assertTrue("Creating backup failed", backup.length() > 0);

        boolean deleted = backup.delete();
        assertTrue(deleted);
    }

    @Test
    public void testCreateDatabaseBackup() throws Exception {

        File backup = mBackupUtils.createEmptyBackupFile(getClass().getResource("").getFile());
        assertNotNull(backup);

        mBackupUtils.createBackup(backup, mDatabase, PASSWORD);
        assertTrue("Creating backup failed", backup.length() > 0);

        boolean deleted = backup.delete();
        assertTrue(deleted);
    }

    private File getDatabaseFile() {

        URL databaseFileUrl = getClass().getResource(RESOURCE_DB_FILE);
        TestCase.assertNotNull(databaseFileUrl);

        return new File(databaseFileUrl.getFile());
    }

    private List<File> getAttachmentFiles() {

        URL attachmentFileUrl = getClass().getResource(RESOURCE_ATTACHMENT_FILE_01);
        TestCase.assertNotNull(attachmentFileUrl);

        List<File> attachmentFiles = new ArrayList<File>();
        attachmentFiles.add(new File(attachmentFileUrl.getFile()));
        return attachmentFiles;
    }
}
