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

    public static synchronized Clipboard getInstance(Context applicationContext) {
        if (INSTANCE == null) {
            INSTANCE = new Clipboard(applicationContext);
        }
        return INSTANCE;
    }

    private Clipboard(Context context) {
        mContext = context;
        initialize();
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

    public boolean hasContent() {
        return (mContent != null);
    }

    public ClipboardContent getContent() {
        return mContent;
    }

    public void setContent(IContentObject contentObject) {
        ClipboardContent cc = ClipboardContent.fromContentObject(contentObject);
        cc.saveToPreferences(sPreferences.edit());
        // mContent will be set by updateContentFromPreferences()
    }

    public void clearClipboard() {
        if (mContent != null) {
            ClipboardContent.clearPreferences(sPreferences.edit());
        }
    }
}
