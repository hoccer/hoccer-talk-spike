package com.hoccer.talk.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.model.IXoTransferState;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentState;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.mime.MimeTypes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class TransferAgent {

    public static final int UNLIMITED = -1;
    public static final int MANUAL = -2;

    protected ScheduledExecutorService mExecutorService;

    protected HttpClient mHttpClient;
    protected final XoClient mClient;

    protected List<TransferListener> mListeners = new ArrayList<TransferListener>();

    public TransferAgent(XoClient client, String executorName) {
        mClient = client;
        mExecutorService = createScheduledThreadPool(executorName);
        mHttpClient = createHttpClient();
        mListeners.add(client);
    }

    protected ScheduledExecutorService createScheduledThreadPool(final String name) {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        threadFactoryBuilder.setNameFormat(name);
        threadFactoryBuilder.setUncaughtExceptionHandler(mClient.getHost().getUncaughtExceptionHandler());
        return Executors.newScheduledThreadPool(mClient.getConfiguration().getTransferThreads(), threadFactoryBuilder.build());
    }

    private HttpClient createHttpClient() {
        // FYI: http://stackoverflow.com/questions/12451687/http-requests-with-httpclient-too-slow
        // && http://stackoverflow.com/questions/3046424/http-post-requests-using-httpclient-take-2-seconds-why
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        return new HttpClientWithKeyStore(httpParams);
    }

    public void registerListener(TransferListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(TransferListener listener) {
        mListeners.remove(listener);
    }

    public HttpClient getHttpClient() {
        return mHttpClient;
    }

    public void resetClient(){
        mHttpClient = createHttpClient();
    }

    public XoClient getXoClient() {
        return mClient;
    }
}
