package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.UriUtils;

public class ImageFileContentCreator implements IContentCreator {

    @Override
    public SelectedContent apply(Context context, Intent intent) {

        String filePath = UriUtils.getFilePathByUri(context, intent.getData());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        int fileWidth = options.outWidth;
        int fileHeight = options.outHeight;
        int orientation = ImageUtils.retrieveOrientation(filePath);
        double aspectRatio = ImageUtils.calculateAspectRatio(fileWidth, fileHeight, orientation);

        String mimeType = UriUtils.getMimeType(context, intent.getData());

        return new SelectedFile(filePath, mimeType, ContentMediaType.IMAGE, aspectRatio);
    }
}
