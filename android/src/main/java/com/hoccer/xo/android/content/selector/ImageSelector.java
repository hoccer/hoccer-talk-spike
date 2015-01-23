package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import org.apache.log4j.Logger;

import java.io.File;

public class ImageSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(ImageSelector.class);

    private String mName;
    private Drawable mIcon;

    public ImageSelector(Context context) {
        mName = context.getResources().getString(R.string.content_images);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_image, true);
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
        IContentCreator creator = findContentObjectCreator(selectedContent);
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

    protected static IContentCreator findContentObjectCreator(Uri selectedContent) {
        String contentString = selectedContent.toString();
        if (isPicasaContent(contentString)) {
            return new PicasaContentObjectCreator();
        } else if (isFileContent(contentString)) {
            return new ImageFileContentObjectCreator();
        }

        return null;
    }

    static private boolean isPicasaContent(String contentString) {
        return
                // picasa images should at least contain this..
                contentString.contains(".android.gallery3d.")

                        // Moto G content string on dirks mobile
                        || contentString.startsWith("content://com.google.android.apps.photos.content/");
    }

    static private boolean isFileContent(String contentString) {
        return contentString.startsWith("content://media/");
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
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