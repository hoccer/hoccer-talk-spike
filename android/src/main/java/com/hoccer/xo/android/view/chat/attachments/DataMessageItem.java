package com.hoccer.xo.android.view.chat.attachments;

import android.content.Context;
import android.view.View;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.view.chat.MessageItem;


public class DataMessageItem extends MessageItem {

    public DataMessageItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithData;
    }

    @Override
    protected void configureViewForMessage(View view) {
        super.configureViewForMessage(view);
        configureAttachmentViewForMessage(view);
    }

    @Override
    protected void displayAttachment(XoTransfer attachment) {
        super.displayAttachment(attachment);
    }
}
