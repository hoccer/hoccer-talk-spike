package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ImageUtils;
import ezvcard.util.org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class PicasaContentObjectCreator implements IContentCreator {

    private static final Logger LOG = Logger.getLogger(PicasaContentObjectCreator.class);

    @Override
    public SelectedContent apply(Context context, Intent intent) {
        final Uri contentUri = intent.getData();
        final String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
        };

        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (displayNameIndex != -1) {
                String displayName = cursor.getString(displayNameIndex);
                InputStream is;
                try {
                    is = context.getContentResolver().openInputStream(contentUri);
                } catch (FileNotFoundException e) {
                    LOG.error("Error while creating image from Picasa: ", e);
                    e.printStackTrace();
                    return null;
                }

                String uriHash = getHashForContentUriPath(contentUri);
                String filename = uriHash + "_" + displayName;
                File imageFile = new File(XoApplication.getAttachmentDirectory(), filename);

                try {
                    FileUtils.copyInputStreamToFile(is, imageFile);
                } catch (IOException e) {
                    LOG.error("Failed reading input stream from content uri: " + contentUri.getPath());
                    e.printStackTrace();
                    return null;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

                int fileWidth = options.outWidth;
                int fileHeight = options.outHeight;
                int orientation = ImageUtils.retrieveOrientation(context, contentUri, imageFile.getAbsolutePath());
                double aspectRatio = ImageUtils.calculateAspectRatio(fileWidth, fileHeight, orientation);

                LOG.debug("Aspect ratio: " + fileWidth + " x " + fileHeight + " @ " + aspectRatio + " / " + orientation + "°");

                SelectedContent contentObject = new SelectedContent(intent, "file://" + imageFile.getAbsolutePath());
                contentObject.setFileName(filename);
                contentObject.setContentType("image/jpeg");
                contentObject.setContentMediaType(ContentMediaType.IMAGE);
                contentObject.setContentLength((int) imageFile.length());
                contentObject.setContentAspectRatio(aspectRatio);
                return contentObject;
            }
        }
        return null;
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
