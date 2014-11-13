package com.hoccer.xo.android.util;

import android.content.Context;
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

public class XoImportExportUtils {

    public static final String FILE_EXTENSION_ZIP = "zip";
    public static final String FILE_EXTENSION_DB = "db";
    public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
    public static final String HOCCER_EXPORT_FILENAME_PREFIX = "hoccer_export_";
    public static final String EXPORT_FILE_NAME_PATTERN = HOCCER_EXPORT_FILENAME_PREFIX + "%s.%s";

    private static XoImportExportUtils INSTANCE = null;

    private String databaseFilepath;

    private XoImportExportUtils(Context context) {
        databaseFilepath = "/data/data/" + context.getPackageName() + "/databases/hoccer-talk.db";
    }

    public static XoImportExportUtils getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new XoImportExportUtils(context);
        }
        return INSTANCE;
    }

    public File exportDatabaseAndAttachments() throws IOException {
        return zipAttachmentsAndDatabaseExport(exportDatabaseToFile());
    }

    public void importDatabaseAndAttachments(File importFile) throws IOException {
        extractAttachmentsAndDatabaseImport(importFile);
    }

    private void extractAttachmentsAndDatabaseImport(File importFile) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(importFile));
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
                    file = new File(databaseFilepath);
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

    private File zipAttachmentsAndDatabaseExport(File databaseExportFile) throws IOException {
        File exportFile = createExportFile("zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exportFile));
        zos.setLevel(ZipOutputStream.STORED);
        File attachmentDirectory = XoApplication.getAttachmentDirectory();
        try {
            for (File fileEntry : attachmentDirectory.listFiles()) {
                if (!fileEntry.isDirectory()) {
                    addZipEntry(zos, fileEntry);
                }
            }
            addZipEntry(zos, databaseExportFile);
            databaseExportFile.delete();
        } finally {
            zos.close();
        }

        return exportFile;
    }

    public File exportDatabaseToFile() {
        File exportFile = createExportFile(FILE_EXTENSION_DB);

        String inFileName = databaseFilepath;
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

    public File createExportFile(String extension) {
        String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        String fileName = String.format(EXPORT_FILE_NAME_PATTERN, timestamp, extension);
        return new File(XoApplication.getExternalStorage(), fileName);
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
                if (filename.startsWith(HOCCER_EXPORT_FILENAME_PREFIX) && extension.equals(FILE_EXTENSION_ZIP)) {
                    exportFiles.add(file);
                }
            }
        }

        return exportFiles;
    }
}
