package com.hoccer.xo.android.content;

import com.hoccer.talk.client.model.TalkClientDownload;
import com.j256.ormlite.field.DatabaseField;

import java.util.List;

/**
 * Encapsulates a list of TalkClientDownload references.
 */
public class MediaCollection {

    private String mName;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private List<TalkClientDownload> mItems;

    public MediaCollection(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public int size() {
        return mItems.size();
    }

    public TalkClientDownload get(int index) {
        return mItems.get(index);
    }
}
