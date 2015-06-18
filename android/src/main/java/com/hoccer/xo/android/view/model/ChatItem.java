package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.view.avatar.AvatarView;


public abstract class ChatItem {

    protected long mUnseenMessageCount;

    protected AvatarView mAvatarView;

    protected final Context mContext;

    protected ChatItem(Context context) {
        mContext = context;
    }

    public abstract void update();

    public View getView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_client, null);
        }

        AvatarView avatarView = (AvatarView) convertView.findViewById(R.id.avatar);
        int layoutId;
        if (avatarView == null) {
            ViewStub avatarStub = (ViewStub) convertView.findViewById(R.id.vs_avatar);
            layoutId = getAvatarLayout();
            avatarStub.setLayoutResource(layoutId);
            mAvatarView = (AvatarView) avatarStub.inflate();
            mAvatarView.setTag(layoutId);
        } else if (mAvatarView == null || mAvatarView.getTag() != avatarView.getTag()) {
            ViewGroup viewGroup = (ViewGroup) convertView.findViewById(R.id.avatar_container);
            viewGroup.removeView(avatarView);

            layoutId = getAvatarLayout();
            mAvatarView = (AvatarView) LayoutInflater.from(convertView.getContext()).inflate(layoutId, viewGroup, false);
            viewGroup.addView(mAvatarView);

            mAvatarView.setTag(layoutId);
        }

        return updateView(convertView);
    }

    protected abstract int getAvatarLayout();

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
}
