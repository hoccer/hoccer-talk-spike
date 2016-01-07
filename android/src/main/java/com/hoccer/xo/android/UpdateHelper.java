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
