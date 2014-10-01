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
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoConfiguration;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


public class ChatImageItem extends ChatMessageItem {

    public static final double HEIGHT_SCALE_FACTOR = 0.6;
    public static final double WIDTH_SCALE_FACTOR = 0.85;
    public static final double WIDTH_AVATAR_SCALE_FACTOR = 0.7;
    public static final double IMAGE_SCALE_FACTOR = 0.5;

    private RelativeLayout mRootView;

    public ChatImageItem(Context context, TalkClientMessage message) {
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
    protected void displayAttachment(final IContentObject contentObject) {
        super.displayAttachment(contentObject);

        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout imageLayout = (RelativeLayout) inflater.inflate(R.layout.content_image, null);
            mContentWrapper.addView(imageLayout);
        }

        mContentWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage(contentObject);
            }
        });


        // calc view size
        double widthScaleFactor = mAvatarView.getVisibility() == View.VISIBLE ? WIDTH_AVATAR_SCALE_FACTOR : WIDTH_SCALE_FACTOR;
        int maxWidth = (int) (DisplayUtils.getDisplaySize(mContext).x * widthScaleFactor);
        int maxHeight = (int) (DisplayUtils.getDisplaySize(mContext).y * HEIGHT_SCALE_FACTOR);
        double aspectRatio = contentObject.getContentAspectRatio();
        Point boundImageSize = ImageUtils.getImageSizeInBounds(aspectRatio, maxWidth, maxHeight);
        int width = boundImageSize.x;
        int height = boundImageSize.y;

        mRootView = (RelativeLayout) mContentWrapper.findViewById(R.id.rl_root);
        mRootView.getLayoutParams().width = width;
        mRootView.getLayoutParams().height = height;

        ImageView overlayView = (ImageView) mRootView.findViewById(R.id.iv_picture_overlay);
        if (mMessage.isIncoming()) {
            mContentWrapper.setGravity(Gravity.LEFT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_incoming));
        } else {
            mContentWrapper.setGravity(Gravity.RIGHT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_outgoing));
        }

        mRootView.setBackgroundDrawable(mAttachmentView.getBackground());
        mRootView.setPadding(0, 0, 0, 0);

        mAttachmentView.setBackgroundDrawable(null);
        mAttachmentView.setPadding(0, 0, 0, 0);

        final View imageTextView = mRootView.findViewById(R.id.tv_image_preview_error);
        imageTextView.setVisibility(View.GONE);

        ImageView targetView = (ImageView) mRootView.findViewById(R.id.iv_picture);
        Picasso.with(mContext).setLoggingEnabled(XoConfiguration.DEVELOPMENT_MODE_ENABLED);
        Picasso.with(mContext).load(mContentObject.getContentDataUrl())
                .resize((int) (width * IMAGE_SCALE_FACTOR), (int) (height * IMAGE_SCALE_FACTOR))
                .error(R.drawable.ic_img_placeholder)
                .centerInside()
                .into(targetView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        imageTextView.setVisibility(View.VISIBLE);
                    }
                });
        LOG.trace(Picasso.with(mContext).getSnapshot().toString());
    }

    @Override
    public void detachView() {
        // check for null in case display attachment has not yet been called
        if (mRootView != null) {
            ImageView targetView = (ImageView) mRootView.findViewById(R.id.iv_picture);
            if (targetView != null) {
                Picasso.with(mContext).cancelRequest(targetView);
            }
        }
    }

    private void openImage(IContentObject contentObject) {
        if (contentObject.getContentDataUrl() == null && contentObject.getContentUrl() == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri dataUri = Uri.parse(contentObject.getContentUrl() != null && !contentObject.getContentUrl().isEmpty() ?
                contentObject.getContentUrl() : contentObject.getContentDataUrl());
        intent.setDataAndType(dataUri, "image/*");
        try {
            XoActivity activity = (XoActivity) mContext;
            activity.startExternalActivity(intent);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }
}

