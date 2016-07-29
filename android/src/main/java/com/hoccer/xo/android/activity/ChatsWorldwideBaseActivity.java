package com.hoccer.xo.android.activity;

import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.fragment.WorldwideChatListFragment;

public class ChatsWorldwideBaseActivity extends ChatsBaseActivity {

    private WorldwideChatListFragment mWorldwideChatListFragment;

    @Override
    protected void onResume() {
        super.onResume();

        if (XoApplication.getConfiguration().isWorldwideFeatureEnabled()) {
            if (mWorldwideChatListFragment == null) {
                mWorldwideChatListFragment = new WorldwideChatListFragment();
            }
            if (!mViewPagerActivityComponent.contains(mWorldwideChatListFragment)) {
                mViewPagerActivityComponent.add(mWorldwideChatListFragment);
            }
        } else if (mWorldwideChatListFragment != null && mViewPagerActivityComponent.contains(mWorldwideChatListFragment)) {
                mViewPagerActivityComponent.remove(mWorldwideChatListFragment);
        }
    }

}
