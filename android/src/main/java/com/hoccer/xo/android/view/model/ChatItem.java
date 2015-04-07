package com.hoccer.xo.android.view.model;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public abstract class ChatItem {

    public static final int TYPE_RELATED = 0;
    public static final int TYPE_CLIENT_NEARBY_HISTORY = 1;
    public static final int TYPE_GROUP_NEARBY_HISTORY = 2;

    protected long mUnseenMessageCount;

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
}
