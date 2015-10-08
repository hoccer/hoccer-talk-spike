package com.hoccer.xo.android.util;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.File;

public class UriUtils {

    private static final Logger LOG = Logger.getLogger(UriUtils.class);

    public static final String CONTENT_SCHEME = "content";
    public static final String FILE_SCHEME = "file";

    public static final String CONTENT_URI_PREFIX = CONTENT_SCHEME + "://";
    public static final String FILE_URI_PREFIX = FILE_SCHEME + "://";
    public static final String PROVIDERS_DOWNLOADS_DOCUMENTS = "com.android.providers.downloads.documents";

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

    public static Uri getContentUriByDataPath(Context context, Uri tableUri, String dataPath) {
        long contentId = getContentIdByDataPath(context, tableUri, dataPath);
        if (contentId > 0) {
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

    public static String getFilePathByUri(Context context, Uri uri) {
        String filePath = null;

        if (isContentUri(uri)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (isDocumentUri(context, uri)) {
                    filePath = getFilePathByDocumentUri(uri, context);
                } else {
                    filePath = getFilePathByContentUri(uri, context);
                }
            } else {
                filePath = getFilePathByContentUri(uri, context);
            }
        } else if (isFileUri(uri)) {
            filePath = uri.getPath();
        }

        return filePath;
    }

    private static String getFilePathByContentUri(Uri uri, Context context) {
        String filePath = null;

        Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
        if (cursor == null) {
            LOG.error("Query failed! Could not resolve cursor for content uri: " + uri);
            return null;
        }

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(cursor.getColumnIndex("_data"));
            cursor.close();
        }

        return filePath;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean isDocumentUri(Context context, Uri uri) {
        return DocumentsContract.isDocumentUri(context, uri);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getFilePathByDocumentUri(Uri uri, Context context) {
        if (uri.getAuthority().equals("com.android.externalstorage.documents")) {
            String relativePath = uri.getLastPathSegment().split(":")[1];
            return XoApplication.getExternalStorage().getPath() + "/" + relativePath;
        }

        String documentId = DocumentsContract.getDocumentId(uri);
        String projection = "_data";
        Uri contentUri;
        String selection = null;
        String[] selectionArgs = null;
        if (uri.getAuthority().equals(PROVIDERS_DOWNLOADS_DOCUMENTS)) {
            contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
        } else {
            contentUri = getExternalMediaContentUri(documentId.split(":")[0]);
            selection = "_id=?";
            selectionArgs = new String[]{documentId.split(":")[1]};
        }

        return getFilePathByCursor(projection,
                context.getContentResolver().query(contentUri, new String[]{projection},
                        selection, selectionArgs, null));
    }

    private static Uri getExternalMediaContentUri(String type) {
        Uri contentUri = null;
        if ("image".equals(type)) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return contentUri;
    }

    private static String getFilePathByCursor(String columnName, Cursor cursor) {
        String path = "";
        if (cursor.moveToFirst()) {
            path = cursor.getString(cursor.getColumnIndex(columnName));
        }
        cursor.close();
        return path;
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;

        if (isContentUri(uri)) {
            mimeType = context.getContentResolver().getType(uri);
        } else if (isFileUri(uri)) {
            String extension = getFileExtension(uri);
            if (extension != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                mimeType = mime.getMimeTypeFromExtension(extension);
            }
        }

        return mimeType;
    }

    public static String getFileExtension(Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
        if ("".equals(extension)) {
            int i = uri.getPath().lastIndexOf('.');
            if (i > 0) {
                extension = uri.getPath().substring(i + 1);
            }
        }
        return extension;
    }
}
