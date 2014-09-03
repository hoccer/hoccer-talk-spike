package com.hoccer.xo.android;

import better.jsonrpc.websocket.JsonRpcWsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.client.IXoClientConfiguration;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClient;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocketClient;

import java.net.URI;

public class XoAndroidClient extends XoClient {

    static final Logger LOG = Logger.getLogger(XoAndroidClient.class);

    static final int DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT = -1;
    static final int DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY = 100;

    private int mImageUploadMaxPixelCount = DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT;

    private int mImageUploadEncodingQuality = DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY;

    /**
     * Create a Hoccer Talk client using the given client database
     */
    public XoAndroidClient(IXoClientHost client_host, XoAndroidClientConfiguration configuration) {
        super(client_host, configuration);

    }

    public boolean isEncodingNecessary() {
        return mImageUploadMaxPixelCount != DEFAULT_IMAGE_UPLOAD_MAX_PIXEL_COUNT
                || mImageUploadEncodingQuality != DEFAULT_IMAGE_UPLOAD_ENCODING_QUALITY;
    }

    public int getImageUploadEncodingQuality() {
        return mImageUploadEncodingQuality;
    }

    public int getImageUploadMaxPixelCount() {
        return mImageUploadMaxPixelCount;
    }

    public void setImageUploadEncodingQuality(int imageUploadEncodingQuality) {
        mImageUploadEncodingQuality = imageUploadEncodingQuality;
        LOG.info("Image max pixel count set to " + mImageUploadMaxPixelCount);
    }

    public void setImageUploadMaxPixelCount(int imageUploadMaxPixelCount) {
        mImageUploadMaxPixelCount = imageUploadMaxPixelCount;
        LOG.info("Image encoding quality set to " + mImageUploadEncodingQuality);
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

}
