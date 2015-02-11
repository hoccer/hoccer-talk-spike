package com.hoccer.xo.android.activity.component;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.MediaPlayer;
import com.hoccer.xo.android.activity.FullscreenPlayerActivity;


/**
 * Adds and manages the media icon in the actionbar based on the current MediaPlayer state.
 */
public class MediaPlayerActivityComponent extends ActivityComponent implements MediaPlayer.Listener {

    private Menu mMenu;

    public MediaPlayerActivityComponent(final FragmentActivity activity) {
        super(activity);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaPlayer.get().registerListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MediaPlayer.get().unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean showMenu = super.onCreateOptionsMenu(menu);
        if (showMenu) {
            mMenu = menu;
            updateActionBarIcons();
        } else {
            mMenu = null;
        }

        return showMenu;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_media_player) {
            openFullScreenPlayer();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFullScreenPlayer() {
        final Intent resultIntent = new Intent(getActivity(), FullscreenPlayerActivity.class);
        getActivity().startActivity(resultIntent);
    }

    private void updateActionBarIcons() {
        if (mMenu != null) {
            final MenuItem mediaPlayerItem = mMenu.findItem(R.id.menu_media_player);

            final MediaPlayer mediaPlayer = MediaPlayer.get();
            if (mediaPlayer.isStopped() || mediaPlayer.isPaused()) {
                mediaPlayerItem.setVisible(false);
            } else {
                mediaPlayerItem.setVisible(true);
            }
        }
    }

    @Override
    public void onStateChanged() {
        updateActionBarIcons();
    }

    @Override
    public void onTrackChanged() {}
}
