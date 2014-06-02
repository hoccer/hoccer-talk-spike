package com.hoccer.xo.android.view.chat.attachements;

import android.content.Context;
import android.view.View;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.view.chat.ChatMessageItem;


public class ChatImageItem extends ChatMessageItem {

    public ChatImageItem(Context context) {
        super(context);
    }

    @Override
    protected void configureViewForMessage(View view, TalkClientMessage message) {
        super.configureViewForMessage(view, message);

        configureAttachmentViewForMessage(view, message);
    }

    @Override
    protected void displayAttachment() {

    }
}
