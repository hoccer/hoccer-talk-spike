package com.hoccer.talk.android.content;

import android.content.Context;
import android.view.View;

public interface IContentViewer {

    public abstract boolean canViewObject(ContentObject object);

    public abstract View getViewForObject(Context context, ContentObject object, ContentView view);

}
