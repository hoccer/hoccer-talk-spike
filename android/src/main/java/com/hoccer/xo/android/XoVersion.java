package com.hoccer.xo.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class XoVersion {

    private static final String UNINITIALIZED = "Uninitialized";

    private static final Logger LOG = Logger.getLogger(XoVersion.class);

    private static Properties sGit;

    private static String sBranch = UNINITIALIZED;

    private static String sBuildTime = UNINITIALIZED;

    private static String sCommitId = UNINITIALIZED;
    private static String sCommitAbbrev = UNINITIALIZED;
    private static String sCommitDescribe = UNINITIALIZED;

    public static void initialize(Context context) {
        LOG.debug("initializing git properties");
        Properties git = new Properties();
        InputStream is = context.getResources().openRawResource(R.raw.git_properties);
        try {
            git.load(is);
            sGit = git;
        } catch (IOException e) {
            LOG.error("error loading git properties", e);
        }
        if (sGit != null) {
            sBranch = sGit.getProperty("git.branch", UNINITIALIZED);
            sBuildTime = sGit.getProperty("git.build.time", UNINITIALIZED);
            sCommitId = sGit.getProperty("git.commit.id", UNINITIALIZED);
            sCommitAbbrev = sGit.getProperty("git.commit.id.abbrev", UNINITIALIZED);
            sCommitDescribe = sGit.getProperty("git.commit.id.describe", UNINITIALIZED);
        }
    }

    public static String getBranch() {
        return sBranch;
    }

    public static String getBuildTime() {
        return sBuildTime;
    }

    public static String getCommitId() {
        return sCommitId;
    }

    public static String getCommitAbbrev() {
        return sCommitAbbrev;
    }

    public static String getCommitDescribe() {
        return sCommitDescribe;
    }

    public static boolean isApplicationUpdated(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;

            SharedPreferences preferences = context.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE);
            int oldVersionCode = preferences.getInt("versionCode", -1);
            if (oldVersionCode == -1 || oldVersionCode < versionCode) {
                preferences.edit().putInt("versionCode", versionCode).apply();
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

}
