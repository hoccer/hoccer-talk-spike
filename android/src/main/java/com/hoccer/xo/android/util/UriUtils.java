package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class UriUtils {

    public static final String CONTENT_URI_PREFIX = "content://";
    public static final String FILE_URI_PREFIX = "file://";

    public static boolean isContentUri(String uri) {
        return uri.startsWith(CONTENT_URI_PREFIX);
    }

    public static boolean isFileUri(String uri) {
        return uri.startsWith(FILE_URI_PREFIX);
    }

    public static String getFilePathByContentUri(Context context, Uri contentUri) throws CursorNotFoundException {

        String[] projection = {
                MediaStore.Images.Media.DATA,
        };

        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();

            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            String filePath = cursor.getString(dataIndex);

            return filePath;
        } else {
            throw new CursorNotFoundException("Couldn't find cursor for content uri: " + contentUri);
        }
    }

    public static class CursorNotFoundException extends Throwable {
        public CursorNotFoundException(String message) {
            super(message);
        }
    }
}
