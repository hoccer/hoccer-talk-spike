package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;


public abstract class ChatItem {

    public static final int TYPE_RELATED = 0;
    public static final int TYPE_CLIENT_HISTORY = 1;
    public static final int TYPE_CLIENT_NEARBY_HISTORY = 2;
    public static final int TYPE_CLIENT_WORLDWIDE_HISTORY = 3;
    public static final int TYPE_GROUP_NEARBY_HISTORY = 4;
    public static final int TYPE_GROUP_WORLDWIDE_HISTORY = 5;

    protected long mUnseenMessageCount;

    private int mType;
    private int mLayout;

    public abstract void update();

    public View getView(View view, ViewGroup parent) {
        if (view == null || view.getTag() == null || (Integer) view.getTag() != getType()) {
            view = LayoutInflater.from(parent.getContext()).inflate(getLayout(), null);
            view.setTag(getType());
        }

        return updateView(view);
    }

    protected abstract View updateView(View view);

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
        if (!contact.isFriendOrBlocked() && contact.isNearbyAcquaintance()) {
            chatItem = createNearbyHistoryChatItem(contact, context);
        } else if (!contact.isFriendOrBlocked() && contact.isWorldwideAcquaintance()) {
            chatItem = createWorldwideHistoryChatItem(contact, context);
        } else if (contact.isClient() && !contact.isFriendOrBlocked() || contact.isKeptGroup()) {
            chatItem = createHistoryChatItem(contact, context);
        } else {
            chatItem = createContactChatItem(contact, context);
        }
        return chatItem;
    }

    public static ChatItem createNearbyGroupHistory() {
        ChatItem chatItem = new NearbyGroupHistoryChatItem();
        chatItem.setType(ChatItem.TYPE_GROUP_NEARBY_HISTORY);
        chatItem.setLayout(R.layout.item_chat_client);
        return chatItem;
    }

    public static ChatItem createWorldwideGroupHistory() {
        ChatItem chatItem = new WorldwideGroupHistoryChatItem();
        chatItem.setType(ChatItem.TYPE_GROUP_WORLDWIDE_HISTORY);
        chatItem.setLayout(R.layout.item_chat_client);
        return chatItem;
    }

    private static ChatItem createHistoryChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_CLIENT_HISTORY);
        chatItem.setLayout(R.layout.item_history_chat_client);
        return chatItem;
    }

    private static ChatItem createNearbyHistoryChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_CLIENT_NEARBY_HISTORY);
        chatItem.setLayout(R.layout.item_nearby_history_chat_client);
        return chatItem;
    }

    private static ChatItem createWorldwideHistoryChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_CLIENT_WORLDWIDE_HISTORY);
        chatItem.setLayout(R.layout.item_worldwide_history_chat_client);
        return chatItem;
    }

    private static ChatItem createContactChatItem(TalkClientContact contact, Context context) {
        ChatItem chatItem = new ContactChatItem(contact, context);
        chatItem.setType(ChatItem.TYPE_RELATED);
        chatItem.setLayout(R.layout.item_chat_client);
        return chatItem;
    }
}
