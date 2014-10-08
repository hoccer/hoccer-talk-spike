package com.hoccer.xo.android.activity.component;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.hoccer.xo.android.activity.FullscreenPlayerActivity;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;


/**
 * Adds and manages the media icon in the actionbar based on the current MediaPlayerService state.
 */
public class MediaPlayerActivityComponent extends ActivityComponent {

    private Menu mMenu;
    private MediaPlayerServiceConnector mMediaPlayerServiceConnector;

    public MediaPlayerActivityComponent(final FragmentActivity activity) {
        super(activity);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaPlayerServiceConnector = new MediaPlayerServiceConnector(getActivity());
        mMediaPlayerServiceConnector.connect(
                IntentHelper.ACTION_PLAYER_STATE_CHANGED,
                new MediaPlayerServiceConnector.Listener() {
                    @Override
                    public void onConnected(final MediaPlayerService service) {
                        updateActionBarIcons();
                    }

                    @Override
                    public void onDisconnected() {
                    }

                    @Override
                    public void onAction(final String action, final MediaPlayerService service) {
                        updateActionBarIcons();
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayerServiceConnector.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        if (result) {
            mMenu = menu;
            updateActionBarIcons();
        } else {
            mMenu = null;
        }

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if(item.getItemId() == R.id.menu_media_player) {
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
        if (mMediaPlayerServiceConnector != null && mMediaPlayerServiceConnector.isConnected() && mMenu != null) {
            final MenuItem mediaPlayerItem = mMenu.findItem(R.id.menu_media_player);

            final MediaPlayerService service = mMediaPlayerServiceConnector.getService();
            if (service.isStopped() || service.isPaused()) {
                mediaPlayerItem.setVisible(false);
            } else {
                mediaPlayerItem.setVisible(true);
            }
        }
    }
}
