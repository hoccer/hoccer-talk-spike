package com.hoccer.xo.android.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;

public class BackupTestResources {

    public static final String RESOURCE_BACKUP_CREDENTIALS_PATH = "/credentials.json";
    public static final String RESOURCE_BACKUP_COMPLETE_PATH = "/hoccer_backup_20141204_114641_with_attachments.zip";
    public static final String RESOURCE_BACKUP_DB_PATH = "/hoccer_backup_20141204_125159_db.zip";
    public static final String RESOURCE_DB_FILE = "/database.db";
    public static final String RESOURCE_ATTACHMENT_FILE_01 = "/IMG_20141120_130456_432.jpg";

    private static final String ATTACHMENTS_TARGET_DIR_NAME = "attachments_target";
    private static final String DB_TARGET_FILE_PATH = "/db_target/database.db";

    public static File getResourceFile(String path) {
        URL url = BackupFileUtilsTest.class.getResource(path);
        assertNotNull(url);

        return new File(url.getFile());
    }

    public static File getResourceDir() {
        URL url = BackupFileUtilsTest.class.getResource(RESOURCE_BACKUP_COMPLETE_PATH);
        assertNotNull(url);

        return new File(url.getFile()).getParentFile();
    }

    public static List<File> getAttachmentFiles() {
        URL attachmentFileUrl = BackupFileUtilsTest.class.getResource(BackupTestResources.RESOURCE_ATTACHMENT_FILE_01);
        assertNotNull(attachmentFileUrl);

        List<File> attachmentFiles = new ArrayList<File>();
        attachmentFiles.add(new File(attachmentFileUrl.getFile()));
        return attachmentFiles;
    }

    public static File createAttachmentsTargetDirectory() {
        return new File(BackupFileUtilsTest.class.getResource("").getFile(), ATTACHMENTS_TARGET_DIR_NAME);
    }

    public static File createDatabaseTargetFile() {
        return new File(BackupFileUtilsTest.class.getResource("").getFile(), DB_TARGET_FILE_PATH);
    }
}
