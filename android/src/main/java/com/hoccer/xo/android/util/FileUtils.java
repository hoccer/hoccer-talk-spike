package com.hoccer.xo.android.util;

import java.io.*;

public class FileUtils {

    public static void readInputStream(InputStream is, File file) throws IOException {
        if (is != null) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                int read;
                byte[] bytes = new byte[1024];
                while ((read = is.read(bytes)) != -1) {
                    os.write(bytes, 0, read);
                }
            } finally {
                is.close();
                if (os != null) {
                    os.close();
                }
            }
        }
    }
}
