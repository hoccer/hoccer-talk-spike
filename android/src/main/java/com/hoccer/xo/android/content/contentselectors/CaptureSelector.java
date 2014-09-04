package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
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
            String fileName = String.format("hoccer_%s.jpg", timestamp);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), fileName);
            return Uri.fromFile(file);
        } else {
            throw new ExternalStorageNotMountedException("External storage is not mounted.");
        }
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {

        File imageFile;
        File tempImageFile = null;
        int orientation;

        if (intent != null && intent.getData() != null) {
            LOG.error("Intent data path: " + intent.getData().getPath());
            imageFile = new File(intent.getData().getPath());
            LOG.error("Path: " + imageFile.getPath());
        } else {
            tempImageFile = new File(mFileUri.getPath());
            imageFile = tempImageFile;
            MediaScannerConnection.scanFile(mContext, new String[]{imageFile.getPath()},
                     new String[]{ContentMediaType.IMAGE}, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            LOG.error("ScanCompleted: " + path + ", " + uri);
                        }
                    }
            );
        }

        // create content object
        SelectedContent contentObject = new SelectedContent(imageFile.getPath(), "file://" + imageFile.getPath());
        contentObject.setFileName(imageFile.getName());
        contentObject.setContentMediaType(ContentMediaType.IMAGE);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(tempImageFile.getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;

        LOG.info("Name: " + imageFile.getName());
        LOG.info("Height: " + imageHeight);
        LOG.info("Width: " + options.outWidth);
        LOG.info("Image type: " + options.outMimeType);

        contentObject.setContentType(imageType);
        contentObject.setContentLength((int) imageFile.length());

        orientation = ImageUtils.retrieveOrientation(context, null, imageFile.getPath());
        double aspectRatio = ImageUtils.calculateAspectRatio(imageWidth, imageHeight, orientation);

        LOG.info("Image orientation: " + orientation);
        LOG.info("Image aspectRatio: " + aspectRatio);

        contentObject.setContentAspectRatio(aspectRatio);

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
