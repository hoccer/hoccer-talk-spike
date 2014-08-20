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
            ThumbnailManager.getInstance(mContext).displayThumbnailForImage(contentObject.getContentDataUrl(), imageView, mask, messageTag);
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

}
