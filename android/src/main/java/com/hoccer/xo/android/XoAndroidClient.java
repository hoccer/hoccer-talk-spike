package com.hoccer.xo.android;

import android.os.AsyncTask;
import better.jsonrpc.websocket.JsonRpcWsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.IXoClientConfiguration;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;

import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.util.ImageContentHelper;
import org.eclipse.jetty.websocket.WebSocketClient;

import android.content.SharedPreferences;

import java.net.URI;

public class XoAndroidClient extends XoClient {

    static final String PREF_KEY_IMAGE_ENCODING_SIZE = "preference_image_encoding_size";
    static final String PREF_KEY_IMAGE_ENCODING_QUALITY = "preference_image_encoding_quality";
    static final int DEFAULT_IMAGE_ENCODING_SIZE = -1;
    static final int DEFAULT_IMAGE_ENCODING_QUALITY = 100;

    private int mImageEncodingSize = DEFAULT_IMAGE_ENCODING_SIZE;
    private int mImageEncodingQuality = DEFAULT_IMAGE_ENCODING_QUALITY;

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PREF_KEY_IMAGE_ENCODING_SIZE)) {
                mImageEncodingSize = sharedPreferences.getInt(PREF_KEY_IMAGE_ENCODING_SIZE, DEFAULT_IMAGE_ENCODING_SIZE);
            } else if (key.equals(PREF_KEY_IMAGE_ENCODING_QUALITY)) {
                mImageEncodingQuality = sharedPreferences.getInt(PREF_KEY_IMAGE_ENCODING_QUALITY,
                        DEFAULT_IMAGE_ENCODING_QUALITY);
            }
        }
    };

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoAndroidClient(IXoClientHost client_host, XoAndroidClientConfiguration configuration) {
        super(client_host, configuration);

        SharedPreferences prefs = configuration.getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
        mImageEncodingSize = prefs.getInt(PREF_KEY_IMAGE_ENCODING_SIZE, DEFAULT_IMAGE_ENCODING_SIZE);
        mImageEncodingQuality = prefs.getInt(PREF_KEY_IMAGE_ENCODING_QUALITY, DEFAULT_IMAGE_ENCODING_QUALITY);
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
    protected void requestDelivery(TalkClientMessage message) {
        // check preferences if image attachments have to be re-encoded
        boolean shallEncode = mImageEncodingSize != DEFAULT_IMAGE_ENCODING_SIZE
                || mImageEncodingQuality != DEFAULT_IMAGE_ENCODING_QUALITY;
        // check for image attachment in message
        boolean messageContainsImageAttachment = message.getAttachmentUpload() != null && message.getAttachmentUpload()
                .getContentMediaType().equals(ContentMediaType.IMAGE);

        if (shallEncode && messageContainsImageAttachment) {
            // encodeBitmap from ImageContentHelper

        } else {
            super.requestDelivery(message);
        }
    }
}
