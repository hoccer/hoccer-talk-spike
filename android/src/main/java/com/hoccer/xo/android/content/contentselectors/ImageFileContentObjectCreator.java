package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ImageContentHelper;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageFileContentObjectCreator implements IContentCreator {

    Logger LOG = Logger.getLogger(getClass());

    @Override
    public SelectedContent apply(Context context, Intent intent) {
        Uri contentUri = intent.getData();

        // Retrieve image metadata from content database
        String[] projection = {
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.TITLE,
        };

        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        cursor.moveToFirst();

        String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
        String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.TITLE));
        int fileSize = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
        int fileWidth = 0;
        int fileHeight = 0;
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

        int orientation = ImageContentHelper.retrieveOrientation(context, contentUri, filePath);
        double aspectRatio = ImageContentHelper.calculateAspectRatio(fileWidth, fileHeight, orientation);

        LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

        //
        SelectedContent contentObject = new SelectedContent(intent, "file://" + filePath);
        contentObject.setFileName(fileName);
        contentObject.setContentType(mimeType);
        contentObject.setContentMediaType(ContentMediaType.IMAGE);
        contentObject.setContentLength(fileSize);
        contentObject.setContentAspectRatio(aspectRatio);
        return contentObject;
    }
}
