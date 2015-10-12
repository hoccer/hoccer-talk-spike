package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.ContentUtils;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(ImageSelector.class);

    private String mName;
    private Drawable mIcon;

    public ImageSelector(Context context) {
        mName = context.getResources().getString(R.string.content_image);
        mIcon = ColoredDrawable.getFromCache(R.drawable.ic_attachment_select_image, R.color.primary);
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
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");

        return intent;
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) throws Exception {
        if (isMimeTypeImage(context, intent)) {
            Uri selectedContent = intent.getData();
            IContentCreator creator = findContentCreator(selectedContent);
            return creator.apply(context, intent);
        } else {
            throw new Exception("Mime type is not 'image/*'");
        }
    }

    private boolean isMimeTypeImage(Context context, Intent intent) {
        return ContentUtils.isMimeTypeImage(UriUtils.getMimeType(context, intent.getData()));
    }

    protected void setName(String name) {
        mName = name;
    }

    protected void setIcon(Drawable icon) {
        mIcon = icon;
    }

    protected static IContentCreator findContentCreator(Uri selectedContent) {
        String contentString = selectedContent.toString();
        if (isPicasaContent(contentString)) {
            return new PicasaContentCreator();
        } else {
            return new ImageFileContentCreator();
        }
    }

    static private boolean isPicasaContent(String contentString) {
        return
                // picasa images should at least contain this..
                contentString.contains(".android.gallery3d.")

                        // Moto G content string on dirks mobile
                        || contentString.startsWith("content://com.google.android.apps.photos.content/");
    }

    public static Intent createCropIntent(Uri data) {
        Intent intent = new Intent("com.android.camera.action.CROP", android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        intent.setDataAndType(data, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("return-data", false);

        File tmpFile = new File(XoApplication.getAttachmentDirectory(), "tmp_crop");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpFile));
        return intent;
    }
}
