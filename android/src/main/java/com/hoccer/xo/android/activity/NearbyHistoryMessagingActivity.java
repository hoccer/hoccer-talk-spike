package com.hoccer.xo.android.activity;

import com.hoccer.xo.android.adapter.NearbyChatAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ThumbnailManager;
import com.hoccer.xo.release.R;

import android.widget.ListView;

public class NearbyHistoryMessagingActivity extends XoActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_nearby_history_messaging;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListView listView = (ListView) findViewById(R.id.lv_nearby_history_chat);
        listView.setAdapter(new NearbyChatAdapter(listView, this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ThumbnailManager.getInstance(this).clearCache();
    }
}

