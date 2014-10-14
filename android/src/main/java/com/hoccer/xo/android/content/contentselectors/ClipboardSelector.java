package com.hoccer.xo.android.content.contentselectors;


import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.activity.ClipboardPreviewActivity;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ClipboardSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(ClipboardSelector.class);

    private Clipboard mClipboard;

    private String mName;
    private Drawable mIcon;

    public ClipboardSelector(Context context) {
        mName = context.getResources().getString(R.string.content_clipboard);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context, R.drawable.ic_attachment_select_data, true);
        mClipboard = Clipboard.getInstance(context);
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
        Intent intent = new Intent(context, ClipboardPreviewActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTENT_OBJECT, mClipboard.getContent());
        return intent;
    }

    public IContentObject selectObjectFromClipboard() {
        IContentObject contentObject = mClipboard.getContent();
        mClipboard.clearClipBoard();
        return contentObject;
    }

    @Override
    public IContentObject createObjectFromSelectionResult(Context context, Intent intent) {
        return selectObjectFromClipboard();
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }

    public boolean canProcessClipboard() {
        return mClipboard.canProcessClipboard();
    }
}
