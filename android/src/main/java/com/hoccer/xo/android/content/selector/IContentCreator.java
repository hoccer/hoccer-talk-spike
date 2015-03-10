package com.hoccer.xo.android.content.selector;


import android.content.Context;
import android.content.Intent;
import com.hoccer.talk.content.SelectedContent;

import java.io.FileNotFoundException;

public interface IContentCreator {
    public SelectedContent apply(Context context, Intent intent) throws Exception;
}
