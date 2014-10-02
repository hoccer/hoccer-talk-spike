package com.hoccer.xo.android.activity;

import android.view.Menu;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.release.R;

public class MediaCollectionSelectionActivity extends XoActionbarActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_media_collection_selection;
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
}
