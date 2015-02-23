package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(ImageSelector.class);

    private String mName;
    private Drawable mIcon;

    public ImageSelector(Context context) {
        mName = context.getResources().getString(R.string.content_images);
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
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        Uri selectedContent = intent.getData();
        IContentCreator creator = findContentCreator(selectedContent);
        if (creator == null) {
            LOG.warn("No IContentCreator found for url '" + selectedContent + "'");
            return null;
        }

        return creator.apply(context, intent);
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

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        if (UriUtils.isFileUri(intent.getData())) {
            return true;
        }

        if (UriUtils.isContentUri(intent.getData())) {
            Uri contentUri = intent.getData();
            if (contentUri != null) {
                String[] columns = {
                        MediaStore.Images.Media.MIME_TYPE
                };
                Cursor cursor = context.getContentResolver().query(contentUri, columns, null, null, null);

                if (cursor != null) {
                    cursor.moveToFirst();
                    int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
                    String mimeType = cursor.getString(mimeTypeIndex);
                    return (mimeType.startsWith("image"));
                }
            }
        }

        return false;
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
