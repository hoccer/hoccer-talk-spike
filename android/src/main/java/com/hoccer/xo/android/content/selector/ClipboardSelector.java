package com.hoccer.xo.android.content.selector;


import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.util.ColorSchemeManager;

public class ClipboardSelector implements IContentSelector {

    private final Clipboard mClipboard;

    private final String mName;
    private final Drawable mIcon;

    public ClipboardSelector(Context context) {
        mName = context.getResources().getString(R.string.content_clipboard);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_data, true);
        mClipboard = Clipboard.getInstance();
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
        return null; // this selector does not use an external activity
    }

    @Override
    public IContentObject createObjectFromSelectionResult(Context context, Intent intent) {
        IContentObject contentObject = mClipboard.getContent();
        mClipboard.clearContent();
        return contentObject;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }

    public boolean hasContent() {
        return mClipboard.hasContent();
    }
}
