package com.hoccer.xo.android.content.selector;

import android.content.Context;
import com.hoccer.xo.android.content.ContentUtils;
import com.hoccer.xo.android.dialog.FileSelector;

public class ContentSelectorFactory {

    public static IContentSelector createContentSelectorByMimeType(String mimeType, Context context) throws Exception {
        if (ContentUtils.isMimeTypeImage(mimeType)) {
            return new ImageSelector(context);
        } else if (ContentUtils.isMimeTypeVideo(mimeType)) {
            return new VideoSelector(context);
        } else if (ContentUtils.isMimeTypeAudio(mimeType)) {
            return new AudioSelector(context);
        } else if (ContentUtils.isMimeTypeContact(mimeType)) {
            return new ContactSelector(context);
        } else {
            return new FileSelector(context);
        }
    }
}
