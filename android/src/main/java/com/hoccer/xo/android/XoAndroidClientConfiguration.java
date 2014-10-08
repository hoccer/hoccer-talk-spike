package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.XoDefaultClientConfiguration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class XoAndroidClientConfiguration extends XoDefaultClientConfiguration {

    private static final Logger LOG = Logger.getLogger(XoAndroidClientConfiguration.class);

    private final SharedPreferences mPreferences;
    private final Properties mProperties;


    public XoAndroidClientConfiguration(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mProperties = new Properties();

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
        return "Hoccer XO";
    }

    public String getAvatarsDirectory() {
        return "avatars";
    }

    public String getHockeyAppId() {
        return "60f2a55705e94d33e62a7b1643671f46";
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
}
