package com.hoccer.xo.android.polling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.artcom.hoccer.R;
import com.hoccer.talk.android.push.TalkPushService;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.service.XoClientService;
import org.apache.log4j.Logger;

/**
 * Created by andreasr on 16.11.15.
 */
public class PollingBroadcastReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logger.getLogger(PollingBroadcastReceiver.class);
    private static XoApplication xoApplication;
    private static Intent intent;

    public static void startPolling(XoApplication xoApplication){
        PollingBroadcastReceiver.xoApplication = xoApplication;
        if (intent == null) {
            intent = new Intent(xoApplication.getApplicationContext(), PollingBroadcastReceiver.class);
        }
        stopPolling(xoApplication);
        SharedPreferences preferences = xoApplication.getApplicationContext().getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE);
        int interval = preferences.getInt(xoApplication.getApplicationContext().getString(R.string.preference_key_polling_interval),15*60*1000);
        LOG.debug("Start polling with interval "+interval);
        AlarmManager alarmManager = (AlarmManager) xoApplication.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, PendingIntent.getBroadcast(xoApplication.getApplicationContext(), 0, intent, 0));
    }

    public static void stopPolling(XoApplication xoApplication){
        PollingBroadcastReceiver.xoApplication = xoApplication;
        if (intent == null) {
            intent = new Intent(xoApplication.getApplicationContext(), PollingBroadcastReceiver.class);
        }
        LOG.debug("Stop polling");
        AlarmManager alarmManager = (AlarmManager) xoApplication.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getBroadcast(xoApplication.getApplicationContext(), 0, intent, 0));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.debug("Polling server "+xoApplication.getClient().getState());
        if (xoApplication.getClient().isDisconnected()) {
            LOG.debug("Sending Wakeup");
            Intent serviceIntent = new Intent(context, XoClientService.class);
            serviceIntent.putExtra(TalkPushService.EXTRA_WAKE_CLIENT, "Polling server");
            context.startService(serviceIntent);
        } else {
            LOG.debug("App is connected. No polling.");
        }
    }
}
