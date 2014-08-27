package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class PicasaContentObjectCreator implements IContentCreator {

    Logger LOG = Logger.getLogger(getClass());

    @Override
    public SelectedContent apply(Context context, Intent intent) {
        final Uri contentUri = intent.getData();
        final String[] projection = {
                MediaStore.EXTRA_OUTPUT,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.PICASA_ID
        };

        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            int picasaIdIndex = cursor.getColumnIndex(MediaStore.Images.Media.PICASA_ID);
            if (displayNameIndex != -1 && picasaIdIndex != -1) {
                String displayName = cursor.getString(displayNameIndex);
                String picasaId = cursor.getString(picasaIdIndex);
                InputStream is = null;
                try {
                    is = context.getContentResolver().openInputStream(contentUri);
                } catch (FileNotFoundException e) {
                    LOG.error("Error while creating image from Picasa: ", e);
                    e.printStackTrace();
                }

                String filename = picasaId + "_" + displayName;
                File imageFile = new File(XoApplication.getAttachmentDirectory(), filename);

                try {
                    FileUtils.readInputStream(is, imageFile);
                } catch (IOException e) {
                    LOG.error("Failed reading input stream from content uri: " + contentUri.getPath());
                    e.printStackTrace();
                    return null;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

                int fileWidth = options.outWidth;
                int fileHeight = options.outHeight;

                int orientation = ImageContentHelper.retrieveOrientation(context, contentUri, imageFile.getAbsolutePath());
                double aspectRatio = ImageContentHelper.calculateAspectRatio(fileWidth, fileHeight, orientation);

                LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

                SelectedContent contentObject = new SelectedContent(intent, "file://" + imageFile.getAbsolutePath());
                contentObject.setFileName(filename);
                contentObject.setContentType("image/jpeg");
                contentObject.setContentMediaType(ContentMediaType.IMAGE);
                contentObject.setContentLength((int) imageFile.length());
                contentObject.setContentAspectRatio(aspectRatio);
                return contentObject;
            }
        }
        return null;
    }


}
