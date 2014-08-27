package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class UriUtils {

    public static String getFilePathByContentUri(Context context, Uri contentUri) throws Exception {

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
            throw new Exception("Couldn't find cursor for content uri: " + contentUri);
        }
    }
}
