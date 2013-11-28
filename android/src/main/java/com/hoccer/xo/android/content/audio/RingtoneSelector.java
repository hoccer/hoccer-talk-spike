package com.hoccer.xo.android.content.audio;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.content.IContentSelector;
import org.apache.log4j.Logger;

public class RingtoneSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(RingtoneSelector.class);

    @Override
    public String getName() {
        return "Ringtone";
    }

    @Override
    public Intent createSelectionIntent(Context context) {
        return new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        Uri selectedContent = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        String[] filePathColumn = {MediaStore.Audio.Media.MIME_TYPE,
                                   MediaStore.Audio.Media.DATA,
                                   MediaStore.Audio.Media.SIZE};

        Cursor cursor = context.getContentResolver().query(
                selectedContent, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int typeIndex = cursor.getColumnIndex(filePathColumn[0]);
        String fileType = cursor.getString(typeIndex);
        int dataIndex = cursor.getColumnIndex(filePathColumn[1]);
        String filePath = cursor.getString(dataIndex);
        int sizeIndex = cursor.getColumnIndex(filePathColumn[2]);
        int fileSize = cursor.getInt(sizeIndex);

        cursor.close();

        if(filePath == null) {
            return null;
        }

        SelectedContent contentObject = new SelectedContent(intent, "file://" + filePath);
        contentObject.setContentMediaType("audio");
        contentObject.setContentType(fileType);
        contentObject.setContentLength(fileSize);

        return contentObject;
    }
}
