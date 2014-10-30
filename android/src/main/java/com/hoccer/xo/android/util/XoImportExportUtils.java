package com.hoccer.xo.android.util;

import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class XoImportExportUtils {

    public static final String EXPORT_DIRECTORY = "export";
    public static String DB_FILEPATH = "/data/data/com.hoccer.xo.release/databases/hoccer-talk.db";

    private static final Logger LOG = Logger.getLogger(XoImportExportUtils.class);

    private static XoImportExportUtils INSTANCE = null;

    private XoImportExportUtils() {
    }

    public static XoImportExportUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new XoImportExportUtils();
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
                    file = new File(DB_FILEPATH);
                    if (file.exists()){
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
                if (!fileEntry.isDirectory() && !fileEntry.getName().equals(EXPORT_DIRECTORY)) {
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
        File exportFile = createExportFile("db");

        String inFileName = DB_FILEPATH;
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
        File directory = new File(XoApplication.getAttachmentDirectory(), XoImportExportUtils.EXPORT_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = String.format("hoccer_talk_export_%s.%s", timestamp, extension);
        return new File(directory, fileName);
    }

    private void initDatabase() {
        try {
            XoApplication.getXoClient().getDatabase().initialize();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
