package com.hoccer.xo.android.content.selector;


import android.content.Context;
import android.content.Intent;
import com.hoccer.xo.android.content.SelectedContent;

public interface IContentCreator {
    public SelectedContent apply(Context context, Intent intent);
}
