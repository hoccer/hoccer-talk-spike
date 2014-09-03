package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(CaptureSelector.class);

    private String mName;
    private Drawable mIcon;
    private Uri mFileUri;
    private Context mContext;

    public CaptureSelector(Context context) {
        mContext = context;
        mName = context.getResources().getString(R.string.content_capture);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context, R.drawable.ic_attachment_select_video, true);
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
        try {
            mFileUri = createOutputMediaFileUri();
        } catch (ExternalStorageNotMountedException e) {
            Toast.makeText(mContext, "Error accessing public storage directory", Toast.LENGTH_LONG).show();
            LOG.error(e.getLocalizedMessage(), e);
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
        return intent;
    }

    private Uri createOutputMediaFileUri() throws ExternalStorageNotMountedException {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = String.format("hoccer_%s", timestamp);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
            return Uri.fromFile(file);
        } else {
            throw new ExternalStorageNotMountedException("External storage is not mounted.");
        }
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {

        File imageFile;
        File tempImageFile = null;
        String contentUriString = null;
        int orientation;

        if (intent != null && intent.getData() != null) {
            LOG.error("Intent data path: " + intent.getData().getPath());
            orientation = ImageUtils.retrieveOrientation(context, intent.getData(), intent.getData().getPath());
            imageFile = new File(intent.getData().getPath());
            LOG.error("Path: " + imageFile.getPath());
        } else {
            tempImageFile = new File(mFileUri.getPath());
            imageFile = tempImageFile;
            orientation = ImageUtils.retrieveOrientation(context, null, imageFile.getPath());
        }

        // Correct rotation if wrong and insert image into MediaStore
        if (orientation > 0) {
            Bitmap bitmap = correctImageRotation(imageFile, orientation);
            contentUriString = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, imageFile.getName(), imageFile.getName());
        } else {
            try {
                contentUriString = MediaStore.Images.Media.insertImage(context.getContentResolver(), imageFile.getPath(), imageFile.getName(), imageFile.getName());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // delete temporary image file
        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
        }

        // get new file path from content resolver
        Uri contentUri = Uri.parse(contentUriString);
        String[] projection = {
                MediaStore.Images.Media.DATA
        };
        Cursor cursor = context.getContentResolver().query(
                contentUri, projection, null, null, null);
        cursor.moveToFirst();
        int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        String filePath = cursor.getString(dataIndex);
        cursor.close();
        if (filePath == null) {
            return null;
        }
        imageFile = new File(filePath);

        // create content object
        SelectedContent contentObject = new SelectedContent(imageFile.getPath(), "file://" + imageFile.getPath());
        contentObject.setFileName(imageFile.getName());
        contentObject.setContentMediaType(ContentMediaType.IMAGE);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;

        LOG.error("Name: " + imageFile.getName());
        LOG.error("Height: " + options.outHeight);
        LOG.error("Width: " + options.outWidth);
        LOG.error("Image type: " + options.outMimeType);

        contentObject.setContentType(imageType);
        contentObject.setContentLength((int) imageFile.length());

        orientation = ImageUtils.retrieveOrientation(context, contentUri, imageFile.getPath());
        double aspectRatio = ImageUtils.calculateAspectRatio(imageWidth, imageHeight, orientation);

        LOG.error("Image orientation: " + orientation);
        LOG.error("Image aspectRatio: " + aspectRatio);

        contentObject.setContentAspectRatio(aspectRatio);

        return contentObject;
    }

    private Bitmap correctImageRotation(File tempFile, int orientation) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(tempFile.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath(), options);
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options) {

        Point size = DisplayUtils.getDisplaySize(mContext);
        int reqWidth = size.x;
        int reqHeight = size.y;

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }

    public class ExternalStorageNotMountedException extends Throwable {

        private ExternalStorageNotMountedException(String message) {
            super(message);
        }
    }

}
