package com.hoccer.xo.android.view.chat.attachments;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;


public class ChatImageItem extends ChatMessageItem implements View.OnLayoutChangeListener {

    private static final double WIDTH_SCALE_FACTOR = 0.7;
    private static final double IMAGE_SCALE_FACTOR = 0.5;

    private Context mContext;
    private int mImageWidth;
    private ImageView mImageView;
    private int mMask;

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
        mImageView.removeOnLayoutChangeListener(this);
        Picasso.with(mContext).cancelRequest(mImageView);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

        LOG.error("Width: " + mImageView.getWidth() + " Height: " + mImageView.getHeight());
        Picasso.with(mContext).load(mContentObject.getContentDataUrl())
                .error(R.drawable.ic_img_placeholder_error)
                .resize((int) (mImageView.getWidth() * IMAGE_SCALE_FACTOR), (int) (mImageView.getHeight() * IMAGE_SCALE_FACTOR))
                .centerInside()
                .transform(new BubbleTransformation(mMask))
                .into(mImageView);

        mImageView.removeOnLayoutChangeListener(this);
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

        mImageView = (ImageView) mContentWrapper.findViewById(R.id.iv_image_view);
        mImageView.addOnLayoutChangeListener(this);

        double aspectRatio = contentObject.getContentAspectRatio();
        int height = (int) (mImageWidth / aspectRatio);

        mImageView.getLayoutParams().width = mImageWidth;
        mImageView.getLayoutParams().height = height;

        mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        RelativeLayout rootView = (RelativeLayout) mContentWrapper.findViewById(R.id.rl_root);
        if (mMessage.isIncoming()) {
            rootView.setGravity(Gravity.LEFT);
            mMask = R.drawable.chat_bubble_error_incoming;
        } else {
            rootView.setGravity(Gravity.RIGHT);
            mMask = R.drawable.chat_bubble_outgoing;
        }
    }

    private class BubbleTransformation implements Transformation {

        private int mask;

        public BubbleTransformation(int mask) {
            this.mask = mask;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            Bitmap mask = getNinePatchMask(this.mask, source.getWidth(), source.getHeight(), mContext);
            Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(result);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            c.drawBitmap(source, 0, 0, null);
            c.drawBitmap(mask, 0, 0, paint);

            paint.setXfermode(null);
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override
        public String key() {
            return String.valueOf(mask);
        }
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

