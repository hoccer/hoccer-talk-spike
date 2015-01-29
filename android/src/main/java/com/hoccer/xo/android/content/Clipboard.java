package com.hoccer.xo.android.content;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.util.UriUtils;

public class Clipboard {

    private static Clipboard sInstance;

    private SelectedContent mContent;

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

    public SelectedContent getContent() {
        return mContent;
    }

    public void setContent(SelectedContent content) {
        mContent = content;
    }

    public void setContent(XoTransfer transfer) {
        mContent = new SelectedFile(UriUtils.getAbsoluteFileUri(transfer.getFilePath()).getPath(), transfer.getMimeType(), transfer.getMediaType(), transfer.getContentAspectRatio());
    }

    public void clearContent() {
        mContent = null;
    }
}
