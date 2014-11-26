package com.hoccer.xo.android.util;

import com.hoccer.talk.crypto.CryptoJSON;
import com.hoccer.xo.android.XoApplication;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupUtils {

    public static final String FILE_EXTENSION_ZIP = "zip";
    public static final String FILE_EXTENSION_DB = "db";
    public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
    public static final String BACKUP_FILENAME_PREFIX = "hoccer_backup_";
    public static final String BACKUP_FILENAME_PATTERN = BACKUP_FILENAME_PREFIX + "%s.zip";
    private static final String DB_FILE_NAME = "database.db";

    private String mDatabaseFilepath;

    public void createBackup(File result, File database, List<File> attachments, String password) throws Exception {
        // 1. encrypt database
        // 2. write metadata file ?
        // 3. create zip with database, metadata and attachments
        byte[] encryptedDatabase = encryptFile(database, password);
        createZip(result, encryptedDatabase, attachments);
    }

    public File createBackup(File database, String password) throws Exception {
        // 1. encrypt database
        // 2. write metadata file ?
        // 3. create zip with database, metadata and attachments
        byte[] encryptedDatabase = encryptFile(database, password);
        return createZip(encryptedDatabase);
    }


    private byte[] encryptFile(File input, String password) throws Exception {

        FileInputStream in = new FileInputStream(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            baos.write(buffer, 0, count);
        }
        in.close();
        byte[] bytes = baos.toByteArray();

        return CryptoJSON.encrypt(bytes, password, "text/plain");
    }

    private File createZip(byte[] database) {
        return null;
    }

    private File createZip(File backup, byte[] data, List<File> attachments) throws IOException {

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            for (File attachment : attachments) {
                addZipEntry(zos, attachment);
            }
            addZipEntry(zos, data, DB_FILE_NAME);
        } finally {
            zos.close();
        }

        return backup;
    }

    public void importDatabaseAndAttachments(File backup) throws IOException {
        extractAttachmentsAndDatabaseImport(backup);
    }

    private void extractAttachmentsAndDatabaseImport(File backup) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(backup));
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                String filename = ze.getName();
                byte[] bytes = baos.toByteArray();

                File file = null;
                String extension = filename.substring(filename.lastIndexOf(".") + 1);
                if (extension.equals("db")) {
                    file = new File(mDatabaseFilepath);
                    if (file.exists()) {
                        file.delete();
                    }
                } else {
                    file = new File(XoApplication.getAttachmentDirectory(), filename);
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
            }
        } finally {
            zis.close();
        }

        initDatabase();
    }


    public File exportDatabaseToFile() {
        File exportFile = createEmptyBackupFile(FILE_EXTENSION_DB);

        String inFileName = mDatabaseFilepath;
        File dbFile = new File(inFileName);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(dbFile);
            OutputStream output = new FileOutputStream(exportFile.getAbsolutePath());
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
            output.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return exportFile;
    }

    private void addZipEntry(ZipOutputStream zos, File fileEntry) throws IOException {

        FileInputStream in = new FileInputStream(fileEntry);
        ZipEntry entry = new ZipEntry(fileEntry.getName());
        zos.putNextEntry(entry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        in.close();
        zos.closeEntry();
    }

    private void addZipEntry(ZipOutputStream zos, byte[] data, String dataName) throws IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ZipEntry entry = new ZipEntry(dataName);
        zos.putNextEntry(entry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        in.close();
        zos.closeEntry();
    }

    public File createEmptyBackupFile(String parent) {
        String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        String fileName = String.format(BACKUP_FILENAME_PATTERN, timestamp);
        return new File(parent, fileName);
    }

    private void initDatabase() {
        try {
            XoApplication.getXoClient().getDatabase().initialize();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<File> getExportFiles() {
        List<File> exportFiles = new ArrayList<File>();

        File[] files = XoApplication.getExternalStorage().listFiles();
        if (files != null) {
            for (File file : files) {
                String filename = file.getName();
                String extension = FilenameUtils.getExtension(filename);
                if (filename.startsWith(BACKUP_FILENAME_PREFIX) && extension.equals(FILE_EXTENSION_ZIP)) {
                    exportFiles.add(file);
                }
            }
        }

        return exportFiles;
    }

//    public static File createBackup() {
//        return null;
//    }
}
