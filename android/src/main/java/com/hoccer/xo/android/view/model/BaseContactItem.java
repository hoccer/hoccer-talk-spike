package com.hoccer.xo.android.view.model;

import android.view.ViewGroup;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.release.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;


public abstract class BaseContactItem<T> {


    public abstract void update();

    public View getView(View view, ViewGroup parent) {
        if(view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_client, null);
        }
        return configure(parent.getContext(), view);
    }

    protected abstract View configure(Context context, View view);

    public abstract T getContent();

    public abstract long getTimeStamp();
}
