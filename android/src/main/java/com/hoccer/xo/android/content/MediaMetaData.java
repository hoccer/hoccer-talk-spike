package com.hoccer.xo.android.content;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import com.hoccer.talk.util.WeakListenerArray;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MediaMetaData {

    private static final Logger LOG = Logger.getLogger(MediaMetaData.class);

    private static Map<String, MediaMetaData> mMetaDataCache = new HashMap<String, MediaMetaData>();

    public static MediaMetaData retrieveMetaData(String mediaFilePath) {
        String mediaFileUri = UriUtils.getAbsoluteFileUri(mediaFilePath).getPath();

        MediaMetaData metaData = null;
        // return cached data if present
        if (mMetaDataCache.containsKey(mediaFileUri)) {
            metaData = mMetaDataCache.get(mediaFileUri);
        } else {

            MediaMetadataRetriever retriever = null;
            try {
                retriever = new MediaMetadataRetriever();
                retriever.setDataSource(mediaFileUri);
                metaData = new MediaMetaData(mediaFileUri);
                String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (album == null) {
                    album = retriever.extractMetadata(25); // workaround bug on Galaxy S3 and S4
                }
                metaData.mAlbumTitle = album;

                String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (artist == null) {
                    artist = retriever.extractMetadata(26); // workaround bug on Galaxy S3 and S4
                }
                metaData.mArtist = artist;

                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                if (title == null) {
                    title = retriever.extractMetadata(31); // workaround bug on Galaxy S3 and S4
                }
                metaData.mTitle = title;

                metaData.mMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

                if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null) {
                    metaData.mHasAudio = true;
                }

                if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null) {
                    metaData.mHasVideo = true;
                }

            } catch (IllegalArgumentException e) {
                LOG.info("Could not read meta data for file: " + mediaFileUri, e);
                // cache the null object to prevent future retrieval attempts
                metaData = null;
            } finally {
                mMetaDataCache.put(mediaFileUri, metaData);
                retriever.release();
            }
        }

        return metaData;
    }

    private final Uri mFileUri;
    private String mTitle;
    private String mArtist;
    private String mAlbumTitle;
    private String mMimeType;
    private boolean mHasAudio;
    private boolean mHasVideo;

    private WeakListenerArray<ArtworkRetrieverListener> mArtworkRetrievalListeners;
    private boolean mArtworkRetrieved;
    private AsyncTask<Void, Void, Drawable> mArtworkRetrievalTask;
    private Drawable mArtwork;

    /*
    * Private constructor, use MediaMetaData.retrieveMetaData() to create instances
    */
    private MediaMetaData(String filePath) {
        mFileUri = UriUtils.getAbsoluteFileUri(filePath);
    }

    public Uri getFileUri() {
        return mFileUri;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getTitleOrFilename() {
        if (mTitle == null || mTitle.isEmpty()) {
            File file = new File(mFileUri.getPath());
            return file.getName();
        }
        return mTitle;
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
                        retriever.setDataSource(mFileUri.toString());
                        byte[] artworkRaw = retriever.getEmbeddedPicture();
                        if (artworkRaw != null) {
                            artwork = new BitmapDrawable(resources, BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length));
                        } else {
                            LOG.warn("Could not read artwork for file: " + mFileUri);
                        }
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Could not read meta data for file: " + mFileUri, e);
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
