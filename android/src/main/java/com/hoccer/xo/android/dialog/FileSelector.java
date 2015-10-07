package com.hoccer.xo.android.dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
        return SelectedContentFactory.createSelectedContent(intent, context);
    }
}
