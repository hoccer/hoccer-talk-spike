package com.hoccer.xo.android.view.chat.attachments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoConfiguration;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class ChatVideoItem extends ChatMessageItem implements View.OnLayoutChangeListener {

    private static final double WIDTH_SCALE_FACTOR = 0.8;
    private static final double HEIGHT_SCALE_FACTOR = 0.7;

    private RelativeLayout mRootView;
    private String mThumbnailPath;

    public ChatVideoItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithVideo;
    }

    @Override
    protected void configureViewForMessage(View view) {
        super.configureViewForMessage(view);
        configureAttachmentViewForMessage(view);
    }

    @Override
    protected void displayAttachment(final IContentObject contentObject) {
        super.displayAttachment(contentObject);

        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout videoLayout = (RelativeLayout) inflater.inflate(R.layout.content_video, null);
            mContentWrapper.addView(videoLayout);
        }

        ImageButton playButton = (ImageButton) mContentWrapper.findViewById(R.id.ib_play_button);
        playButton.setBackgroundResource(R.drawable.ic_light_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVideo(contentObject);
            }
        });

        mAttachmentView.setPadding(0, 0, 0, 0);
        mAttachmentView.setBackgroundDrawable(null);

        // calc default view size
        int maxWidth = (int) (DisplayUtils.getDisplaySize(mContext).x * WIDTH_SCALE_FACTOR);
        int width = maxWidth;
        int maxHeight = (int) (DisplayUtils.getDisplaySize(mContext).y * HEIGHT_SCALE_FACTOR);
        int height = maxHeight;

        if(mThumbnailPath == null) {
            long videoId = getVideoId(contentObject.getContentDataUrl());
            if (videoId > 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(mContext.getContentResolver(), videoId, MediaStore.Video.Thumbnails.MINI_KIND, options);
                mThumbnailPath = saveToThumbnailDirectory(thumbnail, contentObject.getContentDataUrl().substring(contentObject.getContentDataUrl().lastIndexOf(File.separator) + 1));
            }
        }

        // adjust width/height based on thumbnail size
        if(mThumbnailPath != null) {
            // calc image aspect ratio
            Point imageSize = getImageSize(mThumbnailPath);
            double aspectRatio = (double) imageSize.x / (double) imageSize.y;

            // calc view size according to aspect ratio
            if(aspectRatio > 1.0) {
                height = (int) (width / aspectRatio);
                if(height > maxHeight) {
                    width = (int)(maxHeight * aspectRatio);
                    height = maxHeight;
                }
            } else {
                width = (int)(maxHeight * aspectRatio);
                if(width > maxWidth) {
                    width = maxWidth;
                    height = (int)(maxWidth / aspectRatio);
                }
            }
        }

        // register layout change listener and resize thumbnail view
        mRootView = (RelativeLayout) mContentWrapper.findViewById(R.id.rl_root);
        mRootView.getLayoutParams().width = width;
        mRootView.getLayoutParams().height = height;
        mRootView.addOnLayoutChangeListener(this);
        mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVideo(contentObject);
            }
        });

        ImageView overlayView = (ImageView) mContentWrapper.findViewById(R.id.iv_picture_overlay);
        if (mMessage.isIncoming()) {
            mContentWrapper.setGravity(Gravity.LEFT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_incoming));
        } else {
            mContentWrapper.setGravity(Gravity.RIGHT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_outgoing));
        }
    }

    @Override
    public void detachView() {
        LOG.debug("Detach view for: " + "file://" + (mThumbnailPath == null ? "null" : mThumbnailPath));
        // check for null in case display attachment has not yet been called
        if (mRootView != null) {
            mRootView.removeOnLayoutChangeListener(this);
            ImageView targetView = (ImageView) mRootView.findViewById(R.id.iv_picture);
            if (targetView != null) {
                Picasso.with(mContext).cancelRequest(targetView);
            }
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        LOG.debug("Picasso loads: " + "file://" + (mThumbnailPath == null ? "null" : mThumbnailPath));
        ImageView targetView = (ImageView) v.findViewById(R.id.iv_picture);
        Picasso.with(mContext).setLoggingEnabled(XoConfiguration.DEVELOPMENT_MODE_ENABLED);
        Picasso.with(mContext).load("file://" + mThumbnailPath)
                .error(R.drawable.ic_img_placeholder_error)
                .resize(targetView.getWidth(), targetView.getHeight())
                .centerInside()
                .into(targetView);
        mRootView.removeOnLayoutChangeListener(this);
    }


    private void openVideo(IContentObject contentObject) {
        if (contentObject.isContentAvailable()) {
            String url = contentObject.getContentUrl();
            if (url == null) {
                url = contentObject.getContentDataUrl();
            }
            if (url != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(url), "video/*");
                    XoActivity activity = (XoActivity) mContext;
                    activity.startExternalActivity(intent);
                } catch (ActivityNotFoundException exception) {
                    Toast.makeText(mContext, R.string.error_no_videoplayer, Toast.LENGTH_LONG).show();
                    LOG.error("Exception while starting external activity ", exception);
                }
            }
        }
    }

    // private String getThumbnailUri(String videoUri) {
    //    if(mThumbnailUri != null) {
    //        return mThumbnailUri;
    //    }


        //String thumbnailName = videoUri.substring(videoUri.lastIndexOf(File.separator) + 1) + ".png";
        //String thumbnailPath = XoApplication.getThumbnailDirectory() + File.separator + thumbnailName;


    //    String[] projection = new String[] {
    //            MediaStore.Video.Media._ID, // 0
    //            MediaStore.Video.Media.DATA, // 1 from android.provider.MediaStore.Video

    //    };

        //Uri contentUri = MediaStore.Video.Thumbnails.getContentUri("external");

        //long videoId = getVideoThumbnailUri(videoUri);
        //Uri dataUri = Uri.withAppendedPath(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, String.valueOf(videoId));

//        String thumbFilePath = null;
//        Cursor c = mContext.getContentResolver().query(dataUri, projection, null, null, null);
//        if ((c != null) && c.moveToFirst()) {
//            thumbFilePath = c.getString(1);
//        }

//        File file = mContext.getFileStreamPath(thumbnailPath);
//        if (!file.exists()) {
//            Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(getRealPathFromURI(Uri.parse(videoUri)), MediaStore.Images.Thumbnails.MINI_KIND);
//            try {
//                thumbnail.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(thumbnailPath));
//            } catch (FileNotFoundException e) {
//                LOG.error("Error while saving thumbnail bitmap: " + thumbnailPath, e);
//            }
//        }

    //    return Uri.parse(thumbFilePath).getPath();
    //}

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

    private long getVideoId(String videoPath) {
        long videoId = 0;

        Uri videosUri = MediaStore.Video.Media.getContentUri("external");
        String[] projection = {
                MediaStore.Video.VideoColumns._ID
        };
        Cursor cursor = mContext.getContentResolver().query(videosUri, projection, MediaStore.Video.VideoColumns.DATA + " LIKE ?", new String[]{videoPath.substring(7)}, null);

        // if we have found a database entry for the video file
        if (cursor.moveToFirst()) {
            videoId = cursor.getLong(0);
        }
        cursor.close();

        return videoId;
    }

    private String getVideoThumbnailUri(String videoPath) {
        long videoId = 0;

        Uri videosUri = MediaStore.Video.Media.getContentUri("external");
        String[] projection = {MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA};
        Cursor cursor = mContext.getContentResolver().query(videosUri, projection, MediaStore.Video.VideoColumns.DATA + " LIKE ?", new String[] {videoPath.substring(7)}, null);

        // if we have found a database entry for the video file
        if(cursor.moveToFirst()) {
            videoId = cursor.getLong(0);
        }
        cursor.close();

        // retrieve thumbnail for video
        String thumbnailUri = "";
        if(videoId > 0) {
            Uri thumbnailsUri = MediaStore.Video.Thumbnails.getContentUri("external");
            String[] projection2 = {MediaStore.Video.Thumbnails.DATA};
            Cursor cursor2 = mContext.getContentResolver().query(thumbnailsUri, projection2, MediaStore.Video.Thumbnails.VIDEO_ID + " LIKE ?", new String[]{String.valueOf(videoId)}, null);
            if(cursor2.moveToFirst()) {
                thumbnailUri = cursor2.getString(0);
            }
            cursor2.close();
        }

        return thumbnailUri;
    }

    private String saveToThumbnailDirectory(Bitmap bitmap, String filename) {
        String destination = XoApplication.getThumbnailDirectory() + File.separator + filename + "_mini.jpg";
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(destination));
        } catch (FileNotFoundException e) {
            LOG.error("Error while saving thumbnail bitmap: " + destination, e);
        }

        return destination;
    }

    private Point getImageSize(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        return new Point(options.outWidth, options.outHeight);
    }
}
