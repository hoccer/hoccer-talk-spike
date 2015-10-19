package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.content.ContentUtils;
import com.hoccer.xo.android.util.UriUtils;

import static com.hoccer.talk.content.ContentMediaType.FILE;

public class SelectedContentFactory {

    public static SelectedContent createSelectedContent(Intent intent, Context context) throws Exception {
        Uri dataUri = intent.getData();

        if ("com.android.contacts".equals(dataUri.getAuthority())) {
            return new ContactSelector(context).createObjectFromSelectionResult(context, intent);
        }

        String mimeType = UriUtils.getMimeType(context, dataUri);
        if (ContentUtils.isMimeTypeImage(mimeType)) {
            return new ImageSelector(context).createObjectFromSelectionResult(context, intent);
        } else if (ContentUtils.isMimeTypeVideo(mimeType)) {
            return new VideoSelector(context).createObjectFromSelectionResult(context, intent);
        } else if (ContentUtils.isMimeTypeAudio(mimeType)) {
            return new AudioSelector(context).createObjectFromSelectionResult(context, intent);
        } else if (ContentUtils.isMimeTypeContact(mimeType)) {
            return new ContactSelector(context).createObjectFromSelectionResult(context, intent);
        } else {
            String filePath = UriUtils.getFilePathByUri(context, dataUri);
            return new SelectedFile(filePath, mimeType, FILE);
        }
    }
}
