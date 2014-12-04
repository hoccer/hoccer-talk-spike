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

    public static void createBackupFile(File out, BackupMetadata metadata, File database, String password) throws Exception {
        createBackupFile(out, metadata, database, password, Collections.<File>emptyList());
    }

    public static void createBackupFile(File out, BackupMetadata metadata, File database, String password, List<File> attachments) throws Exception {
        byte[] encryptedDatabase = encryptFile(database, password);

        ObjectMapper mapper = new ObjectMapper();
        String metadataJson = mapper.writeValueAsString(metadata);

        createZip(out, metadataJson, encryptedDatabase, attachments);
    }

    private static byte[] encryptFile(File input, String password) throws Exception {
        FileInputStream in = new FileInputStream(input);
        byte[] bytes = IOUtils.toByteArray(in);
        in.close();
        return CryptoJSON.encrypt(bytes, password, DB_CONTENT_TYPE);
    }

    private static void createZip(File zipFile, String metadata, byte[] encryptedDatabase, List<File> attachments) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            for (File attachment : attachments) {
                addZipEntry(zos, attachment);
            }
            addZipEntry(zos, DB_FILENAME_ENCRYPTED, encryptedDatabase);
            addZipEntry(zos, METADATA_FILENAME, metadata);
        } finally {
            zos.close();
        }
    }

    private static void addZipEntry(ZipOutputStream zos, File file) throws IOException {
        InputStream in = new FileInputStream(file);
        addZipEntry(zos, file.getName(), in);
        in.close();
    }

    private static void addZipEntry(ZipOutputStream zos, String filename, byte[] data) throws IOException {
        InputStream in = new ByteArrayInputStream(data);
        addZipEntry(zos, filename, in);
        in.close();
    }

    private static void addZipEntry(ZipOutputStream zos, String filename, String data) throws IOException {
        InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));
        addZipEntry(zos, filename, in);
        in.close();
    }

    private static void addZipEntry(ZipOutputStream zos, String filename, InputStream data) throws IOException {
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

    public static List<Backup> getBackups(File dir) {
        List<Backup> backups = new ArrayList<Backup>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Backup backup = BackupFactory.readBackup(file);
                    backups.add(backup);
                } catch (IOException e) {
                    LOG.info("Ignoring non backup file: " + file.getAbsolutePath());
                } catch (BackupFactory.BackupTypeNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        return backups;
    }

    public static String createUniqueBackupFilename() {
        String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        return String.format(BACKUP_FILENAME_PATTERN, timestamp);
    }

    public static void restoreBackup(File backupFile, File databaseTarget, String password) throws Exception {
        extractAndDecryptDatabase(backupFile, databaseTarget, password);
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
