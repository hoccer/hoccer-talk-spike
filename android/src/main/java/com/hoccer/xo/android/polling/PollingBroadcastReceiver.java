package com.hoccer.xo.android.polling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.hoccer.talk.android.push.TalkPushService;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.service.XoClientService;
import org.apache.log4j.Logger;

public class PollingBroadcastReceiver extends BroadcastReceiver {

    private static final Logger LOG = Logger.getLogger(PollingBroadcastReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (XoApplication.get().getClient().isDisconnected()) {
            LOG.debug("Sending wakeup to start syncing");
            Intent serviceIntent = new Intent(context, XoClientService.class);
            serviceIntent.putExtra(TalkPushService.EXTRA_WAKE_CLIENT, "Polling talkserver");
            context.startService(serviceIntent);
        }
    }
}
