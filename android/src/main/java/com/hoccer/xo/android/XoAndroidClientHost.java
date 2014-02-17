package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.XoClientConfiguration;
import com.hoccer.xo.android.database.AndroidTalkDatabase;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

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
            clientLanguage = locale.getISO3Language();
        }
        return clientLanguage;
    }

    @Override
    public String getClientVersion() {
        String clientVersion = null;
        if (mPackageInfo != null) {
            clientVersion = mPackageInfo.versionName;
        }
        return clientVersion;
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

}
