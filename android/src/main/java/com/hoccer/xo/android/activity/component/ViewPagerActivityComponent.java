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
import org.apache.log4j.Logger;

/**
 * Adds and manages an activity ViewPager.
 */
public class ViewPagerActivityComponent extends ActivityComponent {

    private ViewPager mViewPager;
    private final Fragment[] mFragments;
    private final int mViewPagerId;

    private static final Logger LOG = Logger.getLogger(ViewPagerActivityComponent.class);

    public <T extends Fragment & IPagerFragment> ViewPagerActivityComponent(final FragmentActivity activity, final int viewPagerId, final T... fragments) {
        super(activity);

        mFragments = fragments;
        mViewPagerId = viewPagerId;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create view pager
        mViewPager = (ViewPager)getActivity().findViewById(mViewPagerId);
        mViewPager.setAdapter(new FragmentPagerAdapter(getActivity().getSupportFragmentManager()) {
            @Override
            public Fragment getItem(final int position) {
                return mFragments[position];
            }

            @Override
            public int getCount() {
                return mFragments.length;
            }
        });
        mViewPager.setOnPageChangeListener(new PageChangeListener());

        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for(final Fragment fragment : mFragments) {
            final ActionBar.Tab tab = actionBar.newTab();

            final View tabView = ((IPagerFragment)fragment).getCustomTabView(getActivity());
            if(tabView != null) {
                tab.setCustomView(tabView);
            } else {
                tab.setText(((IPagerFragment)fragment).getTabName(getActivity().getResources()));
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

    public Fragment getSelectedFragment() {
        return mFragments[mViewPager.getCurrentItem()];
    }

    /*
     * Returns the first fragment of the given type or null.
     */
    @SuppressWarnings("unchecked")
    public <T extends Fragment> T getFragment(final Class<T> clazz) {
        for(final Fragment fragment : mFragments) {
            if(clazz.equals(fragment.getClass())) {
                return (T)fragment;
            }
        }

        return null;
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
