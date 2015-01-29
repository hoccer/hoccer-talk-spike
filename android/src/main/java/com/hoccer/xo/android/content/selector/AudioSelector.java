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
import com.hoccer.xo.android.content.SelectedFile;
import com.hoccer.xo.android.util.ColorSchemeManager;

public class AudioSelector implements IContentSelector {

    private final String mName;
    private final Drawable mIcon;

    public AudioSelector(Context context) {
        mName = context.getResources().getString(R.string.content_music);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_media, true);
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
        return new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        Uri selectedContent = intent.getData();
        String[] filePathColumn = {
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA
        };

        Cursor cursor = context.getContentResolver().query(
                selectedContent, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int typeIndex = cursor.getColumnIndex(filePathColumn[0]);
        String fileType = cursor.getString(typeIndex);
        int dataIndex = cursor.getColumnIndex(filePathColumn[1]);
        String filePath = cursor.getString(dataIndex);
        cursor.close();

        if (filePath == null) {
            return null;
        }

        return new SelectedFile(filePath, fileType, ContentMediaType.AUDIO);
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }
}
