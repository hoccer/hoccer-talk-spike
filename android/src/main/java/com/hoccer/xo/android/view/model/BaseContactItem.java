package com.hoccer.xo.android.view.model;

import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.release.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;


public abstract class BaseContactItem<T> {

    protected final XoActivity mXoActivity;

    public BaseContactItem(XoActivity activity) {
        mXoActivity = activity;
    }

    public abstract void update();

    public View getView(View view) {
        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) mXoActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_contact_client, null);
        }
        return configure(view);
    }

    protected abstract View configure(View view);

    public abstract T getContent();
}
