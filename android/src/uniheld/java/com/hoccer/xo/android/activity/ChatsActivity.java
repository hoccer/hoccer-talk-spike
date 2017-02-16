package com.hoccer.xo.android.activity;

import android.os.Bundle;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.fragment.WebviewPagerFragment;

public class ChatsActivity extends ChatsBaseActivity {

    private WebviewPagerFragment mWebViewFragment;

    @Override
    protected void onResume() {
        super.onResume();
        if (!mViewPagerActivityComponent.contains(mWebViewFragment)) {
            mWebViewFragment = new WebviewPagerFragment();

            Bundle bundle = new Bundle();
            bundle.putString("url", getString(R.string.link_benefits));
            bundle.putString("tabName", getResources().getString(R.string.benefits_tab_name));
            mWebViewFragment.setArguments(bundle);

            mViewPagerActivityComponent.add(mWebViewFragment);
            mViewPagerActivityComponent.setCurrentFragment(mWebViewFragment);
        }
    }

    @Override
    public void onBackPressed() {
        if (mViewPagerActivityComponent.getCurrentFragment() == mWebViewFragment && mWebViewFragment.getWebView().canGoBack()) {
            mWebViewFragment.getWebView().goBack();
        } else {
            super.onBackPressed();
        }
    }
}
