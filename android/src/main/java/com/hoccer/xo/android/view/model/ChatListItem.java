package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.view.avatar.AvatarView;


public abstract class ChatListItem {

    protected long mUnseenMessageCount;

    protected AvatarView mAvatarView;

    protected final Context mContext;

    protected ChatListItem(Context context) {
        mContext = context;
    }

    public abstract void update();

    public View getView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_client, null);
        }

        updateAvatarView(convertView);

        return updateView(convertView);
    }

    private void updateAvatarView(View convertView) {
        AvatarView avatarView = (AvatarView) convertView.findViewById(R.id.avatar);
        if (avatarView == null) {
            ViewStub avatarStub = (ViewStub) convertView.findViewById(R.id.vs_avatar);

            int layoutId = getAvatarLayout();
            avatarStub.setLayoutResource(layoutId);

            avatarView = (AvatarView) avatarStub.inflate();
            avatarView.setTag(layoutId);
        } else if (hasAvatarTypeChanged(avatarView)) {
            ViewGroup viewGroup = (ViewGroup) convertView.findViewById(R.id.avatar_container);
            viewGroup.removeView(avatarView);

            int layoutId = getAvatarLayout();
            avatarView = (AvatarView) LayoutInflater.from(convertView.getContext()).inflate(layoutId, viewGroup, false);
            viewGroup.addView(avatarView);

            avatarView.setTag(layoutId);
        }

        mAvatarView = avatarView;
    }

    private boolean hasAvatarTypeChanged(AvatarView avatarView) {
        return (Integer) avatarView.getTag() != getAvatarLayout();
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
