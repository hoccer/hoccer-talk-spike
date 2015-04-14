package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupPresence;


public abstract class ChatItem {

    public static final int TYPE_RELATED = 0;
    public static final int TYPE_CLIENT_HISTORY = 1;
    public static final int TYPE_CLIENT_NEARBY_HISTORY = 2;
    public static final int TYPE_GROUP_NEARBY_HISTORY = 3;

    protected long mUnseenMessageCount;

    private int mType;
    private int mLayout;

    public abstract void update();

    public abstract View getView(View view, ViewGroup parent);

    public abstract Object getContent();

    public abstract long getMessageTimeStamp();

    public abstract long getContactCreationTimeStamp();

    protected void setUnseenMessages(TextView unseenView) {
        if (mUnseenMessageCount <= 0) {
            unseenView.setVisibility(View.INVISIBLE);
        } else {
            unseenView.setText(Long.toString(mUnseenMessageCount));
            unseenView.setVisibility(View.VISIBLE);
        }
    }

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setLayout(int layout) {
        mLayout = layout;
    }

    public int getLayout() {
        return mLayout;
    }

    public static ChatItem create(TalkClientContact contact, Context context) {
        ChatItem chatItem;
        if (!contact.isFriendOrBlocked() && contact.getClientPresence().isNearbyAcquaintance()) {
            chatItem = createNearbyHistoryChatItem(contact, context);
        } else if (!contact.isFriendOrBlocked()) {
            chatItem = createHistoryChatItem(contact, context);
        } else {
            chatItem = createContactChatItem(contact, context);
        }
        return chatItem;
    }

    private static ChatItem createHistoryChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem;
        chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_CLIENT_HISTORY);
        chatItem.setLayout(R.layout.item_history_chat_client);
        return chatItem;
    }

    private static ChatItem createNearbyHistoryChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem;
        chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_CLIENT_NEARBY_HISTORY);
        chatItem.setLayout(R.layout.item_nearby_history_chat_client);
        return chatItem;
    }

    private static ChatItem createContactChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem;
        chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_RELATED);
        chatItem.setLayout(R.layout.item_chat_client);
        return chatItem;
    }

}
