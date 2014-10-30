package com.hoccer.xo.android.credentialtransfer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;

/**
 * Connection state listener which performs an srp secret change when the client is connected if needed.
 */
public class SrpChangeListener implements IXoStateListener {

    private final Context mContext;

    public SrpChangeListener(Context context) {
        mContext = context;
    }

    @Override
    public void onClientStateChange(final XoClient client, final int state) {
        // if we just successfully logged in
        if (state == XoClient.STATE_SYNCING) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            final boolean changeSrpSecret = preferences.getBoolean("change_srp_secret", false);
            if (changeSrpSecret) {
                if(client.changeSrpSecret()) {
                    final SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("change_srp_secret", false);
                    editor.apply();

                    // send disconnect request to import package client
                    CredentialImporter.sendDisconnectRequestToImportPackageClient(mContext);
                }
            }
        }
    }
}
