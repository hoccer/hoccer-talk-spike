package com.hoccer.xo.android.backup;

import com.google.gson.Gson;
import com.hoccer.talk.crypto.CryptoJSON;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupFileUtils {

    public static final String FILE_EXTENSION_ZIP = "zip";
    public static final String FILE_EXTENSION_DB = "db";
    public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
    public static final String BACKUP_FILENAME_PREFIX = "hoccer_backup_";
    public static final String BACKUP_FILENAME_PATTERN = BACKUP_FILENAME_PREFIX + "%s";
    public static final String DB_CONTENT_TYPE = "database";
    public static final String DB_FILENAME_ENCRYPTED = "database.json";
    public static final String METADATA_FILENAME = "metadata.json";

    private static final Logger LOG = Logger.getLogger(BackupFileUtils.class.getName());
    public static final String TEMP_ATTACHMENTS_DIR_NAME = "tmp_attachments";
    public static final String TEMP_DB_DIR_NAME = "tmp_db";

    private static FileFilter mBackupFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            if (file.getName().equals(CredentialsBackup.BACKUP_FILENAME)) {
                return true;
            } else if (FileFilterUtils.suffixFileFilter("zip").accept(file)) {
                try {
                    BackupMetadata metadata = BackupFileUtils.readMetadata(file);
                    if (metadata.getBackupType() != null) {
                        return true;
                    }
                } catch (IOException e) {
                    LOG.error("Reading metadata file of " + file + " failed");
                }
            }
            return false;
        }
    };

    public static void createBackupFile(File out, File database, List<File> attachments, BackupMetadata metadata, String password) throws Exception {

        byte[] encryptedDatabase = encryptFile(database, password);

        Gson gson = new Gson();
        String metadataJson = gson.toJson(metadata);

        createZip(out, encryptedDatabase, attachments, metadataJson);
    }

    public static void createBackupFile(File out, File database, BackupMetadata metadata, String password) throws Exception {

        byte[] encryptedDatabase = encryptFile(database, password);

        Gson gson = new Gson();
        String metadataJson = gson.toJson(metadata);

        createZip(out, encryptedDatabase, metadataJson);
    }

    private static byte[] encryptFile(File input, String password) throws Exception {

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

    private static void createZip(File backup, byte[] encryptedDatabase, String metadata) throws IOException {

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            addZipEntry(zos, encryptedDatabase, DB_FILENAME_ENCRYPTED);
            addMetaDataEntry(zos, metadata);
        } finally {
            zos.close();
        }
    }

    private static void createZip(File backup, byte[] encryptedDatabase, List<File> attachments, String metadata) throws IOException {

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

    private static void addMetaDataEntry(ZipOutputStream zos, String metadata) throws IOException {

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

    private static void addZipEntry(ZipOutputStream zos, File fileEntry) throws IOException {

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

    private static void addZipEntry(ZipOutputStream zos, byte[] data, String dataName) throws IOException {

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

    public static BackupMetadata readMetadata(File backupFile) throws IOException {

        String result = null;

        ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {

            if (zipEntry.getName().equals(METADATA_FILENAME)) {
                byte[] bytes = readFileEntry(zis);
                result = new String(bytes, "UTF-8");
                break;
            }
        }
        zis.close();

        Gson gson = new Gson();
        return gson.fromJson(result, BackupMetadata.class);
    }

    public static List<File> getBackupFiles(File parentDir) {

        List<File> results = new ArrayList<File>();

        if (parentDir != null && parentDir.isDirectory()) {
            File[] files = parentDir.listFiles(mBackupFileFilter);
            if (files != null) {
                Collections.addAll(results, files);
            }
        }
        return results;
    }

    public static void writeBytesToFile(File targetFile, byte[] decryptedData) throws IOException {
        if (!targetFile.exists()) {
            targetFile.getParentFile().mkdirs();
            targetFile.createNewFile();
        }
        FileOutputStream ostream = new FileOutputStream(targetFile);
        ostream.write(decryptedData);
//        ostream.getFD().sync();  TODO: throws exception when disk is full, activate if necessary - how to test?
        ostream.close();
    }

    public static byte[] readFileEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zis.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }

        return out.toByteArray();
    }

    public static String createUniqueBackupFilename() {
        String timestamp = getTimestamp();
        return String.format(BACKUP_FILENAME_PATTERN, timestamp);
    }

    public static void restoreBackup(File backupFile, File databaseTarget, String password) throws Exception {
        extractAndDecryptDatabase(backupFile, databaseTarget, password);
    }

    public static String getTimestamp() {
        return new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
    }

    public static void extractAndDecryptDatabase(File backupFile, File target, String password) throws Exception {

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

    public static long getUncompressedSize(File zipFile) throws IOException {

        long uncompressedSize = 0;
        try {
            ZipFile zf = new ZipFile(zipFile);
            Enumeration e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                uncompressedSize = uncompressedSize + ze.getSize();
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return uncompressedSize;
    }

    public static void extractAttachmentFiles(File backupFile, File targetDir) throws IOException {

        ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile));
        try {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(BackupFileUtils.DB_FILENAME_ENCRYPTED) && !zipEntry.getName().equals(BackupFileUtils.METADATA_FILENAME)) {
                    byte[] bytes = BackupFileUtils.readFileEntry(zis);
                    File file = new File(targetDir, zipEntry.getName());
                    BackupFileUtils.writeBytesToFile(file, bytes);
                }
            }
        } finally {
            zis.close();
        }
    }
}
