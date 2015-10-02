package com.hoccer.xo.android.dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.content.ContentUtils;
import com.hoccer.xo.android.content.selector.*;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

import static com.hoccer.talk.content.ContentMediaType.FILE;

public class FileSelector implements IContentSelector {

    private String mName;
    private Drawable mIcon;

    public FileSelector(Context context) {
        mName = context.getResources().getString(R.string.content_file);
        mIcon = ColoredDrawable.getFromCache(R.drawable.ic_attachment_select_data, R.color.primary);
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        return intent;
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) throws Exception {
        String filePath = UriUtils.getFilePathByUri(context, intent.getData(), MediaStore.Images.Media.DATA);
        String mimeType = UriUtils.getMimeType(context, intent.getData());

        if (ContentUtils.isMimeTypeImage(mimeType)) {
            return new ImageSelector(context).createObjectFromSelectionResult(context, intent);
        } else if (ContentUtils.isMimeTypeVideo(mimeType)) {
            return new VideoSelector(context).createObjectFromSelectionResult(context, intent);
        } else if (ContentUtils.isMimeTypeAudio(mimeType)) {
            return new AudioSelector(context).createObjectFromSelectionResult(context, intent);
        } else if (ContentUtils.isMimeTypeContact(mimeType)) {
            return new ContactSelector(context).createObjectFromSelectionResult(context, intent);
        } else {
            return new SelectedFile(filePath, mimeType, FILE);
        }
    }
}
