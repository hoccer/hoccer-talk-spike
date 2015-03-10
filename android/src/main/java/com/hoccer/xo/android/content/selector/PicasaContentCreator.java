package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.content.SelectedFile;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.ImageUtils;
import ezvcard.util.org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class PicasaContentCreator implements IContentCreator {

    private static final Logger LOG = Logger.getLogger(PicasaContentCreator.class);

    @Override
    public SelectedContent apply(Context context, Intent intent) throws Exception {
        final Uri contentUri = intent.getData();
        final String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
        };
        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) {
            throw new Exception("Could not resolve cursor.");
        }
        cursor.moveToFirst();

        int displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
        String displayName = cursor.getString(displayNameIndex);
        String uriHash = getHashForContentUriPath(contentUri);
        String filename = uriHash + "_" + displayName;
        File imageFile = new File(XoApplication.getAttachmentDirectory(), filename);

        InputStream is;
        is = context.getContentResolver().openInputStream(contentUri);
        if (is == null) {
            throw new Exception("Error while creating image from Picasa. InputStream is null");
        }
        FileUtils.copyInputStreamToFile(is, imageFile);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        int fileWidth = options.outWidth;
        int fileHeight = options.outHeight;
        int orientation = ImageUtils.retrieveOrientation(imageFile.getAbsolutePath());
        double aspectRatio = ImageUtils.calculateAspectRatio(fileWidth, fileHeight, orientation);

        return new SelectedFile(imageFile.getAbsolutePath(), "image/jpeg", ContentMediaType.IMAGE, aspectRatio);
    }

    private static String getHashForContentUriPath(Uri contentUri) {
        String uriHash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(contentUri.getPath().getBytes());
            uriHash = Hex.encodeHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Couldn't generate Hash for content uri path: " + contentUri.getPath());
            e.printStackTrace();
        }
        LOG.info("MD5 Hash for the path of the Picasa URI [" + contentUri.getPath() + "] : " + uriHash);
        return uriHash;
    }
}
