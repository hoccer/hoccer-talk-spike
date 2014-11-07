package com.hoccer.xo.android.content.contentselectors;


import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class ClipboardSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(ClipboardSelector.class);

    private Clipboard mClipboard;

    private String mName;
    private Drawable mIcon;

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
