package com.hoccer.xo.android.view.chat.attachments;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.view.*;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;


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
        String messageTag = (mMessage.getMessageId() != null) ? mMessage.getMessageId() : mMessage.getMessageTag();
        if (mMessage.isIncoming()) {
            rootView.setGravity(Gravity.LEFT);
            mask = R.drawable.chat_bubble_incoming;
        } else {
            rootView.setGravity(Gravity.RIGHT);
            mask = R.drawable.chat_bubble_outgoing;
        }
        if (contentObject.getContentDataUrl() != null) {
            mAttachmentView.setBackgroundDrawable(null);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(contentObject.getContentDataUrl(), options);

            double aspectRatio = contentObject.getContentAspectRatio();

            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = (int) (size.x * 0.8);
            int height = (int) (width / aspectRatio);

//            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mask);
//            byte[] chunk = bitmap.getNinePatchChunk();
//            NinePatchDrawable drawable = new NinePatchDrawable(mContext.getResources(), bitmap, chunk, new Rect(), null);
//            drawable.setBounds(0, 0, width, height);

            Picasso.with(mContext).setIndicatorsEnabled(XoApplication.getConfiguration().isDevelopmentModeEnabled());
            Picasso.with(mContext)
                    .load(contentObject.getContentDataUrl())
//                    .placeholder(drawable)
                    .resize(width, height)
                    .transform(new BubbleTransformation(mask))
                    .into(imageView);

//            ThumbnailManager.getInstance(mContext).displayThumbnailForImage(contentObject.getContentDataUrl(), imageView, mask, messageTag);
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
            return "square()";
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

}
