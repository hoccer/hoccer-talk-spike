package com.hoccer.xo.android.content;

import com.hoccer.talk.content.IContentObject;

public class Clipboard {

    private static Clipboard sInstance;

    private IContentObject mContent;

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

    public IContentObject getContent() {
        return mContent;
    }

    public void setContent(IContentObject contentObject) {
        mContent = contentObject;
    }

    public void clearContent() {
        mContent = null;
    }
}
