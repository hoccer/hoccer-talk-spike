package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.xo.android.XoApplication;

import java.io.File;

public class UriUtils {
    public static final String CONTENT_SCHEMA = "content";
    public static final String FILE_SCHEMA = "file";
    public static final String CONTENT_URI_PREFIX = CONTENT_SCHEMA + "://";
    public static final String FILE_URI_PREFIX = FILE_SCHEMA + "://";

    public static Uri getAbsoluteFileUri(String stringUri) {
        Uri uri = Uri.parse(stringUri);

        if (uri.getScheme() != null || uri.getPath().startsWith(File.separator)) {
            return uri;
        }

        return Uri.parse(FILE_URI_PREFIX + XoApplication.getExternalStorage() + File.separator + stringUri);
    }

    public static boolean isContentUri(Uri uri) {
        return CONTENT_SCHEMA.equals(uri.getScheme());
    }

    public static boolean isFileUri(Uri uri) {
        return FILE_SCHEMA.equals(uri.getScheme());
    }

    public static boolean doesContentFileExist(Context context, Uri contentUri) {
        if (contentUri != null) {
            String filePath = getFilePathByContentUri(context, contentUri);
            return filePath != null && new File(filePath).exists();
        }
        return false;
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
}
