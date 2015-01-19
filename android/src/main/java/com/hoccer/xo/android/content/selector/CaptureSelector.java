package com.hoccer.xo.android.content.selector;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(CaptureSelector.class);

    private final String mName;
    private final Drawable mIcon;
    private Uri mFileUri;
    private final Context mContext;

    public CaptureSelector(Context context) {
        mContext = context;
        mName = context.getResources().getString(R.string.content_capture);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_video, true);
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
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
            return Uri.fromFile(file);
        } else {
            throw new ExternalStorageNotMountedException("External storage is not mounted.");
        }
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        String filePath = mFileUri.getPath();
        String filename = mFileUri.getLastPathSegment();

        Uri contentUri;
        try {
            contentUri = Uri.parse(MediaStore.Images.Media.insertImage(context.getContentResolver(), filePath, filename, null));
        } catch (FileNotFoundException e) {
            LOG.error("Could not insert image into MediaStore", e);
            return null;
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(filePath));

        ImageUtils.ExifData exifData = ImageUtils.getExifData(filePath);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, String.valueOf(exifData.orientation));
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, String.valueOf(exifData.dateTime));
        values.put(MediaStore.Images.ImageColumns.LATITUDE, String.valueOf(exifData.latitude));
        values.put(MediaStore.Images.ImageColumns.LONGITUDE, String.valueOf(exifData.longitude));
        context.getContentResolver().update(contentUri, values, null, null);

        File imageFile = new File(filePath);
        int fileLength = (int) imageFile.length();

        float aspectRatio = (float) calculateAspectRatio(filePath, exifData.orientation);

        SelectedContent contentObject = new SelectedContent(contentUri.toString(), UriUtils.FILE_URI_PREFIX + filePath);
        contentObject.setFileName(filename);
        contentObject.setContentMediaType(ContentMediaType.IMAGE);
        contentObject.setContentType(mimeType);
        contentObject.setContentLength(fileLength);
        contentObject.setContentAspectRatio(aspectRatio);
        return contentObject;
    }

    private static double calculateAspectRatio(String filePath, int orientation) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        return ImageUtils.calculateAspectRatio(imageWidth, imageHeight, orientation);
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
