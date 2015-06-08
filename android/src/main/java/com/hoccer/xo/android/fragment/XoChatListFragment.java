package com.hoccer.xo.android.fragment;

import com.hoccer.xo.android.activity.ChatActivity;
import com.hoccer.xo.android.base.XoListFragment;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class XoChatListFragment extends XoListFragment {

    private static final String KEY_SCROLL_POSITION = "scroll_position";

    @Override
    public void onResume() {
        super.onResume();
        applySavedScrollPosition();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();
    }

    private void saveScrollPosition() {
        SharedPreferences preferences = getActivity().getSharedPreferences(ChatActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(KEY_SCROLL_POSITION + getScrollPositionId(), getListView().getFirstVisiblePosition());
        edit.commit();
    }

    private void applySavedScrollPosition() {
        SharedPreferences preferences = getActivity().getSharedPreferences(ChatActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        int scrollPosition = preferences.getInt(KEY_SCROLL_POSITION + getScrollPositionId(), -1);
        if(scrollPosition >= 0) {
            getListView().setSelection(scrollPosition);
        }
    }

    protected abstract String getScrollPositionId();


}
