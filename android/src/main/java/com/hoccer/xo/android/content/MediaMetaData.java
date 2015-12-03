package com.hoccer.xo.android.content;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import org.apache.log4j.Logger;

import java.io.File;

public class MediaMetaData {

    private static final Logger LOG = Logger.getLogger(MediaMetaData.class);

    // alternative meta data keys (workaround for a bug on Galaxy S3 and S4)
    public static final int ALTERNATIVE_METADATA_KEY_ARTIST = 26;
    public static final int ALTERNATIVE_METADATA_KEY_TITLE = 31;

    private final String mFilePath;
    private String mTitle = "";
    private String mArtist = "";
    private String mMimeType = "";

    public static MediaMetaData retrieveMetaData(String mediaFilePath) {
        MediaMetaData metaData;
        metaData = new MediaMetaData(mediaFilePath);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mediaFilePath);
            metaData.mArtist = retrieveItem(retriever, MediaMetadataRetriever.METADATA_KEY_ARTIST, ALTERNATIVE_METADATA_KEY_ARTIST);
            metaData.mTitle = retrieveItem(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE, ALTERNATIVE_METADATA_KEY_TITLE);

            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            metaData.mMimeType = mimeType != null ? mimeType : "";
        } catch (IllegalArgumentException e) {
            LOG.info("Could not read meta data for file: " + mediaFilePath, e);
        } finally {
            retriever.release();
        }

        return metaData;
    }

    private static String retrieveItem(MediaMetadataRetriever retriever, int key, int alternativeKey) {
        String item = retriever.extractMetadata(key);
        if (item == null) {
            item = retriever.extractMetadata(alternativeKey);
            if (item == null) {
                return "";
            }
        }
        return item.trim();
    }

    private MediaMetaData(String filePath) {
        mFilePath = filePath;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getTitleOrFilename() {
        if (mTitle.isEmpty()) {
            File file = new File(mFilePath);
            return file.getName();
        } else {
            return mTitle;
        }
    }

    public String getArtist() {
        return mArtist;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public interface ArtworkRetrieverListener {
        void onArtworkRetrieveFinished(Drawable artwork);
    }

    public void getResizedArtwork(final Resources resources, final ArtworkRetrieverListener listener, final int width) {
        new AsyncTask<Void, Void, Drawable>() {
            @Override
            protected Drawable doInBackground(Void... params) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                Drawable artwork = null;
                try {
                    retriever.setDataSource(mFilePath);
                    byte[] artworkRaw = retriever.getEmbeddedPicture();
                    if (artworkRaw != null) {
                        artwork = createBitmapDrawable(artworkRaw, width, resources);
                    } else {
                        LOG.warn("Could not read artwork for file: " + mFilePath);
                    }
                } catch (IllegalArgumentException e) {
                    LOG.warn("Could not read meta data for file: " + mFilePath, e);
                } finally {
                    retriever.release();
                }

                return artwork;
            }

            @Override
            protected void onPostExecute(Drawable artwork) {
                listener.onArtworkRetrieveFinished(artwork);
            }
        }.execute();
    }

    private Drawable createBitmapDrawable(byte[] artworkRaw, int width, Resources resources) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length, options);

        int scale = 1;
        if (options.outWidth > width) {
            scale = Math.round((float) options.outWidth / (float) width);
            LOG.debug("##################################### " + scale);
        }

        BitmapFactory.Options outOptions = new BitmapFactory.Options();
        outOptions.inSampleSize = scale;

        Bitmap bitmap = BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length, outOptions);
        return new BitmapDrawable(resources, bitmap);
    }
}
