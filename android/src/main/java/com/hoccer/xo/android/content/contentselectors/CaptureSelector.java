package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        String[] projection = {
                MediaStore.Images.Media.DATA
        };

        File file = new File(mFileUri.getPath());

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;

        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        int exifOrientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
        } else {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }
        Uri contentUri;
        String contentUriString = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, file.getName(), file.getName());

        contentUri = Uri.parse(contentUriString);

        Cursor cursor = context.getContentResolver().query(
                contentUri, projection, null, null, null);
        cursor.moveToFirst();
        int dataIndex = cursor.getColumnIndex(projection[0]);
        String filePath = cursor.getString(dataIndex);
        cursor.close();

        if (filePath == null) {
            return null;
        }
        File imageFile = new File(filePath);

        SelectedContent contentObject = new SelectedContent(contentUriString, "file://" + filePath);
        contentObject.setFileName(imageFile.getName());
        contentObject.setContentMediaType(ContentMediaType.IMAGE);
        contentObject.setContentType(imageType);
        contentObject.setContentLength((int) imageFile.length());
        contentObject.setContentAspectRatio(((float) imageWidth) / ((float) imageHeight));

        if (file.exists()) {
            file.delete();
        }

        return contentObject;
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
