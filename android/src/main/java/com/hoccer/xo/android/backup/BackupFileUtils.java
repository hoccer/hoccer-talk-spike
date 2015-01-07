package com.hoccer.xo.android.backup;

import android.os.Environment;
import android.os.StatFs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.crypto.CryptoJSON;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
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
        } catch (InterruptedException e) {
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

    private static void createZip(File zipFile, String metadata, byte[] encryptedDatabase, List<File> attachments) throws IOException, InterruptedException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        zos.setLevel(ZipOutputStream.DEFLATED);
        try {
            for (File attachment : attachments) {
                addZipEntry(zos, attachment);
            }
            addZipEntry(zos, DB_FILENAME_ENCRYPTED, encryptedDatabase);
            addZipEntry(zos, METADATA_FILENAME, metadata);
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    private static void addZipEntry(ZipOutputStream zos, File file) throws IOException, InterruptedException {
        InputStream in = new FileInputStream(file);
        addZipEntry(zos, file.getName(), in);
        in.close();
    }

    private static void addZipEntry(ZipOutputStream zos, String filename, byte[] data) throws IOException, InterruptedException {
        InputStream in = new ByteArrayInputStream(data);
        addZipEntry(zos, filename, in);
        in.close();
    }

    private static void addZipEntry(ZipOutputStream zos, String filename, String data) throws IOException, InterruptedException {
        InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));
        addZipEntry(zos, filename, in);
        in.close();
    }

    private static void addZipEntry(ZipOutputStream zos, String filename, InputStream data) throws IOException, InterruptedException {
        if (!Thread.interrupted()) {
            ZipEntry entry = new ZipEntry(filename);
            zos.putNextEntry(entry);
            IOUtils.copy(data, zos);
            zos.closeEntry();
        } else {
            throw new InterruptedException();
        }
    }

    public static BackupMetadata extractMetadata(File backupFile) throws IOException, ZipException {
        BackupMetadata metadata;

        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(backupFile);
        InputStream is = zipFile.getInputStream(zipFile.getFileHeader(METADATA_FILENAME));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            metadata = objectMapper.readValue(is, BackupMetadata.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
        is.close();

        return metadata;
    }

    public static void extractAndDecryptDatabase(File backupFile, File target, String password) throws IOException, CryptoJSON.DecryptionException, ZipException {
        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(backupFile);
        InputStream is = zipFile.getInputStream(zipFile.getFileHeader(DB_FILENAME_ENCRYPTED));
        writeDataToFileDecrypted(target, is, password);
        is.close();
    }

    private static void writeDataToFileDecrypted(File target, InputStream is, String password) throws IOException, CryptoJSON.DecryptionException {
        byte[] encrypted = IOUtils.toByteArray(is);
        byte[] decrypted = CryptoJSON.decrypt(encrypted, password, DB_CONTENT_TYPE);

        FileUtils.writeByteArrayToFile(target, decrypted);
    }

    public static void extractAttachmentFiles(File backupFile, File targetDir) throws IOException, InterruptedException, ZipException {
        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(backupFile);
        List<FileHeader> fileHeaderList = zipFile.getFileHeaders();
        for (FileHeader fileHeader : fileHeaderList) {
            if (fileHeader != null) {
                File file = new File(targetDir, fileHeader.getFileName());
                InputStream is = zipFile.getInputStream(fileHeader);
                try {
                    FileUtils.copyInputStreamToFile(is, file);
                } finally {
                    is.close();
                }
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
                } catch (BackupFactory.BackupTypeNotSupportedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    LOG.info("Ignoring non backup file: " + file.getAbsolutePath());
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
}
