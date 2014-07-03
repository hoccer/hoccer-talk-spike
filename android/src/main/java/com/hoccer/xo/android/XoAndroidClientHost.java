package com.hoccer.xo.android;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.TypedValue;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.xo.android.database.AndroidTalkDatabase;
import com.hoccer.xo.release.R;
import com.sun.tools.javac.util.Log;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Android-specific implementation of an XO client host
 *
 * This directs the client towards the android-specific ormlite backend,
 * binds it to the right WS socket factory for SSL security
 * and allows the client to read files from content providers.
 */
public class XoAndroidClientHost implements IXoClientHost {

    private static final String SYSTEM_NAME = "Android";

    Context mContext = null;
    PackageInfo mPackageInfo = null;
    boolean mSendDeliveryConfirmationEnabled = true;
    SharedPreferences mPreferences;

    public XoAndroidClientHost(Context context) {
        mContext = context;
        try {
            PackageManager packageManager = mContext.getPackageManager();
            if (packageManager != null) {
                mPackageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public ScheduledExecutorService getBackgroundExecutor() {
        return XoApplication.getExecutor();
    }

    @Override
    public ScheduledExecutorService getIncomingBackgroundExecutor() {
        return XoApplication.getIncomingExecutor();
    }

    @Override
    public IXoClientDatabaseBackend getDatabaseBackend() {
        return AndroidTalkDatabase.getInstance(mContext);
    }

    @Override
    public WebSocketClientFactory getWebSocketFactory() {
        return XoSsl.getWebSocketClientFactory();
    }

    @Override
    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return XoApplication.getUncaughtExceptionHandler();
    }

    @Override
    public InputStream openInputStreamForUrl(String url) throws IOException {
        return mContext.getContentResolver().openInputStream(Uri.parse(url));
    }

    @Override
    public boolean isSupportModeEnabled() {
        return XoConfiguration.isSupportModeEnabled();
    }

    @Override
    public String getSupportTag() {
        return XoConfiguration.SERVER_SUPPORT_TAG;
    }

    @Override
    public boolean getUseBsonProtocol() {
        return mContext.getResources().getBoolean(R.bool.use_bson_protocol);
    }

    @Override
    public String getBsonProtocolString() {
        return mContext.getResources().getString(R.string.protocol_bson);
    }

    @Override
    public String getJsonProtocolString() {
        return  mContext.getResources().getString(R.string.protocol_json);
    }

    @Override
    public int getTransferThreads() {
        return  mContext.getResources().getInteger(R.integer.transfer_threads);
    }

    @Override
    public int getConnectTimeout() {
        return mContext.getResources().getInteger(R.integer.connect_timeout);
    }

    @Override
    public int getIdleTimeout() {
        return mContext.getResources().getInteger(R.integer.idle_timeout);
    }

    @Override
    public boolean getKeepAliveEnabled() {
        return mContext.getResources().getBoolean(R.bool.keep_alive_enabled);
    }

    @Override
    public int getKeepAliveInterval() {
        return mContext.getResources().getInteger(R.integer.keep_alive_interval);
    }

    @Override
    public int getConnectionIdleTimeout() {
        return mContext.getResources().getInteger(R.integer.connection_idle_timeout);
    }

    @Override
    public float getReconnectBackoffFixedDelay() {
        TypedValue outValue = new TypedValue();
        mContext.getResources().getValue(R.dimen.reconnect_backoff_fixed_delay, outValue, true);
        return outValue.getFloat();
    }

    @Override
    public float getReconnectBackoffVariableFactor() {
        TypedValue outValue = new TypedValue();
        mContext.getResources().getValue(R.dimen.reconnect_backoff_variable_factor, outValue, true);
        return outValue.getFloat();
    }

    @Override
    public float getReconnectBackoffVariableMaximum() {
        TypedValue outValue = new TypedValue();
        mContext.getResources().getValue(R.dimen.reconnect_backoff_variable_maximum, outValue, true);
        return outValue.getFloat();
    }

    @Override
    public String getUrlScheme() {
        return mContext.getResources().getString(R.string.url_scheme);
    }

    @Override
    public String getClientName() {
        String clientName = null;
        if (mPackageInfo != null) {
            clientName = mPackageInfo.packageName;
        }
        return clientName;
    }

    @Override
    public String getClientLanguage() {
        String clientLanguage = null;
        Locale locale = mContext.getResources().getConfiguration().locale;
        if (locale != null) {
            clientLanguage = locale.getLanguage();
        }
        return clientLanguage;
    }

    @Override
    public String getClientVersionName() {
        String clientVersion = null;
        if (mPackageInfo != null) {
            clientVersion = mPackageInfo.versionName;
        }
        return clientVersion;
    }

    @Override
    public int getClientVersionCode() {
        int clientVersionCode = 0;
        if (mPackageInfo != null) {
            clientVersionCode = mPackageInfo.versionCode;
        }
        return clientVersionCode;
    }

    @Override
    public String getClientBuildVariant() {
        return "release";
    }

    @Override
    public Date getClientTime() {
        return new Date();
    }

    @Override
    public String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    @Override
    public String getSystemName() {
        return SYSTEM_NAME;
    }

    @Override
    public String getSystemLanguage() {
        String systemLanguage = null;
        Locale locale = mContext.getResources().getConfiguration().locale;
        if (locale != null) {
            systemLanguage = locale.getISO3Language();
        }
        return systemLanguage;
    }

    @Override
    public String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    @Override
    public String getServerUri() {
        String serverUri;
        try {
            if (XoConfiguration.DEVELOPMENT_MODE_ENABLED) {
                serverUri = PreferenceManager.getDefaultSharedPreferences(mContext).getString("preference_server_uri", "");
            } else {
                serverUri = mContext.getResources().getStringArray(R.array.servers_production)[0];
            }
        }
        catch(Exception e){
            serverUri = "server url missing";
        }
        return serverUri;
    }

    @Override
    public int getRSAKeysize() {
        String keySizeString = PreferenceManager.getDefaultSharedPreferences(mContext).getString("preference_keysize", "2048");
        Integer keySize = Integer.parseInt(keySizeString);
        return keySize.intValue();
    }

    @Override
    public boolean isSendDeliveryConfirmationEnabled() {
        mSendDeliveryConfirmationEnabled = mPreferences.getBoolean("preference_confirm_messages_seen", true);
        return mSendDeliveryConfirmationEnabled;
    }
}
