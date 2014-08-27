package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class UriUtils {

    public static String getFilePathByContentUri(Context context, Uri contentUri) {

        String[] projection = {
                MediaStore.Images.Media.DATA,
        };

        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        cursor.moveToFirst();

        int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        String filePath = cursor.getString(dataIndex);

        return filePath;
    }
}
