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
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;


public class ChatImageItem extends ChatMessageItem implements View.OnLayoutChangeListener {

    private static final double WIDTH_SCALE_FACTOR = 0.8;
    private static final double IMAGE_SCALE_FACTOR = 0.3;

    private Context mContext;
    private int mImageWidth;
    private RelativeLayout mRootView;

    public ChatImageItem(Context context, TalkClientMessage message) {
        super(context, message);
        mContext = context;

        setRequiredImageWidth();
    }

    public ChatItemType getType() {
        return ChatItemType.ChatItemWithImage;
    }

    @Override
    public void detachView() {
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
        ImageView targetView = (ImageView) v.findViewById(R.id.iv_picture);
        Picasso.with(mContext).load(mContentObject.getContentDataUrl())
                .error(R.drawable.ic_img_placeholder_error)
                .resize((int) (targetView.getWidth() * IMAGE_SCALE_FACTOR), (int) (targetView.getHeight() * IMAGE_SCALE_FACTOR))
                .centerInside()
                .into(targetView);
        v.removeOnLayoutChangeListener(this);
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

        mAttachmentView.setBackgroundDrawable(null);

        double aspectRatio = contentObject.getContentAspectRatio();
        int height = (int) (mImageWidth / aspectRatio);

        mRootView = (RelativeLayout) mContentWrapper.findViewById(R.id.rl_root);
        mRootView.addOnLayoutChangeListener(this);
        mRootView.getLayoutParams().width = mImageWidth;
        mRootView.getLayoutParams().height = height;

        ImageView overlayView = (ImageView) mRootView.findViewById(R.id.iv_picture_overlay);

        if (mMessage.isIncoming()) {
            mRootView.setGravity(Gravity.LEFT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_incoming));
        } else {
            mRootView.setGravity(Gravity.RIGHT);
            overlayView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.chat_bubble_inverted_outgoing));
        }
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

    private void setRequiredImageWidth() {
        Point size = DisplayUtils.getDisplaySize(mContext);
        mImageWidth = (int) (size.x * WIDTH_SCALE_FACTOR);
    }
}

