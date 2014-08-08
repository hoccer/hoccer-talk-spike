package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageFileContentObjectCreator implements IContentCreator {

    Logger LOG = Logger.getLogger(getClass());

    @Override
    public SelectedContent apply(Context context, Intent intent) {
        Uri contentUri = intent.getData();

        // Retrieve image metadata from content database
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
