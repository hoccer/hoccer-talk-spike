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

public class UpdateHelper {

    private boolean isUpdated;
    private boolean isFreshInstall;

    private static UpdateHelper instance;

    public static UpdateHelper getInstance(Context context) {
        if (instance == null) {
            instance = new UpdateHelper(context);
        }
        return instance;
    }

    private UpdateHelper(Context context) {
        checkForUpdate(context);
    }

    private boolean checkForUpdate(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            SharedPreferences preferences = context.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE);
            int oldVersionCode = preferences.getInt("versionCode", -1);

            int versionCode = packageInfo.versionCode;
            if (oldVersionCode < versionCode) {
                if (oldVersionCode == -1) {
                    isFreshInstall = true;
                } else {
                    isUpdated = true;
                }
                preferences.edit().putInt("versionCode", versionCode).apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isApplicationUpdated() {
        return isUpdated;
    }

    public boolean isFreshInstall() {
        return isFreshInstall;
    }
}
