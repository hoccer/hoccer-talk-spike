package com.hoccer.xo.android.polling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

public class Polling  {

    private static final Logger LOG = Logger.getLogger(PollingBroadcastReceiver.class);

    public static void update(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.preference_key_enable_polling), false)){
            start(context);
        } else {
            stop(context);
        }
    }

    private static void start(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String intervalString = preferences.getString(context.getString(R.string.preference_key_polling_interval), "900000");
        int interval = Integer.parseInt(intervalString);
        LOG.debug("Start polling with interval " + interval/(1000*60)+"min");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, PollingBroadcastReceiver.class), 0);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }

    private static void stop(Context context){
        LOG.debug("Stop polling");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, new Intent(context, PollingBroadcastReceiver.class), 0));
    }
}
