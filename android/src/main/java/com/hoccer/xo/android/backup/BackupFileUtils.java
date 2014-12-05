package com.hoccer.xo.android.backup;

import android.os.Environment;
import android.os.StatFs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.crypto.CryptoJSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
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

    public static final FileFilter IS_FILE_AND_NOT_HIDDEN_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isFile() && !file.isHidden();
        }
    };

    public static void createBackupFile(File out, BackupMetadata metadata, File database, String password) throws Exception {
        createBackupFile(out, metadata, database, password, Collections.<File>emptyList());
    }

    public static void createBackupFile(File out, BackupMetadata metadata, File database, String password, List<File> attachments) throws Exception {
        byte[] encryptedDatabase = readDataFromFileEncrypted(database, password);

        ObjectMapper mapper = new ObjectMapper();
        String metadataJson = mapper.writeValueAsString(metadata);

        try {
            createZip(out, metadataJson, encryptedDatabase, attachments);
        } catch (IOException e) {
            try {
                FileUtils.forceDelete(out);
            } catch (IOException e1) {
                LOG.info("Could not cleanup. Failed to delete " + out.getAbsolutePath() + ":" + e1.getMessage());
            }
            throw e;
        }
    }

    private static byte[] readDataFromFileEncrypted(File input, String password) throws Exception {
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

    public static BackupMetadata extractMetadata(File backupFile) throws IOException {
        ZipFile zipFile = new ZipFile(backupFile);
        ZipEntry entry = zipFile.getEntry(METADATA_FILENAME);
        if (entry == null) {
            throw new FileNotFoundException(METADATA_FILENAME + " not found in " + backupFile.getName());
        }

        InputStream is = zipFile.getInputStream(entry);
        ObjectMapper objectMapper = new ObjectMapper();
        BackupMetadata metadata = objectMapper.readValue(is, BackupMetadata.class);
        is.close();

        return metadata;
    }

    public static void extractAndDecryptDatabase(File backupFile, File target, String password) throws IOException, CryptoJSON.DecryptionException {
        ZipFile zipFile = new ZipFile(backupFile);
        ZipEntry entry = zipFile.getEntry(DB_FILENAME_ENCRYPTED);
        if (entry == null) {
            throw new FileNotFoundException(DB_FILENAME_ENCRYPTED + " not found in " + backupFile.getName());
        }

        InputStream is = zipFile.getInputStream(entry);
        writeDataToFileDecrypted(target, is, password);
        is.close();
    }

    private static void writeDataToFileDecrypted(File target, InputStream is, String password) throws IOException, CryptoJSON.DecryptionException {
        byte[] encrypted = IOUtils.toByteArray(is);
        byte[] decrypted = CryptoJSON.decrypt(encrypted, password, DB_CONTENT_TYPE);
        FileUtils.writeByteArrayToFile(target, decrypted);
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

    public static List<Backup> getBackups(File dir) {
        List<Backup> backups = new ArrayList<Backup>();

        File[] files = dir.listFiles(IS_FILE_AND_NOT_HIDDEN_FILTER);
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

    public static long getAvailableDiskStorage() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
    }

    public static boolean isEnoughDiskSpaceAvailable(File backupFile) throws IOException {
        long requiredDiskSpace = BackupFileUtils.getUncompressedSize(backupFile);
        long availableDiskSpace = BackupFileUtils.getAvailableDiskStorage();
        if (requiredDiskSpace < availableDiskSpace) {
            return true;
        }
        return false;
    }

    public static String getHumanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
