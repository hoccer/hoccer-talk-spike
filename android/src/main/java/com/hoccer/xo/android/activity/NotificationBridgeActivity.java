package com.hoccer.xo.android.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationBridgeActivity extends Activity{

    public static final String ACTION_FULLSCREEN_PLAYER_ACTIVITY_TO_TOP = "com.hoccer.xo.android.action.ACTION_FULLSCREEN_PLAYER_TO_TOP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ACTION_FULLSCREEN_PLAYER_ACTIVITY_TO_TOP.equals(getIntent().getAction())) {
            Intent intent = new Intent(this, FullscreenPlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);

            finish();
        }
    }
}
