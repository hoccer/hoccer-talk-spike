package com.hoccer.xo.android.util;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageContentHelper {

    private static final Logger LOG = Logger.getLogger(ImageContentHelper.class);

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

    public static void encodeBitmap(final File in, final File out, final int maxPixelCount, final int imageQuality,
                             final Bitmap.CompressFormat format, final Runnable successCallback,
                             final Runnable errorCallback) {
        AsyncTask<Void, Void, Boolean> encodingTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void[] params) {
                boolean encodingSuccessful = false;
                FileOutputStream outStream = null;
                try {
                    LOG.debug("BAZINGA start decoding original for bounds ");
                    BitmapFactory.Options encodingOptions = new BitmapFactory.Options();
                    encodingOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(in.getAbsolutePath(), encodingOptions);
                    LOG.debug("BAZINGA decoding original finished");
                    LOG.debug("BAZINGA preparing options");

                    long originalPixelCount = encodingOptions.outWidth * encodingOptions.outHeight;
                    if (maxPixelCount < originalPixelCount) {
                        double resizeRatio = Math.sqrt(originalPixelCount / maxPixelCount);
                        encodingOptions.inSampleSize = (int) resizeRatio + 1;
                        encodingOptions.outWidth = (int) (encodingOptions.outWidth / resizeRatio);
                        encodingOptions.outHeight = (int) (encodingOptions.outHeight / resizeRatio);
                    }

                    encodingOptions.inJustDecodeBounds = false;
                    LOG.debug("BAZINGA decode final bitmap");
                    Bitmap encodedImage = BitmapFactory.decodeFile(in.getAbsolutePath(), encodingOptions);
                    outStream = new FileOutputStream(out);

                    LOG.debug("BAZINGA compress final bitmap");
                    encodingSuccessful = encodedImage.compress(format, imageQuality, outStream);
                } catch (FileNotFoundException e) {
                    LOG.error("Fatal error in creating temporary file " + out.getPath(), e);
                } finally {
                    try {
                        if (outStream != null) {
                            outStream.close();
                        }
                    } catch (IOException e) {
                        LOG.error("Fatal error while closing output stream for " + out.getPath(), e);
                    }
                }

                return encodingSuccessful;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                LOG.debug("Encoding finished");

                if (result) {
                    successCallback.run();
                } else {
                    errorCallback.run();
                }
            }
        };

        encodingTask.execute();
    }
}
