package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.util.ColorSchemeManager;

public class DataSelector implements IContentSelector {

    private final String mName;
    private final Drawable mIcon;

    public DataSelector(Context context) {
        mName = context.getResources().getString(R.string.content_data);
        mIcon = ColorSchemeManager.getInkedDrawable(R.drawable.ic_attachment_select_data, R.color.primary);
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
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        return Intent.createChooser(intent, context.getString(R.string.file_chooser_string));
    }

    @Override
    public IContentObject createObjectFromSelectionResult(Context context, Intent intent) {
        return null;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }
}
