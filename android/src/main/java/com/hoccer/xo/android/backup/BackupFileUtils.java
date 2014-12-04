package com.hoccer.xo.android.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.crypto.CryptoJSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

    public static void createBackupFile(File out, File database, List<File> attachments, BackupMetadata metadata, String password) throws Exception {
        byte[] encryptedDatabase = encryptFile(database, password);

        ObjectMapper mapper = new ObjectMapper();
        String metadataJson = mapper.writeValueAsString(metadata);

        createZip(out, encryptedDatabase, metadataJson, attachments);
    }

    public static void createBackupFile(File out, File database, BackupMetadata metadata, String password) throws Exception {
        byte[] encryptedDatabase = encryptFile(database, password);

        ObjectMapper mapper = new ObjectMapper();
        String metadataJson = mapper.writeValueAsString(metadata);

        createZip(out, encryptedDatabase, metadataJson);
    }

    private static byte[] encryptFile(File input, String password) throws Exception {
        FileInputStream in = new FileInputStream(input);
        byte[] bytes = IOUtils.toByteArray(in);
        in.close();
        return CryptoJSON.encrypt(bytes, password, DB_CONTENT_TYPE);
    }

    private static void createZip(File backup, byte[] encryptedDatabase, String metadata) throws IOException {
        createZip(backup, encryptedDatabase, metadata, Collections.<File>emptyList());
    }

    private static void createZip(File backup, byte[] encryptedDatabase, String metadata, List<File> attachments) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            for (File attachment : attachments) {
                addZipEntry(attachment, zos);
            }
            addZipEntry(DB_FILENAME_ENCRYPTED, encryptedDatabase, zos);
            addZipEntry(METADATA_FILENAME, metadata, zos);
        } finally {
            zos.close();
        }
    }

    private static void addZipEntry(String filename, String data, ZipOutputStream zos) throws IOException {
        InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));
        addZipEntry(filename, in, zos);
        in.close();
    }

    private static void addZipEntry(File file, ZipOutputStream zos) throws IOException {
        InputStream in = new FileInputStream(file);
        addZipEntry(file.getName(), in, zos);
        in.close();
    }

    private static void addZipEntry(String filename, byte[] data, ZipOutputStream zos) throws IOException {
        InputStream in = new ByteArrayInputStream(data);
        addZipEntry(filename, in, zos);
        in.close();
    }

    private static void addZipEntry(String filename, InputStream data, ZipOutputStream zos) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        IOUtils.copy(data, zos);
        zos.closeEntry();
    }

    public static BackupMetadata readMetadata(File backupFile) throws IOException {
        String result = null;

        ZipFile zipFile = new ZipFile(backupFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().equals(METADATA_FILENAME)) {
                InputStream is = zipFile.getInputStream(entry);
                byte[] bytes = IOUtils.toByteArray(is);
                is.close();
                result = new String(bytes, "UTF-8");
                break;
            }
        }

        if (result == null) {
            throw new FileNotFoundException(BackupFileUtils.METADATA_FILENAME + " not found in " + backupFile.getName());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(result, BackupMetadata.class);
    }

    public static Map<File, BackupMetadata> getBackupFiles(File dir) {
        Map<File, BackupMetadata> backupfiles = new HashMap<File, BackupMetadata>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    BackupMetadata metadata = readMetadata(file);
                    if (metadata != null) {
                        backupfiles.put(file, metadata);
                    }
                } catch (IOException e) {
                    LOG.info("Ignoring non backup file: " + file.getAbsolutePath());
                }
            }
        }
        return backupfiles;
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
        ZipFile zipFile = new ZipFile(backupFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().equals(DB_FILENAME_ENCRYPTED)) {

                InputStream is = zipFile.getInputStream(entry);
                byte[] encrypted = IOUtils.toByteArray(is);
                is.close();

                byte[] decrypted = CryptoJSON.decrypt(encrypted, password, DB_CONTENT_TYPE);
                FileUtils.writeByteArrayToFile(target, decrypted);

                break;
            }
        }
    }

    public static long getUncompressedSize(File zipFile) throws IOException {
        long uncompressedSize = 0;
        ZipFile zf = new ZipFile(zipFile);
        Enumeration entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) entries.nextElement();
            uncompressedSize = uncompressedSize + ze.getSize();
        }
        return uncompressedSize;
    }

    public static void extractAttachmentFiles(File backupFile, File targetDir) throws IOException {

        ZipFile zipFile = new ZipFile(backupFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.getName().equals(DB_FILENAME_ENCRYPTED) && !entry.getName().equals(METADATA_FILENAME)) {
                File file = new File(targetDir, entry.getName());
                InputStream is = zipFile.getInputStream(entry);
                FileUtils.copyInputStreamToFile(is, file);
                is.close();
            }
        }
    }
}
