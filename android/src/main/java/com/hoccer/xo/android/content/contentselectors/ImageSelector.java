package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.*;

public class ImageSelector implements IContentSelector {

    private Logger LOG = Logger.getLogger(ImageSelector.class);

    private String mName;
    private Drawable mIcon;

    public ImageSelector(Context context) {
        mName = context.getResources().getString(R.string.content_images);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context, R.drawable.ic_attachment_select_image, true);
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

    private IContentCreator findContentObjectCreator(Uri selectedContent) {
        String contentString = selectedContent.toString();
        if (contentString.contains(".android.gallery3d.")) {
            return new PicasaContentObjectCreator();
        } else if (contentString.startsWith("content://media/")) {
            return new FileContentObjectCreator();
        }

        return null;
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        Uri contentUri = intent.getData();
        String[] columns = {
                MediaStore.Images.Media.MIME_TYPE
        };
        Cursor cursor = context.getContentResolver().query(contentUri, columns, null, null, null);
        cursor.moveToFirst();
        int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
        String mimeType = cursor.getString(mimeTypeIndex);
        return (mimeType.startsWith("image"));
    }

    public Intent createCropIntent(Context context, Uri data) {
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
