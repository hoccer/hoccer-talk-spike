package com.hoccer.xo.android.polling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.hoccer.talk.android.push.TalkPushService;
import com.hoccer.xo.android.service.XoClientService;
import org.apache.log4j.Logger;

/**
 * Created by andreasr on 16.11.15.
 */
public class PollingBroadcastReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logger.getLogger(PollingBroadcastReceiver.class);


    public static void startPolling(Context context){
        LOG.debug("Start polling");
        Intent serviceIntent = new Intent(context, PollingBroadcastReceiver
                .class);
        serviceIntent.putExtra(TalkPushService.EXTRA_WAKE_CLIENT, "Polling server");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 2000, PendingIntent.getBroadcast(context, 0, serviceIntent, 0));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.debug("Polling server");
        Intent serviceIntent = new Intent(context, XoClientService.class);
        serviceIntent.putExtra(TalkPushService.EXTRA_WAKE_CLIENT, "Polling server");
        context.startService(serviceIntent);
    }
}
