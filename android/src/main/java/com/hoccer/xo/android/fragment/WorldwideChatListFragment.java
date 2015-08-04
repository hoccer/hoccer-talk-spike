package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.FeaturePromoter;
import com.hoccer.xo.android.WorldwideController;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.view.Placeholder;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class WorldwideChatListFragment extends EnvironmentChatListFragment {

    private boolean mPageSelected;
    private boolean mOnResumeHandled;

    public WorldwideChatListFragment() {
        mPlaceholder = new Placeholder(R.drawable.placeholder_world, R.string.placeholder_worldwide_text);
    }

    private void activateWorldwide() {
        WorldwideController.get().activateWorldwide();

        TalkClientContact group = XoApplication.get().getXoClient().getCurrentWorldwideGroup();
        createAdapter();
        mListAdapter.scheduleUpdate(group);

        FeaturePromoter.displayWorldwideTutorialOnFirstStart(getActivity());
    }

    @Override
    public void onDestroy() {
        if (mListAdapter != null) {
            mListAdapter.unregisterListeners();
        }

        destroyAdapter();
        super.onDestroy();
    }

    private void createAdapter() {
        if (mListAdapter == null) {
            mListAdapter = new EnvironmentChatListAdapter(TYPE_WORLDWIDE, mActivity);
            mListAdapter.registerListeners();
            setListAdapter(mListAdapter);
        }
    }

    private void destroyAdapter() {
        if (mListAdapter != null) {
            setListAdapter(null);
            mListAdapter.unregisterListeners();
            mListAdapter = null;
        }
    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.worldwide_tab_name);
    }

    @Override
    public void onPageSelected() {
        activateWorldwide();
    }

    @Override
    public void onPageUnselected() {
        WorldwideController.get().deactivateWorldWide();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
