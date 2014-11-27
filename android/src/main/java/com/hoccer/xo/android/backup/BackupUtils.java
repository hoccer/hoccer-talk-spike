package com.hoccer.xo.android.backup;

import com.google.gson.Gson;
import com.hoccer.talk.crypto.CryptoJSON;
import com.hoccer.xo.android.XoApplication;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
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
    public static final String BACKUP_FILENAME_PATTERN = BACKUP_FILENAME_PREFIX + "%s";
    public static final String DB_CONTENT_TYPE = "database";
    public static final String DB_FILENAME_ENCRYPTED = "database.json";
    public static final String METADATA_FILENAME = "metadata.json";

    public void createBackup(File out, File database, List<File> attachments, String clientName, String password) throws Exception {

        byte[] encryptedDatabase = encryptFile(database, password);

        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, clientName, new Date());

        Gson gson = new Gson();
        String metadataJson = gson.toJson(metadata);

        createZip(out, encryptedDatabase, attachments, metadataJson);
    }

    public void createBackup(File out, File database, String clientName, String password) throws Exception {

        byte[] encryptedDatabase = encryptFile(database, password);

        BackupMetadata metadata = new BackupMetadata(BackupType.DATABASE, clientName, new Date());

        Gson gson = new Gson();
        String metadataJson = gson.toJson(metadata);

        createZip(out, encryptedDatabase, metadataJson);
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

        return CryptoJSON.encrypt(bytes, password, DB_CONTENT_TYPE);
    }

    private void createZip(File backup, byte[] encryptedDatabase, String metadata) throws IOException {

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            addZipEntry(zos, encryptedDatabase, DB_FILENAME_ENCRYPTED);
            addMetaDataEntry(zos, metadata);
        } finally {
            zos.close();
        }
    }

    private void createZip(File backup, byte[] encryptedDatabase, List<File> attachments, String metadata) throws IOException {

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            for (File attachment : attachments) {
                addZipEntry(zos, attachment);
            }
            addZipEntry(zos, encryptedDatabase, DB_FILENAME_ENCRYPTED);
            addMetaDataEntry(zos, metadata);
        } finally {
            zos.close();
        }
    }

    private void addMetaDataEntry(ZipOutputStream zos, String metadata) throws IOException {

        InputStream in = new ByteArrayInputStream(metadata.getBytes("UTF-8"));

        ZipEntry entry = new ZipEntry(METADATA_FILENAME);
        zos.putNextEntry(entry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        in.close();
        zos.closeEntry();
    }

    private void addZipEntry(ZipOutputStream zos, File fileEntry) throws IOException {

        InputStream in = new FileInputStream(fileEntry);

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

        InputStream in = new ByteArrayInputStream(data);
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

    public BackupMetadata readMetadata(File backupFile) throws IOException {

        String result = null;

        ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {

            if (zipEntry.getName().equals(METADATA_FILENAME)) {
                byte[] bytes = readFileEntry(zis);
                // convert bytes to json string and parse TODO
                result = new String(bytes, "UTF-8");
                break;
            }
        }
        zis.close();

        Gson gson = new Gson();
        return gson.fromJson(result, BackupMetadata.class);
    }

    public void extractAndDecryptDatabase(File backupFile, File target, String password) throws Exception {

        ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile));
        try {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {

                if (zipEntry.getName().equals(DB_FILENAME_ENCRYPTED)) {
                    byte[] encrypted = readFileEntry(zis);
                    byte[] decrypted = CryptoJSON.decrypt(encrypted, password, DB_CONTENT_TYPE);
                    writeBytesToFile(target, decrypted);

                    break;
                }
            }
        } finally {
            zis.close();
        }
    }

    public List<File> getBackupFiles(File parentDir) {

        List<File> results = new ArrayList<File>();

        if (parentDir != null && parentDir.isDirectory()) {

            File[] files = parentDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String filename = file.getName();
                    String extension = FilenameUtils.getExtension(filename);
                    if (filename.startsWith(BACKUP_FILENAME_PREFIX) && extension.equals(FILE_EXTENSION_ZIP)) {
                        results.add(file);
                    }
                }
            }
        }
        return results;
    }

    private void writeBytesToFile(File databaseTarget, byte[] decrypted) throws IOException {
        FileOutputStream fos = new FileOutputStream(databaseTarget);
        fos.write(decrypted);
        fos.close();
    }

    private byte[] readFileEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zis.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        byte[] bytes = out.toByteArray();

        return bytes;
    }

    public static String createUniqueBackupFilename() {
        String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        return String.format(BACKUP_FILENAME_PATTERN, timestamp);
    }

    public static void importBackup(Backup backup, File databasePath, File attachmentDirectory) {

    }
}
