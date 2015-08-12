package com.hoccer.xo.android.fragment;

import android.support.v4.app.ListFragment;
import com.hoccer.xo.android.activity.ChatActivity;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class XoChatListFragment extends ListFragment {

    private static final String KEY_SCROLL_POSITION = "scroll_position:";
    private static final String KEY_LIST_SIZE = "list_size:";

    protected void saveScrollPosition() {
        if(getListView().getLastVisiblePosition() == (getListView().getCount() - 1)) {
            removeScrollPositionFromPreferences();
        } else {
            saveScrollPositionToPreferences();
        }
    }

    protected void applySavedScrollPosition() {
        SharedPreferences preferences = getActivity().getSharedPreferences(ChatActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        int listSize = preferences.getInt(KEY_LIST_SIZE + getScrollPositionId(), -1);
        if(listSize == -1 || listSize != getListView().getCount()) {
            removeScrollPositionFromPreferences();
        } else {
            int scrollPosition = preferences.getInt(KEY_SCROLL_POSITION + getScrollPositionId(), -1);
            if (scrollPosition >= 0) {
                getListView().setSelection(scrollPosition);
            }
        }
    }

    private void saveScrollPositionToPreferences() {
        SharedPreferences preferences = getActivity().getSharedPreferences(ChatActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(KEY_SCROLL_POSITION + getScrollPositionId(), getListView().getFirstVisiblePosition());
        edit.putInt(KEY_LIST_SIZE + getScrollPositionId(), getListView().getCount());
        edit.commit();
    }

    private void removeScrollPositionFromPreferences() {
        SharedPreferences preferences = getActivity().getSharedPreferences(ChatActivity.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.remove(KEY_SCROLL_POSITION + getScrollPositionId());
        edit.remove(KEY_LIST_SIZE + getScrollPositionId());
        edit.commit();
    }

    protected abstract String getScrollPositionId();


}
