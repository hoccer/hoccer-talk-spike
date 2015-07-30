package com.hoccer.xo.android.activity.component;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import com.hoccer.xo.android.fragment.IPagerFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds and manages an activity ViewPager.
 */
public class ViewPagerActivityComponent extends ActivityComponent {

    private ViewPager mViewPager;
    private final List<Fragment> mFragments = new ArrayList<Fragment>();
    private final int mViewPagerId;
    private boolean mResumeAfterPause;

    public ViewPagerActivityComponent(FragmentActivity activity, int viewPagerId, Fragment... fragments) {
        super(activity);

        for (Fragment fragment : fragments) {
            mFragments.add(fragment);
        }
        mViewPagerId = viewPagerId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create view pager
        mViewPager = (ViewPager) getActivity().findViewById(mViewPagerId);
        mViewPager.setAdapter(new FragmentPagerAdapter(getActivity().getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }
        });
        mViewPager.setOnPageChangeListener(new PageChangeListener());

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for (Fragment fragment : mFragments) {
            ActionBar.Tab tab = actionBar.newTab();

            View tabView = ((IPagerFragment) fragment).getCustomTabView(getActivity());
            if (tabView != null) {
                tab.setCustomView(tabView);
            } else {
                tab.setText(((IPagerFragment) fragment).getTabName(getActivity().getResources()));
            }
            tab.setTabListener(new TabListener());
            actionBar.addTab(tab);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        // do not call onPageResume() on Activity start because this is already done in TabListener.onTabSelected()
        if (mResumeAfterPause) {
            ((IPagerFragment) mFragments.get(mViewPager.getCurrentItem())).onPageResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((IPagerFragment) mFragments.get(mViewPager.getCurrentItem())).onPagePause();
        mResumeAfterPause = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public Fragment getSelectedFragment() {
        return mFragments.get(mViewPager.getCurrentItem());
    }

    public void selectFragment(Fragment fragment) {
        mViewPager.setCurrentItem(mFragments.indexOf(fragment));
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            ActionBar actionBar = getActivity().getActionBar();

            if (actionBar.getSelectedNavigationIndex() != mViewPager.getCurrentItem()) {
                getActivity().getActionBar().setSelectedNavigationItem(mViewPager.getCurrentItem());
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            ((IPagerFragment) mFragments.get(mViewPager.getCurrentItem())).onPageScrollStateChanged(state);
        }
    }

    private class TabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();
            mViewPager.setCurrentItem(position);

            IPagerFragment fragment = (IPagerFragment) mFragments.get(position);
            fragment.onPageResume();
            fragment.onPageSelected();
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();

            IPagerFragment fragment = (IPagerFragment) mFragments.get(position);
            fragment.onPageUnselected();
            fragment.onPagePause();
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
