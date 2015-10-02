package com.hoccer.xo.android.dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

public class FileSelector implements IContentSelector {

    private String mName;
    private Drawable mIcon;

    public FileSelector(Context context) {
        mName = "File";
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

        return new SelectedFile(filePath, mimeType, ContentMediaType.DATA);
    }
}
