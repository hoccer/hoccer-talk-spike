package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.ContentMediaTypes;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.bofrostmessenger.R;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;

public class ImageSelector implements IContentSelector {

    Logger LOG = Logger.getLogger(ImageSelector.class);

    private String mName;
    private Drawable mIcon;

    public ImageSelector(Context context) {
        mName = context.getResources().getString(R.string.content_images);
        mIcon = ColorSchemeManager.fillBackground(context, R.drawable.ic_attachment_select_image, true);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public Drawable getContentIcon() {
        return mIcon;
    }

    @Override
    public Intent createSelectionIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        return intent;
    }

    public Intent createCropIntent(Context context, Uri data) {
        Intent intent = new Intent("com.android.camera.action.CROP", android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        intent.setDataAndType(data, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("return-data", false);

        File tmpFile = new File(XoApplication.getAttachmentDirectory(), "tmp_crop");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpFile));
        return intent;
    }


    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        Uri selectedContent = intent.getData();
        if (selectedContent.toString().startsWith("content://com.google.android.gallery3d")) {
            return createFromPicasa(context, intent);
        } else {
            return createFromFile(context, intent);
        }
    }

    private SelectedContent createFromPicasa(final Context context, Intent intent) {
        Uri selectedContent = intent.getData();
        final String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.Images.Media.ORIENTATION};
        Cursor cursor = context.getContentResolver().query(selectedContent, filePathColumn, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (columnIndex != -1) {
                try {
                    String displayName = cursor.getString(columnIndex);
                    final Uri contentUri = selectedContent;
                    Bitmap bmp = getBitmap(context, contentUri);
                    File imageFile = new File(XoApplication.getAttachmentDirectory(), displayName);

                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(imageFile));

                    int fileWidth = bmp.getWidth();
                    int fileHeight = bmp.getHeight();

                    int orientation = retrieveOrientation(context, contentUri, imageFile.getAbsolutePath());
                    double aspectRatio = calculateAspectRatio(fileWidth, fileHeight, orientation);

                    LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

                    SelectedContent contentObject = new SelectedContent(intent, "file://" + imageFile.getAbsolutePath());
                    contentObject.setFileName(displayName);
                    contentObject.setContentType("image/jpeg");
                    contentObject.setContentMediaType(ContentMediaTypes.MediaTypeImage);
                    contentObject.setContentLength((int) imageFile.length());
                    contentObject.setContentAspectRatio(aspectRatio);
                    return contentObject;
                } catch (FileNotFoundException e) {
                    LOG.error("Error while creating image from Picasa: ", e);
                }
            }
        }

        return null;
    }

    private Bitmap getBitmap(Context context, Uri url) {
        try {
            InputStream is;
            if (url.toString().startsWith("content://com.google.android.gallery3d")) {
                is = context.getContentResolver().openInputStream(url);
            } else {
                is = new URL(url.toString()).openStream();
            }
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            LOG.error("Error while creating bitmap: ", e);
            return null;
        }
    }

    private SelectedContent createFromFile(Context context, Intent intent) {
        Uri contentUri = intent.getData();
        String[] columns = {
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                // TODO: since this fields are not available below API Level 16 we will not use them for now. Comment all following lines in when fully available.
                // MediaStore.Images.Media.WIDTH,
                // MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.TITLE,
        };

        Cursor cursor = context.getContentResolver().query(contentUri, columns, null, null, null);
        cursor.moveToFirst();

        int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
        int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
        // int widthIndex = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH);
        // int heightIndex = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT);
        int fileNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.TITLE);

        String mimeType = cursor.getString(mimeTypeIndex);
        String filePath = cursor.getString(dataIndex);
        String fileName = cursor.getString(fileNameIndex);
        int fileSize = cursor.getInt(sizeIndex);
        int fileWidth = 0; // cursor.getInt(widthIndex);
        int fileHeight = 0; // cursor.getInt(heightIndex);
        cursor.close();

        if (filePath == null) {
            filePath = contentUri.toString();
        }

        // Validating file size
        File file = new File(filePath);
        int realFileSize = (int) file.length();
        if (fileSize != realFileSize) {
            LOG.debug("File size from content database is not actual file size. Reading file size from actual file.");
            fileSize = realFileSize;
        }

        // Validating image measurements
        //if (fileWidth == 0 || fileHeight == 0) {
        //    LOG.debug("Could not retrieve image measurements from content database. Will use values extracted from file instead.");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        fileWidth = options.outWidth;
        fileHeight = options.outHeight;
        //}

        int orientation = retrieveOrientation(context, contentUri, filePath);
        double aspectRatio = calculateAspectRatio(fileWidth, fileHeight, orientation);

        LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

        //
        SelectedContent contentObject = new SelectedContent(intent, "file://" + filePath);
        contentObject.setFileName(fileName);
        contentObject.setContentType(mimeType);
        contentObject.setContentMediaType(ContentMediaTypes.MediaTypeImage);
        contentObject.setContentLength(fileSize);
        contentObject.setContentAspectRatio(aspectRatio);
        return contentObject;
    }

    private int retrieveOrientation(Context context, Uri contentUri, String filePath) {
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

    private double calculateAspectRatio(int fileWidth, int fileHeight, int orientation) {
        double aspectRatio;
        if (orientation == 0 || orientation == 180) {
            aspectRatio = (double) fileWidth / (double) fileHeight;
        } else {
            aspectRatio = (double) fileHeight / (double) fileWidth;
        }
        return aspectRatio;
    }

}
