package com.hoccer.xo.android.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.hoccer.xo.android.fragment.IPagerFragment;


public abstract class PagerFragment extends Fragment implements IPagerFragment {

    private PagerLifecycle mPagerLifecycle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPagerLifecycle = new PagerLifecycle();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPagerLifecycle.onResume(this);
    }

    @Override
    public void onTabSelected() {
        mPagerLifecycle.onTabSelected(this);
    }

    @Override
    public void onTabUnselected() {
        mPagerLifecycle.onTabUnselected(this);
    }
}
