package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageFileContentObjectCreator implements IContentCreator {

    private static final Logger LOG = Logger.getLogger(ImageFileContentObjectCreator.class);

    @Override
    public SelectedContent apply(Context context, Intent intent) {
        Uri contentUri = intent.getData();

        String[] projection = {
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.TITLE,
        };
        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) {
            LOG.error("Query failed! Could not resolve cursor for content uri: " + contentUri);
            return null;
        }
        cursor.moveToFirst();

        String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
        String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.TITLE));
        int fileSize = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
        cursor.close();

        if (filePath == null) {
            filePath = contentUri.toString();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            LOG.error("The image file at '" + filePath + "' does not exist.");
            return null;
        }

        // validating file size
        int realFileSize = (int) file.length();
        if (fileSize != realFileSize) {
            LOG.debug("File size from content database is not actual file size. Reading file size from actual file.");
            fileSize = realFileSize;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        int fileWidth = options.outWidth;
        int fileHeight = options.outHeight;
        int orientation = ImageUtils.retrieveOrientation(context, contentUri, filePath);
        double aspectRatio = ImageUtils.calculateAspectRatio(fileWidth, fileHeight, orientation);
        LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

        SelectedContent contentObject = new SelectedContent(intent, UriUtils.FILE_URI_PREFIX + filePath);
        contentObject.setFileName(fileName);
        contentObject.setContentType(mimeType);
        contentObject.setContentMediaType(ContentMediaType.IMAGE);
        contentObject.setContentLength(fileSize);
        contentObject.setContentAspectRatio(aspectRatio);
        return contentObject;
    }
}
