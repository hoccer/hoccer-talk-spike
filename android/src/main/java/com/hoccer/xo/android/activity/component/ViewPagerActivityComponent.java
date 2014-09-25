package com.hoccer.xo.android.activity.component;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.hoccer.xo.android.fragment.IPagerFragment;

/**
 * Adds and manages an activity ViewPager.
 */
public class ViewPagerActivityComponent extends ActivityComponent {

    private ViewPager mViewPager;
    private final Fragment[] mFragments;
    private final int mViewPagerId;
    private final int mTabNamesId;

    public <T extends Fragment & IPagerFragment> ViewPagerActivityComponent(final FragmentActivity activity, final int viewPagerId, final int tabNamesId, final T... fragments) {
        super(activity);

        mFragments = fragments;
        mViewPagerId = viewPagerId;
        mTabNamesId = tabNamesId;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] tabs = getActivity().getResources().getStringArray(mTabNamesId);

        mViewPager = (ViewPager)getActivity().findViewById(mViewPagerId);
        mViewPager.setAdapter(new FragmentPagerAdapter(getActivity().getSupportFragmentManager()) {
            @Override
            public Fragment getItem(final int position) {
                return mFragments[position];
            }

            @Override
            public int getCount() {
                return tabs.length;
            }
        });
        mViewPager.setOnPageChangeListener(new PageChangeListener());

        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (final String tabName : tabs) {
            final ActionBar.Tab tab = actionBar.newTab()
                    .setText(tabName)
                    .setTabListener(new TabListener());
            actionBar.addTab(tab);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((IPagerFragment)mFragments[mViewPager.getCurrentItem()]).onPageSelected();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((IPagerFragment)mFragments[mViewPager.getCurrentItem()]).onPageUnselected();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {}

        @Override
        public void onPageSelected(final int position) {
            final ActionBar actionBar = getActivity().getActionBar();

            if (actionBar.getSelectedNavigationIndex() != mViewPager.getCurrentItem()) {
                getActivity().getActionBar().setSelectedNavigationItem(mViewPager.getCurrentItem());
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            ((IPagerFragment)mFragments[mViewPager.getCurrentItem()]).onPageScrollStateChanged(state);
        }
    }

    private class TabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(final ActionBar.Tab tab, final FragmentTransaction ft) {
            final int position = tab.getPosition();
            mViewPager.setCurrentItem(position);
            ((IPagerFragment)mFragments[position]).onPageSelected();
        }

        @Override
        public void onTabUnselected(final ActionBar.Tab tab, final FragmentTransaction ft) {
            final int position = tab.getPosition();
            ((IPagerFragment)mFragments[position]).onPageUnselected();
        }

        @Override
        public void onTabReselected(final ActionBar.Tab tab, final FragmentTransaction ft) {}
    }
}
