package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import com.hoccer.xo.android.XoApplication;

import java.io.File;

public class UriUtils {
    public static final String CONTENT_SCHEME = "content";
    public static final String FILE_SCHEME = "file";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";
    public static final String CONTENT_URI_PREFIX = CONTENT_SCHEME + "://";
    public static final String FILE_URI_PREFIX = FILE_SCHEME + "://";

    public static Uri getAbsoluteFileUri(String stringUri) {
        Uri uri = Uri.parse(stringUri);

        if (uri.getScheme() != null) {
            return uri;
        }

        if (uri.getPath().startsWith(File.separator)) {
            return Uri.parse(FILE_URI_PREFIX + stringUri);
        } else {
            return Uri.parse(FILE_URI_PREFIX + XoApplication.getExternalStorage() + File.separator + stringUri);
        }
    }

    public static boolean isContentUri(Uri uri) {
        return CONTENT_SCHEME.equals(uri.getScheme());
    }

    public static boolean isFileUri(Uri uri) {
        return FILE_SCHEME.equals(uri.getScheme()) || uri.toString().startsWith("/");
    }

    public static boolean isRemoteUri(Uri uri) {
        return HTTP_SCHEME.equals(uri.getScheme()) || HTTPS_SCHEME.equals(uri.getScheme());
    }

    public static Uri getContentUriByDataPath(Context context, Uri tableUri, String dataPath) {
        long contentId = getContentIdByDataPath(context, tableUri, dataPath);
        if(contentId > 0) {
            return Uri.parse(tableUri + File.separator + contentId);
        }

        return null;
    }

    public static long getContentIdByDataPath(Context context, Uri tableUri, String dataPath) {
        String[] projection = {
                BaseColumns._ID
        };
        Cursor cursor = context.getContentResolver().query(tableUri, projection, MediaStore.MediaColumns.DATA + " LIKE ?", new String[]{dataPath}, null);

        long contentId = -1;
        if (cursor.moveToFirst()) {
            contentId = cursor.getLong(0);
        }
        cursor.close();

        return contentId;
    }

    public static boolean contentExists(Context context, Uri contentUri) {
        Uri dataUri = getDataUriByContentUri(context, contentUri);
        if (dataUri != null) {
            if (isFileUri(dataUri)) {
                return new File(dataUri.getPath()).exists();
            } else if (isRemoteUri(dataUri)) {
                return true;
            }
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

    private static Uri getDataUriByContentUri(Context context, Uri contentUri) {
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
