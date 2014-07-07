package com.hoccer.xo.android.util;

import android.content.Context;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XoImportExportUtils {

    private static final String EXPORT_DIRECTORY = "export";
    public static String DB_FILEPATH = "/data/data/com.hoccer.xo.release/databases/hoccer.db";

    private static final Logger LOG = Logger.getLogger(XoImportExportUtils.class);

    private Context context;

    private static XoImportExportUtils INSTANCE = null;

    private XoImportExportUtils(Context context) {
        this.context = context;
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

    private File zipAttachmentsAndDatabaseExport(File databaseExportFile) throws IOException {
        File exportFile = createExportFile("zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exportFile));
        zos.setLevel(ZipOutputStream.STORED);
        File attachmentDirectory = XoApplication.getAttachmentDirectory();
        try {
            for (File fileEntry : attachmentDirectory.listFiles()) {
                addZipEntry(zos, fileEntry);
            }
            addZipEntry(zos, databaseExportFile);
            databaseExportFile.delete();
        } finally {
            zos.close();
        }

        return exportFile;
    }

    private File exportDatabaseToFile() {
        File exportFile = createExportFile("db");

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
        File directory = context.getExternalFilesDir(XoImportExportUtils.EXPORT_DIRECTORY);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = String.format("hoccer_talk_export_%s.%s", timestamp, extension);
        return new File(directory, fileName);
    }
}
