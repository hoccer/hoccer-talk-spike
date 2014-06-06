package com.hoccer.xo.android.util;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import com.hoccer.xo.release.R;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


public class ImageLoader {
    private static ImageLoader mInstance;
    private LruCache memoryLruCache;
    private Context mContext;
    private Map imageViews = Collections.synchronizedMap(new WeakHashMap());
    private Drawable mStubDrawable;

    private ImageLoader(Context context) {
        mContext = context;
        init(context);
    }

    public static ImageLoader getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ImageLoader(context);
        }
        return mInstance;
    }

    private void init(Context context) {
        final int memClass = ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
        // 1/8 of the available mem
        final int cacheSize = 1024 * 1024 * memClass / 8;
        memoryLruCache = new LruCache(cacheSize);
        mStubDrawable = new ColorDrawable(Color.LTGRAY);
    }

    public void displayImage(String uri, ImageView imageView, boolean isIncoming) {
        imageViews.put(imageView, uri);
        Bitmap bitmap = null;
        if (uri != null)
            bitmap = (Bitmap) memoryLruCache.get(uri);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setImageDrawable(mStubDrawable);
            if (uri != null) {
                queuePhoto(uri, imageView, isIncoming);
            }
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
            e.printStackTrace();
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap scaleBitmap(Bitmap bitmap, Context context) {
        //200dp in item_chat_message.xml -> rl_message_attachment -> height
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float scaledHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, metrics);
        float scaledWidth = bitmap.getWidth() * (scaledHeight/bitmap.getHeight());
        return Bitmap.createScaledBitmap(bitmap, Math.round(scaledWidth), Math.round(scaledHeight), false);
    }

    private Bitmap getNinePatchMask(int id,int x, int y, Context context){
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        byte[] chunk = bitmap.getNinePatchChunk();
        NinePatchDrawable drawable = new NinePatchDrawable(context.getResources(), bitmap, chunk, new Rect(), null);
        drawable.setBounds(0, 0,x, y);
        Bitmap result = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        drawable.draw(canvas);
        return result;
    }

    private String getRealPathFromURI(Uri contentURI, Context context) {
        String result = ""  ;
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

    private void queuePhoto(String uri, ImageView imageView, boolean isIncoming) {
        new LoadBitmapTask().execute(uri, imageView, isIncoming);
    }

    private Bitmap getBitmap(String url, boolean isIncoming) {
        Bitmap ret = null;
        File f = new File(getRealPathFromURI(Uri.parse(url), mContext));
        if (f.exists()) {
            ret = decodeFile(f, isIncoming);
            if (ret != null)
                return ret;
        }
        return ret;
    }

    private Bitmap decodeFile(File f, boolean isIncoming) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;
        Bitmap original = BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
        original = rotateBitmap(original, f.getAbsolutePath());
        original = scaleBitmap(original, mContext);
        //Load mask
        int maskResource = R.drawable.bubble_green;
        if (isIncoming) {
            maskResource = R.drawable.bubble_grey;
        }
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

    private class PhotoToLoad {
        public String url;
        public ImageView imageView;

        public PhotoToLoad(String u, ImageView i) {
            url = u;
            imageView = i;
        }
    }

    private boolean imageViewReused(PhotoToLoad photoToLoad) {
        String tag = (String) imageViews.get(photoToLoad.imageView);
        if (tag == null || !tag.equals(photoToLoad.url))
            return true;
        return false;
    }

    class LoadBitmapTask extends AsyncTask<Object, Object, Bitmap> {
        private PhotoToLoad mPhoto;
        private boolean isIncoming;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Object[] params) {
            String uri = (String) params[0];
            mPhoto = new PhotoToLoad(uri, (ImageView) params[1]);
            if (imageViewReused(mPhoto))
                return null;
            isIncoming = (Boolean) params[2];
            Bitmap bitmap = getBitmap(mPhoto.url, isIncoming);
            if (bitmap == null)
                return null;
            memoryLruCache.put(mPhoto.url, bitmap);

            if (bitmap != null) {
                Drawable[] drawables = new Drawable[2];
                drawables[0] = mStubDrawable;
                drawables[1] = new BitmapDrawable(mContext.getResources(), bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReused(mPhoto)) {
                return;
            }
            if (bitmap != null) {
                mPhoto.imageView.setImageBitmap(bitmap);
                mPhoto.imageView.setVisibility(View.VISIBLE);
            } else {
                mPhoto.imageView.setImageDrawable(mStubDrawable);
            }
        }
    }
}
