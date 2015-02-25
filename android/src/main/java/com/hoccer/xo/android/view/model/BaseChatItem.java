package com.hoccer.xo.android.view.model;

import android.view.ViewGroup;
import android.widget.TextView;
import com.hoccer.xo.android.base.XoActivity;
import com.artcom.hoccer.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Date;


public abstract class BaseChatItem<T> {

    protected long mUnseenMessageCount;

    public abstract void update();

    public View getView(View view, ViewGroup parent) {
        if(view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_client, null);
        }
        return configure(parent.getContext(), view);
    }

    protected abstract View configure(Context context, View view);

    public abstract T getContent();

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
