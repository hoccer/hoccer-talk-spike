package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.XoDefaultClientConfiguration;
import com.hoccer.xo.release.R;

public class XoAndroidClientConfiguration extends XoDefaultClientConfiguration {
    private Context mContext;
    private SharedPreferences mPreferences;

    public XoAndroidClientConfiguration(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public String getServerUri() {
        String serverUri;

        try {
            if (XoConfiguration.DEVELOPMENT_MODE_ENABLED) {
                serverUri = mPreferences.getString("preference_server_uri", mContext.getResources().getStringArray(R.array.servers_development)[0]);
            } else {
                serverUri = mContext.getResources().getString(R.string.servers_production);
            }
        }
        catch(Exception e){
            serverUri = "server url missing";
        }

        return serverUri;
    }

    @Override
    public int getRSAKeysize() {
        String keySizeString = mPreferences.getString("preference_keysize", "2048");
        Integer keySize = Integer.parseInt(keySizeString);
        return keySize.intValue();
    }

    @Override
    public boolean isSendDeliveryConfirmationEnabled() {
        return mPreferences.getBoolean("preference_confirm_messages_seen", true);
    }

    @Override
    public boolean isSupportModeEnabled() {
        return XoConfiguration.isSupportModeEnabled();
    }

    @Override
    public String getSupportTag() {
        return XoConfiguration.SERVER_SUPPORT_TAG;
    }

}
