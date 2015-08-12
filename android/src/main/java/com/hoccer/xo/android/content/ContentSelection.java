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
        if (isMimeTypeImage(mimeType)) {
            return new ImageSelector(context);
        } else if (isMimeTypeVideo(mimeType)) {
            return new VideoSelector(context);
        } else if (isMimeTypeAudio(mimeType)) {
            return new AudioSelector(context);
        } else if (isMimeTypeContact(mimeType)) {
            return new ContactSelector(context);
        } else {
            throw new Exception("Content is not supported.");
        }
    }

    public static boolean isMimeTypeContact(String mimeType) {
        return mimeType.startsWith("vnd.android.cursor.item/contact");
    }

    public static boolean isMimeTypeAudio(String mimeType) {
        return mimeType.startsWith("audio/") || mimeType.startsWith("application/ogg");
    }

    public static boolean isMimeTypeVideo(String mimeType) {
        return mimeType.startsWith("video/");
    }

    public static boolean isMimeTypeImage(String mimeType) {
        return mimeType.startsWith("image/");
    }

}
