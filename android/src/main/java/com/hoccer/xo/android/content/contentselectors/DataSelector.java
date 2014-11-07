package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

public class DataSelector implements IContentSelector {

    private String mName;
    private Drawable mIcon;

    public DataSelector(Context context) {
        mName = context.getResources().getString(R.string.content_data);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_data, true);
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

        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        SelectedContent content = null;
        return null;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }
}
