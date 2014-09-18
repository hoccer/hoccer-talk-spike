package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.QrCodeGeneratorFragment;
import com.hoccer.xo.android.fragment.QrCodeScannerFragment;
import com.hoccer.xo.release.R;

public class QrCodeActivity extends XoActionbarActivity {

    private ViewPager mViewPager;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_qr_code;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();

        final String[] tabs = getResources().getStringArray(R.array.qr_code_tab_names);
        final Fragment[] fragments = new Fragment[]{
                new QrCodeScannerFragment(),
                new QrCodeGeneratorFragment()
        };

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public int getCount() {
                return tabs.length;
            }
        });
        mViewPager.setOnPageChangeListener(new QrCodePageChangeListener());

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (String tabName : tabs) {
            ActionBar.Tab tab = actionBar.newTab()
                    .setText(tabName)
                    .setTabListener(new QrCodeTabListener());
            actionBar.addTab(tab);
        }
    }

    private class QrCodePageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                ActionBar actionBar = getActionBar();

                if (actionBar.getSelectedNavigationIndex() != mViewPager.getCurrentItem()) {
                    getActionBar().setSelectedNavigationItem(mViewPager.getCurrentItem());
                }
            }
        }
    }

    private class QrCodeTabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }
}
