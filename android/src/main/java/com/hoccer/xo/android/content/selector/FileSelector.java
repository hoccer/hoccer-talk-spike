package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.content.selector.SelectedContentFactory;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent.setType("file/*");
        }

        return intent;
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) throws Exception {
        return SelectedContentFactory.createSelectedContent(intent, context);
    }
}
