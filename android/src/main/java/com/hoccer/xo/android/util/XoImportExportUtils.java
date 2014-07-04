package com.hoccer.xo.android.util;

import android.os.Environment;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XoImportExportUtils {

    public static String DB_FILEPATH = "/data/data/com.hoccer.xo.release/databases/hoccer.db";

    private static final Logger LOG = Logger.getLogger(XoImportExportUtils.class);

    private File exportFile;

    public static void exportData() throws IOException {
        File databaseExport = exportDatabase();
        zipAttachmentsAndDatabaseExport(databaseExport);
    }

    public static void importDatabase(File importFile) {
    }

    private static File exportDatabase() {

        File filesExternalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String now = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = String.format("hoccer_talk_export_%s.db", now);
        File exportFile = new File(filesExternalDir, fileName);

        String inFileName = DB_FILEPATH;
        File dbFile = new File(inFileName);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(dbFile);
            //Open the empty db as the output stream
            OutputStream output = new FileOutputStream(exportFile.getAbsolutePath());
            //transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            //Close the streams
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

    private static void zipAttachmentsAndDatabaseExport(File databaseExportFile) throws IOException {

        File filesExternalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String now = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String exportFileName = String.format("hoccer_talk_export_%s.zip", now);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(filesExternalDir, exportFileName)));
        zos.setLevel(ZipOutputStream.STORED);

        File attachmentDirectory = XoApplication.getAttachmentDirectory();
        try {
            for (File fileEntry : attachmentDirectory.listFiles()) {
                addZipEntry(zos, fileEntry);
            }
            addZipEntry(zos, databaseExportFile);
        } finally {
            zos.close();
        }

    }

    private static void addZipEntry(ZipOutputStream zos, File fileEntry) throws IOException {
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
}
