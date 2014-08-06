package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.MediaCollectionSelectionListFragment;
import com.hoccer.xo.release.R;

public class MediaCollectionSelectionActivity extends XoActionbarActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.default_framelayout;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            menu.findItem(R.id.menu_my_profile).setVisible(false);
            menu.findItem(R.id.menu_settings).setVisible(false);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMediaCollectionSelectionListFragment();
    }

    private void showMediaCollectionSelectionListFragment() {
        MediaCollectionSelectionListFragment mediaCollectionSelectionListFragment = new MediaCollectionSelectionListFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, mediaCollectionSelectionListFragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}
