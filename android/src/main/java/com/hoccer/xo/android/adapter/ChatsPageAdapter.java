package com.hoccer.xo.android.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.hoccer.xo.android.fragment.ChatListFragment;
import com.hoccer.xo.android.fragment.NearbyChatListFragment;

public class ChatsPageAdapter extends FragmentPagerAdapter {
    private ChatListFragment mChatListFragment;
    private NearbyChatListFragment mNearbyChatFragment;

    public ChatsPageAdapter(FragmentManager fm) {
        super(fm);

        mChatListFragment = new ChatListFragment();
        mNearbyChatFragment = new NearbyChatListFragment();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return mChatListFragment;
            case 1:
                return mNearbyChatFragment;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
