package com.hoccer.xo.android.util;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static final String MIME_TYPE_IMAGE_PREFIX = "image/";

    private static Logger LOG = Logger.getLogger(ImageUtils.class);

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

    public static Point getImageSize(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        return new Point(options.outWidth, options.outHeight);
    }

    /*
     * Returns the calculated size within the given bounds.
     */
    public static Point getImageSizeInBounds(double aspectRatio, int maxWidth, int maxHeight) {
        Point result = new Point(maxWidth, (int) (maxWidth / aspectRatio));

        if (result.y > maxHeight) {
            result.x = (int) (maxHeight * aspectRatio);
            result.y = maxHeight;
        }
        return result;
    }

    public static Bitmap resizeImageToMaxPixelCount(File input, int maxPixelCount) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(input.getAbsolutePath(), options);

        long originalPixelCount = options.outWidth * options.outHeight;
        if (maxPixelCount < originalPixelCount) {
            double resizeRatio = Math.sqrt(originalPixelCount / maxPixelCount);
            options.inSampleSize = (int) resizeRatio + 1;
            options.outWidth = (int) (options.outWidth / resizeRatio);
            options.outHeight = (int) (options.outHeight / resizeRatio);
        }
        options.inJustDecodeBounds = false;

        Bitmap encodedBitmap;
        try {
            encodedBitmap = BitmapFactory.decodeFile(input.getAbsolutePath(), options);
        } catch (OutOfMemoryError error) {
            LOG.error(error.getMessage(), error);
            return null;
        }

        return encodedBitmap;
    }

    public static boolean compressBitmapToFile(Bitmap srcBitmap, File destFile, int imageQuality, Bitmap.CompressFormat format) {
        FileOutputStream os;
        try {
            os = new FileOutputStream(destFile);
            return srcBitmap.compress(format, imageQuality, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean copyExifData(String inPath, String outPath) {
        boolean success = false;
        try {
            ExifInterface exifIn = new ExifInterface(inPath);
            ExifInterface exifOut = new ExifInterface(outPath);
            if (exifIn != null && exifOut != null) {
                exifOut.setAttribute(ExifInterface.TAG_ORIENTATION, exifIn.getAttribute(ExifInterface.TAG_ORIENTATION));
                exifOut.saveAttributes();
            }
        } catch (IOException e) {
            LOG.error("Error loading Exif data", e);
        }

        return success;
    }
}
