package com.hoccer.xo.android.view.chat.attachments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.MessageItem;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;


public class ImageMessageItem extends MessageItem {

    private final static Logger LOG = Logger.getLogger(ImageMessageItem.class);

    public static final double HEIGHT_SCALE_FACTOR = 0.6;
    public static final double WIDTH_SCALE_FACTOR = 0.85;
    public static final double WIDTH_AVATAR_SCALE_FACTOR = 0.7;
    public static final double IMAGE_SCALE_FACTOR = 0.5;

    private ImageView mTargetView;

    public ImageMessageItem(Context context, TalkClientMessage message) {
        super(context, message);
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
    protected void displayAttachment(final XoTransfer attachment) {
        super.displayAttachment(attachment);

        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout imageLayout = (RelativeLayout) inflater.inflate(R.layout.content_image, null);
            mContentWrapper.addView(imageLayout);
        }

        mContentWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage(attachment);
            }
        });

        // calc view size
        double widthScaleFactor = mSimpleAvatarView.getVisibility() == View.VISIBLE ? WIDTH_AVATAR_SCALE_FACTOR : WIDTH_SCALE_FACTOR;
        int maxWidth = (int) (DisplayUtils.getDisplaySize(mContext).x * widthScaleFactor);
        int maxHeight = (int) (DisplayUtils.getDisplaySize(mContext).y * HEIGHT_SCALE_FACTOR);
        double aspectRatio = attachment.getContentAspectRatio();
        Point boundImageSize = ImageUtils.getImageSizeInBounds(aspectRatio, maxWidth, maxHeight);
        int width = boundImageSize.x;
        int height = boundImageSize.y;

        RelativeLayout rootView = (RelativeLayout) mContentWrapper.findViewById(R.id.rl_root);
        rootView.getLayoutParams().width = width;
        rootView.getLayoutParams().height = height;

        // set gravity and message bubble mask
        ImageView overlayView = (ImageView) rootView.findViewById(R.id.iv_picture_overlay);
        if (mMessage.isIncoming()) {
            mContentWrapper.setGravity(Gravity.LEFT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_incoming));
        } else {
            mContentWrapper.setGravity(Gravity.RIGHT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_outgoing));
        }

        // we need to copy the background to rootview which will have the correct bubble size
        rootView.setBackgroundDrawable(mAttachmentView.getBackground());
        rootView.setPadding(0, 0, 0, 0);
        mAttachmentView.setBackgroundDrawable(null);
        mAttachmentView.setPadding(0, 0, 0, 0);

        mTargetView = (ImageView) rootView.findViewById(R.id.iv_picture);
        Picasso.with(mContext).setLoggingEnabled(XoApplication.getConfiguration().isDevelopmentModeEnabled());

        Uri imageUri = UriUtils.getAbsoluteFileUri(attachment.getFilePath());
        Picasso.with(mContext).load(imageUri)
                .error(R.drawable.ic_img_placeholder)
                .resize((int) (width * IMAGE_SCALE_FACTOR), (int) (height * IMAGE_SCALE_FACTOR))
                .centerInside()
                .into(mTargetView);
        LOG.trace(Picasso.with(mContext).getSnapshot().toString());
    }

    @Override
    public void detachView() {
        // cancel image loading if in case display attachment has been called
        if (mTargetView != null) {
            Picasso.with(mContext).cancelRequest(mTargetView);
        }
    }

    private void openImage(XoTransfer transfer) {
        if (transfer.isContentAvailable()) {
            Uri imageUri = UriUtils.getAbsoluteFileUri(transfer.getFilePath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(imageUri, "image/*");
            try {
                XoActivity activity = (XoActivity) mContext;
                activity.startExternalActivity(intent);
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }
}

