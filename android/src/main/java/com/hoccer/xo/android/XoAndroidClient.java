package com.hoccer.xo.android;

import better.jsonrpc.websocket.JsonRpcWsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.IXoClientConfiguration;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;

import org.eclipse.jetty.websocket.WebSocketClient;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.net.URI;

public class XoAndroidClient extends XoClient implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static String sPreferenceUploadLimitKey = "preference_upload_limit";
    private static String sPreferenceDownloadLimitKey = "preference_download_limit";

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoAndroidClient(IXoClientHost client_host, XoAndroidClientConfiguration configuration, XoApplication xoApplication) {
        super(client_host, configuration);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(xoApplication);
        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, sPreferenceUploadLimitKey);
        onSharedPreferenceChanged(preferences, sPreferenceDownloadLimitKey);
    }

    @Override
    protected void createJsonRpcClient(URI uri, WebSocketClient wsClient, ObjectMapper rpcMapper) {
        IXoClientConfiguration configuration = getConfiguration();

        String protocol = configuration.getUseBsonProtocol()
                ? configuration.getBsonProtocolString()
                : configuration.getJsonProtocolString();

        mConnection = new JsonRpcWsClient(uri, protocol, wsClient, rpcMapper, getHost().getIncomingBackgroundExecutor());
        mConnection.setMaxIdleTime(configuration.getConnectionIdleTimeout());
        mConnection.setSendKeepAlives(configuration.getKeepAliveEnabled());

        if(configuration.getUseBsonProtocol()) {
            mConnection.setSendBinaryMessages(true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        loadPreferences(preferences, key);
    }

    private void loadPreferences(SharedPreferences preferences, String key) {
        if(key != null && key.equals(sPreferenceUploadLimitKey)) {
            String uploadLimitString = preferences.getString(key, "-1");
            setUploadLimit(Integer.parseInt(uploadLimitString));
        } else if(key != null && key.equals(sPreferenceDownloadLimitKey)) {
            String downloadLimitString = preferences.getString(key, "-1");
            setDownloadLimit(Integer.parseInt(downloadLimitString));
        }
    }
}
