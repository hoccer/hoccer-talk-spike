package com.hoccer.xo.android.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.talk.content.IContentObject;

public class Clipboard {

    private static Clipboard INSTANCE = null;

    private static SharedPreferences sPreferences;
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferencesListener;

    private Context mContext;
    private ClipboardContent mContent;

    private Clipboard(Context context) {
        mContext = context;
        initialize();
    }

    public static synchronized Clipboard getInstance(Context applicationContext) {
        if (INSTANCE == null) {
            INSTANCE = new Clipboard(applicationContext);
        }
        return INSTANCE;
    }

    public ClipboardContent getContent() {
        return mContent;
    }

    public void storeAttachment(IContentObject contentObject) {
        ClipboardContent cc = ClipboardContent.fromContentObject(contentObject);
        cc.saveToPreferences(sPreferences.edit());
        // mContent will be set by updateContentFromPreferences()
    }

    private void initialize() {
        sPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        sPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.startsWith(ClipboardContent.PREFERENCE_KEY_PREFIX)) {
                    updateContentFromPreferences();
                }
            }
        };
        sPreferences.registerOnSharedPreferenceChangeListener(sPreferencesListener);

        updateContentFromPreferences();
    }

    private void updateContentFromPreferences() {
        mContent = ClipboardContent.fromPreferences(sPreferences);
    }

    public void clearClipboard() {
        if (mContent != null) {
            ClipboardContent.clearPreferences(sPreferences.edit());
        }
    }

    public boolean hasContent() {
        return (mContent != null);
    }
}
