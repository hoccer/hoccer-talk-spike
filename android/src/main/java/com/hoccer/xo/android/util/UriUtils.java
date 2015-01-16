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
    public static final String HTTP_SCHEMA = "http";
    public static final String HTTPS_SCHEMA = "https";
    public static final String CONTENT_URI_PREFIX = CONTENT_SCHEMA + "://";
    public static final String FILE_URI_PREFIX = FILE_SCHEMA + "://";

    public static Uri getAbsoluteFileUri(String stringUri) {
        Uri uri = Uri.parse(stringUri);

        if (uri.getScheme() != null) {
            return uri;
        }

        if (uri.getPath().startsWith(File.separator)) {
            return Uri.parse(FILE_URI_PREFIX + File.separator + stringUri);
        } else {
            return Uri.parse(FILE_URI_PREFIX + XoApplication.getExternalStorage() + File.separator + stringUri);
        }
    }

    public static boolean isContentUri(Uri uri) {
        return CONTENT_SCHEMA.equals(uri.getScheme());
    }

    public static boolean isFileUri(Uri uri) {
        return FILE_SCHEMA.equals(uri.getScheme()) || uri.toString().startsWith("/");
    }

    public static boolean isRemoteUri(Uri uri) {
        return HTTP_SCHEMA.equals(uri.getScheme()) || HTTPS_SCHEMA.equals(uri.getScheme());
    }

    public static boolean contentExists(Context context, Uri contentUri) {
        Uri dataUri = getDataUriByContentUri(context, contentUri);
        if (isFileUri(dataUri)) {
            return new File(dataUri.getPath()).exists();
        } else if (isRemoteUri(dataUri)) {
            return true;
        }

        return false;
    }

    public static Uri getFileUriByContentUri(Context context, Uri contentUri) {
        Uri dataUri = getDataUriByContentUri(context, contentUri);
        if (dataUri != null && isFileUri(dataUri)) {
            return dataUri;
        } else {
            return null;
        }
    }

    public static Uri getDataUriByContentUri(Context context, Uri contentUri) {
        String[] projection = {
                MediaStore.Images.Media.DATA,
        };

        try {
            Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                return Uri.parse(cursor.getString(dataIndex));
            }
        } catch (Exception ignored) {}

        return null;
    }
}
