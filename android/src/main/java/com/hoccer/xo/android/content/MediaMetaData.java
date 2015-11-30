package com.hoccer.xo.android.content;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import com.hoccer.talk.util.WeakListenerArray;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MediaMetaData {

    private static final Logger LOG = Logger.getLogger(MediaMetaData.class);

    // alternative meta data keys (workaround for a bug on Galaxy S3 and S4)
    public static final int ALTERNATIVE_METADATA_KEY_ALBUM = 25;
    public static final int ALTERNATIVE_METADATA_KEY_ARTIST = 26;
    public static final int ALTERNATIVE_METADATA_KEY_TITLE = 31;

    final int THUMBNAIL_WIDTH = 400;

    private final String mFilePath;
    private String mTitle = "";
    private String mArtist = "";
    private String mAlbumTitle = "";
    private String mMimeType = "";
    private boolean mHasAudio;
    private boolean mHasVideo;

    private static final Map<String, MediaMetaData> mMetaDataCache = new HashMap<String, MediaMetaData>();

    private WeakListenerArray<ArtworkRetrieverListener> mArtworkRetrievalListeners;
    private boolean mArtworkRetrieved;
    private AsyncTask<Void, Void, Drawable> mArtworkRetrievalTask;
    private Drawable mArtwork;

    public static MediaMetaData retrieveMetaData(String mediaFilePath) {
        MediaMetaData metaData;
        if (mMetaDataCache.containsKey(mediaFilePath)) {
            metaData = mMetaDataCache.get(mediaFilePath);
        } else {
            metaData = new MediaMetaData(mediaFilePath);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(mediaFilePath);
                metaData.mAlbumTitle = retrieveItem(retriever, MediaMetadataRetriever.METADATA_KEY_ALBUM, ALTERNATIVE_METADATA_KEY_ALBUM);
                metaData.mArtist = retrieveItem(retriever, MediaMetadataRetriever.METADATA_KEY_ARTIST, ALTERNATIVE_METADATA_KEY_ARTIST);
                metaData.mTitle = retrieveItem(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE, ALTERNATIVE_METADATA_KEY_TITLE);

                String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                metaData.mMimeType = mimeType != null ? mimeType : "";

                if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null) {
                    metaData.mHasAudio = true;
                }

                if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null) {
                    metaData.mHasVideo = true;
                }
            } catch (IllegalArgumentException e) {
                LOG.info("Could not read meta data for file: " + mediaFilePath, e);
            } finally {
                mMetaDataCache.put(mediaFilePath, metaData);
                retriever.release();
            }
        }

        return metaData;
    }

    private static String retrieveItem(MediaMetadataRetriever retriever, int key, int alternativeKey) {
        String item = retriever.extractMetadata(key);
        if (item == null) {
            item = retriever.extractMetadata(alternativeKey);
            if(item == null) {
                return "";
            }
        }
        return item.trim();
    }

    /*
    * Private constructor, use MediaMetaData.retrieveMetaData() to create instances
    */
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

    public String getAlbumTitle() {
        return mAlbumTitle;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public boolean hasAudio() {
        return mHasAudio;
    }

    public boolean hasVideo() {
        return mHasVideo;
    }

    // Listener interface for artwork retrieval
    public interface ArtworkRetrieverListener {
        void onArtworkRetrieveFinished(Drawable artwork);
    }

    /* Retrieves the artwork asynchronously
     * Use the listener to perform tasks with the artwork image.
     * See unregisterTrackRetrievalListener() to remove listener before task is finished
     */
    public void getArtwork(final Resources resources, final ArtworkRetrieverListener listener) {
        // already retrieved
        if (mArtworkRetrieved) {
            listener.onArtworkRetrieveFinished(mArtwork);
            return;
        }

        // create listener list
        if (mArtworkRetrievalListeners == null) {
            mArtworkRetrievalListeners = new WeakListenerArray<ArtworkRetrieverListener>();
        }

        // add listener
        mArtworkRetrievalListeners.registerListener(listener);

        // start retrieval task if not already present
        if (mArtworkRetrievalTask == null) {
            mArtworkRetrievalTask = new AsyncTask<Void, Void, Drawable>() {
                @Override
                protected Drawable doInBackground(Void... params) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    Drawable artwork = null;
                    try {
                        retriever.setDataSource(mFilePath);
                        byte[] artworkRaw = retriever.getEmbeddedPicture();
                        if (artworkRaw != null) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length, options);

                            int scale = 1;
                            if (options.outWidth > THUMBNAIL_WIDTH) {
                                scale = Math.round((float) options.outWidth / (float) THUMBNAIL_WIDTH);;
                            }

                            BitmapFactory.Options outOptions = new BitmapFactory.Options();
                            outOptions.inSampleSize = scale;
                            outOptions.inPurgeable = true;
                            outOptions.inInputShareable = true;

                            Bitmap bitmap = BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length, outOptions);
                            artwork = new BitmapDrawable(resources, bitmap);
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
                    mArtwork = artwork;
                    mArtworkRetrieved = true;
                    for (ArtworkRetrieverListener listener : mArtworkRetrievalListeners) {
                        listener.onArtworkRetrieveFinished(artwork);
                    }

                    // cleanup listeners and task
                    mArtworkRetrievalListeners = null;
                    mArtworkRetrievalTask = null;
                }
            }.execute();
        }
    }

    public void unregisterArtworkRetrievalListener(ArtworkRetrieverListener listener) {
        if (mArtworkRetrievalListeners != null) {
            mArtworkRetrievalListeners.unregisterListener(listener);
        }
    }
}
