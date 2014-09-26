package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.fragment.AttachmentListFragment;
import com.hoccer.xo.android.fragment.MediaCollectionListFragment;
import com.hoccer.xo.release.R;

public class MediaBrowserActivity extends ComposableActivity {

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[] { new MediaPlayerActivityComponent(this) };
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.default_framelayout;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableUpNavigation();

        showAudioAttachmentListFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actitvity_media_browser, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return true;
                }
                break;
            case R.id.menu_collections:
                showCollectionListFragment();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAudioAttachmentListFragment() {
        AttachmentListFragment attachmentListFragment = new AttachmentListFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, attachmentListFragment);
        ft.commit();
    }

    private void showCollectionListFragment() {
        MediaCollectionListFragment mediaCollectionListFragment = new MediaCollectionListFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, mediaCollectionListFragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}