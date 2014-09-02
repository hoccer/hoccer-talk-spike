package com.hoccer.xo.android.util;


import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ImageContentHelper {

    private static Logger LOG = Logger.getLogger(ImageContentHelper.class);

    public static int retrieveOrientation(Context context, Uri contentUri, String filePath) {
        String[] columns = {
                MediaStore.Images.Media.ORIENTATION
        };

        // Try with content database first
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(contentUri, columns, null, null, null);
        } catch (Exception e) {
            LOG.error("Exception while retrieving image orientation " + contentUri);
        }
        if (cursor != null) {
            cursor.moveToFirst();
            int orientationIndex = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
            return cursor.getInt(orientationIndex);

        } else {

            // Try exif data instead
            int degree = 0;
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(filePath);
            } catch (IOException ex) {
                LOG.error("IOException while retrieving image orientation " + filePath);
            }
            if (exif != null) {
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                if (orientation != -1) {
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            degree = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            degree = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            degree = 270;
                            break;
                    }
                }
            }
            return degree;
        }
    }

    public static double calculateAspectRatio(int fileWidth, int fileHeight, int orientation) {
        double aspectRatio;
        if (orientation == 0 || orientation == 180) {
            aspectRatio = (double) fileWidth / (double) fileHeight;
        } else {
            aspectRatio = (double) fileHeight / (double) fileWidth;
        }
        return aspectRatio;
    }
}
