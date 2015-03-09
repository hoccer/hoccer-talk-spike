package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

public class AudioSelector implements IContentSelector {

    private final String mName;
    private final Drawable mIcon;

    public AudioSelector(Context context) {
        mName = context.getResources().getString(R.string.content_music);
        mIcon = ColoredDrawable.getFromCache(R.drawable.ic_attachment_select_media, R.color.primary);
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
        return new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        if (isMimeTypeAudio(context, intent)) {
            String filePath = UriUtils.getFilePathByUri(context, intent.getData(), MediaStore.Audio.Media.DATA);
            String mimeType = UriUtils.getMimeType(context, intent.getData());

            return new SelectedFile(filePath, mimeType, ContentMediaType.AUDIO);
        }
        return null;
    }

    private boolean isMimeTypeAudio(Context context, Intent intent) {
        String mimeType = UriUtils.getMimeType(context, intent.getData());
        return mimeType.startsWith("audio");
    }
}
