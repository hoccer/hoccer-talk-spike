package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.content.ContentSelection;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

import java.io.File;
import java.io.FileNotFoundException;

public class VideoSelector implements IContentSelector {

    private final String mName;
    private final Drawable mIcon;

    public VideoSelector(Context context) {
        mName = context.getResources().getString(R.string.content_video);
        mIcon = ColoredDrawable.getFromCache(R.drawable.ic_attachment_select_video, R.color.primary);
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        return intent;
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) throws Exception {
        String mimeType = UriUtils.getMimeType(context, intent.getData());
        if (!ContentSelection.isMimeTypeVideo(mimeType)) {
            throw new Exception("Mime type is not 'video/*'");
        }

        String filePath = UriUtils.getFilePathByUri(context, intent.getData(), MediaStore.Video.Media.DATA);
        if (filePath == null || !new File(filePath).exists()) {
            throw new FileNotFoundException("File not found for " + intent.getData());
        }

        Uri mediaStoreUri = UriUtils.getContentUriByDataPath(context, MediaStore.Video.Media.getContentUri("external"), filePath);
        String[] projection = {
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
        };
        Cursor cursor = context.getContentResolver().query(mediaStoreUri, projection, null, null, null);
        cursor.moveToFirst();

        int widthIndex = cursor.getColumnIndex(projection[0]);
        int width = cursor.getInt(widthIndex);
        int heightIndex = cursor.getColumnIndex(projection[1]);
        int height = cursor.getInt(heightIndex);
        cursor.close();

        double aspectRatio = ((double) width) / ((double) height);
        return new SelectedFile(filePath, mimeType, ContentMediaType.VIDEO, aspectRatio);
    }
}
