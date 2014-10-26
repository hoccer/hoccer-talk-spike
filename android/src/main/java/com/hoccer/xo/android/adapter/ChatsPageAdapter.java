package com.hoccer.xo.android.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.hoccer.xo.android.fragment.ChatListFragment;
import com.hoccer.xo.android.fragment.FriendRequestFragment;
import com.hoccer.xo.android.fragment.NearbyChatListFragment;

public class ChatsPageAdapter extends FragmentPagerAdapter {
    private int mCount;
    private ChatListFragment mChatListFragment;
    private FriendRequestFragment mFriendRequestFragment;
    private NearbyChatListFragment mNearbyChatFragment;

    public ChatsPageAdapter(FragmentManager fm, int count) {
        super(fm);
        mCount = count;

        mChatListFragment = new ChatListFragment();
        mFriendRequestFragment = new FriendRequestFragment();
        mNearbyChatFragment = new NearbyChatListFragment();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return mChatListFragment;
            case 1:
                return mFriendRequestFragment;
            case 2:
                return mNearbyChatFragment;
        }
        return null;
    }

    @Override
    public int getCount() {
        return mCount;
    }
}
