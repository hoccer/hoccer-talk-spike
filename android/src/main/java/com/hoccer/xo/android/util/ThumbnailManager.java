package com.hoccer.xo.android.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This class is the central source for thumbnail representations of given image attachments.
 * It creates, stores, caches scaled and masked bitmaps of specified images.
 */
public class ThumbnailManager {
    private static Logger LOG = Logger.getLogger(ThumbnailManager.class);
    private static int DEFAULT_HEIGHT_DP = 200;
    private static ThumbnailManager mInstance;
    private final Map<String, AsyncTask> mRunningRenderJobs;
    private LruCache<String, Bitmap> mMemoryLruCache;

    private Context mContext;
    private Drawable mStubDrawable;


    private ThumbnailManager(Context context) {
        mContext = context;
        mRunningRenderJobs = new ConcurrentHashMap<String, AsyncTask>();
        init(context);
    }

    public static ThumbnailManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ThumbnailManager(context);
        }
        return mInstance;
    }

    /**
     * Clears the cache and removes memory.
     */
    public void clearCache() {
        if (mMemoryLruCache != null) {
            LOG.info("Will evict thumbnail cache with size: " + mMemoryLruCache.size());
            mMemoryLruCache.evictAll();
            LOG.info("New cache size: " + mMemoryLruCache.size());
        }
    }

    private void init(Context context) {
        // Use 1/8th of the available memory for this memory cache.
        final int maxMemoryInKiloByte = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemoryInKiloByte / 8;
        LOG.debug("Creating LruCache with size of [" + cacheSize + "] kb");
        mMemoryLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        mStubDrawable = new ColorDrawable(Color.LTGRAY);
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryLruCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryLruCache.get(key);
    }

    /**
     * Retrieves a thumbnail representation of an image at a specified URI + specified tag and adds it to a given ImageView.
     *
     * @param uri          The URI of the image
     * @param imageView    The ImageView which will display the thumbnail
     * @param maskResource The resource id of a drawable to mask the thumbnail
     * @param tag          The tag to identify this specific thumbnail representation
     */
    public void displayThumbnailForImage(String uri, ImageView imageView, int maskResource, String tag) {

        String thumbnailUri = taggedThumbnailUri(uri, tag);

        Bitmap bitmap = null;
        if (uri != null) {
            bitmap = getBitmapFromMemCache(thumbnailUri);
        }
        if (bitmap == null) {
            bitmap = loadThumbnailForUri(uri, tag);
        }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setImageDrawable(mStubDrawable);
            if (uri != null) {
                queueImageThumbnailCreation(uri, imageView, maskResource, tag);
            }
        }
    }

    private String taggedThumbnailUri(String uri, String tag) {
        String thumbnailFilename = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
        int index = thumbnailFilename.lastIndexOf(".");
        String taggedFilename = thumbnailFilename.substring(0, index) + String.valueOf(tag) + thumbnailFilename.substring(index);
        return XoApplication.getThumbnailDirectory() + File.separator + taggedFilename;
    }

    // TODO: use DiskLruCache instead
    private Bitmap loadThumbnailForUri(String uri, String tag) {
        String thumbnailUri = taggedThumbnailUri(uri, tag);
        File thumbnail = new File(thumbnailUri);
        Bitmap bitmap = null;
        if (thumbnail.exists()) {
            bitmap = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
            if (bitmap != null) {
                addBitmapToMemoryCache(thumbnailUri, bitmap);
            }
        }
        return bitmap;
    }

    // TODO: use DiskLruCache instead
    private void saveToThumbnailDirectory(Bitmap bitmap, String uri, String tag) {
        File destination = new File(taggedThumbnailUri(uri, tag));
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(destination));
        } catch (FileNotFoundException e) {
            LOG.error("Error while saving thumbnail bitmap: " + destination.getAbsolutePath(), e);
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, String filePath) {
        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case 6:
                    rotation = 90;
                    break;
                case 3:
                    rotation = 180;
                    break;
                case 8:
                    rotation = -90;
                    break;
                default:
                    rotation = 0;
                    break;
            }

        } catch (IOException e) {
            LOG.error("Error while accessing Exif information for image: " + filePath, e);
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap scaleBitmap(Bitmap bitmap, Context context) {
        if (bitmap == null || context == null) {
            return null;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float scaledHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_HEIGHT_DP, metrics);
        float scaledWidth = bitmap.getWidth() * (scaledHeight / bitmap.getHeight());
        return Bitmap.createScaledBitmap(bitmap, Math.round(scaledWidth), Math.round(scaledHeight), false);
    }

    private Bitmap getNinePatchMask(int id, int x, int y, Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        byte[] chunk = bitmap.getNinePatchChunk();
        NinePatchDrawable drawable = new NinePatchDrawable(context.getResources(), bitmap, chunk, new Rect(), null);
        drawable.setBounds(0, 0, x, y);
        Bitmap result = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        drawable.draw(canvas);
        return result;
    }

    private String getRealPathFromURI(Uri contentURI, Context context) {
        String result = "";
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                result = cursor.getString(idx);
                cursor.close();
            }
        }
        return result;
    }

    private void queueImageThumbnailCreation(String uri, ImageView imageView, int maskResource, String tag) {
        String key = taggedThumbnailUri(uri, tag);
        synchronized (mRunningRenderJobs) {
            if (!mRunningRenderJobs.containsKey(key)) {
                ImageThumbnailRenderer imageThumbnailRenderer = new ImageThumbnailRenderer();
                mRunningRenderJobs.put(key, imageThumbnailRenderer);
                imageThumbnailRenderer.execute(uri, imageView, maskResource, tag, key);
            }
        }
    }

    private Bitmap createImageThumbnail(String uri, int maskResource, String tag) {
        if (uri == null) {
            return null;
        }
        Bitmap thumbnail;
        File imageFile = new File(getRealPathFromURI(Uri.parse(uri), mContext));
        if (imageFile.exists()) {
            thumbnail = renderImageThumbnail(imageFile, maskResource);
            if (thumbnail != null) {
                saveToThumbnailDirectory(thumbnail, uri, tag);
                return thumbnail;
            }
        }
        return null;
    }

    // TODO: use ThumbnailUtils methods for all this.
    private Bitmap renderImageThumbnail(File file, int maskResource) {
        // Dry-loading of bitmap to calculate sample size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        int fileHeight = options.outHeight;
        int sampleSize = fileHeight / DEFAULT_HEIGHT_DP;
        // Load bitmap in appropriate size
        options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap original = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        original = rotateBitmap(original, file.getAbsolutePath());
        original = scaleBitmap(original, mContext);
        //Load mask
        Bitmap mask = getNinePatchMask(maskResource, original.getWidth(), original.getHeight(), mContext);
        //Draw everything on canvas
        Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        c.drawBitmap(original, 0, 0, null);
        c.drawBitmap(mask, 0, 0, paint);
        paint.setXfermode(null);
        return result;
    }


    private class ImageToLoad {
        public String mUrl;
        public ImageView mImageView;

        public ImageToLoad(String url, ImageView imageView) {
            mUrl = url;
            mImageView = imageView;
        }
    }

    private class ImageThumbnailRenderer extends AsyncTask<Object, Object, Bitmap> {
        private ImageToLoad mImageToLoad;
        private int mMaskResource;
        private String mTag;
        public String mThumbnailUri;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Object[] params) {
            String uri = (String) params[0];
            mImageToLoad = new ImageToLoad(uri, (ImageView) params[1]);
            mMaskResource = (Integer) params[2];
            mTag = (String) params[3];
            mThumbnailUri = (String) params[4];

            Bitmap thumbnail = createImageThumbnail(mImageToLoad.mUrl, mMaskResource, mTag);
            if (thumbnail == null) {
                return null;
            }

            return thumbnail;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                addBitmapToMemoryCache(mThumbnailUri, bitmap);
                mImageToLoad.mImageView.setImageBitmap(bitmap);
                mImageToLoad.mImageView.setVisibility(View.VISIBLE);
            } else {
                mImageToLoad.mImageView.setImageDrawable(mStubDrawable);
            }

            unregisterRenderJob(mThumbnailUri);
        }
    }

    private class VideoThumbnailRenderer extends AsyncTask<Object, Void, Bitmap> {
        private String mUri;
        private int mMaskResource;
        private String mTag;
        private ImageView mThumbnailView;
        public String mThumbnailUri;

        @Override
        protected Bitmap doInBackground(Object... params) {
            mUri = (String) params[0];
            mThumbnailView = (ImageView) params[1];
            mMaskResource = (Integer) params[2];
            mTag = (String) params[3];
            mThumbnailUri = (String) params[4];

            return createVideoThumbnail(mUri, mMaskResource, mTag);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (bitmap != null) {
                addBitmapToMemoryCache(mThumbnailUri, bitmap);
                mThumbnailView.setImageBitmap(bitmap);
                mThumbnailView.setVisibility(View.VISIBLE);
            } else {
                mThumbnailView.setImageDrawable(mStubDrawable);
            }

            unregisterRenderJob(mThumbnailUri);
        }
    }

    public void unregisterRenderJob(String key) {
        synchronized (mRunningRenderJobs) {
            if (mRunningRenderJobs.containsKey(key)) {
                mRunningRenderJobs.remove(key);
            }
        }
    }

    /**
     * Retrieves a thumbnail representation of a video at a specified URI + specified tag and adds it to a given ImageView.
     *
     * @param uri          The URI of the image
     * @param imageView    The ImageView which will display the thumbnail
     * @param maskResource The resource id of a drawable to mask the thumbnail
     * @param tag          The tag to identify this specific thumbnail representation
     */
    public void displayThumbnailForVideo(String uri, ImageView imageView, int maskResource, String tag) {
        String taggedUri = taggedThumbnailUri(uri, tag);
        Bitmap bitmap = getBitmapFromMemCache(taggedUri);

        if (bitmap == null) {
            bitmap = loadThumbnailForUri(uri, tag);
        }
        if (bitmap == null) {
            queueVideoThumbnailCreation(uri, imageView, maskResource, tag);
        } else {
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private void queueVideoThumbnailCreation(String uri, ImageView imageView, int maskResource, String tag) {
        String taggedUri = taggedThumbnailUri(uri, tag);
        synchronized (mRunningRenderJobs) {
            if (!mRunningRenderJobs.containsKey(taggedUri)) {
                imageView.setImageDrawable(mStubDrawable);
                VideoThumbnailRenderer videoThumbnailRenderer = new VideoThumbnailRenderer();
                mRunningRenderJobs.put(taggedUri, videoThumbnailRenderer);
                videoThumbnailRenderer.execute(uri, imageView, maskResource, tag, taggedUri);
            }
        }
    }

    private Bitmap createVideoThumbnail(String uri, int maskResource, String tag) {
        String path = getRealPathFromURI(Uri.parse(uri), mContext);
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
        if (bitmap == null) {
            return null;
        }
        Bitmap result;
        // scale up if necessary
        if (bitmap.getHeight() < DEFAULT_HEIGHT_DP) {
            Bitmap scaled = scaleBitmap(bitmap, mContext);
            result = renderThumbnailForVideo(scaled, maskResource);
        } else {
            result = renderThumbnailForVideo(bitmap, maskResource);
        }
        if (result != null) {
            saveToThumbnailDirectory(result, uri, tag);
        }
        return result;
    }

    private Bitmap renderThumbnailForVideo(Bitmap bitmap, int maskResource) {
        Bitmap mask = getNinePatchMask(maskResource, bitmap.getWidth(), bitmap.getHeight(), mContext);
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap overlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        overlay.eraseColor(0x88000000);
        Canvas c = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        c.drawBitmap(bitmap, 0, 0, null);
        c.drawBitmap(overlay, 0, 0, null);
        c.drawBitmap(mask, 0, 0, paint);
        paint.setXfermode(null);

        return result;
    }
}
