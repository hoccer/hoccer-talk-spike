package com.hoccer.xo.android.content;

import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MediaMetaData {

    private static final Logger LOG = Logger.getLogger(MediaMetaData.class);

    private static Map<String, MediaMetaData> mMetaDataCache = new HashMap<String, MediaMetaData>();

    public static MediaMetaData retrieveMetaData(String mediaFilePath) {
        // return cached data if present
        if(mMetaDataCache.containsKey(mediaFilePath)) {
            return mMetaDataCache.get(mediaFilePath);
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mediaFilePath);
        } catch(IllegalArgumentException e) {
            LOG.error("Could not read meta data for file: " + mediaFilePath, e);
            mMetaDataCache.put(mediaFilePath, null);
            return null;
        }

        MediaMetaData metaData = new MediaMetaData();
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

        mMetaDataCache.put(mediaFilePath, metaData);
        return metaData;
    }

    public static byte[] retrieveArtwork(String filePath) {
        String path = Uri.parse(filePath).getPath();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

        return retriever.getEmbeddedPicture();
    }

    private String mTitle = null;
    private String mArtist = null;
    private String mAlbumTitle = null;
    private String mMimeType = null;
    private boolean mHasAudio = false;
    private boolean mHasVideo = false;
    private Drawable mArtwork = null;

    /*
    * Private constructor, use MediaMetaData.retrieveMetaData() to create instances
    */
    private MediaMetaData() {
    }

    public String getTitle() {
        return mTitle;
    }

    public String getTitleOrFilename(String pFilePath) {
        if (mTitle == null || mTitle.isEmpty()) {
            File file = new File(pFilePath);
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

    public void getArtwork(Resources resources, ) {
        return mArtwork;
    }

    public void setArtwork(Drawable artwork) {
        mArtwork = artwork;
    }
}
