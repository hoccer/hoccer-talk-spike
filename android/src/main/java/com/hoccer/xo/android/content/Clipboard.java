package com.hoccer.xo.android.content;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.SelectedAttachment;
import com.hoccer.xo.android.util.UriUtils;

public class Clipboard {

    private static Clipboard sInstance;

    private SelectedAttachment mContent;

    public static synchronized Clipboard getInstance() {
        if (sInstance == null) {
            sInstance = new Clipboard();
        }

        return sInstance;
    }

    private Clipboard() {
        // private constructor to make sure that getInstance() is used instead
    }

    public boolean hasContent() {
        return (mContent != null);
    }

    public SelectedAttachment getContent() {
        return mContent;
    }

    public void setContent(SelectedAttachment contentObject) {
        mContent = contentObject;
    }

    public void setContent(XoTransfer transfer) {
        mContent = new SelectedFile(UriUtils.getAbsoluteFileUri(transfer.getFilePath()).getPath(), transfer.getContentType(), transfer.getContentMediaType(), transfer.getContentAspectRatio());
    }

    public void clearContent() {
        mContent = null;
    }
}
