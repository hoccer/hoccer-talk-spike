package com.hoccer.xo.android.base;

import android.content.*;
import android.os.*;
import android.view.*;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.release.R;

/**
 * Activity keeps track of synchronizing the mediaplay icon
 * according to the mediaplayer state.
 * It can also be used to pause the current media.
 */
public abstract class XoActionbarActivity extends XoActivity {

    private MediaPlayerService mMediaPlayerService;
    private ServiceConnection mMediaPlayerServiceConnection;
    private Menu mMenu;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
        bindMediaPlayerService(intent);
        createMediaPlayerBroadcastReceiver();
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

    private void updatePlayState(){
        if ( mMediaPlayerService != null){
            if (mMediaPlayerService.isPaused() || mMediaPlayerService.isStopped()) {
                mMediaPlayerService.play(true);
            } else {
                mMediaPlayerService.pause();
            }
        }
    }

    private void createMediaPlayerBroadcastReceiver() {
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

    private void updateActionBarIcons( Menu menu){
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

    private void bindMediaPlayerService(Intent intent) {

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
