package com.hoccer.xo.android.view.model;

import android.view.ViewGroup;
import android.widget.TextView;
import com.hoccer.xo.android.base.XoActivity;
import com.artcom.hoccer.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Date;


public abstract class ChatItem {

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
