package com.hoccer.xo.android.view.chat.attachments;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.view.chat.MessageItem;


public class DataMessageItem extends MessageItem {

    private ImageButton mOpenFileButton;

    public DataMessageItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithData;
    }

    @Override
    protected void displayAttachment() {
        super.displayAttachment();

        // add view lazily
        if (mAttachmentContentContainer.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.content_file, null);
            mAttachmentContentContainer.addView(v);
        }
        LinearLayout audioLayout = (LinearLayout) mAttachmentContentContainer.getChildAt(0);
        TextView nameTextView = (TextView) audioLayout.findViewById(R.id.tv_content_file_name);
        mOpenFileButton = (ImageButton) audioLayout.findViewById(R.id.ib_content_file_open);

        if (mMessage.isIncoming()) {
            nameTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
        } else {
            nameTextView.setTextColor(mContext.getResources().getColor(R.color.compose_message_text));
        }
    }
}
