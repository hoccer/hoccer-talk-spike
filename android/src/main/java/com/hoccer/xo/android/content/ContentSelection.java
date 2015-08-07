package com.hoccer.xo.android.content;

import android.content.Context;
import com.hoccer.xo.android.content.selector.*;

public class ContentSelection {

    IContentSelector mSelector;

    public ContentSelection(IContentSelector selector) {
        mSelector = selector;
    }

    public IContentSelector getSelector() {
        return mSelector;
    }

    public void setSelector(IContentSelector selector) {
        this.mSelector = selector;
    }

    public static IContentSelector createContentSelectorByMimeType(String mimeType, Context context) throws Exception {
        if (mimeType.startsWith("image/")) {
            return new ImageSelector(context);
        } else if (mimeType.startsWith("video/")) {
            return new VideoSelector(context);
        } else if (mimeType.startsWith("audio/") || mimeType.startsWith("application/ogg")) {
            return new AudioSelector(context);
        } else if (mimeType.startsWith("vnd.android.cursor.item/contact")) {
            return new ContactSelector(context);
        } else {
            throw new Exception("Content is not supported.");
        }
    }

}
