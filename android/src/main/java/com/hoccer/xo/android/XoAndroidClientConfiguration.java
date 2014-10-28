package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.XoDefaultClientConfiguration;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class XoAndroidClientConfiguration extends XoDefaultClientConfiguration {

    private static final Logger LOG = Logger.getLogger(XoAndroidClientConfiguration.class);

    private final SharedPreferences mPreferences;
    private final Properties mProperties;
    private String mAppName;

    public XoAndroidClientConfiguration(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mProperties = new Properties();
        mAppName = context.getString(R.string.app_name);

        try {
            InputStream inputStream = context.getAssets().open("environment.properties");
            mProperties.load(inputStream);
        } catch (IOException e) {
            LOG.error("Failed to load environment.properties file", e);
        }
    }

    // Override generic XoClient configuration settings

    @Override
    public String getServerUri() {
        return mProperties.getProperty("hoccer.talkserver.uri");
    }

    @Override
    public String getUrlScheme() {
        return mProperties.getProperty("hoccer.invitation.uri.scheme") + "://";
    }

    public String getInvitationServerUri() {
        return mProperties.getProperty("hoccer.invitation.server.uri");
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

    // The following configuration settings are specific to the Android app

    public boolean isDevelopmentModeEnabled() {
        return Boolean.parseBoolean(mProperties.getProperty("hoccer.android.enable.development.mode", "false"));
    }

    public boolean isCrashReportingEnabled() {
        return isDevelopmentModeEnabled() || mPreferences.getBoolean("preference_crash_report", false);
    }

    public String getAttachmentsDirectory() {
        return mAppName;
    }

    public String getAvatarsDirectory() {
        return "avatars";
    }

    public String getHockeyAppId() {
        return mProperties.getProperty("hockey.app.id");
    }

    public String getLogLevel() {
        return mProperties.getProperty("hoccer.android.log.level", "INFO");
    }

    public boolean isLoggingToSdEnabled() {
        return Boolean.parseBoolean(mProperties.getProperty("hoccer.android.log.to.sd", "false"));
    }

    public boolean isLoggingToLogcatEnabled() {
        return Boolean.parseBoolean(mProperties.getProperty("hoccer.android.log.to.logcat", "true"));
    }

    public String getAppName() {
        return mAppName;
    }

    public String getCredentialImportPackage() {
        return mProperties.getProperty("hoccer.android.credential.import.package", null);
    }

    public String getCredentialExportPackage() {
        return mProperties.getProperty("hoccer.android.credential.export.package", null);
    }
}
