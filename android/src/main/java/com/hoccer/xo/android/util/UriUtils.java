package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.xo.android.XoApplication;

import java.io.File;

public class UriUtils {

    public static final String CONTENT_URI_PREFIX = "content://";
    public static final String FILE_URI_PREFIX = "file://";

    public static Uri getAbsoluteFileUri(String stringUri) {
        Uri uri = Uri.parse(stringUri);
        if (isContentUri(uri) || isFileUri(uri)) {
            return uri;
        }
        return Uri.parse(FILE_URI_PREFIX + XoApplication.getExternalStorage() + File.separator + stringUri);
    }

    public static boolean isContentUri(Uri uri) {
        return CONTENT_URI_PREFIX.equals(uri.getScheme());
    }

    public static boolean isFileUri(Uri uri) {
        return FILE_URI_PREFIX.equals(uri.getScheme());
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
