package com.hoccer.xo.android.util;

import java.util.ArrayList;

/**
 * Holds device contact data.
 */
public class DeviceContact {

    private String mLookupKey;

    private String mDisplayName;

    private String mThumbnailUri = "";

    private ArrayList<String> mPhoneNumbers = new ArrayList<String>();

    private ArrayList<String> mEMailAddresses = new ArrayList<String>();

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

    public void addPhoneNumber(String phoneNumber) {
        mPhoneNumbers.add(phoneNumber);
    }

    public String[] getPhoneNumbers() {
        return mPhoneNumbers.toArray(new String[mPhoneNumbers.size()]);
    }

    public void addEMailAddress(String eMailAddress) {
        mEMailAddresses.add(eMailAddress);
    }

    public String[] getEMailAddresses() {
        return mEMailAddresses.toArray(new String[mEMailAddresses.size()]);
    }

}
