package com.hoccer.xo.android.util;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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

    public static boolean compressImageFile(File input, File output, int maxPixelCount, int imageQuality, Bitmap.CompressFormat format) {

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

        // TODO: too big images cause out of memory exceptions !!!!!!!!!!!!!!!!
        Bitmap encodedBitmap = BitmapFactory.decodeFile(input.getAbsolutePath(), options);

        Bitmap.CompressFormat compressFormat = getFormatByMimeType(options.outMimeType);

        boolean shallCopyExif = false;
        if (compressFormat == null) {
            compressFormat = format;
        } else if (compressFormat == Bitmap.CompressFormat.JPEG) {
            shallCopyExif = true;
        }

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(output);
            if (encodedBitmap.compress(compressFormat, imageQuality, os)) {
                if (shallCopyExif) {
                    copyExifData(input.getAbsolutePath(), output.getAbsolutePath());
                }
                return true;
            }
            ;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        } catch (FileNotFoundException e) {
//            LOG.error("Fatal error in creating temporary file " + out.getPath(), e);
//        } finally {
//            try {
//                if (outStream != null) {
//                    outStream.close();
//                }
//            } catch (IOException e) {
//                LOG.error("Fatal error while closing output stream for " + out.getPath(), e);
//            }
//        }
        return false;
    }


    public static void encodeBitmap(final ArrayList<File> input, final File out, final int maxPixelCount, final int imageQuality,
                                    final Bitmap.CompressFormat format, final EncodingCompleteCallback callback) {


        AsyncTask<Void, Void, Boolean> encodingTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void[] params) {
                boolean encodingSuccessful = false;

                for (File in : input) {
                    FileOutputStream outStream = null;

                    try {
                        BitmapFactory.Options encodingOptions = new BitmapFactory.Options();
                        encodingOptions.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(in.getAbsolutePath(), encodingOptions);

                        long originalPixelCount = encodingOptions.outWidth * encodingOptions.outHeight;
                        if (maxPixelCount < originalPixelCount) {
                            double resizeRatio = Math.sqrt(originalPixelCount / maxPixelCount);
                            encodingOptions.inSampleSize = (int) resizeRatio + 1;
                            encodingOptions.outWidth = (int) (encodingOptions.outWidth / resizeRatio);
                            encodingOptions.outHeight = (int) (encodingOptions.outHeight / resizeRatio);
                        }
                        encodingOptions.inJustDecodeBounds = false;
                        Bitmap encodedImage = BitmapFactory.decodeFile(in.getAbsolutePath(), encodingOptions);
                        outStream = new FileOutputStream(out);

                        Bitmap.CompressFormat compressFormat = getFormatByMimeType(encodingOptions.outMimeType);

                        boolean shallCopyExif = false;
                        if (compressFormat == null) {
                            compressFormat = format;
                        } else {
                            if (compressFormat == Bitmap.CompressFormat.JPEG) {
                                shallCopyExif = true;
                            }
                        }

                        encodingSuccessful = encodedImage.compress(compressFormat, imageQuality, outStream);

                        if (shallCopyExif) {
                            copyExifData(in.getAbsolutePath(), out.getAbsolutePath());
                        }

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
                }
                return encodingSuccessful;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if (result) {
                    callback.onSuccessfulEncoding(null);
                } else {
                    callback.onError();
                }
            }
        };

        encodingTask.execute();
    }

    public static Bitmap.CompressFormat getFormatByMimeType(String mimeType) {
        Bitmap.CompressFormat format = null;
        if (mimeType != null || !mimeType.isEmpty()) {
            if (mimeType.equalsIgnoreCase(MIME_TYPE_IMAGE_PREFIX + Bitmap.CompressFormat.JPEG.name())) {
                format = Bitmap.CompressFormat.JPEG;
            } else if (mimeType.equalsIgnoreCase(MIME_TYPE_IMAGE_PREFIX + Bitmap.CompressFormat.PNG.name())) {
                format = Bitmap.CompressFormat.PNG;
            } else if (mimeType.equalsIgnoreCase(MIME_TYPE_IMAGE_PREFIX + Bitmap.CompressFormat.WEBP.name())) {
                format = Bitmap.CompressFormat.WEBP;
            }
        }

        return format;
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

    public interface EncodingCompleteCallback {
        public void onSuccessfulEncoding(ArrayList<File> files);

        public void onError();
    }
}
