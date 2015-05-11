package com.hoccer.xo.android.activity;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.EnvironmentChatListAdapter;
import com.hoccer.xo.android.fragment.EnvironmentChatListFragment;
import com.hoccer.xo.android.view.Placeholder;

import java.util.Date;

import static com.hoccer.talk.model.TalkEnvironment.*;

public class WorldwideChatListFragment extends EnvironmentChatListFragment {

    private EnvironmentChatListAdapter mListAdapter;

    public WorldwideChatListFragment() {
        // TODO: use placeholder resources for worldwide
        mPlaceholder = new Placeholder(
                R.drawable.placeholder_nearby,
                R.drawable.placeholder_nearby_point,
                R.string.placeholder_nearby_text);
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
    public void onPageResume() {
        TalkEnvironment environment = createWorldwideEnvironment();
        if (environment.isValid()) {
            XoApplication.get().getXoClient().sendEnvironmentUpdate(environment);
        }
    }

    private TalkEnvironment createWorldwideEnvironment() {
        TalkEnvironment environment = new TalkEnvironment();
        environment.setType(TYPE_WORLDWIDE);
        environment.setTimestamp(new Date());
//        environment.setTag("*"); TODO: which tag?
        return environment;
    }

    @Override
    public void onPageSelected() {}

    @Override
    public void onPageUnselected() {
        XoApplication.get().getXoClient().sendDestroyEnvironment(TalkEnvironment.TYPE_WORLDWIDE);
    }

    @Override
    public void onPagePause() {}

    @Override
    public void onPageScrollStateChanged(int state) {}
}
