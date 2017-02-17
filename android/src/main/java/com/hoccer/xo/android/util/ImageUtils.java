package com.hoccer.xo.android.util;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ImageUtils {

    private static final Logger LOG = Logger.getLogger(ImageUtils.class);

    public static class ExifData {
        public int orientation;
        public long dateTime;
        public double latitude;
        public double longitude;
    }

    public static ExifData getExifData(String filePath) {
        ExifData result = new ExifData();

        try {
            ExifInterface exif = new ExifInterface(filePath);
            result.orientation = getOrientationInDegree(exif);

            String dateTimeString = exif.getAttribute(ExifInterface.TAG_DATETIME);
            result.dateTime = dateTimeString == null ? 0 : new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(dateTimeString).getTime();

            float latLong[] = new float[2];
            exif.getLatLong(latLong);
            result.latitude = latLong[0];
            result.longitude = latLong[1];
        } catch (IOException e) {
            LOG.error("IOException while reading image exif data " + filePath, e);
        } catch (ParseException e) {
            LOG.error("ParseException while reading image exif data " + filePath, e);
        }

        return result;
    }

    public static Bitmap correctRotationAndResize(String filePath, int outWidth, int outHeight) {
        int sourceRotationAngle = retrieveOrientation(filePath);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

        Matrix matrix = new Matrix();

        if (bitmap.getWidth() > bitmap.getHeight()){
            int targetAngle = 0;
            switch (sourceRotationAngle){
                case 0:
                case 90: targetAngle = 90;
                    break;
                case 180:
                case 270: targetAngle = 270;
            }
            matrix.setRotate(targetAngle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, outWidth, outHeight, matrix, true);

        try {
            ExifInterface exif = new ExifInterface(filePath);
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_NORMAL));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static int retrieveOrientation(String filePath) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException ex) {
            LOG.error("IOException while retrieving image orientation " + filePath);
            return 0;
        }

        return getOrientationInDegree(exif);
    }

    private static int getOrientationInDegree(ExifInterface exif) {
        int orientationId = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
        switch (orientationId) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
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

    public static int calculateInSampleSize(int originalWidth, int origianlHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (origianlHeight > reqHeight || originalWidth > reqWidth) {

            final int halfHeight = origianlHeight / 2;
            final int halfWidth = originalWidth / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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

    public static void copyExifData(String inPath, String outPath) throws IOException {
        ExifInterface exifIn = new ExifInterface(inPath);
        applyExifData(exifIn, outPath);
    }

    public static void applyExifData(ExifInterface exifInterface, String targetPath) throws IOException {
        ExifInterface exifOut = new ExifInterface(targetPath);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_APERTURE);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_DATETIME);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_EXPOSURE_TIME);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_FLASH);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_FOCAL_LENGTH);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_ALTITUDE);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_ALTITUDE_REF);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_DATESTAMP);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_LATITUDE);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_LATITUDE_REF);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_LONGITUDE);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_LONGITUDE_REF);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_PROCESSING_METHOD);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_GPS_TIMESTAMP);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_IMAGE_LENGTH);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_IMAGE_WIDTH);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_ISO);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_MAKE);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_MODEL);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_ORIENTATION);
        copyExifAttribute(exifInterface, exifOut, ExifInterface.TAG_WHITE_BALANCE);
        exifOut.saveAttributes();
    }

    private static void copyExifAttribute(ExifInterface in, ExifInterface out, String tag) {
        String value = in.getAttribute(tag);
        if (value != null && !value.isEmpty()) {
            out.setAttribute(tag, value);
        }
    }
}
