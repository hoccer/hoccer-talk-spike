package com.hoccer.xo.android.base;

import android.content.*;
import android.os.*;
import android.view.*;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.release.R;

/**
 * Base class for our activities
 * <p/>
 * All our activities inherit from SherlockFragmentActivity
 * to maintain a common look and feel in the whole application.
 * <p/>
 * These activites continually keep the background service which
 * we use for connection retention alive by calling it via RPC.
 */
public abstract class XoActionbarActivity extends XoActivity {

    protected MediaPlayerService mMediaPlayerService;
    protected ServiceConnection mMediaPlayerServiceConnection;
    protected Menu mMenu;
    protected BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
        bindService(intent);
        createBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mMediaPlayerServiceConnection);
        unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        boolean result = super.onCreateOptionsMenu(menu);

        mMenu = menu;
        updateActionBarIcons(menu);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_media_player:
                updatePlayState();
                updateActionBarIcons(mMenu);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void updatePlayState(){
        if ( mMediaPlayerService != null){
            if (mMediaPlayerService.isPaused() || mMediaPlayerService.isStopped()) {
                mMediaPlayerService.play(true);
            } else {
                mMediaPlayerService.pause();
            }
        }
    }

    protected void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MediaPlayerService.PLAYSTATE_CHANGED_ACTION)) {
                    updateActionBarIcons(mMenu);
                }
            }
        };
        IntentFilter filter = new IntentFilter(MediaPlayerService.PLAYSTATE_CHANGED_ACTION);
        registerReceiver(mBroadcastReceiver, filter);
    }

    protected void updateActionBarIcons( Menu menu){
        if ( mMediaPlayerService != null && menu != null) {
            MenuItem mediaPlayerItem = menu.findItem(R.id.menu_media_player);

            if ( mMediaPlayerService.isStopped() || mMediaPlayerService.isPaused()) {
                mediaPlayerItem.setVisible(false);
            }else {
                mediaPlayerItem.setVisible(true);

                //if ( mMediaPlayerService.isPaused()){
                //    mediaPlayerItem.setIcon(R.drawable.ic_dark_play);
                //}else{
                //    mediaPlayerItem.setIcon(R.drawable.ic_dark_pause);
                //}
            }
        }
    }

    protected void bindService(Intent intent) {

        mMediaPlayerServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) service;
                mMediaPlayerService = binder.getService();
                updateActionBarIcons( mMenu);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mMediaPlayerService = null;
            }
        };

        bindService(intent, mMediaPlayerServiceConnection, Context.BIND_AUTO_CREATE);
    }
}
