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
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

import java.io.File;

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
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        String filePath = UriUtils.getFilePathByUri(context, intent.getData(), MediaStore.Video.Media.DATA);
        if (filePath == null || !new File(filePath).exists()) {
            return null;
        }

        Uri selectedContent = intent.getData();
        String[] projection = {
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
        };

        Cursor cursor = context.getContentResolver().query(selectedContent, projection, null, null, null);
        cursor.moveToFirst();

        int typeIndex = cursor.getColumnIndex(projection[0]);
        String mimeType = cursor.getString(typeIndex);
        int widthIndex = cursor.getColumnIndex(projection[1]);
        int width = cursor.getInt(widthIndex);
        int heightIndex = cursor.getColumnIndex(projection[2]);
        int height = cursor.getInt(heightIndex);
        cursor.close();

        double aspectRatio = ((double) width) / ((double) height);
        return new SelectedFile(filePath, mimeType, ContentMediaType.VIDEO, aspectRatio);
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        Uri contentUri = intent.getData();
        String[] columns = {MediaStore.Video.Media.MIME_TYPE};
        Cursor cursor = context.getContentResolver().query(contentUri, columns, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
            String mimeType = cursor.getString(mimeTypeIndex);
            return (mimeType.startsWith("video"));
        }

        return false;
    }
}
