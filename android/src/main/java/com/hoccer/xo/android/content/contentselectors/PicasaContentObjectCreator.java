package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;


public class PicasaContentObjectCreator implements IContentCreator {

    Logger LOG = Logger.getLogger(getClass());

    @Override
    public SelectedContent apply(Context context, Intent intent) {
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

                    int orientation = ImageContentHelper.retrieveOrientation(context, contentUri, imageFile.getAbsolutePath());
                    double aspectRatio = ImageContentHelper.calculateAspectRatio(fileWidth, fileHeight, orientation);

                    LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

                    SelectedContent contentObject = new SelectedContent(intent, "file://" + imageFile.getAbsolutePath());
                    contentObject.setFileName(displayName);
                    contentObject.setContentType("image/jpeg");
                    contentObject.setContentMediaType(ContentMediaType.IMAGE);
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

    private Bitmap getBitmap(Context context, Uri url) throws FileNotFoundException {
        InputStream is = context.getContentResolver().openInputStream(url);
        return BitmapFactory.decodeStream(is);
    }
}
