package com.hoccer.xo.android.content;

import com.hoccer.talk.content.IContentObject;

public class Clipboard {

    private static Clipboard INSTANCE = null;

    private ClipboardContent mContent;

    public static synchronized Clipboard getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Clipboard();
        }

        return INSTANCE;
    }

    private Clipboard() {
        // private constructor to make sure that getInstance() is used instead
    }

    public boolean hasContent() {
        return (mContent != null);
    }

    public ClipboardContent getContent() {
        return mContent;
    }

    public void setContent(IContentObject contentObject) {
        mContent = ClipboardContent.fromContentObject(contentObject);
    }

    public void clearContent() {
        mContent = null;
    }
}
