package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.content.SelectedFile;
import com.hoccer.xo.android.util.ImageUtils;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageFileContentCreator implements IContentCreator {

    private static final Logger LOG = Logger.getLogger(ImageFileContentCreator.class);

    @Override
    public SelectedContent apply(Context context, Intent intent) {
        Uri contentUri = intent.getData();

        String[] projection = {
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA
        };
        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) {
            LOG.error("Query failed! Could not resolve cursor for content uri: " + contentUri);
            return null;
        }
        cursor.moveToFirst();

        String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
        String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        if (filePath == null) {
            filePath = contentUri.toString();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            LOG.error("The image file at '" + filePath + "' does not exist.");
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        int fileWidth = options.outWidth;
        int fileHeight = options.outHeight;
        int orientation = ImageUtils.retrieveOrientation(filePath);
        double aspectRatio = ImageUtils.calculateAspectRatio(fileWidth, fileHeight, orientation);

        return new SelectedFile(filePath, mimeType, ContentMediaType.IMAGE, aspectRatio);
    }
}
