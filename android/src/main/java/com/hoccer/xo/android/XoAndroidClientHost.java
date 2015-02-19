package com.hoccer.xo.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.xo.android.database.AndroidTalkDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Android-specific implementation of an XO client host
 * This directs the client towards the android-specific ormlite backend,
 * and allows the client to read files from content providers.
 */
public class XoAndroidClientHost implements IXoClientHost {

    private static final String SYSTEM_NAME = "Android";

    Context mContext;
    PackageInfo mPackageInfo;

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
        return XoApplication.get().getExecutor();
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
    public KeyStore getKeyStore() {
        return XoSsl.getKeyStore();
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
            systemLanguage = locale.getLanguage();
        }
        return systemLanguage;
    }

    @Override
    public String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

}
