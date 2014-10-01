package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.ListFragment;
import android.view.View;
import com.hoccer.xo.release.R;

public class GroupListFragment extends ListFragment implements IPagerFragment {

    @Override
    public void onPageSelected() {

    }

    @Override
    public void onPageUnselected() {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.contacts_tab_groups);
    }
}
