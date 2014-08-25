package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.activity.MultiImagePickerActivity;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;

public class ImageSelector implements IContentSelector {

    private Logger LOG = Logger.getLogger(ImageSelector.class);

    private String mName;
    private Drawable mIcon;

    public ImageSelector(Context context) {
        mName = context.getResources().getString(R.string.content_images);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context, R.drawable.ic_attachment_select_image, true);
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
        Intent intent = new Intent(context, MultiImagePickerActivity.class);
        return intent;
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        Uri selectedContent = intent.getData();
        IContentCreator creator = findContentObjectCreator(selectedContent);
        if (creator == null) {
            LOG.warn("No IContentCreator found for url '" + selectedContent + "'");
            return null;
        }

        return creator.apply(context, intent);
    }

    public ArrayList<IContentObject> createImageObjectsFromSelectionResult(Context context, Intent intent) {
        String[] uris = intent.getStringArrayExtra("IMAGES");
        ArrayList<IContentObject> selected = new ArrayList<IContentObject>();
//        if (!isValidIntent) {
//            return null;
//        } else {
//            Uri selectedContent = intent.getData();
        for (String uri: uris) {
            if (uri.toString().startsWith("content://com.google.android.gallery3d")) {
//                    return createFromPicasa(context, intent);
            } else {
                selected.add(createFromUri(context, Uri.parse(uri)));
//                    return createFromFile(context, intent);
            }
        }
//        }
        return selected;
    }

    private IContentCreator findContentObjectCreator(Uri selectedContent) {
        String contentString = selectedContent.toString();
        if (isPicasaContent(contentString)) {
            return new PicasaContentObjectCreator();
        } else if (isFileContent(contentString)) {
            return new ImageFileContentObjectCreator();
        }

        return null;
    }

    static private boolean isPicasaContent(String contentString) {
        return
                // picasa images should at least contain this..
                contentString.contains(".android.gallery3d.")

                        // Moto G content string on dirks mobile
                        || contentString.startsWith("content://com.google.android.apps.photos.content/");
    }

    static private boolean isFileContent(String contentString) {
        return contentString.startsWith("content://media/");
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        Uri contentUri = intent.getData();
        if (contentUri != null) {
            String[] columns = {
                    MediaStore.Images.Media.MIME_TYPE
            };
            Cursor cursor = context.getContentResolver().query(contentUri, columns, null, null, null);
            cursor.moveToFirst();
            int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
            String mimeType = cursor.getString(mimeTypeIndex);
            return (mimeType.startsWith("image"));
        } else {
            return false;
        }
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

    private SelectedContent createFromUri(Context context, Uri uri) {
        Uri contentUri = uri;
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
        SelectedContent contentObject = new SelectedContent(uri.toString(), "file://" + filePath);
        contentObject.setFileName(fileName);
        contentObject.setContentType(mimeType);
        contentObject.setContentMediaType(ContentMediaType.IMAGE);
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
