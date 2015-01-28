package com.hoccer.xo.android.content.selector;


import android.content.Context;
import android.content.Intent;
import com.hoccer.talk.content.SelectedAttachment;

public interface IContentCreator {
    public SelectedAttachment apply(Context context, Intent intent);
}
