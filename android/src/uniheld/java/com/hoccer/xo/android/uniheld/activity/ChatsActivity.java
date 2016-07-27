package com.hoccer.xo.android.uniheld.activity;

import android.support.v4.app.Fragment;
import com.hoccer.xo.android.activity.ChatsBaseActivity;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.fragment.WebviewFragment;

public class ChatsActivity extends ChatsBaseActivity {

    private Fragment mWebViewFragment;

    @Override
    protected void onResume() {
        super.onResume();
        if (!mViewPagerActivityComponent.contains(mWebViewFragment)) {
            mWebViewFragment = new WebviewFragment();
            mViewPagerActivityComponent.add(mWebViewFragment);
        }
    }
}
