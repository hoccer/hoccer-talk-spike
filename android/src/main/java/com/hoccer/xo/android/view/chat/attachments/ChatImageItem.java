package com.hoccer.xo.android.view.chat.attachments;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.*;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.WindowManager;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ThumbnailManager;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;


public class ChatImageItem extends ChatMessageItem {

    private Context mContext;

    public ChatImageItem(Context context, TalkClientMessage message) {
        super(context, message);
        mContext = context;
    }

    public ChatItemType getType() {
        return ChatItemType.ChatItemWithImage;
    }

    @Override
    protected void configureViewForMessage(View view) {
        super.configureViewForMessage(view);
        configureAttachmentViewForMessage(view);
    }

    @Override
    protected void displayAttachment(final IContentObject contentObject) {
        super.displayAttachment(contentObject);
        mAttachmentView.setPadding(0, 0, 0, 0);
        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout imageLayout = (RelativeLayout) inflater.inflate(R.layout.content_image, null);
            mContentWrapper.addView(imageLayout);
        }

        mContentWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayImage(contentObject);
            }
        });

        ImageView imageView = (ImageView) mContentWrapper.findViewById(R.id.iv_image_view);
        RelativeLayout rootView = (RelativeLayout) mContentWrapper.findViewById(R.id.rl_root);
        int mask;
        String tag = (mMessage.getMessageId() != null) ? mMessage.getMessageId() : mMessage.getMessageTag();
        if (mMessage.isIncoming()) {
            rootView.setGravity(Gravity.LEFT);
            mask = R.drawable.chat_bubble_incoming;
        } else {
            rootView.setGravity(Gravity.RIGHT);
            mask = R.drawable.chat_bubble_outgoing;
        }
        if (contentObject.getContentDataUrl() != null) {
            mAttachmentView.setBackgroundDrawable(null);

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_dark_content_pause)
                    .showImageForEmptyUri(R.drawable.ic_dark_content_pause)
                    .showImageOnFail(R.drawable.ic_dark_social_group)
                    .cacheInMemory(true)
                    .cacheOnDisc(true)
                    .build();

            String imageUri = null;
            String contentUri = contentObject.getContentUrl();
            if (contentUri == null || contentUri.isEmpty()) {
                try {
                    Uri fileUri = Uri.parse(contentObject.getContentDataUrl());

                    contentUri = MediaStore.Images.Media.insertImage(mContext.getContentResolver(), fileUri.getPath(), null, null);
                    if (contentObject instanceof TalkClientDownload) {
                        TalkClientDownload download = ((TalkClientDownload) contentObject);
                        download.setContentUri(contentUri);
                        XoApplication.getXoClient().getDatabase().saveClientDownload(download);
                    }

                } catch (FileNotFoundException e) {
                    // through setting DisplayImageOptions.Builder().showImageOnFail(...) we have a fallback image
                    e.printStackTrace();
                    imageUri = contentObject.getContentDataUrl();
                } catch (SQLException e) {
                    e.printStackTrace();
                    imageUri = contentObject.getContentDataUrl();
                }
            }

            if (contentUri != null && !contentUri.isEmpty()) {
                String thumbnailContentUri = getThumbnailFileUriByContentUri(contentUri);
                if (thumbnailContentUri != null && !thumbnailContentUri.isEmpty()) {
                    imageUri = thumbnailContentUri;
                } else {
                    imageUri = renderImageThumbnail(contentObject.getContentDataUrl(), tag);
                }
            }

            imageUri = renderImageThumbnail(contentObject.getContentDataUrl(), tag);
            ImageLoader.getInstance().displayImage(imageUri, imageView, options);

//            ThumbnailManager.getInstance(mContext).displayThumbnailForImage(contentObject.getContentDataUrl(), imageView, mask, tag);
        }
    }

    // TODO: use ThumbnailUtils methods for all this.
    private String renderImageThumbnail(String fileUri, String tag) {
        String filePath = getRealPathFromURI(Uri.parse(fileUri));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int deviceWidth = windowManager.getDefaultDisplay().getWidth();
        int deviceHeight = windowManager.getDefaultDisplay().getHeight();

        int sampleSize = calculateInSampleSize(options, deviceWidth / 4, deviceHeight / 4);
        LOG.error("#foo " + sampleSize);
        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
        Bitmap original = BitmapFactory.decodeFile(filePath, options);
        LOG.error("#foo " + original.getByteCount());
//        Bitmap mask = getNinePatchMask(maskResource, original.getWidth(), original.getHeight(), mContext);
//        Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);

        File file = new File(taggedThumbnailUri(fileUri, tag));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            original.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        Canvas c = new Canvas(result);
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
//        c.drawBitmap(original, 0, 0, null);
//        c.drawBitmap(mask, 0, 0, paint);
//        paint.setXfermode(null);
        return "file://" + file.getAbsolutePath();
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result = "";
        Cursor cursor = mContext.getContentResolver().query(contentURI, null, null, null, null);
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

    private String taggedThumbnailUri(String uri, String tag) {
        String thumbnailFilename = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
        int index = thumbnailFilename.lastIndexOf(".");
        String taggedFilename = thumbnailFilename.substring(0, index) + String.valueOf(tag) + thumbnailFilename.substring(index);
        return XoApplication.getThumbnailDirectory() + File.separator + taggedFilename;
    }

    private void displayImage(IContentObject contentObject) {
        if (contentObject.getContentDataUrl() == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(contentObject.getContentDataUrl()), "image/*");
        try {
            XoActivity activity = (XoActivity) mContext;
            activity.startExternalActivity(intent);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private String getThumbnailFileUriByContentUri(String contentUriString) {

        Uri contentUri = Uri.parse(contentUriString);
        long id = ContentUris.parseId(contentUri);

        String thumbnailUri = null;
        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(mContext.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            thumbnailUri = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            cursor.close();
        }

        if (thumbnailUri != null && !thumbnailUri.isEmpty()) {
            return "file://" + thumbnailUri;
        } else {
            return null;
        }
    }

}
