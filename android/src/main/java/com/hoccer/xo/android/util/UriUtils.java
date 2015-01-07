package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import org.apache.log4j.Logger;

import java.io.File;

public class UriUtils {

    private static final Logger LOG = Logger.getLogger(UriUtils.class);

    public static final String CONTENT_URI_PREFIX = "content://";
    public static final String FILE_URI_PREFIX = "file://";

    public static boolean isContentUri(String uri) {
        return uri.startsWith(CONTENT_URI_PREFIX);
    }

    public static boolean isFileUri(String uri) {
        return uri.startsWith(FILE_URI_PREFIX);
    }

    public static String getFilePathByContentUri(Context context, Uri contentUri) {
        String[] projection = {
                MediaStore.Images.Media.DATA,
        };

        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            return cursor.getString(dataIndex);
        }

        return null;
    }

    public static boolean isExistingContentUri(Context context, String contentUrl) {
        if (contentUrl != null && !contentUrl.isEmpty()) {
            String filePath = getFilePathByContentUri(context, Uri.parse(contentUrl));
            return filePath != null && new File(filePath).exists();
        }

        return false;
    }
}
