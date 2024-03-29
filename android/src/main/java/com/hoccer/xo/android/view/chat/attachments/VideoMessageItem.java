package com.hoccer.xo.android.view.chat.attachments;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.FlavorBaseActivity;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.MessageItem;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class VideoMessageItem extends MessageItem {

    private final static Logger LOG = Logger.getLogger(VideoMessageItem.class);

    private String mThumbnailPath;
    private ImageView mTargetView;
    private MediaScannedReceiver mMediaScannedReceiver;

    public VideoMessageItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithVideo;
    }

    @Override
    protected void displayAttachment() {
        super.displayAttachment();

        // add view lazily
        if (mAttachmentContentContainer.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout videoLayout = (RelativeLayout) inflater.inflate(R.layout.content_video, null);
            mAttachmentContentContainer.addView(videoLayout);
        }

        ImageButton playButton = (ImageButton) mAttachmentContentContainer.findViewById(R.id.ib_play_button);
        playButton.setBackgroundResource(R.drawable.ic_light_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVideo(mAttachment);
            }
        });

        mAttachmentContentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVideo(mAttachment);
            }
        });

        // calc default view size
        double width_scale_factor = mSimpleAvatarView.getVisibility() == View.VISIBLE ? ImageMessageItem.WIDTH_AVATAR_SCALE_FACTOR : ImageMessageItem.WIDTH_SCALE_FACTOR;
        int maxWidth = (int) (DisplayUtils.getDisplaySize(mContext).x * width_scale_factor);
        int width = maxWidth;
        int maxHeight = (int) (DisplayUtils.getDisplaySize(mContext).y * ImageMessageItem.HEIGHT_SCALE_FACTOR);
        int height = maxHeight;

        // retrieve thumbnail path if not set already
        if (mThumbnailPath == null) {
            mThumbnailPath = retrieveThumbnailPath(UriUtils.getAbsoluteFileUri(mAttachment.getFilePath()));
        }

        // adjust width/height based on thumbnail size if it exists
        if (mThumbnailPath != null) {
            // calc image aspect ratio
            Point imageSize = ImageUtils.getImageSize(mThumbnailPath);
            double aspectRatio = (double) imageSize.x / (double) imageSize.y;
            Point boundImageSize = ImageUtils.getImageSizeInBounds(aspectRatio, maxWidth, maxHeight);
            width = boundImageSize.x;
            height = boundImageSize.y;
        } else {
            // register an intent listener in case the image is currently scanned and will be finished soon
            listenToMediaScannedIntent(true);
        }

        // register layout change listener and resize thumbnail view
        RelativeLayout rootView = (RelativeLayout) mAttachmentContentContainer.findViewById(R.id.rl_root);
        rootView.getLayoutParams().width = width;
        rootView.getLayoutParams().height = height;

        // set gravity and message bubble mask
        ImageView overlayView = (ImageView) mAttachmentContentContainer.findViewById(R.id.iv_picture_overlay);
        if (mMessage.isIncoming()) {
            mAttachmentContentContainer.setGravity(Gravity.LEFT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_incoming));
        } else {
            mAttachmentContentContainer.setGravity(Gravity.RIGHT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_outgoing));
        }

        mMessageContainer.setBackgroundDrawable(null);
        mMessageContainer.setPadding(0, 0, 0, 0);

        // load thumbnail with picasso
        mTargetView = (ImageView) rootView.findViewById(R.id.iv_picture);
        Picasso.with(mContext).setLoggingEnabled(XoApplication.getConfiguration().isDevelopmentModeEnabled());
        Picasso.with(mContext).load("file://" + mThumbnailPath)
                .error(R.drawable.ic_img_placeholder)
                .resize((int) (width * ImageMessageItem.IMAGE_SCALE_FACTOR), (int) (height * ImageMessageItem.IMAGE_SCALE_FACTOR))
                .centerInside()
                .into(mTargetView);
        LOG.trace(Picasso.with(mContext).getSnapshot().toString());
    }

    private void listenToMediaScannedIntent(boolean doListen) {
        if (doListen) {
            if (mMediaScannedReceiver == null) {
                IntentFilter filter = new IntentFilter(MediaScannedReceiver.class.getName());
                filter.addAction(IntentHelper.ACTION_DOWNLOAD_SCANNED);
                mMediaScannedReceiver = new MediaScannedReceiver();
                mContext.registerReceiver(mMediaScannedReceiver, filter);
            }
        } else {
            if (mMediaScannedReceiver != null) {
                mContext.unregisterReceiver(mMediaScannedReceiver);
                mMediaScannedReceiver = null;
            }
        }
    }

    @Override
    public void detachView() {
        super.detachView();
        // cancel image loading if in case display attachment has been called
        if (mTargetView != null) {
            Picasso.with(mContext).cancelRequest(mTargetView);
        }
        listenToMediaScannedIntent(false);
    }

    /*
     * Checks whether a thumbnail image file for the given video exists in the hoccer thumbnail directory.
     * If it does its uri is returned.
     * If it does not the method tries to retrieve the video id from media store and the thumbnail as bitmap.
     * This bitmap is then stored as thumbnail file in the thumbnail directory.
     * Returns null if the video cannot be found in the media store database.
     */
    private String retrieveThumbnailPath(Uri videoUri) {
        String videoFilename = videoUri.getLastPathSegment();
        String thumbnailFileName = videoFilename + "_mini.jpg";
        Uri thumbnailDestination = Uri.parse(XoApplication.getThumbnailDirectory() + File.separator + thumbnailFileName);

        // return thumbnail path if the thumbnail file already exists
        File file = new File(thumbnailDestination.toString());
        if (file.exists()) {
            return thumbnailDestination.toString();
        }

        // try to create the video thumbnail
        if (createVideoThumbnail(videoUri, thumbnailDestination)) {
            return thumbnailDestination.toString();
        } else {
            return null;
        }
    }

    private boolean createVideoThumbnail(Uri videoUri, Uri thumbnailUri) {
        long videoId = UriUtils.getContentIdByDataPath(mContext, MediaStore.Video.Media.getContentUri("external"), videoUri.getPath());
        if (videoId > 0) {
            Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(mContext.getContentResolver(), videoId, MediaStore.Video.Thumbnails.MINI_KIND, new BitmapFactory.Options());
            if (thumbnail != null) {
                try {
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(thumbnailUri.toString()));
                    return true;
                } catch (FileNotFoundException e) {
                    LOG.error("Error while saving thumbnail bitmap: " + thumbnailUri, e);
                }
            }
        }

        return false;
    }

    private void openVideo(XoTransfer attachment) {
        if (attachment.isContentAvailable()) {
            Uri videoUri = UriUtils.getAbsoluteFileUri(attachment.getFilePath());
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(videoUri, "video/*");
                FlavorBaseActivity activity = (FlavorBaseActivity) mContext;
                activity.startExternalActivity(intent);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(mContext, R.string.error_no_videoplayer, Toast.LENGTH_LONG).show();
                LOG.error("Exception while starting external activity ", exception);
            }
        }
    }

    private class MediaScannedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {

            // call displayAttachment assuming that Android has scanned it and can generate a thumbnail
            Uri scannedFileUri = intent.getParcelableExtra(IntentHelper.EXTRA_ATTACHMENT_FILE_URI);
            Uri videoFileUri = UriUtils.getAbsoluteFileUri(mAttachment.getFilePath());
            if (scannedFileUri.equals(videoFileUri)) {
                displayAttachment();
                listenToMediaScannedIntent(false);
            }
        }
    }
}
