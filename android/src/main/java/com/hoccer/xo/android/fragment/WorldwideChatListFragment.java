package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.WorldwideController;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.dialog.WorldWideTutorialDialog;
import com.hoccer.xo.android.view.Placeholder;

import static com.hoccer.talk.model.TalkEnvironment.TYPE_WORLDWIDE;

public class WorldwideChatListFragment extends EnvironmentChatListFragment {

    private static final String DIALOG_TAG = "ww_tutorial";

    public WorldwideChatListFragment() {
        mPlaceholder = new Placeholder(R.drawable.placeholder_world, R.string.placeholder_worldwide_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        createAdapter();
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
    public void onPageResume() {}

    @Override
    public void onPageSelected() {
        WorldwideController.get().activateWorldwide();

        TalkClientContact group = XoApplication.get().getXoClient().getCurrentWorldwideGroup();
        mListAdapter.scheduleUpdate(group);

        displayWorldwideTutorialIfNeeded();
    }

    private void displayWorldwideTutorialIfNeeded() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isTutorialViewed = preferences.getBoolean(WorldWideTutorialDialog.PREFERENCE_KEY_WORLDWIDE_TUTORIAL_VIEWED, false);
        if(!isTutorialViewed) {
            new WorldWideTutorialDialog().show(getActivity().getFragmentManager(), DIALOG_TAG);
        }
    }

    @Override
    public void onPageUnselected() {
        WorldwideController.get().deactivateWorldWide();
    }

    @Override
    public void onPagePause() {}

    @Override
    public void onPageScrollStateChanged(int state) {}
}
