package com.hoccer.xo.android.util;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.*;


public class BackupUtilsTest {

    public static final String RESOURCE_DB_FILE = "/database.db";
    public static final String RESOURCE_ATTACHMENT_FILE_01 = "/attachment_01.txt";

    private File mDatabase;
    private List<File> mAttachments = new ArrayList<File>();

    @Before
    public void setup() {

        mDatabase = getDatabaseFile();
        assertNotNull("Database file missing", mDatabase);

        mAttachments = getAttachmentFiles();
        TestCase.assertNotNull(mAttachments);
        assertFalse("Database file missing", mAttachments.isEmpty());
    }

    @Test
    public void testCreateBackup() throws Exception {

        BackupUtils backupUtils = new BackupUtils();

        File backup = backupUtils.createEmptyBackupFile(getClass().getResource("").getFile());
        backupUtils.createBackup(backup, mDatabase, mAttachments, "12345678");

        assertNotNull("Creating backup failed", backup);
        assertTrue(backup.length() > 0);
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
