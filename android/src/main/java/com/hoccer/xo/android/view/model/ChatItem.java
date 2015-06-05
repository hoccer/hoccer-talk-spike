package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;


public abstract class ChatItem {

    public static final int TYPE_RELATED = 0;
    public static final int TYPE_CLIENT_PRESENCE_NEARBY = 1;
    public static final int TYPE_CLIENT_PRESENCE_WORLDWIDE = 2;
    public static final int TYPE_CLIENT_KEPT = 3;
    public static final int TYPE_CLIENT_ACQUAINTANCE_NEARBY = 4;
    public static final int TYPE_CLIENT_ACQUAINTANCE_WORLDWIDE = 5;
    public static final int TYPE_GROUP_WORLDWIDE = 6;
    public static final int TYPE_GROUP_HISTORY_WORLDWIDE = 7;
    public static final int TYPE_GROUP_HISTORY_NEARBY = 8;

    protected long mUnseenMessageCount;

    private int mType;
    private int mLayout;
    private int mAvatarView;

    protected final Context mContext;

    protected ChatItem(Context context) {
        mContext = context;
    }

    public abstract void update();

    public View getView(View view, ViewGroup parent) {
        if (view == null || view.getTag() == null || (Integer) view.getTag() != getType()) {
            view = LayoutInflater.from(parent.getContext()).inflate(getLayout(), null);
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

    public void setAvatarView(int avatarView) {
        mAvatarView = avatarView;
    }

    public int getAvatarView() {
        return mAvatarView;
    }

    public static ChatItem create(TalkClientContact contact, Context context) {
        ChatItem chatItem = new ContactChatItem(contact, context);
        chatItem.setLayout(R.layout.item_chat_client);

        if (!contact.isFriendOrBlocked() && contact.isWorldwide()) {
            chatItem.setType(ChatItem.TYPE_CLIENT_PRESENCE_WORLDWIDE);
            chatItem.setAvatarView(R.layout.view_avatar_client_presence_worldwide);
        } else if (!contact.isFriendOrBlocked() && contact.isNearby()) {
            chatItem.setType(ChatItem.TYPE_CLIENT_PRESENCE_NEARBY);
            chatItem.setAvatarView(R.layout.view_avatar_client_presence_nearby);
        } else if (!contact.isFriendOrBlocked() && contact.isNearbyAcquaintance()) {
            chatItem.setType(ChatItem.TYPE_CLIENT_ACQUAINTANCE_NEARBY);
            chatItem.setAvatarView(R.layout.view_avatar_client_acquaintance_nearby);
        } else if (!contact.isFriendOrBlocked() && contact.isWorldwideAcquaintance()) {
            chatItem.setType(ChatItem.TYPE_CLIENT_ACQUAINTANCE_WORLDWIDE);
            chatItem.setAvatarView(R.layout.view_avatar_client_acquaintance_worldwide);
        } else if (!contact.isFriendOrBlocked() && contact.isKept() || contact.isKeptGroup()) {
            chatItem.setType(ChatItem.TYPE_CLIENT_KEPT);
            chatItem.setAvatarView(R.layout.view_avatar_client_kept);
        } else if (contact.isWorldwideGroup()) {
            chatItem.setType(ChatItem.TYPE_GROUP_WORLDWIDE);
            chatItem.setAvatarView(R.layout.view_avatar_group_worldwide);
        } else {
            chatItem.setType(ChatItem.TYPE_RELATED);
            chatItem.setAvatarView(R.layout.view_avatar_presence);
        }

        return chatItem;
    }

    public static ChatItem createNearbyGroupHistory(Context context) {
        ChatItem chatItem = new NearbyGroupHistoryChatItem(context);
        chatItem.setType(ChatItem.TYPE_GROUP_HISTORY_NEARBY);
        chatItem.setLayout(R.layout.item_chat_client);
        chatItem.setAvatarView(R.layout.view_avatar_simple);
        return chatItem;
    }

    public static ChatItem createWorldwideGroupHistory(Context context) {
        ChatItem chatItem = new WorldwideGroupHistoryChatItem(context);
        chatItem.setType(ChatItem.TYPE_GROUP_HISTORY_WORLDWIDE);
        chatItem.setLayout(R.layout.item_chat_client);
        chatItem.setAvatarView(R.layout.view_avatar_simple);
        return chatItem;
    }
}
