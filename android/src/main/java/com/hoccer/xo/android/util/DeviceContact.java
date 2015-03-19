package com.hoccer.xo.android.util;

import java.util.ArrayList;

/**
 * Holds device contact data and a list of generic data item strings.
 */
public class DeviceContact {

    private final String mLookupKey;

    private final String mDisplayName;

    private String mThumbnailUri = "";

    private final ArrayList<String> mDataItems = new ArrayList<String>();

    public DeviceContact(String lookupKey, String displayName) {
        mLookupKey = lookupKey;
        mDisplayName = displayName;
    }

    public String getLookupKey() {
        return mLookupKey;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getThumbnailUri() {
        return mThumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        mThumbnailUri = thumbnailUri;
    }

    public void addDataItem(String item) {
        mDataItems.add(item);
    }

    public String[] getDataItem() {
        return mDataItems.toArray(new String[mDataItems.size()]);
    }
}
